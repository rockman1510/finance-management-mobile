package com.financemanage.mobile.core.ui.components

import com.financemanage.mobile.core.model.format.MoneyFormatter
import com.ionspin.kotlin.bignum.decimal.BigDecimal

private val DIGIT_RANGE = 0..9

/** A key of [AmountKeypad]. */
sealed interface KeypadKey {
    data class Digit(
        val value: Int,
    ) : KeypadKey {
        init {
            require(value in DIGIT_RANGE)
        }
    }

    data object TripleZero : KeypadKey

    data object Decimal : KeypadKey

    data object Backspace : KeypadKey

    data object Clear : KeypadKey
}

/**
 * Text typed on the amount keypad, kept as a plain string (`"12500"`, `"12.5"`) – never a floating point.
 *
 * @param maxFractionDigits the currency's decimals (VND 0, USD 2).
 */
data class AmountInput(
    val raw: String = "",
    val maxFractionDigits: Int = 0,
) {
    private val hasDecimal: Boolean get() = raw.contains('.')
    private val integerPart: String get() = raw.substringBefore('.')
    private val fractionPart: String get() = raw.substringAfter('.', missingDelimiterValue = "")

    val isEmpty: Boolean get() = raw.isEmpty()

    /** The exact value (`0` when empty). */
    val value: BigDecimal
        get() = raw.trimEnd('.').takeIf { it.isNotEmpty() }?.let { BigDecimal.parseString(it) } ?: BigDecimal.ZERO

    /** What the keypad screen shows: `1.250.000`, `12,5`, `0,`. */
    val display: String
        get() {
            val grouped = MoneyFormatter.groupThousands(integerPart.ifEmpty { "0" })
            return if (hasDecimal) "$grouped,$fractionPart" else grouped
        }

    fun press(key: KeypadKey): AmountInput =
        when (key) {
            is KeypadKey.Digit -> appendDigits(key.value.toString())
            KeypadKey.TripleZero -> if (isEmpty) this else appendDigits("000")
            KeypadKey.Decimal ->
                when {
                    maxFractionDigits == 0 || hasDecimal -> this
                    isEmpty -> copy(raw = "0.")
                    else -> copy(raw = "$raw.")
                }
            KeypadKey.Backspace -> copy(raw = raw.dropLast(1).let { if (it == "0") "" else it })
            KeypadKey.Clear -> copy(raw = "")
        }

    private fun appendDigits(digits: String): AmountInput {
        var result = this
        for (digit in digits) result = result.appendDigit(digit)
        return result
    }

    private fun appendDigit(digit: Char): AmountInput =
        when {
            hasDecimal -> if (fractionPart.length < maxFractionDigits) copy(raw = raw + digit) else this
            integerPart == "0" -> copy(raw = digit.toString())
            integerPart.isEmpty() && digit == '0' -> copy(raw = "0")
            integerPart.length >= MAX_INTEGER_DIGITS -> this
            else -> copy(raw = raw + digit)
        }

    companion object {
        /** 999.999.999.999.999 – far above any personal amount, well below precision issues. */
        const val MAX_INTEGER_DIGITS = 15
    }
}
