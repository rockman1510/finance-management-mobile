package com.financemanage.mobile.feature.budget.domain.model

import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.model.serialization.FallbackEnumSerializer
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable(with = BudgetPeriod.Serializer::class)
enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM,
    UNKNOWN,
    ;

    internal object Serializer : FallbackEnumSerializer<BudgetPeriod>("BudgetPeriod", BudgetPeriod.entries, UNKNOWN)
}

@Serializable(with = BudgetState.Serializer::class)
enum class BudgetState {
    NOT_STARTED,
    ON_TRACK,
    WARNING,
    EXCEEDED,
    ENDED,
    UNKNOWN,
    ;

    internal object Serializer : FallbackEnumSerializer<BudgetState>("BudgetState", BudgetState.entries, UNKNOWN)
}

@Serializable
data class CreateBudgetRequest(
    val name: String,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val currency: String? = null,
    val categoryIds: List<String> = emptyList(),
    val walletId: String? = null,
    val period: BudgetPeriod,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val repeat: Boolean? = null,
)

@Serializable
data class UpdateBudgetRequest(
    val name: String? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val categoryIds: List<String>? = null,
    val walletId: String? = null,
    val allWallets: Boolean? = null,
    val repeat: Boolean? = null,
)

/** Minimal transaction row for the budget's daily chart and list. */
@Serializable
data class BudgetTransactionDto(
    val id: String,
    val type: TransactionType = TransactionType.UNKNOWN,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val categoryId: String? = null,
    val occurredOn: LocalDate? = null,
    val note: String? = null,
)

data class BudgetStatus(
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
    val spent: Money,
    val remaining: Money,
    val percent: Int,
    val dailySuggestion: Money?,
    val daysLeft: Long,
    val state: BudgetState,
    val unconvertedCurrencies: List<String>,
)

data class Budget(
    val id: String,
    val name: String,
    val amount: Money,
    val categoryIds: List<String>,
    val walletId: String?,
    val period: BudgetPeriod,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val repeat: Boolean,
    val current: BudgetStatus?,
)
