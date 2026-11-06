package com.financemanage.mobile.feature.report

import com.financemanage.mobile.core.data.shared.ProfileRepositoryImpl
import com.financemanage.mobile.core.domain.usecase.GetDefaultCurrencyUseCase
import com.financemanage.mobile.core.domain.usecase.GetTodayUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.keepCollecting
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.feature.report.data.ReportRepositoryImpl
import com.financemanage.mobile.feature.report.domain.usecase.ExportTransactionsCsvUseCase
import com.financemanage.mobile.feature.report.domain.usecase.GetCashFlowUseCase
import com.financemanage.mobile.feature.report.domain.usecase.GetCategoryReportUseCase
import com.financemanage.mobile.feature.report.domain.usecase.GetDashboardUseCase
import com.financemanage.mobile.feature.report.domain.usecase.GetReportSummaryUseCase
import com.financemanage.mobile.feature.report.domain.usecase.ObserveNetWorthUseCase
import com.financemanage.mobile.feature.report.domain.usecase.RefreshNetWorthUseCase
import com.financemanage.mobile.feature.report.presentation.NetWorthViewModel
import com.financemanage.mobile.feature.report.presentation.PeriodPreset
import com.financemanage.mobile.feature.report.presentation.ReportsViewModel
import com.financemanage.mobile.feature.report.presentation.presetRange
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val repository = ReportRepositoryImpl(backend.client, cache)
    private val profiles = ProfileRepositoryImpl(backend.client, cache)

    private fun reportsViewModel() =
        ReportsViewModel(
            GetReportSummaryUseCase(repository),
            GetCategoryReportUseCase(repository),
            GetCashFlowUseCase(repository),
            ExportTransactionsCsvUseCase(repository),
            GetTodayUseCase(profiles),
            GetDefaultCurrencyUseCase(profiles),
        )

    private fun stubReports() {
        backend.on(HttpMethod.Get, "reports/summary") { request ->
            respondJson("""{"currency":"${request.url.parameters["currency"]}","income":100,"expense":40,"net":60}""")
        }
        backend.get("reports/categories", """{"currency":"VND","total":40,"categories":[]}""")
        backend.on(HttpMethod.Get, "reports/cash-flow") { request ->
            respondJson("""{"currency":"VND","granularity":"${request.url.parameters["granularity"]}","points":[]}""")
        }
    }

    @Test
    fun presetRanges() {
        val today = LocalDate(2026, 11, 3)
        assertEquals(LocalDate(2026, 11, 1) to LocalDate(2026, 11, 30), presetRange(PeriodPreset.THIS_MONTH, today))
        assertEquals(LocalDate(2026, 10, 1) to LocalDate(2026, 10, 31), presetRange(PeriodPreset.LAST_MONTH, today))
        assertEquals(LocalDate(2026, 10, 1) to LocalDate(2026, 12, 31), presetRange(PeriodPreset.THIS_QUARTER, today))
        assertEquals(LocalDate(2026, 1, 1) to LocalDate(2026, 12, 31), presetRange(PeriodPreset.THIS_YEAR, today))
        assertEquals(
            LocalDate(2026, 12, 1) to LocalDate(2026, 12, 31),
            presetRange(PeriodPreset.LAST_MONTH, LocalDate(2027, 1, 15)),
        )
        assertEquals(
            LocalDate(2028, 2, 1) to LocalDate(2028, 2, 29),
            presetRange(PeriodPreset.THIS_MONTH, LocalDate(2028, 2, 10)),
        )
    }

    @Test
    fun loadsTheMonthDailyAndTheYearMonthly() =
        runViewModelTest {
            stubReports()
            val viewModel = reportsViewModel()
            val month = viewModel.state.awaitValue { !it.loading && it.summary != null }
            assertEquals("DAY", month.cashFlow?.granularity)
            assertEquals(1, month.from?.dayOfMonth)
            val query = backend.single(HttpMethod.Get, "reports/categories").url.parameters
            assertEquals("EXPENSE", query["type"])
            assertEquals(month.from.toString(), query["from"])
            assertEquals(month.to.toString(), query["to"])

            viewModel.setPreset(PeriodPreset.THIS_YEAR)
            viewModel.state.awaitValue {
                it.preset == PeriodPreset.THIS_YEAR &&
                    !it.loading &&
                    it.cashFlow?.granularity == "MONTH"
            }

            viewModel.shiftMonth(-1)
            val shifted = viewModel.state.awaitValue { it.preset == PeriodPreset.CUSTOM && !it.loading }
            assertEquals(1, shifted.from?.dayOfMonth)
            assertEquals("DAY", shifted.cashFlow?.granularity)
        }

    @Test
    fun errorsAreShownAndCsvIsExported() =
        runViewModelTest {
            stubReports()
            backend.fail(HttpMethod.Get, "reports/cash-flow", HttpStatusCode.BadRequest, "PERIOD_TOO_LONG")
            backend.on(HttpMethod.Get, "reports/transactions.csv") {
                respond(
                    "date,type\n".encodeToByteArray(),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "text/csv"),
                )
            }
            val viewModel = reportsViewModel()
            val failed = viewModel.state.awaitValue { !it.loading && it.error != null }
            assertEquals("PERIOD_TOO_LONG", (failed.error as AppError.Api).code)
            assertEquals("60", failed.summary?.net?.toStringExpanded())

            viewModel.exportCsv()
            val exported = viewModel.state.awaitValue { it.csv != null }
            assertContentEquals("date,type\n".encodeToByteArray(), exported.csv)
            viewModel.csvHandled()
            assertNull(viewModel.state.value.csv)
        }

    @Test
    fun netWorthIsCachedWithTheDashboard() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            profiles.refresh()
            backend.get(
                "reports/net-worth",
                """{"asOf":"2026-11-03","currency":"VND","assets":10,"liabilities":4,"netWorth":6}""",
            )
            backend.get("dashboard", """{"activeDeposits":1,"activeLoans":2,"overdueInstallments":1}""")
            val viewModel =
                NetWorthViewModel(
                    ObserveNetWorthUseCase(repository),
                    RefreshNetWorthUseCase(repository),
                    GetDashboardUseCase(repository),
                    GetDefaultCurrencyUseCase(profiles),
                )
            keepCollecting(viewModel.state)
            viewModel.refresh()
            val state = viewModel.state.awaitValue { it.netWorth.data != null && it.dashboard != null }
            assertEquals(
                "6",
                state.netWorth.data
                    ?.netWorth
                    ?.toStringExpanded(),
            )
            assertEquals(2, state.dashboard?.activeLoans)
            assertEquals("VND", backend.single(HttpMethod.Get, "reports/net-worth").url.parameters["currency"])
        }
}
