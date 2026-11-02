package com.financemanage.mobile.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncStatusTest {
    @Serializable
    private data class Payment(
        val status: AsyncStatus,
    )

    @Test
    fun knownValuesAreDecoded() {
        assertEquals(AsyncStatus.RECORDED, Json.decodeFromString<Payment>("""{"status":"RECORDED"}""").status)
    }

    @Test
    fun unknownValuesFallBack() {
        assertEquals(AsyncStatus.UNKNOWN, Json.decodeFromString<Payment>("""{"status":"REVERSED"}""").status)
    }
}
