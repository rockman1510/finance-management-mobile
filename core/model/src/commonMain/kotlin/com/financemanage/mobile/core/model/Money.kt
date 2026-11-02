package com.financemanage.mobile.core.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal

/**
 * An exact amount of money in one currency. Never use `Double`/`Float` for money.
 */
data class Money(
    val amount: BigDecimal,
    val currency: CurrencyCode,
) {
    val isNegative: Boolean get() = amount.signum() < 0
    val isPositive: Boolean get() = amount.signum() > 0
    val isZero: Boolean get() = amount.isZero()

    /** Rounded to the currency's minor unit – what must be sent to the backend. */
    fun rounded(): Money = copy(amount = amount.roundToFractionDigits(currency.fractionDigits))

    /** Wire format with the currency's number of decimals: `45000` (VND), `12.50` (USD). */
    fun toPlainString(): String = amount.toPlainString(currency.fractionDigits)

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amount = amount + other.amount)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amount = amount - other.amount)
    }

    operator fun unaryMinus(): Money = copy(amount = amount.negate())

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
    }

    // BigDecimal.equals is scale-sensitive in some implementations; compare numerically.
    override fun equals(other: Any?): Boolean =
        other is Money && currency == other.currency && amount.compareTo(other.amount) == 0

    override fun hashCode(): Int = 31 * currency.hashCode() + amount.toPlainString(SCALE_FOR_HASH).hashCode()

    override fun toString(): String = "${toPlainString()} $currency"

    companion object {
        private const val SCALE_FOR_HASH = 8

        fun zero(currency: CurrencyCode): Money = Money(BigDecimal.ZERO, currency)

        /** Parses a plain decimal string, e.g. `Money.of("12.50", CurrencyCode.USD)`. */
        fun of(
            amount: String,
            currency: CurrencyCode,
        ): Money = Money(BigDecimal.parseString(amount), currency)

        fun of(
            amount: Long,
            currency: CurrencyCode,
        ): Money = Money(BigDecimal.fromLong(amount), currency)
    }
}
