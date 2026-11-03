package com.financemanage.mobile.core.data

import com.financemanage.mobile.core.data.cache.JsonCacheEntity
import com.financemanage.mobile.core.testing.FakeJsonCacheDao
import com.financemanage.mobile.core.testing.FixedClock
import com.financemanage.mobile.core.testing.testJsonCache
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonCacheTest {
    @Serializable
    data class Item(
        val id: String,
        val name: String,
    )

    private val serializer = ListSerializer(Item.serializer())

    @Test
    fun putThenGetAndObserveReturnTheValueAndItsTime() =
        runTest {
            val clock = FixedClock(Instant.parse("2026-11-01T08:30:00Z"))
            val cache = testJsonCache(clock = clock)

            cache.put("items", serializer, listOf(Item("1", "Tiền mặt")))

            val cached = cache.get("items", serializer)!!
            assertEquals(listOf(Item("1", "Tiền mặt")), cached.value)
            assertEquals(clock.now, cached.updatedAt)
            assertEquals(cached, cache.observe("items", serializer).first())
        }

    @Test
    fun corruptedRowsReadAsMissing() =
        runTest {
            val dao = FakeJsonCacheDao()
            dao.upsert(JsonCacheEntity("items", "{not json", 0))
            val cache = testJsonCache(dao)

            assertNull(cache.get("items", serializer))
            assertNull(cache.observe("items", serializer).first())
        }

    @Test
    fun removeByPrefixOnlyDropsMatchingKeys() =
        runTest {
            val cache = testJsonCache()
            cache.put("budget:1", serializer, emptyList())
            cache.put("budget:2", serializer, emptyList())
            cache.put("bills", serializer, emptyList())

            cache.removeByPrefix("budget:")

            assertNull(cache.get("budget:1", serializer))
            assertNull(cache.get("budget:2", serializer))
            assertEquals(emptyList(), cache.get("bills", serializer)?.value)

            cache.clear()
            assertNull(cache.get("bills", serializer))
        }
}
