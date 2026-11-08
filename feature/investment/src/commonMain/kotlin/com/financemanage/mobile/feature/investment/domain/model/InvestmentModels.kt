package com.financemanage.mobile.feature.investment.domain.model

import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

val AssetTypes = listOf("STOCK", "FUND", "GOLD", "CRYPTO", "BOND", "REAL_ESTATE", "OTHER")

@Serializable
data class TradeDto(
    val id: String,
    val type: String = "BUY",
    @Serializable(with = MoneySerializer::class) val quantity: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val price: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val fee: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val cash: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val realized: BigDecimal? = null,
    val date: LocalDate? = null,
    val walletId: String? = null,
    val cashStatus: AsyncStatus = AsyncStatus.NONE,
    val transactionId: String? = null,
)

@Serializable
data class AssetDto(
    val id: String,
    val name: String = "",
    val symbol: String? = null,
    val type: String = "OTHER",
    val currency: String? = null,
    val note: String? = null,
    @Serializable(with = MoneySerializer::class) val quantity: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val averageCost: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val costBasis: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val lastPrice: BigDecimal? = null,
    val priceDate: LocalDate? = null,
    val priced: Boolean = false,
    @Serializable(with = MoneySerializer::class) val marketValue: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val unrealized: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val unrealizedPercent: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val realized: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalReturn: BigDecimal? = null,
    val trades: List<TradeDto> = emptyList(),
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?) = Money(value ?: BigDecimal.ZERO, currencyCode)
}

@Serializable
data class AllocationDto(
    val type: String = "OTHER",
    @Serializable(with = MoneySerializer::class) val value: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val percent: BigDecimal? = null,
)

@Serializable
data class PortfolioDto(
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val marketValue: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val costBasis: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val unrealized: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val unrealizedPercent: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val realized: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalReturn: BigDecimal? = null,
    val allocation: List<AllocationDto> = emptyList(),
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?) = Money(value ?: BigDecimal.ZERO, currencyCode)
}

@Serializable
data class AssetRequest(
    val name: String,
    val symbol: String? = null,
    val type: String? = null,
    val currency: String? = null,
    val note: String? = null,
)

@Serializable
data class TradeRequest(
    @Serializable(with = MoneySerializer::class) val quantity: BigDecimal,
    @Serializable(with = MoneySerializer::class) val price: BigDecimal,
    @Serializable(with = MoneySerializer::class) val fee: BigDecimal? = null,
    val walletId: String? = null,
    val date: LocalDate? = null,
)

@Serializable
data class IncomeRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val walletId: String? = null,
    val categoryId: String? = null,
    val date: LocalDate? = null,
)

enum class TradeKind(
    val path: String,
) {
    BUY("buys"),
    SELL("sells"),
    INCOME("incomes"),
}
