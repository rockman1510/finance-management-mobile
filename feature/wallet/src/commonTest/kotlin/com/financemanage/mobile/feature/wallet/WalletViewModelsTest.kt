package com.financemanage.mobile.feature.wallet

import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.bodyJson
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.wallet.presentation.AdjustBalanceViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletDetailViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletFormProblem
import com.financemanage.mobile.feature.wallet.presentation.WalletFormViewModel
import com.financemanage.mobile.feature.wallet.presentation.WalletListViewModel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val WALLET_ID = "1e1a4d7a-b041-43d7-bea8-542d2b4eaeee"

private const val USD_PROFILE =
    """{"id":"u1","email":"a@example.com","defaultCurrency":"USD","timeZone":"Asia/Ho_Chi_Minh"}"""

private const val CARD =
    """{"id":"c1","name":"Visa","type":"CREDIT_CARD","currency":"USD","initialBalance":-120.5,"balance":-120.5,
    "creditLimit":2000,"availableCredit":1879.5,"statementDay":25,"paymentDueDay":10}"""

class WalletViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val wallets = WalletRepository(backend.client, cache)
    private val profiles = ProfileRepository(backend.client, cache)

    @Test
    fun listShowsCachedWalletsAndTheTotalInTheDefaultCurrency() =
        runViewModelTest {
            backend.get("me", USD_PROFILE)
            profiles.refresh()
            backend.get("wallets", "[${SampleJson.WALLET},$CARD]")
            backend.get("wallets/total", """{"currency":"USD","total":79.5,"missingRates":["VND/USD"]}""")
            val viewModel = WalletListViewModel(wallets, profiles)
            keepCollecting(viewModel.state)

            viewModel.refresh()
            val state = viewModel.state.awaitValue { it.total != null && it.wallets.data?.size == 2 }

            assertEquals(listOf("Tiền mặt", "Visa"), state.wallets.data?.map { it.name })
            assertEquals(
                "79.5",
                state.total
                    ?.total
                    ?.amount
                    ?.toStringExpanded(),
            )
            assertEquals(listOf("VND/USD"), state.total?.missingRates)
            assertEquals("USD", backend.single(HttpMethod.Get, "wallets/total").url.parameters["currency"])
            assertEquals("true", backend.single(HttpMethod.Get, "wallets").url.parameters["includeArchived"])
        }

    @Test
    fun listKeepsCacheAndReportsTheErrorWhenOffline() =
        runViewModelTest {
            backend.get("wallets", "[${SampleJson.WALLET}]")
            wallets.refresh()
            backend.offline(HttpMethod.Get, "wallets")
            val viewModel = WalletListViewModel(wallets, profiles)
            keepCollecting(viewModel.state)

            viewModel.refresh()
            val state = viewModel.state.awaitValue { it.wallets.error != null }

            assertIs<AppError.Network>(state.wallets.error)
            assertEquals(1, state.wallets.data?.size)
        }

    @Test
    fun createValidatesThenSendsTheRequestWithTheCurrencyDecimals() =
        runViewModelTest {
            backend.get("me", USD_PROFILE)
            profiles.refresh()
            backend.post("wallets", CARD, HttpStatusCode.Created)
            backend.get("wallets", "[$CARD]")
            val viewModel = WalletFormViewModel(null, wallets, profiles)
            viewModel.state.awaitValue { it.loaded }
            assertEquals(CurrencyCode("USD"), viewModel.state.value.currency)

            viewModel.onType(WalletType.CREDIT_CARD)
            viewModel.onInitialBalance("12a")
            viewModel.onStatementDay("32")
            viewModel.save()
            assertEquals(
                setOf(WalletFormProblem.NAME, WalletFormProblem.AMOUNT, WalletFormProblem.STATEMENT_DAY),
                viewModel.state.value.problems,
            )
            assertTrue(backend.requests(HttpMethod.Post, "wallets").isEmpty())

            viewModel.onName("  Visa ")
            viewModel.onInitialBalance("-120,50")
            viewModel.onCreditLimit("2.000")
            viewModel.onStatementDay("25")
            viewModel.onPaymentDueDay("1a0")
            viewModel.save()
            viewModel.state.awaitValue { it.saved }

            val request = backend.single(HttpMethod.Post, "wallets")
            assertEquals("Visa", request.bodyField("name"))
            assertEquals("CREDIT_CARD", request.bodyField("type"))
            assertEquals("USD", request.bodyField("currency"))
            assertEquals("-120.5", request.bodyField("initialBalance"))
            assertEquals("2000", request.bodyField("creditLimit"))
            assertEquals("25", request.bodyField("statementDay"))
            assertEquals("10", request.bodyField("paymentDueDay"))
        }

    @Test
    fun cardFieldsAreNotSentForCashWallets() =
        runViewModelTest {
            backend.post("wallets", SampleJson.WALLET, HttpStatusCode.Created)
            backend.get("wallets", "[${SampleJson.WALLET}]")
            val viewModel = WalletFormViewModel(null, wallets, profiles)
            viewModel.state.awaitValue { it.loaded }

            viewModel.onName("Tiền mặt")
            viewModel.onStatementDay("99")
            viewModel.save()
            viewModel.state.awaitValue { it.saved }

            val body = backend.single(HttpMethod.Post, "wallets").bodyJson()
            assertEquals("0", body["initialBalance"].toString())
            assertFalse("statementDay" in body)
            assertFalse("creditLimit" in body)
        }

    @Test
    fun editLoadsTheWalletAndShowsServerErrors() =
        runViewModelTest {
            backend.get("wallets/c1", CARD)
            backend.fail(HttpMethod.Patch, "wallets/c1", HttpStatusCode.Conflict, "CONCURRENT_MODIFICATION")
            val viewModel = WalletFormViewModel("c1", wallets, profiles)
            val loaded = viewModel.state.awaitValue { it.loaded }
            assertEquals("Visa", loaded.name)
            assertEquals("-120.50", loaded.initialBalance)
            assertEquals("2000.00", loaded.creditLimit)

            viewModel.save()
            val failed = viewModel.state.awaitValue { it.error != null }
            assertEquals("CONCURRENT_MODIFICATION", (failed.error as AppError.Api).code)
            assertFalse(failed.saved)
            assertFalse(failed.saving)
        }

    @Test
    fun detailArchivesAndDeletes() =
        runViewModelTest {
            backend.get("wallets", "[${SampleJson.WALLET}]")
            wallets.refresh()
            backend.patch("wallets/$WALLET_ID", SampleJson.WALLET.replace("\"archived\":false", "\"archived\":true"))
            backend.delete("wallets/$WALLET_ID")
            val viewModel = WalletDetailViewModel(WALLET_ID, wallets)
            keepCollecting(viewModel.state)
            assertEquals(
                "Tiền mặt",
                viewModel.state
                    .awaitValue { it.wallet != null }
                    .wallet
                    ?.name,
            )

            viewModel.setArchived(true)
            backend.await(HttpMethod.Patch, "wallets/$WALLET_ID")
            viewModel.state.awaitValue { !it.busy }
            assertEquals("true", backend.single(HttpMethod.Patch, "wallets/$WALLET_ID").bodyField("archived"))

            viewModel.delete()
            assertTrue(viewModel.state.awaitValue { it.deleted }.deleted)
        }

    @Test
    fun adjustBalancePrefillsTheCurrentBalanceAndRejectsBadInput() =
        runViewModelTest {
            backend.get("wallets/$WALLET_ID", SampleJson.WALLET)
            backend.post("wallets/$WALLET_ID/adjust-balance", "{}")
            backend.get("wallets", "[${SampleJson.WALLET}]")
            val viewModel = AdjustBalanceViewModel(WALLET_ID, wallets, profiles)
            val loaded = viewModel.state.awaitValue { it.wallet != null }
            assertEquals("5000000", loaded.newBalance)

            viewModel.onBalance("abc")
            viewModel.save()
            assertTrue(viewModel.state.value.invalidAmount)

            viewModel.onBalance("4.750.000")
            viewModel.onNote(" ")
            viewModel.save()
            viewModel.state.awaitValue { it.saved }
            val request = backend.single(HttpMethod.Post, "wallets/$WALLET_ID/adjust-balance")
            assertEquals("4750000", request.bodyField("newBalance"))
            assertNull(request.bodyField("note"))
            assertEquals(loaded.date.toString(), request.bodyField("occurredOn"))
        }
}
