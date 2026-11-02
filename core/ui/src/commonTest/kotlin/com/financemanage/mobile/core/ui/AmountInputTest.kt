package com.financemanage.mobile.core.ui

import com.financemanage.mobile.core.model.toPlainString
import com.financemanage.mobile.core.ui.components.AmountInput
import com.financemanage.mobile.core.ui.components.KeypadKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmountInputTest {
    private fun type(
        vararg keys: KeypadKey,
        maxFractionDigits: Int = 0,
    ): AmountInput = keys.fold(AmountInput(maxFractionDigits = maxFractionDigits)) { input, key -> input.press(key) }

    private fun digits(text: String): Array<KeypadKey> =
        text
            .map {
                when (it) {
                    ',' -> KeypadKey.Decimal
                    else -> KeypadKey.Digit(it.digitToInt())
                }
            }.toTypedArray()

    @Test
    fun groupsThousands() {
        val input = type(*digits("1250000"))
        assertEquals("1.250.000", input.display)
        assertEquals("1250000", input.value.toPlainString(0))
    }

    @Test
    fun tripleZeroKey() {
        val input = type(KeypadKey.Digit(4), KeypadKey.Digit(5), KeypadKey.TripleZero)
        assertEquals("45.000", input.display)
    }

    @Test
    fun tripleZeroOnEmptyDoesNothing() {
        assertTrue(type(KeypadKey.TripleZero).isEmpty)
    }

    @Test
    fun leadingZerosAreDropped() {
        assertEquals("5", type(KeypadKey.Digit(0), KeypadKey.Digit(0), KeypadKey.Digit(5)).display)
    }

    @Test
    fun backspace() {
        val input = type(*digits("123"), KeypadKey.Backspace)
        assertEquals("12", input.display)
        assertEquals("0", type(*digits("7"), KeypadKey.Backspace).display)
        assertTrue(type(*digits("7"), KeypadKey.Backspace).isEmpty)
    }

    @Test
    fun decimalsAreIgnoredForVnd() {
        assertEquals("125", type(*digits("12,5")).display)
    }

    @Test
    fun decimalsAreLimitedToTheCurrency() {
        val input = type(*digits("12,505"), maxFractionDigits = 2)
        assertEquals("12,50", input.display)
        assertEquals("12.50", input.value.toPlainString(2))
    }

    @Test
    fun decimalFirstStartsWithZero() {
        assertEquals("0,", type(KeypadKey.Decimal, maxFractionDigits = 2).display)
    }

    @Test
    fun integerDigitsAreCapped() {
        val input = type(*digits("1".repeat(20)))
        assertEquals(AmountInput.MAX_INTEGER_DIGITS, input.raw.length)
    }

    @Test
    fun clear() {
        assertTrue(type(*digits("123"), KeypadKey.Clear).isEmpty)
    }
}
