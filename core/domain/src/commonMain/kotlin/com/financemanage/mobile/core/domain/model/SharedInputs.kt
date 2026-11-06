package com.financemanage.mobile.core.domain.model

import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// Inputs and results of the shared repositories. They mirror the API 1:1, so the data layer sends / reads them
// as they are (no extra mapping layer).

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val defaultCurrency: String? = null,
    val locale: String? = null,
    val timeZone: String? = null,
    val monthStartDay: Int? = null,
)

@Serializable
data class CreateWalletRequest(
    val name: String,
    val type: WalletType,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val initialBalance: BigDecimal? = null,
    val icon: String? = null,
    val excludedFromTotal: Boolean? = null,
    @Serializable(with = MoneySerializer::class) val creditLimit: BigDecimal? = null,
    val statementDay: Int? = null,
    val paymentDueDay: Int? = null,
)

@Serializable
data class UpdateWalletRequest(
    val name: String? = null,
    val icon: String? = null,
    val excludedFromTotal: Boolean? = null,
    val archived: Boolean? = null,
    @Serializable(with = MoneySerializer::class) val initialBalance: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val creditLimit: BigDecimal? = null,
    val statementDay: Int? = null,
    val paymentDueDay: Int? = null,
)

@Serializable
data class AdjustBalanceRequest(
    @Serializable(with = MoneySerializer::class) val newBalance: BigDecimal,
    val occurredOn: LocalDate? = null,
    val note: String? = null,
)

/** Total of the wallets converted to one currency (API §5.1). */
data class WalletTotal(
    val total: Money,
    val missingRates: List<String>,
)

@Serializable
data class ConversionDto(
    @Serializable(with = MoneySerializer::class) val amount: BigDecimal? = null,
    val from: String? = null,
    val to: String? = null,
    @Serializable(with = MoneySerializer::class) val rate: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val converted: BigDecimal? = null,
    val date: LocalDate? = null,
)
