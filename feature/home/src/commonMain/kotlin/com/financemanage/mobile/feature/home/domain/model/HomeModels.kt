package com.financemanage.mobile.feature.home.domain.model

import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// Read-only views of other modules' data, owned by Home (features never depend on each other).

@Serializable
data class HomeSummary(
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val income: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expense: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val net: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val incomeChange: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expenseChange: BigDecimal? = null,
)

@Serializable
data class HomeBudgetStatus(
    @Serializable(with = MoneySerializer::class) val spent: BigDecimal? = null,
    val percent: Int = 0,
    val state: String = "",
)

@Serializable
data class HomeBudget(
    val id: String,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val current: HomeBudgetStatus? = null,
)

@Serializable
data class HomePayment(
    val id: String,
    val status: AsyncStatus = AsyncStatus.UNKNOWN,
    val failureReason: String? = null,
)

@Serializable
data class HomeBill(
    val id: String,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val nextDueDate: LocalDate? = null,
    val daysUntilDue: Long? = null,
    val overdue: Boolean = false,
    val payments: List<HomePayment> = emptyList(),
)

@Serializable
data class HomeGoal(
    val id: String,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val target: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val saved: BigDecimal? = null,
    val currency: String? = null,
    val percent: Int = 0,
    val deadline: LocalDate? = null,
    val reached: Boolean = false,
)

@Serializable
data class HomeTransaction(
    val id: String,
    val walletId: String? = null,
    val type: TransactionType = TransactionType.UNKNOWN,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val categoryId: String? = null,
    val occurredOn: LocalDate? = null,
    val note: String? = null,
)
