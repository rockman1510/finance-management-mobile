package com.financemanage.mobile.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun wireFormatUsesTheCurrencyDecimals() {
        assertEquals("45000", Money.of("45000", CurrencyCode.VND).toPlainString())
        assertEquals("12.50", Money.of("12.5", CurrencyCode.USD).toPlainString())
        assertEquals("1.000", Money.of("1", CurrencyCode("KWD")).toPlainString())
        assertEquals("0", Money.of("-0.4", CurrencyCode.VND).toPlainString())
    }

    @Test
    fun detectsTooManyDecimals() {
        assertTrue(Money.of("12.50", CurrencyCode.USD).amount.hasAtMostFractionDigits(2))
        assertFalse(Money.of("12.505", CurrencyCode.USD).amount.hasAtMostFractionDigits(2))
        assertFalse(Money.of("1.5", CurrencyCode.VND).amount.hasAtMostFractionDigits(0))
    }

    @Test
    fun equalityIgnoresScale() {
        assertEquals(Money.of("40", CurrencyCode.USD), Money.of("40.00", CurrencyCode.USD))
        assertEquals(Money.of("40", CurrencyCode.USD).hashCode(), Money.of("40.00", CurrencyCode.USD).hashCode())
    }

    @Test
    fun arithmeticRequiresTheSameCurrency() {
        assertEquals(Money.of("3", CurrencyCode.VND), Money.of("1", CurrencyCode.VND) + Money.of("2", CurrencyCode.VND))
        assertFailsWith<IllegalArgumentException> { Money.of("1", CurrencyCode.VND) + Money.of("1", CurrencyCode.USD) }
    }

    @Test
    fun parsesCurrencyCodes() {
        assertEquals(CurrencyCode.USD, CurrencyCode.parse(" usd "))
        assertNull(CurrencyCode.parse("US"))
        assertNull(CurrencyCode.parse("U5D"))
    }

    @Test
    fun parsesPlainDecimals() {
        assertEquals("12.50", "12.5".toBigDecimalOrNull()!!.toPlainString(2))
        assertNull("1e5".toBigDecimalOrNull())
        assertNull("abc".toBigDecimalOrNull())
    }
}
