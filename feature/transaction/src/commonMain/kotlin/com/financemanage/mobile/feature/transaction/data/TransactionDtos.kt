package com.financemanage.mobile.feature.transaction.data

import com.financemanage.mobile.core.model.domain.Frequency
import com.financemanage.mobile.core.model.domain.TransactionType
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.financemanage.mobile.feature.transaction.domain.model.AttachmentDto
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: String,
    val walletId: String,
    val type: TransactionType = TransactionType.UNKNOWN,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val categoryId: String? = null,
    val occurredOn: LocalDate? = null,
    val note: String? = null,
    val withPerson: String? = null,
    val excludeFromReport: Boolean = false,
    val transferId: String? = null,
    val counterpartWalletId: String? = null,
    @Serializable(with = MoneySerializer::class) val exchangeRate: BigDecimal? = null,
    val recurringId: String? = null,
    val source: String? = null,
    val sourceId: String? = null,
    val createdAt: Instant? = null,
    val attachments: List<AttachmentDto>? = null,
)

@Serializable
data class TransactionPageDto(
    val items: List<TransactionDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
)

@Serializable
data class TotalsDto(
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val income: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expense: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val net: BigDecimal? = null,
)

@Serializable
data class TransferResponseDto(
    val transferId: String? = null,
)

@Serializable
data class RecurringDto(
    val id: String,
    val walletId: String? = null,
    val categoryId: String? = null,
    val type: TransactionType = TransactionType.UNKNOWN,
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val currency: String? = null,
    val note: String? = null,
    val withPerson: String? = null,
    val frequency: Frequency = Frequency.UNKNOWN,
    val interval: Int = 1,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val nextOccurrence: LocalDate? = null,
    val generatedCount: Int = 0,
    val active: Boolean = true,
)
