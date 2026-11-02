package com.financemanage.mobile.core.model.serialization

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

/**
 * Exact (de)serialization of JSON numbers as [BigDecimal].
 *
 * Reads the **raw text** of the JSON number (never through `Double`) and writes it back as an unquoted
 * literal, so `40.00`, `94.67` or `12345678901234567.89` keep their exact value.
 *
 * Usage: `val amount: @Serializable(with = MoneySerializer::class) BigDecimal`.
 */
object MoneySerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.financemanage.mobile.BigDecimal", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val raw =
            if (decoder is JsonDecoder) {
                val element = decoder.decodeJsonElement()
                (element as? JsonPrimitive)?.content ?: throw SerializationException("Expected a number, got $element")
            } else {
                decoder.decodeString()
            }
        return try {
            BigDecimal.parseString(raw.trim())
        } catch (e: NumberFormatException) {
            throw SerializationException("Invalid decimal '$raw'", e)
        } catch (e: ArithmeticException) {
            throw SerializationException("Invalid decimal '$raw'", e)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        encoder: Encoder,
        value: BigDecimal,
    ) {
        val text = value.toStringExpanded()
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonUnquotedLiteral(text))
        } else {
            encoder.encodeString(text)
        }
    }
}
