package com.financemanage.mobile.core.network

import io.ktor.client.request.post
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdempotencyKeyTest {
    @Test
    fun holderKeepsTheKeyUntilReset() {
        var counter = 0
        val holder = IdempotencyKeyHolder { IdempotencyKey("key-${++counter}") }
        val first = holder.current()
        assertEquals(first, holder.current(), "retries reuse the key")
        holder.reset()
        assertNotEquals(first, holder.current())
    }

    @Test
    fun generatedKeysAreUuids() {
        val key = IdempotencyKey.generate().value
        assertEquals(36, key.length)
        assertNotEquals(key, IdempotencyKey.generate().value)
    }

    @Test
    fun headerIsSent() =
        runTest {
            val sent = mutableListOf<String?>()
            val client =
                testClient { request ->
                    sent += request.headers[IDEMPOTENCY_KEY_HEADER]
                    respondJson("{}")
                }
            val key = IdempotencyKey("3f1c0c1e-2b2a-4b8e-9b51-0d0f5f2f9a11")
            repeat(2) { client.post(api("transactions")) { idempotencyKey(key) } }
            assertEquals<List<String?>>(listOf(key.value, key.value), sent)
        }
}
