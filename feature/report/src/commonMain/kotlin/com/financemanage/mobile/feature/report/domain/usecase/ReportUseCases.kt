package com.financemanage.mobile.feature.report.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.feature.report.domain.model.CashFlowDto
import com.financemanage.mobile.feature.report.domain.model.CategoryReportDto
import com.financemanage.mobile.feature.report.domain.model.DashboardDto
import com.financemanage.mobile.feature.report.domain.model.NetWorthDto
import com.financemanage.mobile.feature.report.domain.model.SummaryDto
import com.financemanage.mobile.feature.report.domain.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Observes the cached net worth. */
class ObserveNetWorthUseCase(
    private val repository: ReportRepository,
) {
    operator fun invoke(): Flow<Cached<NetWorthDto>?> = repository.netWorth
}

/** Income, expense and balance of a period, compared with the previous one. */
class GetReportSummaryUseCase(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
    ): AppResult<SummaryDto> = repository.summary(from, to, currency)
}

/** Totals per category of a period. */
class GetCategoryReportUseCase(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
        type: String,
    ): AppResult<CategoryReportDto> = repository.categories(from, to, currency, type)
}

/** Income/expense per day, week or month of a period. */
class GetCashFlowUseCase(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
        granularity: String,
    ): AppResult<CashFlowDto> = repository.cashFlow(from, to, currency, granularity)
}

class RefreshNetWorthUseCase(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(currency: CurrencyCode): AppResult<NetWorthDto> = repository.refreshNetWorth(currency)
}

class GetDashboardUseCase(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(): AppResult<DashboardDto> = repository.dashboard()
}

/** The transactions of a period as CSV. */
class ExportTransactionsCsvUseCase(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<ByteArray> = repository.exportCsv(from, to)
}
