package com.financemanage.mobile.feature.transaction.domain

import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.feature.transaction.data.AttachmentDto
import com.financemanage.mobile.feature.transaction.data.RecurringDto
import com.financemanage.mobile.feature.transaction.data.TotalsDto
import com.financemanage.mobile.feature.transaction.data.TransactionDto
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate

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

private fun currency(code: String?) = code?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

fun AttachmentDto.toDomain() = Attachment(id, fileName.orEmpty(), contentType.orEmpty(), size ?: 0)

fun TransactionDto.toDomain(): Transaction =
    Transaction(
        id = id,
        walletId = walletId,
        type = type,
        amount = Money(amount ?: BigDecimal.ZERO, currency(currency)),
        categoryId = categoryId,
        occurredOn = occurredOn,
        note = note,
        withPerson = withPerson,
        excludeFromReport = excludeFromReport,
        transferId = transferId,
        counterpartWalletId = counterpartWalletId,
        recurringId = recurringId,
        source =
            source?.let { raw ->
                TransactionSource.entries.firstOrNull { it.name == raw }
                    ?: TransactionSource.OTHER
            },
        attachments = attachments.orEmpty().map { it.toDomain() },
    )

fun TotalsDto.toDomain(): TransactionTotals {
    val code = currency(currency)
    return TransactionTotals(
        Money(income ?: BigDecimal.ZERO, code),
        Money(expense ?: BigDecimal.ZERO, code),
        Money(net ?: BigDecimal.ZERO, code),
    )
}

fun RecurringDto.toDomain() =
    Recurring(
        id = id,
        walletId = walletId,
        categoryId = categoryId,
        type = type,
        amount = Money(amount ?: BigDecimal.ZERO, currency(currency)),
        note = note,
        withPerson = withPerson,
        frequency = frequency,
        interval = interval,
        startDate = startDate,
        endDate = endDate,
        nextOccurrence = nextOccurrence,
        generatedCount = generatedCount,
        active = active,
    )
