package com.financemanage.mobile.core.data.outbox

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.financemanage.mobile.core.domain.model.OutboxStatus
import com.financemanage.mobile.core.domain.model.PendingWrite
import com.financemanage.mobile.core.domain.repository.NetworkMonitor
import com.financemanage.mobile.core.domain.repository.OutboxRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * A write created offline (income/expense transaction, transfer). It keeps its Idempotency-Key so that sending it
 * again never records it twice.
 */
@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val payload: String,
    val idempotencyKey: String,
    val status: String = OutboxStatus.WAITING.name,
    val errorCode: String? = null,
    val summary: String = "",
    val createdAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
)

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox ORDER BY createdAtEpochMillis")
    fun observeAll(): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM outbox WHERE id = :id")
    suspend fun get(id: String): OutboxEntity?

    @Query("SELECT * FROM outbox WHERE status = 'WAITING' ORDER BY createdAtEpochMillis")
    suspend fun waiting(): List<OutboxEntity>

    @Upsert
    suspend fun upsert(entity: OutboxEntity)

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM outbox")
    suspend fun clear()
}

/** Sends one kind of outbox entry. Registered by the feature that owns the write. */
interface OutboxSender {
    val kind: String

    suspend fun send(entry: OutboxEntity): AppResult<Unit>
}

/**
 * Sends waiting entries in order whenever the device is online. Network errors keep the entry waiting; business
 * errors (4xx) mark it FAILED for the user to fix.
 */
class OutboxSync(
    private val dao: OutboxDao,
    private val senders: () -> List<OutboxSender>,
    private val networkMonitor: NetworkMonitor,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    fun start() {
        scope.launch {
            networkMonitor.isOnline.filter { it }.collect { flush() }
        }
    }

    suspend fun enqueue(entry: OutboxEntity) {
        dao.upsert(entry)
        scope.launch { flush() }
    }

    /** Puts a FAILED entry back in the queue after the user fixed the cause (e.g. unarchived the wallet). */
    suspend fun retry(entry: OutboxEntity) {
        dao.upsert(entry.copy(status = OutboxStatus.WAITING.name, errorCode = null))
        flush()
    }

    suspend fun discard(id: String) = dao.delete(id)

    suspend fun flush() {
        mutex.withLock {
            val byKind = senders().associateBy { it.kind }
            for (entry in dao.waiting()) {
                val sender = byKind[entry.kind] ?: continue
                when (val result = sender.send(entry)) {
                    is AppResult.Success -> dao.delete(entry.id)
                    is AppResult.Failure -> {
                        val error = result.error
                        if (error is AppError.Api && error.status in CLIENT_ERRORS) {
                            dao.upsert(entry.copy(status = OutboxStatus.FAILED.name, errorCode = error.code))
                        } else {
                            return // offline or server error: try again later, keep the order
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val CLIENT_ERRORS = 400..499
    }
}

fun OutboxEntity.toPendingWrite(): PendingWrite =
    PendingWrite(
        id = id,
        kind = kind,
        summary = summary,
        status = OutboxStatus.entries.firstOrNull { it.name == status } ?: OutboxStatus.WAITING,
        errorCode = errorCode,
        createdAtEpochMillis = createdAtEpochMillis,
    )

/** [OutboxRepository] over the Room `outbox` table and [OutboxSync]. */
class OutboxRepositoryImpl(
    private val dao: OutboxDao,
    private val sync: OutboxSync,
) : OutboxRepository {
    override val pending: Flow<List<PendingWrite>> =
        dao.observeAll().map { list -> list.map(OutboxEntity::toPendingWrite) }

    override suspend fun retry(id: String) {
        dao.get(id)?.let { sync.retry(it) }
    }

    override suspend fun discard(id: String) = sync.discard(id)
}
