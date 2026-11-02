package com.financemanage.mobile.core.network.pending

import com.financemanage.mobile.core.model.AppResult
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed interface PollResult<out T> {
    /** The object left the `PENDING` state (`RECORDED`, `FAILED`…). */
    data class Settled<out T>(
        val value: T,
    ) : PollResult<T>

    /** Still pending after the timeout: show "Will update shortly" and refresh when the screen is shown again. */
    data class TimedOut<out T>(
        val lastValue: T?,
    ) : PollResult<T>
}

/**
 * Polls an object after a `202`/`PENDING` answer (API §2.1) every [interval] for up to [timeout].
 * Failed polls are ignored (the next one is tried) – the action itself was already accepted.
 */
class PendingPoller(
    private val interval: Duration = 1_500.milliseconds,
    private val timeout: Duration = 10.seconds,
) {
    suspend fun <T> poll(
        fetch: suspend () -> AppResult<T>,
        isPending: (T) -> Boolean,
    ): PollResult<T> {
        var lastValue: T? = null
        var elapsed = Duration.ZERO
        while (elapsed < timeout) {
            delay(interval)
            elapsed += interval
            val result = fetch()
            if (result is AppResult.Success) {
                lastValue = result.value
                if (!isPending(result.value)) return PollResult.Settled(result.value)
            }
        }
        return PollResult.TimedOut(lastValue)
    }
}
