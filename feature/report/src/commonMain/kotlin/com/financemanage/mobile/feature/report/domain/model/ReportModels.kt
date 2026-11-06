package com.financemanage.mobile.feature.report.domain.model

import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
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
