package com.financemanage.mobile.feature.report.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.feature.report.domain.model.CashFlowDto
import com.financemanage.mobile.feature.report.domain.model.CategoryReportDto
import com.financemanage.mobile.feature.report.domain.model.DashboardDto
import com.financemanage.mobile.feature.report.domain.model.NetWorthDto
import com.financemanage.mobile.feature.report.domain.model.SummaryDto
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** Reports, net worth and dashboard (API §16). */
interface ReportRepository {
    val netWorth: Flow<Cached<NetWorthDto>?>

    suspend fun summary(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
    ): AppResult<SummaryDto>

    suspend fun categories(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
        type: String,
    ): AppResult<CategoryReportDto>

    suspend fun cashFlow(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
        granularity: String,
    ): AppResult<CashFlowDto>

    suspend fun refreshNetWorth(currency: CurrencyCode): AppResult<NetWorthDto>

    suspend fun dashboard(): AppResult<DashboardDto>

    /** CSV (UTF-8 with BOM): `date,type,wallet,category,amount,currency,source,excluded`. */
    suspend fun exportCsv(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<ByteArray>
}
