package com.financemanage.mobile.feature.bill.domain.model

import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class CreateBillRequest(
    val name: String,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val walletId: String,
    val categoryId: String,
    val frequency: Frequency,
    val interval: Int? = null,
    val firstDueDate: LocalDate,
    val endDate: LocalDate? = null,
    val remindDaysBefore: Int? = null,
    val note: String? = null,
)

@Serializable
data class UpdateBillRequest(
    val name: String? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val categoryId: String? = null,
    val endDate: LocalDate? = null,
    val remindDaysBefore: Int? = null,
    val note: String? = null,
    val active: Boolean? = null,
)

@Serializable
data class PayBillRequest(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val walletId: String? = null,
    val paidOn: LocalDate? = null,
)

data class Payment(
    val id: String,
    val dueDate: LocalDate?,
    val amount: Money,
    val paidOn: LocalDate?,
    val status: AsyncStatus,
    val transactionId: String?,
    val failureReason: String?,
)

data class Bill(
    val id: String,
    val name: String,
    val amount: Money,
    val walletId: String?,
    val categoryId: String?,
    val frequency: Frequency,
    val interval: Int,
    val firstDueDate: LocalDate?,
    val endDate: LocalDate?,
    val remindDaysBefore: Int,
    val note: String?,
    val nextDueDate: LocalDate?,
    val daysUntilDue: Long?,
    val overdue: Boolean,
    val active: Boolean,
    val payments: List<Payment>,
) {
    val dueSoon: Boolean get() = !overdue && (daysUntilDue ?: Long.MAX_VALUE) <= remindDaysBefore
}
