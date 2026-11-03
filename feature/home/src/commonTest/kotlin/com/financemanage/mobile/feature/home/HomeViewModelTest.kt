package com.financemanage.mobile.feature.home

import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.data.shared.CategoryRepository
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.data.shared.WalletRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.core.testing.tracked
import com.financemanage.mobile.core.ui.components.PendingAction
import com.financemanage.mobile.feature.home.data.HomeRepository
import com.financemanage.mobile.feature.home.presentation.HomeViewModel
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val USD_ME = SampleJson.ME.replace("\"defaultCurrency\":\"VND\"", "\"defaultCurrency\":\"USD\"")

private fun budget(
    id: String,
    percent: Int,
) =
    """{"id":"$id","name":"$id","amount":100,"currency":"USD",
    "current":{"spent":$percent,"percent":$percent,"state":"ON_TRACK"}}"""

private fun goal(
    id: String,
    percent: Int,
    reached: Boolean = false,
) = """{"id":"$id","name":"$id","target":100,"saved":$percent,"currency":"USD","percent":$percent,"reached":$reached}"""

private const val BILL =
    """{"id":"b1","name":"Internet","amount":20,"currency":"USD","nextDueDate":"2026-11-03","daysUntilDue":2}"""

class HomeViewModelTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val settings = AppSettingsRepository(InMemoryPreferencesDataStore())

    private fun TestScope.viewModel(): HomeViewModel {
        val viewModel =
            HomeViewModel(
                HomeRepository(backend.client, cache, PendingPoller()),
                ProfileRepository(backend.client, cache),
                WalletRepository(backend.client, cache),
                CategoryRepository(backend.client, cache),
                settings,
            ).tracked()
        keepCollecting(viewModel.state)
        return viewModel
    }

    private fun stubEverything() {
        backend.get("me", USD_ME)
        backend.get("wallets", "[${SampleJson.WALLET}]")
        backend.get("wallets/total", """{"currency":"USD","total":200,"missingRates":[]}""")
        backend.get("notifications/unread-count", """{"count":4}""")
        backend.get("reports/summary", """{"currency":"USD","income":1000,"expense":250.5,"net":749.5}""")
        backend.get("budgets", "[${budget("a", 10)},${budget("b", 95)},${budget("c", 50)},${budget("d", 120)}]")
        backend.get("bills/upcoming", "[$BILL]")
        backend.get("saving-goals", "[${goal("g1", 30)},${goal("g2", 80)},${goal("g3", 100, reached = true)}]")
        backend.get("transactions", """{"items":[{"id":"t1","type":"EXPENSE","amount":5,"currency":"USD"}],"page":0}""")
        backend.get("categories", "[]")
    }

    @Test
    fun everyBlockLoadsInTheProfileCurrency() =
        runViewModelTest {
            stubEverything()
            val viewModel = viewModel()
            viewModel.refresh()
            val state =
                viewModel.state.awaitValue {
                    it.summary.data != null &&
                        it.transactions.data != null &&
                        it.extras.unread == 4L &&
                        it.extras.total != null &&
                        it.goals.data != null &&
                        it.bills.data != null
                }

            assertEquals("USD", backend.single(HttpMethod.Get, "reports/summary").url.parameters["currency"])
            assertEquals("USD", state.currency.value)
            assertEquals(listOf("d", "b", "c"), state.topBudgets.map { it.id })
            assertEquals("g2", state.featuredGoal?.id)
            assertEquals(
                "200",
                state.extras.total
                    ?.total
                    ?.amount
                    ?.toStringExpanded(),
            )
            assertEquals("7", backend.single(HttpMethod.Get, "bills/upcoming").url.parameters["days"])
        }

    @Test
    fun oneFailingBlockDoesNotHideTheOthers() =
        runViewModelTest {
            stubEverything()
            backend.fail(HttpMethod.Get, "budgets", HttpStatusCode.InternalServerError, "INTERNAL_ERROR")
            val viewModel = viewModel()
            viewModel.refresh()
            val state = viewModel.state.awaitValue { it.budgets.error != null && it.summary.data != null }
            assertTrue(state.budgets.data == null)
            assertNotNull(
                state.goals.data ?: viewModel.state
                    .awaitValue { it.goals.data != null }
                    .goals.data,
            )
            assertEquals(500, (state.budgets.error as AppError.Api).status)
        }

    @Test
    fun quickPayPollsAndRefreshesTheBalances() =
        runViewModelTest {
            stubEverything()
            backend.post("bills/b1/pay", """{"id":"p1","status":"PENDING"}""", HttpStatusCode.Accepted)
            backend.on(HttpMethod.Get, "bills/b1") {
                respondJson(BILL.replace("}", ""","payments":[{"id":"p1","status":"RECORDED"}]}"""))
            }
            val viewModel = viewModel()
            viewModel.refresh()
            val bill =
                viewModel.state
                    .awaitValue { it.bills.data != null }
                    .bills.data!!
                    .single()
            val walletRefreshes = backend.requests(HttpMethod.Get, "wallets").size

            viewModel.payBill(bill)
            val paid = viewModel.state.awaitValue { it.extras.payResult == PendingAction.Recorded }
            assertEquals(null, paid.extras.payingBillId)
            backend.await(HttpMethod.Get, "wallets", count = walletRefreshes + 1)
        }

    @Test
    fun hideBalancesToggles() =
        runViewModelTest {
            val viewModel = viewModel()
            viewModel.toggleHideBalances()
            settings.preferences.first { it.hideBalances }
            viewModel.toggleHideBalances()
            settings.preferences.first { !it.hideBalances }
        }
}
