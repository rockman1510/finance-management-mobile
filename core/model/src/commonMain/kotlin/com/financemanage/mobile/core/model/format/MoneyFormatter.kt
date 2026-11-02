package com.financemanage.mobile.core.model.format

import com.financemanage.mobile.core.model.CurrencyCode
import com.financemanage.mobile.core.model.Money
import com.financemanage.mobile.core.model.toPlainString
import com.ionspin.kotlin.bignum.decimal.BigDecimal

/**
 * Formats money with the `vi-VN` conventions: `.` groups thousands, `,` separates decimals and the symbol
 * follows the amount – `1.250.000 ₫`, `12,50 US$`, `-45.000 ₫`.
 */
object MoneyFormatter {
    /** Shown instead of amounts when the user hides balances. */
    const val HIDDEN = "••••••"

    /** Non-breaking space so that the symbol never wraps onto its own line. */
    const val SPACE = ' '

    private const val GROUP_SEPARATOR = '.'
    private const val DECIMAL_SEPARATOR = ','
    private const val GROUP_SIZE = 3

    private val SYMBOLS =
        mapOf(
            "VND" to "₫",
            "USD" to "US$",
            "EUR" to "€",
            "GBP" to "£",
            "JPY" to "JP¥",
            "CNY" to "CN¥",
            "KRW" to "₩",
            "AUD" to "AU$",
            "CAD" to "CA$",
            "SGD" to "SGD",
            "THB" to "THB",
        )

    fun symbol(currency: CurrencyCode): String = SYMBOLS[currency.value] ?: currency.value

    /**
     * @param hidden replace the amount by [HIDDEN].
     * @param showPlusSign prefix positive amounts with `+` (income rows).
     */
    fun format(
        money: Money,
        hidden: Boolean = false,
        showPlusSign: Boolean = false,
    ): String {
        if (hidden) return "$HIDDEN$SPACE${symbol(money.currency)}"
        val sign =
            when {
                money.isNegative -> "-"
                showPlusSign && money.isPositive -> "+"
                else -> ""
            }
        val number = formatNumber(money.amount.abs(), money.currency.fractionDigits)
        return "$sign$number$SPACE${symbol(money.currency)}"
    }

    /** Only the number, without symbol: `1.250.000`, `12,50`. */
    fun formatNumber(
        amount: BigDecimal,
        fractionDigits: Int,
    ): String {
        val plain = amount.toPlainString(fractionDigits)
        val negative = plain.startsWith("-")
        val unsigned = plain.removePrefix("-")
        val integerPart = groupThousands(unsigned.substringBefore('.'))
        val fractionPart = unsigned.substringAfter('.', missingDelimiterValue = "")
        val body = if (fractionPart.isEmpty()) integerPart else "$integerPart$DECIMAL_SEPARATOR$fractionPart"
        return if (negative) "-$body" else body
    }

    /** `"1250000"` -> `"1.250.000"`. */
    fun groupThousands(digits: String): String {
        if (digits.length <= GROUP_SIZE) return digits
        val builder = StringBuilder()
        digits.forEachIndexed { index, c ->
            if (index > 0 && (digits.length - index) % GROUP_SIZE == 0) builder.append(GROUP_SEPARATOR)
            builder.append(c)
        }
        return builder.toString()
    }
}
