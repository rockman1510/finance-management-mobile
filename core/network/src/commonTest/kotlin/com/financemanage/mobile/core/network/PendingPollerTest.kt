package com.financemanage.mobile.core.network

import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.AsyncStatus
import com.financemanage.mobile.core.network.pending.PendingPoller
import com.financemanage.mobile.core.network.pending.PollResult
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PendingPollerTest {
    private val poller = PendingPoller()

    @Test
    fun stopsWhenRecorded() =
        runTest {
            val statuses =
                ArrayDeque(listOf(AsyncStatus.PENDING, AsyncStatus.PENDING, AsyncStatus.RECORDED))
            val result =
                poller.poll(
                    fetch = { AppResult.Success(statuses.removeFirst()) },
                    isPending = { it.isPending },
                )
            assertEquals(AsyncStatus.RECORDED, assertIs<PollResult.Settled<AsyncStatus>>(result).value)
            assertEquals(4_500L, currentTime, "3 polls, 1.5 s apart")
        }

    @Test
    fun reportsFailedAsSettled() =
        runTest {
            val result = poller.poll(fetch = { AppResult.Success(AsyncStatus.FAILED) }, isPending = { it.isPending })
            assertEquals(AsyncStatus.FAILED, assertIs<PollResult.Settled<AsyncStatus>>(result).value)
        }

    @Test
    fun timesOutAfterTenSeconds() =
        runTest {
            var calls = 0
            val result =
                poller.poll(
                    fetch = {
                        calls++
                        AppResult.Success(AsyncStatus.PENDING)
                    },
                    isPending = { it.isPending },
                )
            assertEquals(AsyncStatus.PENDING, assertIs<PollResult.TimedOut<AsyncStatus>>(result).lastValue)
            assertEquals(7, calls)
            assertEquals(10_500L, currentTime)
        }

    @Test
    fun ignoresTransientFailures() =
        runTest {
            val answers =
                ArrayDeque<AppResult<AsyncStatus>>(
                    listOf(AppResult.Failure(AppError.Network()), AppResult.Success(AsyncStatus.RECORDED)),
                )
            val result = poller.poll(fetch = { answers.removeFirst() }, isPending = { it.isPending })
            assertIs<PollResult.Settled<AsyncStatus>>(result)
        }
}
