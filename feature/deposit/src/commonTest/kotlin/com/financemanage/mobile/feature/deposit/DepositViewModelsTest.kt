package com.financemanage.mobile.feature.deposit

import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.feature.deposit.data.DepositRepository
import com.financemanage.mobile.feature.deposit.presentation.DepositDetailViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositFormViewModel
import com.financemanage.mobile.feature.deposit.presentation.DepositListViewModel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun depositJson(
    funding: String? = null,
    payout: String? = null,
    status: String = "ACTIVE",
) = """{"id":"d1","institutionName":"VCB","type":"TERM","principal":100000000,"currency":"VND","annualRate":6,
    "termMonths":6,"openDate":"2026-11-01","maturityDate":"2027-05-01","status":"$status",
    "expectedInterestAtMaturity":2975342,"fundingWalletId":"w1",
    "fundingStatus":${funding?.let { "\"$it\"" } ?: "null"},"payoutStatus":${payout?.let { "\"$it\"" } ?: "null"}}"""

private const val QUOTE =
    """{"date":"2027-01-01","principal":100000000,"interest":100000,"total":100100000,"early":true,
    "interestLostByEarlyWithdrawal":900000,"currency":"VND"}"""

class DepositViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val repository = DepositRepository(backend.client, cache, PendingPoller())
    private val wallets = WalletRepository(backend.client, cache)
    private val profiles = ProfileRepository(backend.client, cache)

    @Test
    fun listIsCached() =
        runViewModelTest {
            backend.get("deposits", "[${depositJson()}]")
            val viewModel = DepositListViewModel(repository)
            viewModel.refresh()
            val deposit =
                viewModel.state
                    .awaitValue { it.data != null }
                    .data!!
                    .single()
            assertEquals("2975342", deposit.money(deposit.expectedInterestAtMaturity).amount.toStringExpanded())
        }

    @Test
    fun formPreviewsWhileTypingThenCreatesAndWaitsForTheFunding() =
        runViewModelTest {
            backend.post(
                "deposits/preview",
                """{"maturityDate":"2027-05-01","expectedInterest":2975342,
                "totalAtMaturity":102975342,"currency":"VND"}""",
            )
            backend.post("deposits", depositJson(funding = "PENDING"), HttpStatusCode.Created)
            backend.get("deposits", "[${depositJson()}]")
            backend.get("deposits/d1", depositJson(funding = "RECORDED"))
            val viewModel = DepositFormViewModel(repository, wallets, profiles)
            viewModel.state.awaitValue { it.openDate != null }

            viewModel.save()
            assertTrue(viewModel.state.value.invalid)

            viewModel.update { it.copy(institution = "VCB", principal = "100.000.000", annualRate = "6") }
            viewModel.update { it.copy(termMonths = "6", fromWalletId = "w1", openDate = LocalDate(2026, 11, 1)) }
            val previewed = viewModel.state.awaitValue { it.preview != null }
            assertEquals("102975342", previewed.preview?.totalAtMaturity?.toStringExpanded())
            assertEquals(1, backend.requests(HttpMethod.Post, "deposits/preview").size, "debounced")

            viewModel.save()
            val created = viewModel.state.awaitValue { it.createdId != null }
            assertEquals(PendingAction.Recorded, created.pending)
            val request = backend.single(HttpMethod.Post, "deposits")
            assertEquals("100000000", request.bodyField("principal"))
            assertEquals("6", request.bodyField("termMonths"))
            assertEquals("w1", request.bodyField("fromWalletId"))
            assertEquals("2026-11-01", request.bodyField("openDate"))
        }

    @Test
    fun demandDepositsHaveNoTerm() =
        runViewModelTest {
            backend.post("deposits", depositJson(), HttpStatusCode.Created)
            backend.get("deposits", "[]")
            backend.post("deposits/preview", "{}")
            val viewModel = DepositFormViewModel(repository, wallets, profiles)
            viewModel.state.awaitValue { it.openDate != null }
            viewModel.update {
                it.copy(
                    institution = "ACB",
                    type = "DEMAND",
                    principal = "1.000.000",
                    annualRate = "0,5",
                    termMonths = "",
                )
            }
            viewModel.save()
            val created = viewModel.state.awaitValue { it.createdId != null }
            assertNull(created.pending)
            val request = backend.single(HttpMethod.Post, "deposits")
            assertNull(request.bodyField("termMonths"))
            assertEquals("0.5", request.bodyField("annualRate"))
        }

    @Test
    fun earlySettlementShowsTheQuoteThenPaysOut() =
        runViewModelTest {
            var settled = false
            backend.on(HttpMethod.Get, "deposits/d1") {
                respondJson(if (settled) depositJson("RECORDED", "RECORDED", "SETTLED") else depositJson("RECORDED"))
            }
            backend.get("deposits/d1/settlement-quote", QUOTE)
            backend.on(HttpMethod.Post, "deposits/d1/settle") {
                settled = true
                respondJson(QUOTE)
            }
            backend.get("deposits", "[]")
            val viewModel = DepositDetailViewModel("d1", repository, wallets, profiles)
            viewModel.refresh()
            viewModel.state.awaitValue { it.deposit != null }

            viewModel.loadQuote(LocalDate(2027, 1, 1))
            val quoted = viewModel.state.awaitValue { it.quote != null }
            assertTrue(quoted.quote!!.early)
            assertEquals(
                "2027-01-01",
                backend.single(HttpMethod.Get, "deposits/d1/settlement-quote").url.parameters["date"],
            )

            viewModel.settle(LocalDate(2027, 1, 1), "w1")
            val done = viewModel.state.awaitValue { !it.busy && it.deposit?.status == "SETTLED" }
            assertEquals(PendingAction.Recorded, done.pending)
            assertEquals("w1", backend.single(HttpMethod.Post, "deposits/d1/settle").bodyField("toWalletId"))
        }

    @Test
    fun aFailedPayoutIsReported() =
        runViewModelTest {
            backend.get("deposits/d1", depositJson("RECORDED", "FAILED", "SETTLED"))
            backend.post("deposits/d1/settle", QUOTE)
            backend.get("deposits", "[]")
            val viewModel = DepositDetailViewModel("d1", repository, wallets, profiles)
            viewModel.settle(null, "w1")
            assertEquals(PendingAction.Failed(null), viewModel.state.awaitValue { !it.busy }.pending)
        }

    @Test
    fun quoteErrorsAreShown() =
        runViewModelTest {
            backend.fail(
                HttpMethod.Get,
                "deposits/d1/settlement-quote",
                HttpStatusCode.UnprocessableEntity,
                "DEPOSIT_SETTLED",
            )
            val viewModel = DepositDetailViewModel("d1", repository, wallets, profiles)
            viewModel.loadQuote(null)
            assertEquals(
                "DEPOSIT_SETTLED",
                (viewModel.state.awaitValue { it.error != null }.error as AppError.Api).code,
            )
        }
}
