package com.financemanage.mobile.core.model

import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

/**
 * Sent with `POST /transactions`, `/transfers`, `/loans/{id}/repayments`, `/loans/{id}/prepayments` (API §2).
 * Create one when the user taps Save and reuse it for every retry of that same action.
 */
@JvmInline
value class IdempotencyKey(
    val value: String,
) {
    companion object {
        fun generate(): IdempotencyKey = IdempotencyKey(Uuid.random().toString())
    }
}

/**
 * Keeps the key of the action in progress (one per form / ViewModel).
 * [current] returns the same key until [reset] is called after a success or when the user edits the form.
 */
class IdempotencyKeyHolder(
    private val generator: () -> IdempotencyKey = IdempotencyKey::generate,
) {
    private var key: IdempotencyKey? = null

    fun current(): IdempotencyKey = key ?: generator().also { key = it }

    fun reset() {
        key = null
    }
}
