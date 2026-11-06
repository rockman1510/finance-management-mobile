package com.financemanage.mobile.feature.report.data

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.feature.report.domain.model.CashFlowDto
import com.financemanage.mobile.feature.report.domain.model.CategoryReportDto
import com.financemanage.mobile.feature.report.domain.model.DashboardDto
import com.financemanage.mobile.feature.report.domain.model.NetWorthDto
import com.financemanage.mobile.feature.report.domain.model.SummaryDto
import com.financemanage.mobile.feature.report.domain.repository.ReportRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class ReportRepositoryImpl(
    private val client: HttpClient,
    private val cache: JsonCache,
) : ReportRepository {
    override val netWorth: Flow<Cached<NetWorthDto>?> = cache.observe(KEY_NET_WORTH, NetWorthDto.serializer())

    private fun HttpRequestBuilder.period(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
    ) {
        parameter("from", from.toString())
        parameter("to", to.toString())
        parameter("currency", currency.value)
    }

    override suspend fun summary(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
    ): AppResult<SummaryDto> =
        apiCall {
            client.get(api("reports/summary")) { period(from, to, currency) }.body<SummaryDto>()
        }

    override suspend fun categories(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
        type: String,
    ): AppResult<CategoryReportDto> =
        apiCall {
            client
                .get(api("reports/categories")) {
                    period(from, to, currency)
                    parameter("type", type)
                }.body<CategoryReportDto>()
        }

    override suspend fun cashFlow(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
        granularity: String,
    ): AppResult<CashFlowDto> =
        apiCall {
            client
                .get(api("reports/cash-flow")) {
                    period(from, to, currency)
                    parameter("granularity", granularity)
                }.body<CashFlowDto>()
        }

    override suspend fun refreshNetWorth(currency: CurrencyCode): AppResult<NetWorthDto> =
        apiCall { client.get(api("reports/net-worth")) { parameter("currency", currency.value) }.body<NetWorthDto>() }
            .onSuccess { cache.put(KEY_NET_WORTH, NetWorthDto.serializer(), it) }

    override suspend fun dashboard(): AppResult<DashboardDto> =
        apiCall {
            client.get(api("dashboard")).body<DashboardDto>()
        }

    /** CSV (UTF-8 with BOM): `date,type,wallet,category,amount,currency,source,excluded`. */
    override suspend fun exportCsv(
        from: LocalDate,
        to: LocalDate,
    ): AppResult<ByteArray> =
        apiCall {
            client
                .get(api("reports/transactions.csv")) {
                    header(HttpHeaders.Accept, "text/csv")
                    parameter("from", from.toString())
                    parameter("to", to.toString())
                }.body<ByteArray>()
        }

    private companion object {
        const val KEY_NET_WORTH = "net-worth"
    }
}
