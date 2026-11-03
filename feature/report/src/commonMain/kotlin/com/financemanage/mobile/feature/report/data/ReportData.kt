package com.financemanage.mobile.feature.report.data

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class PeriodTotals(
    @Serializable(with = MoneySerializer::class) val income: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expense: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val net: BigDecimal? = null,
)

@Serializable
data class SummaryDto(
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val income: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expense: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val net: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val savingsRate: BigDecimal? = null,
    val previous: PeriodTotals? = null,
    @Serializable(with = MoneySerializer::class) val incomeChange: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expenseChange: BigDecimal? = null,
)

@Serializable
data class CategoryTotalDto(
    val categoryId: String? = null,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val percent: BigDecimal? = null,
    val children: List<CategoryTotalDto> = emptyList(),
)

@Serializable
data class CategoryReportDto(
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val total: BigDecimal? = null,
    val categories: List<CategoryTotalDto> = emptyList(),
)

@Serializable
data class CashFlowPointDto(
    val period: String = "",
    @Serializable(with = MoneySerializer::class) val income: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expense: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val net: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val externalIn: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val externalOut: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val cashFlow: BigDecimal? = null,
)

@Serializable
data class CashFlowDto(
    val currency: String? = null,
    val granularity: String = "MONTH",
    val points: List<CashFlowPointDto> = emptyList(),
)

@Serializable
data class NetWorthItemDto(
    val id: String? = null,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val converted: BigDecimal? = null,
)

@Serializable
data class NetWorthGroupDto(
    val kind: String = "",
    @Serializable(with = MoneySerializer::class) val total: BigDecimal? = null,
    val items: List<NetWorthItemDto> = emptyList(),
)

@Serializable
data class NetWorthDto(
    val asOf: LocalDate? = null,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val assets: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val liabilities: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val netWorth: BigDecimal? = null,
    val groups: List<NetWorthGroupDto> = emptyList(),
)

@Serializable
data class UpcomingItemDto(
    val date: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val direction: String = "OUT",
    val sourceType: String? = null,
    val sourceId: String? = null,
    val title: String = "",
)

@Serializable
data class MonthlyCashFlowDto(
    val month: String = "",
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val inflow: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val outflow: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val net: BigDecimal? = null,
)

@Serializable
data class DashboardDto(
    val activeDeposits: Int = 0,
    val activeLoans: Int = 0,
    val overdueInstallments: Int = 0,
    val upcoming: List<UpcomingItemDto> = emptyList(),
    val monthlyCashFlow: List<MonthlyCashFlowDto> = emptyList(),
)

fun currencyOf(code: String?): CurrencyCode = code?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

fun moneyOf(
    value: BigDecimal?,
    code: String?,
) = Money(value ?: BigDecimal.ZERO, currencyOf(code))

class ReportRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
) {
    val netWorth: Flow<Cached<NetWorthDto>?> = cache.observe(KEY_NET_WORTH, NetWorthDto.serializer())

    private fun HttpRequestBuilder.period(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
    ) {
        parameter("from", from.toString())
        parameter("to", to.toString())
        parameter("currency", currency.value)
    }

    suspend fun summary(
        from: LocalDate,
        to: LocalDate,
        currency: CurrencyCode,
    ): AppResult<SummaryDto> =
        apiCall {
            client.get(api("reports/summary")) { period(from, to, currency) }.body<SummaryDto>()
        }

    suspend fun categories(
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

    suspend fun cashFlow(
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

    suspend fun refreshNetWorth(currency: CurrencyCode): AppResult<NetWorthDto> =
        apiCall { client.get(api("reports/net-worth")) { parameter("currency", currency.value) }.body<NetWorthDto>() }
            .onSuccess { cache.put(KEY_NET_WORTH, NetWorthDto.serializer(), it) }

    suspend fun dashboard(): AppResult<DashboardDto> = apiCall { client.get(api("dashboard")).body<DashboardDto>() }

    /** CSV (UTF-8 with BOM): `date,type,wallet,category,amount,currency,source,excluded`. */
    suspend fun exportCsv(
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
