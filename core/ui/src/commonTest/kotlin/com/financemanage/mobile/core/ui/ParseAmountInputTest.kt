package com.financemanage.mobile.core.ui

import com.financemanage.mobile.core.ui.components.parseAmountInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseAmountInputTest {
    private fun parse(
        text: String,
        digits: Int,
    ) = parseAmountInput(text, digits)?.toStringExpanded()

    @Test
    fun vndIgnoresEverySeparator() {
        assertEquals("1250000", parse("1.250.000", 0))
        assertEquals("1250000", parse(" 1250000 ", 0))
        assertEquals("1250000", parse("1,250,000", 0))
    }

    @Test
    fun usdUsesTheLastSeparatorAsDecimalWhenItFits() {
        assertEquals("12.5", parse("12,5", 2))
        assertEquals("12.5", parse("12.50", 2))
        assertEquals("1250.75", parse("1.250,75", 2))
        assertEquals("-120.5", parse("-120,50", 2))
    }

    @Test
    fun usdTreatsAThreeDigitTailAsGrouping() {
        assertEquals("2000", parse("2.000", 2))
        assertEquals("1250000", parse("1.250.000", 2))
    }

    @Test
    fun rejectsTextAndTooManyDecimals() {
        assertNull(parse("", 2))
        assertNull(parse("abc", 0))
        assertNull(parse("1,2,3x", 2))
        assertEquals("1.234", parse("1,234", 3))
    }
}
