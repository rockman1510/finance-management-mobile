package com.financemanage.mobile.core.network.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface SessionEvent {
    /** The session cannot be refreshed any more (expired / reused / revoked refresh token): go to Sign in. */
    data object SignedOut : SessionEvent
}

/** App-wide session events, observed by the navigation layer. */
class SessionEvents {
    private val mutableEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SessionEvent> = mutableEvents.asSharedFlow()

    fun emit(event: SessionEvent) {
        mutableEvents.tryEmit(event)
    }
}
