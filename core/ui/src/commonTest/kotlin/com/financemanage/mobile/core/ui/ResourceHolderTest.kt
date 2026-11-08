package com.financemanage.mobile.core.ui

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.ui.state.ResourceHolder
import com.financemanage.mobile.core.ui.state.memoryResource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceHolderTest {
    private val time = Instant.parse("2026-11-01T10:00:00Z")

    @Test
    fun loadingUntilTheFirstRefreshThenEmptyWithoutData() =
        runTest(UnconfinedTestDispatcher()) {
            val source = MutableStateFlow<Cached<String>?>(null)
            val gate = CompletableDeferred<AppResult<Unit>>()
            val holder = ResourceHolder(backgroundScope, source) { gate.await() }

            assertTrue(holder.state.value.loading)
            holder.refresh()
            assertTrue(holder.state.value.loading)

            gate.complete(AppResult.Success(Unit))
            assertFalse(holder.state.value.loading)
            assertNull(holder.state.value.data)
        }

    @Test
    fun cachedDataIsShownAtOnceAndAFailedRefreshKeepsItWithTheError() =
        runTest(UnconfinedTestDispatcher()) {
            val source = MutableStateFlow<Cached<String>?>(Cached("cached", time))
            val holder =
                ResourceHolder(backgroundScope, source) { AppResult.Failure(AppError.Network()) }

            assertEquals("cached", holder.state.value.data)
            assertFalse(holder.state.value.loading)
            assertEquals(time, holder.state.value.updatedAt)

            holder.refresh()
            assertEquals("cached", holder.state.value.data)
            assertIs<AppError.Network>(holder.state.value.error)

            holder.clearError()
            assertNull(holder.state.value.error)
        }

    @Test
    fun refreshesRequestedWhileOneIsRunningAreCoalescedIntoOneMore() =
        runTest(UnconfinedTestDispatcher()) {
            var calls = 0
            val gate = CompletableDeferred<AppResult<Unit>>()
            val holder =
                ResourceHolder(backgroundScope, MutableStateFlow<Cached<String>?>(null)) {
                    calls++
                    gate.await()
                }
            holder.refresh()
            holder.refresh()
            holder.refresh()
            assertEquals(1, calls)
            gate.complete(AppResult.Success(Unit))
            assertEquals(2, calls)
            assertFalse(holder.state.value.loading)
        }

    @Test
    fun memoryResourceKeepsTheLastSuccessfulValue() =
        runTest(UnconfinedTestDispatcher()) {
            var next: AppResult<Int> = AppResult.Success(1)
            val holder = backgroundScope.memoryResource { next }

            holder.refresh()
            assertEquals(1, holder.state.value.data)

            next = AppResult.Failure(AppError.Network())
            holder.refresh()
            assertEquals(1, holder.state.value.data)
            assertIs<AppError.Network>(holder.state.value.error)
        }
}
