package com.financemanage.mobile.feature.transaction.domain.model

import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.feature.transaction.domain.model.Recurring
import com.financemanage.mobile.feature.transaction.domain.model.Transaction
import com.financemanage.mobile.feature.transaction.domain.model.TransactionFilter
import com.financemanage.mobile.feature.transaction.domain.model.TransactionPage
import com.financemanage.mobile.feature.transaction.domain.model.TransactionTotals
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

data class Attachment(
    val id: String,
    val fileName: String,
    val contentType: String,
    val size: Long,
) {
    val isImage: Boolean get() = contentType.startsWith("image/")
}

/** Where a transaction comes from when another module created it (API §5.3). */
enum class TransactionSource { BILL, SAVING, INVESTMENT, DEPOSIT_OPEN, DEPOSIT_SETTLE, OTHER }

data class Transaction(
    val id: String,
    val walletId: String,
    val type: TransactionType,
    val amount: Money,
    val categoryId: String?,
    val occurredOn: LocalDate?,
    val note: String?,
    val withPerson: String?,
    val excludeFromReport: Boolean,
    val transferId: String?,
    val counterpartWalletId: String?,
    val recurringId: String?,
    val source: TransactionSource?,
    val attachments: List<Attachment>,
) {
    /** Only income/expense entered by the user can be edited or deleted (`TRANSACTION_NOT_EDITABLE`). */
    val editable: Boolean
        get() =
            (type == TransactionType.INCOME || type == TransactionType.EXPENSE) &&
                source == null &&
                transferId == null

    /** Signed amount for display: inflows positive, outflows negative. */
    val signedAmount: Money get() = if (type.isInflow) amount else -amount
}

/** Filters of `GET /transactions` (API §5.3). */
data class TransactionFilter(
    val walletId: String? = null,
    val categoryIds: List<String> = emptyList(),
    val types: List<TransactionType> = emptyList(),
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val query: String = "",
) {
    val isEmpty: Boolean
        get() =
            walletId == null &&
                categoryIds.isEmpty() &&
                types.isEmpty() &&
                from == null &&
                to == null &&
                minAmount == null &&
                maxAmount == null &&
                query.isBlank()
}

data class TransactionTotals(
    val income: Money,
    val expense: Money,
    val net: Money,
)

data class TransactionPage(
    val items: List<Transaction>,
    val page: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = page + 1 < totalPages
}

data class Recurring(
    val id: String,
    val walletId: String?,
    val categoryId: String?,
    val type: TransactionType,
    val amount: Money,
    val note: String?,
    val withPerson: String?,
    val frequency: Frequency,
    val interval: Int,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val nextOccurrence: LocalDate?,
    val generatedCount: Int,
    val active: Boolean,
)

@Serializable
data class AttachmentDto(
    val id: String,
    val fileName: String? = null,
    val contentType: String? = null,
    val size: Long? = null,
    val createdAt: Instant? = null,
)

@Serializable
data class CreateTransactionRequest(
    val walletId: String,
    val categoryId: String,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val occurredOn: LocalDate? = null,
    val note: String? = null,
    val withPerson: String? = null,
    val excludeFromReport: Boolean? = null,
)

@Serializable
data class UpdateTransactionRequest(
    val walletId: String? = null,
    val categoryId: String? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val occurredOn: LocalDate? = null,
    val note: String? = null,
    val withPerson: String? = null,
    val excludeFromReport: Boolean? = null,
)

@Serializable
data class TransferRequest(
    val fromWalletId: String,
    val toWalletId: String,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    @Serializable(with = MoneySerializer::class) val receivedAmount: BigDecimal? = null,
    val occurredOn: LocalDate? = null,
    val note: String? = null,
)

@Serializable
data class CreateRecurringRequest(
    val walletId: String,
    val categoryId: String,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
    val note: String? = null,
    val withPerson: String? = null,
    val frequency: Frequency,
    val interval: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
)

@Serializable
data class UpdateRecurringRequest(
    val walletId: String? = null,
    val categoryId: String? = null,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val note: String? = null,
    val withPerson: String? = null,
    val endDate: LocalDate? = null,
    val active: Boolean? = null,
)

/** Result of a create call: saved on the server, or queued because the device is offline (F21). */
sealed interface WriteOutcome {
    data object Saved : WriteOutcome

    data object Queued : WriteOutcome
}
