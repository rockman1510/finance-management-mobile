package com.financemanage.mobile.core.model

import com.financemanage.mobile.core.model.serialization.MoneySerializer
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneySerializerTest {
    @Serializable
    private data class Payload(
        @Serializable(with = MoneySerializer::class) val amount: BigDecimal,
        @Serializable(with = MoneySerializer::class) val percent: BigDecimal? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun keepsTrailingZeroDecimalValue() {
        val payload = json.decodeFromString<Payload>("""{"amount":40.00}""")
        assertTrue(payload.amount.compareTo(BigDecimal.parseString("40")) == 0)
        assertEquals("40.00", payload.amount.toPlainString(2))
    }

    @Test
    fun keepsExactDecimals() {
        val payload = json.decodeFromString<Payload>("""{"amount":845000,"percent":94.67}""")
        assertEquals("845000", payload.amount.toPlainString(0))
        assertEquals("94.67", payload.percent!!.toStringExpanded())
    }

    @Test
    fun doesNotLosePrecisionOnHugeNumbers() {
        val payload = json.decodeFromString<Payload>("""{"amount":12345678901234567.89}""")
        assertEquals("12345678901234567.89", payload.amount.toStringExpanded())
    }

    @Test
    fun writesAnUnquotedNumber() {
        val text = json.encodeToString(Payload.serializer(), Payload(BigDecimal.parseString("0.1")))
        assertEquals("""{"amount":0.1}""", text)
    }

    @Test
    fun roundTrips() {
        val original = Payload(BigDecimal.parseString("-1250000.5"), BigDecimal.parseString("100"))
        val decoded = json.decodeFromString<Payload>(json.encodeToString(Payload.serializer(), original))
        assertTrue(original.amount.compareTo(decoded.amount) == 0)
        assertTrue(original.percent!!.compareTo(decoded.percent!!) == 0)
    }
}
