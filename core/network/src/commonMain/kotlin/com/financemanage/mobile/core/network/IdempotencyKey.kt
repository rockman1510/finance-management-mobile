package com.financemanage.mobile.core.network

import com.financemanage.mobile.core.model.IdempotencyKey
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

/** Sends [key] as the `Idempotency-Key` header (API §2). */
fun HttpRequestBuilder.idempotencyKey(key: IdempotencyKey) {
    header(IDEMPOTENCY_KEY_HEADER, key.value)
}
