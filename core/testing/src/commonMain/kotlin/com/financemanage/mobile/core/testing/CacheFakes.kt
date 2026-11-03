package com.financemanage.mobile.core.testing

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.data.cache.JsonCacheDao
import com.financemanage.mobile.core.data.cache.JsonCacheEntity
import com.financemanage.mobile.core.data.outbox.OutboxDao
import com.financemanage.mobile.core.data.outbox.OutboxEntity
import com.financemanage.mobile.core.data.outbox.OutboxStatus
import com.financemanage.mobile.core.network.NetworkJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** In-memory replacement for the Room `json_cache` table. */
class FakeJsonCacheDao : JsonCacheDao {
    val rows = MutableStateFlow<Map<String, JsonCacheEntity>>(emptyMap())

    override fun observe(cacheKey: String): Flow<JsonCacheEntity?> = rows.map { it[cacheKey] }

    override suspend fun get(cacheKey: String): JsonCacheEntity? = rows.value[cacheKey]

    override suspend fun upsert(entity: JsonCacheEntity) {
        rows.value = rows.value + (entity.cacheKey to entity)
    }

    override suspend fun delete(cacheKey: String) {
        rows.value = rows.value - cacheKey
    }

    override suspend fun deleteByPrefix(prefix: String) {
        rows.value = rows.value.filterKeys { !it.startsWith(prefix) }
    }

    override suspend fun clear() {
        rows.value = emptyMap()
    }
}

/** In-memory replacement for the Room `outbox` table. */
class FakeOutboxDao : OutboxDao {
    val rows = MutableStateFlow<List<OutboxEntity>>(emptyList())

    override fun observeAll(): Flow<List<OutboxEntity>> = rows.map { list -> list.sortedBy { it.createdAtEpochMillis } }

    override suspend fun waiting(): List<OutboxEntity> =
        rows.value.filter { it.status == OutboxStatus.WAITING.name }.sortedBy { it.createdAtEpochMillis }

    override suspend fun upsert(entity: OutboxEntity) {
        val index = rows.value.indexOfFirst { it.id == entity.id }
        rows.value =
            if (index < 0) {
                rows.value + entity
            } else {
                rows.value.toMutableList().also { it[index] = entity }
            }
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }
}

/** A clock tests can move. */
class FixedClock(
    var now: Instant = Instant.parse("2026-11-01T10:15:00Z"),
) : Clock {
    override fun now(): Instant = now
}

/** A [JsonCache] backed by [FakeJsonCacheDao] and the app's JSON configuration. */
fun testJsonCache(
    dao: FakeJsonCacheDao = FakeJsonCacheDao(),
    clock: Clock = FixedClock(),
): JsonCache = JsonCache(dao, NetworkJson, clock)

/**
 * Waits (in real time, max 5 s) until [condition] holds. Ktor's engine and Room run on their own dispatchers, so work
 * launched in a `viewModelScope` may finish after the test's virtual time has advanced.
 */
suspend fun awaitCondition(
    message: String = "condition",
    condition: () -> Boolean,
) {
    withContext(Dispatchers.Default) {
        withTimeoutOrNull(AWAIT_TIMEOUT_MS) {
            while (!condition()) delay(AWAIT_STEP_MS)
        } ?: error("Timed out waiting for $message")
    }
}

private const val AWAIT_TIMEOUT_MS = 5_000L
private const val AWAIT_STEP_MS = 5L
