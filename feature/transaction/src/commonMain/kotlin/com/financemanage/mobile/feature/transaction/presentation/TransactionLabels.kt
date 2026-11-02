package com.financemanage.mobile.feature.transaction.presentation

import androidx.compose.runtime.Composable
import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.feature.transaction.domain.Transaction
import com.financemanage.mobile.feature.transaction.domain.TransactionSource
import com.financemanage.mobile.feature.transaction.resources.Res
import com.financemanage.mobile.feature.transaction.resources.error_attachment_count
import com.financemanage.mobile.feature.transaction.resources.error_attachment_size
import com.financemanage.mobile.feature.transaction.resources.error_attachment_type
import com.financemanage.mobile.feature.transaction.resources.error_not_editable
import com.financemanage.mobile.feature.transaction.resources.error_rate_missing
import com.financemanage.mobile.feature.transaction.resources.error_same_wallet
import com.financemanage.mobile.feature.transaction.resources.error_transfer_mismatch
import com.financemanage.mobile.feature.transaction.resources.error_wallet_archived
import com.financemanage.mobile.feature.transaction.resources.freq_DAILY
import com.financemanage.mobile.feature.transaction.resources.freq_MONTHLY
import com.financemanage.mobile.feature.transaction.resources.freq_QUARTERLY
import com.financemanage.mobile.feature.transaction.resources.freq_WEEKLY
import com.financemanage.mobile.feature.transaction.resources.freq_YEARLY
import com.financemanage.mobile.feature.transaction.resources.label_adjustment
import com.financemanage.mobile.feature.transaction.resources.label_transfer
import com.financemanage.mobile.feature.transaction.resources.source_BILL
import com.financemanage.mobile.feature.transaction.resources.source_DEPOSIT_OPEN
import com.financemanage.mobile.feature.transaction.resources.source_DEPOSIT_SETTLE
import com.financemanage.mobile.feature.transaction.resources.source_INVESTMENT
import com.financemanage.mobile.feature.transaction.resources.source_OTHER
import com.financemanage.mobile.feature.transaction.resources.source_SAVING
import com.financemanage.mobile.feature.transaction.resources.type_ADJUSTMENT_IN
import com.financemanage.mobile.feature.transaction.resources.type_ADJUSTMENT_OUT
import com.financemanage.mobile.feature.transaction.resources.type_EXPENSE
import com.financemanage.mobile.feature.transaction.resources.type_EXTERNAL_IN
import com.financemanage.mobile.feature.transaction.resources.type_EXTERNAL_OUT
import com.financemanage.mobile.feature.transaction.resources.type_INCOME
import com.financemanage.mobile.feature.transaction.resources.type_TRANSFER_IN
import com.financemanage.mobile.feature.transaction.resources.type_TRANSFER_OUT
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val TransactionErrors: Map<String, StringResource> =
    mapOf(
        "TRANSACTION_NOT_EDITABLE" to Res.string.error_not_editable,
        "WALLET_ARCHIVED" to Res.string.error_wallet_archived,
        "SAME_WALLET" to Res.string.error_same_wallet,
        "EXCHANGE_RATE_MISSING" to Res.string.error_rate_missing,
        "TRANSFER_AMOUNT_MISMATCH" to Res.string.error_transfer_mismatch,
        "ATTACHMENT_TYPE_NOT_ALLOWED" to Res.string.error_attachment_type,
        "ATTACHMENT_TOO_LARGE" to Res.string.error_attachment_size,
        "TOO_MANY_ATTACHMENTS" to Res.string.error_attachment_count,
    )

internal val FilterableTypes: List<TransactionType> = TransactionType.entries.filter { it != TransactionType.UNKNOWN }

@Composable
internal fun typeLabel(type: TransactionType): String =
    stringResource(
        when (type) {
            TransactionType.INCOME -> Res.string.type_INCOME
            TransactionType.EXPENSE, TransactionType.UNKNOWN -> Res.string.type_EXPENSE
            TransactionType.TRANSFER_IN -> Res.string.type_TRANSFER_IN
            TransactionType.TRANSFER_OUT -> Res.string.type_TRANSFER_OUT
            TransactionType.ADJUSTMENT_IN -> Res.string.type_ADJUSTMENT_IN
            TransactionType.ADJUSTMENT_OUT -> Res.string.type_ADJUSTMENT_OUT
            TransactionType.EXTERNAL_IN -> Res.string.type_EXTERNAL_IN
            TransactionType.EXTERNAL_OUT -> Res.string.type_EXTERNAL_OUT
        },
    )

/** Source label shown on rows: Bill / Savings / Investment / Deposit / Transfer / Adjustment (F07). */
@Composable
internal fun sourceLabel(transaction: Transaction): String? {
    val source = transaction.source
    return when {
        source != null ->
            stringResource(
                when (source) {
                    TransactionSource.BILL -> Res.string.source_BILL
                    TransactionSource.SAVING -> Res.string.source_SAVING
                    TransactionSource.INVESTMENT -> Res.string.source_INVESTMENT
                    TransactionSource.DEPOSIT_OPEN -> Res.string.source_DEPOSIT_OPEN
                    TransactionSource.DEPOSIT_SETTLE -> Res.string.source_DEPOSIT_SETTLE
                    TransactionSource.OTHER -> Res.string.source_OTHER
                },
            )
        transaction.transferId != null -> stringResource(Res.string.label_transfer)
        transaction.type == TransactionType.ADJUSTMENT_IN || transaction.type == TransactionType.ADJUSTMENT_OUT ->
            stringResource(Res.string.label_adjustment)
        else -> null
    }
}

internal val RepeatFrequencies = listOf(Frequency.DAILY, Frequency.WEEKLY, Frequency.MONTHLY, Frequency.YEARLY)

@Composable
internal fun frequencyLabel(frequency: Frequency): String =
    stringResource(
        when (frequency) {
            Frequency.DAILY -> Res.string.freq_DAILY
            Frequency.WEEKLY -> Res.string.freq_WEEKLY
            Frequency.QUARTERLY -> Res.string.freq_QUARTERLY
            Frequency.YEARLY -> Res.string.freq_YEARLY
            Frequency.MONTHLY, Frequency.UNKNOWN -> Res.string.freq_MONTHLY
        },
    )
