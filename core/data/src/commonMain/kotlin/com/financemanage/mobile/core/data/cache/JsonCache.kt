package com.financemanage.mobile.core.data.cache

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Last known server response for a resource, stored as JSON. This is the single source of truth the screens
 * observe: they show it at once (offline included) and repositories overwrite it after each refresh.
 */
@Entity(tableName = "json_cache")
data class JsonCacheEntity(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val updatedAtEpochMillis: Long,
)

@Dao
interface JsonCacheDao {
    @Query("SELECT * FROM json_cache WHERE cacheKey = :cacheKey")
    fun observe(cacheKey: String): Flow<JsonCacheEntity?>

    @Query("SELECT * FROM json_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): JsonCacheEntity?

    @Upsert
    suspend fun upsert(entity: JsonCacheEntity)

    @Query("DELETE FROM json_cache WHERE cacheKey = :cacheKey")
    suspend fun delete(cacheKey: String)

    @Query("DELETE FROM json_cache WHERE cacheKey LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)

    @Query("DELETE FROM json_cache")
    suspend fun clear()
}

/** A cached value and when it was fetched ("Updated at HH:mm"). */
data class Cached<out T>(
    val value: T,
    val updatedAt: Instant,
)

class JsonCache(
    private val dao: JsonCacheDao,
    private val json: Json,
    private val clock: Clock = Clock.System,
) {
    fun <T> observe(
        key: String,
        serializer: KSerializer<T>,
    ): Flow<Cached<T>?> = dao.observe(key).map { entity -> entity?.decode(serializer) }

    suspend fun <T> get(
        key: String,
        serializer: KSerializer<T>,
    ): Cached<T>? = dao.get(key)?.decode(serializer)

    suspend fun <T> put(
        key: String,
        serializer: KSerializer<T>,
        value: T,
    ) {
        dao.upsert(
            JsonCacheEntity(
                cacheKey = key,
                json = json.encodeToString(serializer, value),
                updatedAtEpochMillis = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun remove(key: String) = dao.delete(key)

    suspend fun removeByPrefix(prefix: String) = dao.deleteByPrefix(prefix)

    suspend fun clear() = dao.clear()

    private fun <T> JsonCacheEntity.decode(serializer: KSerializer<T>): Cached<T>? =
        try {
            Cached(json.decodeFromString(serializer, this.json), Instant.fromEpochMilliseconds(updatedAtEpochMillis))
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}
