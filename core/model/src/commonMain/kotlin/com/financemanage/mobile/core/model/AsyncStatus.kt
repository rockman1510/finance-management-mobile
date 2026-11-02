package com.financemanage.mobile.core.model

import com.financemanage.mobile.core.model.serialization.FallbackEnumSerializer
import kotlinx.serialization.Serializable

/**
 * Wallet side of an asynchronous action (API §2.1): bill payments, saving movements, deposit funding/payout
 * and investment `cashStatus`.
 */
@Serializable(with = AsyncStatus.Serializer::class)
enum class AsyncStatus {
    NONE,
    PENDING,
    RECORDED,
    FAILED,
    SKIPPED,
    UNKNOWN,
    ;

    val isPending: Boolean get() = this == PENDING

    internal object Serializer : FallbackEnumSerializer<AsyncStatus>("AsyncStatus", AsyncStatus.entries, UNKNOWN)
}
