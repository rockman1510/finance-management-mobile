package com.financemanage.mobile.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** ISO 4217 currency code, upper case (`VND`, `USD`). */
@Serializable
@JvmInline
value class CurrencyCode(
    val value: String,
) {
    /** Number of decimals of the currency's minor unit: VND 0, USD 2, KWD 3. */
    val fractionDigits: Int
        get() =
            when (value) {
                in ZERO_DECIMAL_CURRENCIES -> 0
                in THREE_DECIMAL_CURRENCIES -> 3
                else -> 2
            }

    override fun toString(): String = value

    companion object {
        val VND = CurrencyCode("VND")
        val USD = CurrencyCode("USD")

        /** The backend's default when a currency is omitted. */
        val DEFAULT = VND

        private val ZERO_DECIMAL_CURRENCIES =
            setOf(
                "BIF",
                "CLP",
                "DJF",
                "GNF",
                "ISK",
                "JPY",
                "KMF",
                "KRW",
                "PYG",
                "RWF",
                "UGX",
                "UYI",
                "VND",
                "VUV",
                "XAF",
                "XOF",
                "XPF",
            )
        private const val ISO_CODE_LENGTH = 3

        private val THREE_DECIMAL_CURRENCIES = setOf("BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND")

        /** Returns a code for a 3-letter input (any case), or `null` when it is not a valid ISO code shape. */
        fun parse(raw: String): CurrencyCode? {
            val code = raw.trim().uppercase()
            return if (code.length == ISO_CODE_LENGTH && code.all { it in 'A'..'Z' }) CurrencyCode(code) else null
        }
    }
}

/** Currencies offered in pickers (any ISO code can still be typed). VND first. */
val CommonCurrencies: List<CurrencyCode> =
    listOf(
        "VND",
        "USD",
        "EUR",
        "JPY",
        "KRW",
        "CNY",
        "SGD",
        "THB",
        "AUD",
        "GBP",
        "CAD",
        "CHF",
        "HKD",
        "TWD",
        "MYR",
        "IDR",
        "PHP",
        "INR",
        "NZD",
        "LAK",
        "KHR",
    ).map(::CurrencyCode)
