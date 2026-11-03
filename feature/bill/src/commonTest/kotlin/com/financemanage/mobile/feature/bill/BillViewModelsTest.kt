package com.financemanage.mobile.feature.bill

import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.bodyText
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.feature.bill.data.BillRepository
import com.financemanage.mobile.feature.bill.presentation.BillDetailViewModel
import com.financemanage.mobile.feature.bill.presentation.BillFormViewModel
import com.financemanage.mobile.feature.bill.presentation.BillListViewModel
import com.financemanage.mobile.feature.bill.presentation.BillProblem
import com.financemanage.mobile.feature.bill.presentation.BillTemplate
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val WALLET_ID = "1e1a4d7a-b041-43d7-bea8-542d2b4eaeee"

private fun billJson(
    paymentStatus: String? = null,
    failure: String? = null,
) = """{"id":"b1","name":"Tiền điện","amount":800000,"currency":"VND","walletId":"$WALLET_ID",
    "categoryId":"elec","frequency":"MONTHLY","interval":1,"firstDueDate":"2026-11-01","remindDaysBefore":3,
    "nextDueDate":"2026-11-03","daysUntilDue":2,"overdue":false,"active":true,
    "payments":[${paymentStatus?.let {
    """{"id":"p1","dueDate":"2026-11-01","amount":800000,"status":"$it","failureReason":${failure?.let { f ->
        "\"$f\""
    } ?: "null"}}"""
} ?: ""}]}"""

class BillViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val repository = BillRepository(backend.client, cache, PendingPoller())
    private val wallets = WalletRepository(backend.client, cache)
    private val categories = CategoryRepository(backend.client, cache)

    @Test
    fun dueSoonFollowsTheReminderDays() =
        runViewModelTest {
            backend.get("bills", "[${billJson()}]")
            val bill = (repository.refresh() as com.financemanage.mobile.core.model.AppResult.Success).value.single()
            assertTrue(bill.dueSoon)
            assertEquals(3, bill.remindDaysBefore)
        }

    @Test
    fun quickPayPollsUntilRecorded() =
        runViewModelTest {
            backend.get("bills", "[${billJson()}]")
            backend.post("bills/b1/pay", """{"id":"p1","status":"PENDING"}""", HttpStatusCode.Accepted)
            var polls = 0
            backend.on(HttpMethod.Get, "bills/b1") {
                polls++
                respondJson(billJson(if (polls < 2) "PENDING" else "RECORDED"))
            }
            val viewModel = BillListViewModel(repository)
            keepCollecting(viewModel.state)
            viewModel.refresh()
            val bill =
                viewModel.state
                    .awaitValue { it.bills.data != null }
                    .bills.data!!
                    .single()

            viewModel.pay(bill)
            val done =
                viewModel.state.awaitValue {
                    it.payingId == null &&
                        it.pending.let { p -> p != null && p != PendingAction.Processing }
                }
            assertEquals(PendingAction.Recorded, done.pending)
            assertEquals(2, polls)
            assertEquals("{}", backend.single(HttpMethod.Post, "bills/b1/pay").bodyText())
        }

    @Test
    fun aFailedPaymentShowsTheReason() =
        runViewModelTest {
            backend.get("bills/b1", billJson("FAILED", "Ví đã lưu trữ"))
            backend.post("bills/b1/pay", """{"id":"p1","status":"PENDING"}""", HttpStatusCode.Accepted)
            backend.get("bills", "[${billJson()}]")
            val viewModel = BillDetailViewModel("b1", repository, wallets, categories)
            viewModel.refresh()
            viewModel.state.awaitValue { it.bill != null }

            viewModel.pay(null, WALLET_ID, null)
            val failed = viewModel.state.awaitValue { !it.busy }
            assertEquals(PendingAction.Failed("Ví đã lưu trữ"), failed.pending)
            assertEquals(WALLET_ID, backend.single(HttpMethod.Post, "bills/b1/pay").bodyField("walletId"))
        }

    @Test
    fun stillPendingAfterTenSecondsWillUpdateLater() =
        runViewModelTest {
            backend.get("bills/b1", billJson("PENDING"))
            backend.post("bills/b1/pay", """{"id":"p1","status":"PENDING"}""", HttpStatusCode.Accepted)
            backend.get("bills", "[${billJson()}]")
            val viewModel = BillDetailViewModel("b1", repository, wallets, categories)
            viewModel.refresh()
            viewModel.state.awaitValue { it.bill != null }
            viewModel.pay(null, null, null)
            assertEquals(PendingAction.WillUpdate, viewModel.state.awaitValue { !it.busy }.pending)
        }

    @Test
    fun payRefusedIsAnError() =
        runViewModelTest {
            backend.get("bills/b1", billJson())
            backend.fail(HttpMethod.Post, "bills/b1/pay", HttpStatusCode.UnprocessableEntity, "BILL_INACTIVE")
            val viewModel = BillDetailViewModel("b1", repository, wallets, categories)
            viewModel.refresh()
            viewModel.state.awaitValue { it.bill != null }
            viewModel.pay(null, null, null)
            val failed = viewModel.state.awaitValue { !it.busy }
            assertEquals("BILL_INACTIVE", (failed.error as AppError.Api).code)
            assertNull(failed.pending)
        }

    @Test
    fun detailSkipsPausesAndDeletes() =
        runViewModelTest {
            backend.get("bills/b1", billJson())
            backend.get("bills", "[${billJson()}]")
            backend.post("bills/b1/skip", "", HttpStatusCode.NoContent)
            backend.patch("bills/b1", billJson())
            backend.delete("bills/b1")
            val viewModel = BillDetailViewModel("b1", repository, wallets, categories)
            viewModel.skip()
            backend.await(HttpMethod.Post, "bills/b1/skip")
            viewModel.state.awaitValue { !it.busy }
            viewModel.setActive(false)
            assertEquals("false", backend.await(HttpMethod.Patch, "bills/b1").bodyField("active"))
            viewModel.state.awaitValue { !it.busy }
            viewModel.delete()
            viewModel.state.awaitValue { it.deleted && !it.busy }
        }

    @Test
    fun formUsesTemplatesAndValidates() =
        runViewModelTest {
            backend.get("wallets", "[${SampleJson.WALLET}]")
            wallets.refresh()
            backend.get(
                "categories",
                """[{"id":"elec","name":"Điện","type":"EXPENSE","code":"BILLS_ELECTRICITY"},
                {"id":"sal","name":"Lương","type":"INCOME"}]""",
            )
            categories.refresh()
            backend.post("bills", billJson(), HttpStatusCode.Created)
            backend.get("bills", "[${billJson()}]")
            val viewModel =
                BillFormViewModel(null, repository, wallets, categories, ProfileRepository(backend.client, cache))
            val loaded = viewModel.state.awaitValue { it.walletId != null && it.firstDueDate != null }
            assertEquals(listOf("elec"), loaded.categories.map { it.id })

            viewModel.save()
            assertEquals(
                setOf(BillProblem.NAME, BillProblem.AMOUNT, BillProblem.CATEGORY),
                viewModel.state.value.problems,
            )

            viewModel.applyTemplate(BillTemplate.ELECTRICITY, "Tiền điện")
            assertEquals("elec", viewModel.state.value.categoryId)
            viewModel.onAmount("800.000")
            viewModel.onInterval("0")
            viewModel.onRemind("5")
            viewModel.save()
            viewModel.state.awaitValue { it.saved }
            val request = backend.single(HttpMethod.Post, "bills")
            assertEquals("Tiền điện", request.bodyField("name"))
            assertEquals("800000", request.bodyField("amount"))
            assertEquals("MONTHLY", request.bodyField("frequency"))
            assertEquals("1", request.bodyField("interval"))
            assertEquals("5", request.bodyField("remindDaysBefore"))
            assertEquals(WALLET_ID, request.bodyField("walletId"))
        }
}
