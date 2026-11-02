package com.financemanage.mobile.core.model.domain

import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.serialization.FallbackEnumSerializer
import kotlinx.serialization.Serializable

/** The signed-in user's profile (API §4.2). */
data class Profile(
    val id: String,
    val email: String,
    val fullName: String,
    val emailVerified: Boolean,
    val hasPassword: Boolean,
    val googleLinked: Boolean,
    val twoFactorEnabled: Boolean,
    val defaultCurrency: CurrencyCode,
    val locale: String,
    val timeZone: String,
    val monthStartDay: Int,
)

@Serializable(with = WalletType.Serializer::class)
enum class WalletType {
    CASH,
    BANK_ACCOUNT,
    E_WALLET,
    CREDIT_CARD,
    OTHER,
    UNKNOWN,
    ;

    internal object Serializer : FallbackEnumSerializer<WalletType>("WalletType", WalletType.entries, UNKNOWN)
}

/** A wallet (API §5.1). Balances may be negative. */
data class Wallet(
    val id: String,
    val name: String,
    val type: WalletType,
    val currency: CurrencyCode,
    val initialBalance: Money,
    val balance: Money,
    val excludedFromTotal: Boolean,
    val archived: Boolean,
    val icon: String?,
    val creditLimit: Money?,
    val availableCredit: Money?,
    val statementDay: Int?,
    val paymentDueDay: Int?,
)

@Serializable(with = CategoryType.Serializer::class)
enum class CategoryType {
    INCOME,
    EXPENSE,
    UNKNOWN,
    ;

    internal object Serializer : FallbackEnumSerializer<CategoryType>("CategoryType", CategoryType.entries, UNKNOWN)
}

/** A category (API §5.2). Default categories have a fixed [code]; custom ones have `null`. */
data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val parentId: String?,
    val icon: String?,
    val code: String?,
)

@Serializable(with = TransactionType.Serializer::class)
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER_IN,
    TRANSFER_OUT,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT,
    EXTERNAL_IN,
    EXTERNAL_OUT,
    UNKNOWN,
    ;

    /** Money entering the wallet. */
    val isInflow: Boolean get() = this == INCOME || this == TRANSFER_IN || this == ADJUSTMENT_IN || this == EXTERNAL_IN

    internal object Serializer :
        FallbackEnumSerializer<TransactionType>("TransactionType", TransactionType.entries, UNKNOWN)
}

@Serializable(with = Frequency.Serializer::class)
enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    YEARLY,
    UNKNOWN,
    ;

    internal object Serializer : FallbackEnumSerializer<Frequency>("Frequency", Frequency.entries, UNKNOWN)
}

/** An exchange rate: 1 [base] = [rate] [quote] (API §6). */
data class ExchangeRate(
    val id: String,
    val base: CurrencyCode,
    val quote: CurrencyCode,
    val rate: com.ionspin.kotlin.bignum.decimal.BigDecimal,
    val effectiveDate: kotlinx.datetime.LocalDate?,
    val global: Boolean,
)
