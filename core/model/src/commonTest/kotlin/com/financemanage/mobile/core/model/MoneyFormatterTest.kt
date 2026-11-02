package com.financemanage.mobile.core.model

import com.financemanage.mobile.core.model.format.MoneyFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatterTest {
    private val nbsp = MoneyFormatter.SPACE

    @Test
    fun formatsVndWithoutDecimals() {
        assertEquals("1.250.000$nbsp₫", MoneyFormatter.format(Money.of("1250000", CurrencyCode.VND)))
        assertEquals("0$nbsp₫", MoneyFormatter.format(Money.of("0", CurrencyCode.VND)))
        assertEquals("999$nbsp₫", MoneyFormatter.format(Money.of("999", CurrencyCode.VND)))
        assertEquals("1.000$nbsp₫", MoneyFormatter.format(Money.of("1000", CurrencyCode.VND)))
    }

    @Test
    fun formatsUsdWithTwoDecimals() {
        assertEquals("12,50${nbsp}US$", MoneyFormatter.format(Money.of("12.5", CurrencyCode.USD)))
        assertEquals("1.234.567,89${nbsp}US$", MoneyFormatter.format(Money.of("1234567.89", CurrencyCode.USD)))
    }

    @Test
    fun formatsNegativeAndPlusSign() {
        assertEquals("-45.000$nbsp₫", MoneyFormatter.format(Money.of("-45000", CurrencyCode.VND)))
        assertEquals("+45.000$nbsp₫", MoneyFormatter.format(Money.of("45000", CurrencyCode.VND), showPlusSign = true))
        assertEquals("0$nbsp₫", MoneyFormatter.format(Money.of("0", CurrencyCode.VND), showPlusSign = true))
    }

    @Test
    fun roundsHalfAwayFromZeroToTheMinorUnit() {
        assertEquals("1.001$nbsp₫", MoneyFormatter.format(Money.of("1000.5", CurrencyCode.VND)))
        assertEquals("-1.001$nbsp₫", MoneyFormatter.format(Money.of("-1000.5", CurrencyCode.VND)))
        assertEquals("0,01${nbsp}US$", MoneyFormatter.format(Money.of("0.005", CurrencyCode.USD)))
    }

    @Test
    fun hidesAmounts() {
        assertEquals(
            "${MoneyFormatter.HIDDEN}$nbsp₫",
            MoneyFormatter.format(Money.of("1", CurrencyCode.VND), hidden = true),
        )
    }

    @Test
    fun unknownCurrencyUsesItsCode() {
        assertEquals("10,00${nbsp}CHF", MoneyFormatter.format(Money.of("10", CurrencyCode("CHF"))))
    }
}
