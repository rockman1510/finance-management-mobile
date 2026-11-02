package com.financemanage.mobile.core.data.shared

import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.domain.Category
import com.financemanage.mobile.core.model.domain.CategoryType
import com.financemanage.mobile.core.model.domain.ExchangeRate
import com.financemanage.mobile.core.model.domain.Profile
import com.financemanage.mobile.core.model.domain.Wallet
import com.financemanage.mobile.core.model.domain.WalletType
import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

internal fun currencyOf(code: String?): CurrencyCode = code?.let { CurrencyCode.parse(it) } ?: CurrencyCode.DEFAULT

internal fun money(
    amount: BigDecimal?,
    currency: CurrencyCode,
): Money = Money(amount ?: BigDecimal.ZERO, currency)

@Serializable
data class ProfileDto(
    val id: String,
    val email: String = "",
    val fullName: String? = null,
    val emailVerified: Boolean = false,
    val hasPassword: Boolean = true,
    val googleLinked: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val defaultCurrency: String? = null,
    val locale: String? = null,
    val timeZone: String? = null,
    val monthStartDay: Int? = null,
) {
    fun toDomain() =
        Profile(
            id = id,
            email = email,
            fullName = fullName.orEmpty(),
            emailVerified = emailVerified,
            hasPassword = hasPassword,
            googleLinked = googleLinked,
            twoFactorEnabled = twoFactorEnabled,
            defaultCurrency = currencyOf(defaultCurrency),
            locale = locale ?: "vi",
            timeZone = timeZone ?: "Asia/Ho_Chi_Minh",
            monthStartDay = monthStartDay ?: 1,
        )
}

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val defaultCurrency: String? = null,
    val locale: String? = null,
    val timeZone: String? = null,
    val monthStartDay: Int? = null,
)

@Serializable
data class WalletDto(
    val id: String,
    val name: String = "",
    val type: WalletType = WalletType.UNKNOWN,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val initialBalance: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val balance: BigDecimal? = null,
    val excludedFromTotal: Boolean = false,
    val archived: Boolean = false,
    val icon: String? = null,
    @Serializable(with = MoneySerializer::class) val creditLimit: BigDecimal? = null,
    @Serializable(with = MoneySerializer::class) val availableCredit: BigDecimal? = null,
    val statementDay: Int? = null,
    val paymentDueDay: Int? = null,
) {
    fun toDomain(): Wallet {
        val currency = currencyOf(currency)
        return Wallet(
            id = id,
            name = name,
            type = type,
            currency = currency,
            initialBalance = money(initialBalance, currency),
            balance = money(balance, currency),
            excludedFromTotal = excludedFromTotal,
            archived = archived,
            icon = icon,
            creditLimit = creditLimit?.let { Money(it, currency) },
            availableCredit = availableCredit?.let { Money(it, currency) },
            statementDay = statementDay,
            paymentDueDay = paymentDueDay,
        )
    }
}

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

@Serializable
data class WalletTotalLineDto(
    val walletId: String,
    val name: String = "",
    @Serializable(with = MoneySerializer::class) val balance: BigDecimal? = null,
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val convertedBalance: BigDecimal? = null,
)

@Serializable
data class WalletTotalDto(
    val currency: String? = null,
    @Serializable(with = MoneySerializer::class) val total: BigDecimal? = null,
    val wallets: List<WalletTotalLineDto> = emptyList(),
    val missingRates: List<String> = emptyList(),
)

/** Total of the wallets converted to one currency (API §5.1). */
data class WalletTotal(
    val total: Money,
    val missingRates: List<String>,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String = "",
    val type: CategoryType = CategoryType.UNKNOWN,
    val parentId: String? = null,
    val icon: String? = null,
    val code: String? = null,
) {
    fun toDomain() = Category(id, name, type, parentId, icon, code)
}

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val type: CategoryType,
    val parentId: String? = null,
    val icon: String? = null,
)

@Serializable
data class UpdateCategoryRequest(
    val name: String? = null,
    val icon: String? = null,
)

@Serializable
data class ExchangeRateDto(
    val id: String,
    val base: String,
    val quote: String,
    @Serializable(with = MoneySerializer::class) val rate: BigDecimal,
    val effectiveDate: LocalDate? = null,
    val source: String? = null,
    val global: Boolean = false,
) {
    fun toDomain() = ExchangeRate(id, currencyOf(base), currencyOf(quote), rate, effectiveDate, global)
}

@Serializable
data class SaveExchangeRateRequest(
    val base: String,
    val quote: String,
    @Serializable(with = MoneySerializer::class) val rate: BigDecimal,
    val effectiveDate: LocalDate? = null,
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
