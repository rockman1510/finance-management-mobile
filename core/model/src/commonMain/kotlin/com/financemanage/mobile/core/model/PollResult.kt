package com.financemanage.mobile.core.model

/** Outcome of polling an object that was accepted as `PENDING` (API §2.1). */
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
