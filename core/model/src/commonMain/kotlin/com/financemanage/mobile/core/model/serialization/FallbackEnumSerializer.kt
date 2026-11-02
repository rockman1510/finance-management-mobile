package com.financemanage.mobile.core.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Enum serializer that maps values unknown to this app version to [fallback] (usually `UNKNOWN`) instead of
 * failing, so older apps keep working when the backend adds a value.
 *
 * ```
 * @Serializable(with = WalletType.Serializer::class)
 * enum class WalletType { CASH, …, UNKNOWN;
 *     internal object Serializer : FallbackEnumSerializer<WalletType>("WalletType", entries, UNKNOWN)
 * }
 * ```
 */
open class FallbackEnumSerializer<E : Enum<E>>(
    serialName: String,
    private val values: List<E>,
    private val fallback: E,
) : KSerializer<E> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: E,
    ) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): E {
        val raw = decoder.decodeString()
        return values.firstOrNull { it.name == raw } ?: fallback
    }
}
