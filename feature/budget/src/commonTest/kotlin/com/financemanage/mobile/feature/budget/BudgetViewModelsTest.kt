package com.financemanage.mobile.feature.budget

import com.financemanage.mobile.core.data.push.PushRepositoryImpl
import com.financemanage.mobile.core.data.shared.CategoryRepositoryImpl
import com.financemanage.mobile.core.data.shared.ProfileRepositoryImpl
import com.financemanage.mobile.core.data.shared.WalletRepositoryImpl
import com.financemanage.mobile.core.domain.usecase.GetCachedCategoriesUseCase
import com.financemanage.mobile.core.domain.usecase.GetCachedWalletsUseCase
import com.financemanage.mobile.core.domain.usecase.GetDefaultCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.domain.usecase.RequestNotificationPermissionUseCase
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyField
import com.financemanage.mobile.core.testing.bodyJson
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.budget.data.BudgetRepositoryImpl
import com.financemanage.mobile.feature.budget.domain.model.BudgetPeriod
import com.financemanage.mobile.feature.budget.domain.model.BudgetState
import com.financemanage.mobile.feature.budget.domain.usecase.CreateBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.DeleteBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.GetBudgetTransactionsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.GetBudgetUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.ObserveBudgetsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.RefreshBudgetsUseCase
import com.financemanage.mobile.feature.budget.domain.usecase.UpdateBudgetUseCase
import com.financemanage.mobile.feature.budget.presentation.BudgetDetailViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetFormViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetListViewModel
import com.financemanage.mobile.feature.budget.presentation.BudgetProblem
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun budgetJson(
    start: String = "2026-11-01",
    end: String = "2026-11-30",
    spent: Int = 45000,
) = """{"id":"b1","name":"Ăn uống","amount":3000000,"currency":"VND","categoryIds":["food"],"walletId":null,
    "period":"MONTHLY","startDate":"2026-11-01","endDate":null,"repeat":true,
    "current":{"periodStart":"$start","periodEnd":"$end","spent":$spent,"remaining":${3000000 - spent},"percent":1,
    "dailySuggestion":102413,"daysLeft":29,"state":"ON_TRACK","unconvertedCurrencies":[]}}"""

class BudgetViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val repository = BudgetRepositoryImpl(backend.client, cache)
    private val categories = CategoryRepositoryImpl(backend.client, cache)

    private fun formViewModel(id: String?): BudgetFormViewModel {
        val profiles = ProfileRepositoryImpl(backend.client, cache)
        return BudgetFormViewModel(
            id,
            GetBudgetUseCase(repository),
            CreateBudgetUseCase(repository),
            UpdateBudgetUseCase(repository),
            GetCachedCategoriesUseCase(categories),
            GetCachedWalletsUseCase(WalletRepositoryImpl(backend.client, cache)),
            GetDefaultCurrencyUseCase(profiles),
            GetTodayUseCase(profiles),
            RequestNotificationPermissionUseCase(PushRepositoryImpl()),
        )
    }

    @Test
    fun listMapsTheCurrentPeriod() =
        runViewModelTest {
            backend.get("budgets", "[${budgetJson()}]")
            val viewModel = BudgetListViewModel(ObserveBudgetsUseCase(repository), RefreshBudgetsUseCase(repository))
            viewModel.refresh()
            val budget =
                viewModel.state
                    .awaitValue { it.data != null }
                    .data!!
                    .single()
            assertEquals(BudgetState.ON_TRACK, budget.current?.state)
            assertEquals(
                "2955000",
                budget.current
                    ?.remaining
                    ?.amount
                    ?.toStringExpanded(),
            )
            assertEquals(
                "102413",
                budget.current
                    ?.dailySuggestion
                    ?.amount
                    ?.toStringExpanded(),
            )
        }

    @Test
    fun detailNavigatesPeriodsAndLoadsTheCountedExpenses() =
        runViewModelTest {
            backend.get("categories", """[{"id":"food","name":"Ăn uống","type":"EXPENSE"}]""")
            categories.refresh()
            backend.on(HttpMethod.Get, "budgets/b1") { request ->
                val date = request.url.parameters["date"]
                val json =
                    if (date == "2026-10-31") budgetJson("2026-10-01", "2026-10-31", 2000000) else budgetJson()
                respondJson(json)
            }
            backend.get(
                "transactions",
                """{"items":[{"id":"t1","type":"EXPENSE","amount":45000,"currency":"VND","categoryId":"food",
                "occurredOn":"2026-11-01"}],"page":0,"totalPages":1}""",
            )
            val viewModel =
                BudgetDetailViewModel(
                    "b1",
                    GetBudgetUseCase(repository),
                    GetBudgetTransactionsUseCase(repository),
                    DeleteBudgetUseCase(repository),
                    GetCachedCategoriesUseCase(categories),
                )
            viewModel.refresh()
            val current = viewModel.state.awaitValue { it.budget != null && !it.loading }
            assertEquals(1, current.transactions.size)
            assertEquals(1, current.categories.size)
            val query =
                backend
                    .requests(HttpMethod.Get, "transactions")
                    .last()
                    .url.parameters
            assertEquals("EXPENSE", query["type"])
            assertEquals("food", query["categoryId"])
            assertEquals("2026-11-01", query["from"])
            assertEquals("2026-11-30", query["to"])

            viewModel.previousPeriod()
            val previous = viewModel.state.awaitValue { it.date == LocalDate(2026, 10, 31) && !it.loading }
            assertEquals(LocalDate(2026, 10, 1), previous.budget?.current?.periodStart)
            assertEquals(
                "2026-10-01",
                backend
                    .requests(HttpMethod.Get, "transactions")
                    .last()
                    .url.parameters["from"],
            )

            viewModel.nextPeriod()
            viewModel.state.awaitValue { it.date == LocalDate(2026, 11, 1) && !it.loading }
        }

    @Test
    fun formValidatesCustomPeriodsAndCreates() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            ProfileRepositoryImpl(backend.client, cache).refresh()
            backend.post("budgets", budgetJson(), HttpStatusCode.Created)
            backend.get("budgets", "[${budgetJson()}]")
            val viewModel =
                formViewModel(null)
            viewModel.state.awaitValue { it.startDate != null }

            viewModel.onPeriod(BudgetPeriod.CUSTOM)
            viewModel.onStart(LocalDate(2026, 11, 10))
            viewModel.onEnd(LocalDate(2026, 11, 1))
            viewModel.save()
            assertEquals(
                setOf(BudgetProblem.NAME, BudgetProblem.AMOUNT, BudgetProblem.END_BEFORE_START),
                viewModel.state.value.problems,
            )

            viewModel.onName(" Du lịch ")
            viewModel.onAmount("5.000.000")
            viewModel.onEnd(LocalDate(2026, 12, 10))
            viewModel.toggleCategory("food")
            viewModel.toggleCategory("travel")
            viewModel.toggleCategory("food")
            viewModel.save()
            viewModel.state.awaitValue { it.saved }

            val request = backend.single(HttpMethod.Post, "budgets")
            assertEquals("Du lịch", request.bodyField("name"))
            assertEquals("5000000", request.bodyField("amount"))
            assertEquals("CUSTOM", request.bodyField("period"))
            assertEquals("2026-11-10", request.bodyField("startDate"))
            assertEquals("2026-12-10", request.bodyField("endDate"))
            assertEquals("[\"travel\"]", request.bodyJson()["categoryIds"].toString())
        }

    @Test
    fun monthlyBudgetsDoNotSendAnEndDateAndEditSendsAllWallets() =
        runViewModelTest {
            backend.get("budgets/b1", budgetJson())
            backend.patch("budgets/b1", budgetJson())
            backend.get("budgets", "[${budgetJson()}]")
            val viewModel =
                formViewModel("b1")
            val loaded = viewModel.state.awaitValue { it.name.isNotEmpty() }
            assertEquals("3000000", loaded.amount)
            assertEquals(setOf("food"), loaded.categoryIds)
            assertTrue(loaded.repeat)

            viewModel.onRepeat(false)
            viewModel.save()
            viewModel.state.awaitValue { it.saved }
            val body = backend.single(HttpMethod.Patch, "budgets/b1").bodyJson()
            assertEquals("true", body["allWallets"].toString())
            assertEquals("false", body["repeat"].toString())
            assertFalse("period" in body)
        }
}
