package com.financemanage.mobile.core.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.RoundingMode

/** Rounds half away from zero to [digits] decimals (the backend's rounding for money). */
fun BigDecimal.roundToFractionDigits(digits: Int): BigDecimal =
    roundToDigitPositionAfterDecimalPoint(digits.toLong(), RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)

/** `true` when this value has no more than [digits] decimals (otherwise the backend answers `INVALID_AMOUNT`). */
fun BigDecimal.hasAtMostFractionDigits(digits: Int): Boolean = roundToFractionDigits(digits).compareTo(this) == 0

/**
 * Plain decimal string (no exponent) with exactly [digits] decimals, using `.` as the decimal separator.
 * `45000` with 0 digits -> `"45000"`; `12.5` with 2 digits -> `"12.50"`.
 */
fun BigDecimal.toPlainString(digits: Int): String {
    val rounded = roundToFractionDigits(digits)
    val expanded = rounded.toStringExpanded()
    val negative = expanded.startsWith("-")
    val unsigned = expanded.removePrefix("-")
    val integerPart = unsigned.substringBefore('.')
    val fractionPart = unsigned.substringAfter('.', missingDelimiterValue = "")
    val body = if (digits > 0) "$integerPart.${fractionPart.padEnd(digits, '0').take(digits)}" else integerPart
    val isZero = body.all { it == '0' || it == '.' }
    return if (negative && !isZero) "-$body" else body
}

/** Parses a plain decimal string ("45000", "12.50", "-3"). Returns `null` for anything else. */
fun String.toBigDecimalOrNull(): BigDecimal? {
    val text = trim()
    if (!DECIMAL_PATTERN.matches(text)) return null
    return runCatching { BigDecimal.parseString(text) }.getOrNull()
}

private val DECIMAL_PATTERN = Regex("^-?\\d+(\\.\\d+)?$")
