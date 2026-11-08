package com.financemanage.mobile.core.data

import com.financemanage.mobile.core.data.outbox.OutboxEntity
import com.financemanage.mobile.core.data.outbox.OutboxSender
import com.financemanage.mobile.core.data.outbox.OutboxSync
import com.financemanage.mobile.core.domain.model.OutboxStatus
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.testing.FakeNetworkMonitor
import com.financemanage.mobile.core.testing.FakeOutboxDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OutboxSyncTest {
    private class RecordingSender(
        override val kind: String = "transaction",
        var result: (OutboxEntity) -> AppResult<Unit> = { AppResult.Success(Unit) },
    ) : OutboxSender {
        val sent = mutableListOf<OutboxEntity>()

        override suspend fun send(entry: OutboxEntity): AppResult<Unit> {
            sent += entry
            return result(entry)
        }
    }

    private fun entry(
        id: String,
        createdAt: Long,
    ) = OutboxEntity(
        id = id,
        kind = "transaction",
        payload = "{}",
        idempotencyKey = "key-$id",
        createdAtEpochMillis = createdAt,
    )

    @Test
    fun sendsWaitingEntriesInOrderAndRemovesThem() =
        runTest(UnconfinedTestDispatcher()) {
            val dao = FakeOutboxDao()
            val sender = RecordingSender()
            val sync = OutboxSync(dao, { listOf(sender) }, FakeNetworkMonitor(online = false), backgroundScope)
            dao.upsert(entry("b", 2))
            dao.upsert(entry("a", 1))

            sync.flush()

            assertEquals(listOf("a", "b"), sender.sent.map { it.id })
            assertTrue(dao.rows.value.isEmpty())
        }

    @Test
    fun networkErrorKeepsEverythingWaitingAndStops() =
        runTest(UnconfinedTestDispatcher()) {
            val dao = FakeOutboxDao()
            val sender = RecordingSender(result = { AppResult.Failure(AppError.Network()) })
            val sync = OutboxSync(dao, { listOf(sender) }, FakeNetworkMonitor(), backgroundScope)
            dao.upsert(entry("a", 1))
            dao.upsert(entry("b", 2))

            sync.flush()

            assertEquals(listOf("a"), sender.sent.map { it.id })
            assertEquals(listOf(OutboxStatus.WAITING.name, OutboxStatus.WAITING.name), dao.rows.value.map { it.status })
        }

    @Test
    fun businessErrorMarksTheEntryFailedAndRetryPutsItBack() =
        runTest(UnconfinedTestDispatcher()) {
            val dao = FakeOutboxDao()
            val sender =
                RecordingSender(result = { AppResult.Failure(AppError.Api(422, "WALLET_ARCHIVED")) })
            val sync = OutboxSync(dao, { listOf(sender) }, FakeNetworkMonitor(), backgroundScope)
            dao.upsert(entry("a", 1))

            sync.flush()
            val failed = dao.rows.value.single()
            assertEquals(OutboxStatus.FAILED.name, failed.status)
            assertEquals("WALLET_ARCHIVED", failed.errorCode)

            sender.result = { AppResult.Success(Unit) }
            sync.retry(failed)
            assertTrue(dao.rows.value.isEmpty())
            assertEquals(listOf("key-a", "key-a"), sender.sent.map { it.idempotencyKey })
        }

    @Test
    fun flushesWhenTheDeviceComesOnline() =
        runTest(UnconfinedTestDispatcher()) {
            val dao = FakeOutboxDao()
            val network = FakeNetworkMonitor(online = false)
            val sender = RecordingSender()
            val sync = OutboxSync(dao, { listOf(sender) }, network, backgroundScope)
            dao.upsert(entry("a", 1))
            sync.start()
            assertTrue(sender.sent.isEmpty())

            network.online.value = true

            assertEquals(listOf("a"), sender.sent.map { it.id })
        }

    @Test
    fun entriesWithoutASenderAreLeftAlone() =
        runTest(UnconfinedTestDispatcher()) {
            val dao = FakeOutboxDao()
            val sync = OutboxSync(dao, { emptyList() }, FakeNetworkMonitor(), backgroundScope)
            dao.upsert(entry("a", 1))
            sync.flush()
            assertEquals(1, dao.rows.value.size)
        }
}
