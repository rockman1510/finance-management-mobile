package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.SessionState
import kotlinx.coroutines.flow.StateFlow

/** The signed-in state of the app. Signing out wipes tokens and every local cache. */
interface SessionRepository {
    val state: StateFlow<SessionState>

    /** Revokes the session on the server (best effort) and wipes local data. */
    suspend fun signOut()

    /** After the account was deleted on the server: only local cleanup. */
    suspend fun clearLocalSession(wipeSettings: Boolean = false)
}
