package com.financemanage.mobile.feature.transaction.data

import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.feature.transaction.data.RecurringDto
import com.financemanage.mobile.feature.transaction.data.TotalsDto
import com.financemanage.mobile.feature.transaction.data.TransactionDto
import com.financemanage.mobile.feature.transaction.domain.model.Attachment
import com.financemanage.mobile.feature.transaction.domain.model.AttachmentDto
import com.financemanage.mobile.feature.transaction.domain.model.Recurring
import com.financemanage.mobile.feature.transaction.domain.model.Transaction
import com.financemanage.mobile.feature.transaction.domain.model.TransactionSource
import com.financemanage.mobile.feature.transaction.domain.model.TransactionTotals
import com.ionspin.kotlin.bignum.decimal.BigDecimal

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
