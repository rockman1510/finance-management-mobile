package com.financemanage.mobile.feature.deposit.domain.model

import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DepositTxDto(
    val id: String,
    val type: String = "",
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val occurredOn: LocalDate? = null,
)

@Serializable
data class DepositDto(
    val id: String,
    val institutionName: String = "",
    val accountNumber: String? = null,
    val type: String = "TERM",
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal? = null,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val annualRate: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val earlyWithdrawalRate: BigDecimal? = null,
    val termMonths: Int? = null,
    val openDate: LocalDate? = null,
    val maturityDate: LocalDate? = null,
    val interestPaymentMethod: String? = null,
    val rolloverPolicy: String? = null,
    val calculationMethod: String? = null,
    val status: String = "ACTIVE",
    val note: String? = null,
    val settledOn: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val settledAmount: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val accruedInterest: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val expectedInterestAtMaturity: BigDecimal? = null,
    val transactions: List<DepositTxDto> = emptyList(),
    val fundingWalletId: String? = null,
    val fundingStatus: AsyncStatus? = null,
    val payoutWalletId: String? = null,
    val payoutStatus: AsyncStatus? = null,
) {
    val currencyCode: CurrencyCode get() = currency?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

    fun money(value: BigDecimal?) = Money(value ?: BigDecimal.ZERO, currencyCode)

    val anyPending: Boolean get() = fundingStatus == AsyncStatus.PENDING || payoutStatus == AsyncStatus.PENDING
}

@Serializable
data class CreateDepositRequest(
    val institutionName: String,
    val accountNumber: String? = null,
    val type: String,
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val annualRate: BigDecimal,
    @Serializable(with = MoneySerializer::class) val earlyWithdrawalRate: BigDecimal? = null,
    val termMonths: Int? = null,
    val openDate: LocalDate,
    val interestPaymentMethod: String? = null,
    val rolloverPolicy: String? = null,
    val calculationMethod: String? = null,
    val note: String? = null,
    val fromWalletId: String? = null,
)

@Serializable
data class DepositPreviewDto(
    val maturityDate: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val expectedInterest: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val totalAtMaturity: BigDecimal? = null,
    val currency: String? = null,
)

@Serializable
data class SettlementQuoteDto(
    val date: LocalDate? = null,
    @Serializable(with = MoneySerializer::class) val principal: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val interest: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val total: BigDecimal? = null,
    val early: Boolean = false,
    @Serializable(with = MoneySerializer::class) val interestLostByEarlyWithdrawal: BigDecimal? = null,
    val currency: String? = null,
)
