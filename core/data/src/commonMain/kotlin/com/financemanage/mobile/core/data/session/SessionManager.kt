package com.financemanage.mobile.core.data.session

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.data.outbox.OutboxDao
import com.financemanage.mobile.core.domain.model.SessionState
import com.financemanage.mobile.core.domain.repository.AppSettingsRepository
import com.financemanage.mobile.core.domain.repository.SessionRepository
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.network.auth.SessionEvent
import com.financemanage.mobile.core.network.auth.SessionEvents
import com.financemanage.mobile.core.network.auth.TokenStore
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.core.network.resetCachedTokens
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Work to do before the session ends (e.g. unregister the push token while still authenticated). */
fun interface SignOutHook {
    suspend fun beforeSignOut()
}

@Serializable
private data class LogoutRequest(
    val refreshToken: String,
)

/**
 * Owns the signed-in state of the app. Signing out (by the user, after account deletion, or because the refresh
 * token was rejected) wipes tokens and every local cache.
 */
class SessionManager(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient,
    private val cache: JsonCache,
    private val outboxDao: OutboxDao,
    private val settings: AppSettingsRepository,
    private val hooks: () -> List<SignOutHook>,
    sessionEvents: SessionEvents,
    private val scope: CoroutineScope,
) : SessionRepository {
    private val mutableState = MutableStateFlow(SessionState.UNKNOWN)
    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    init {
        scope.launch {
            mutableState.value = if (tokenStore.read() != null) SessionState.SIGNED_IN else SessionState.SIGNED_OUT
        }
        scope.launch {
            sessionEvents.events.collect { event ->
                if (event == SessionEvent.SignedOut) clearLocalSession()
            }
        }
    }

    suspend fun signIn(tokens: AuthTokens) {
        tokenStore.save(tokens)
        httpClient.resetCachedTokens()
        mutableState.value = SessionState.SIGNED_IN
    }

    /** Revokes the session on the server (best effort) and wipes local data. */
    override suspend fun signOut() {
        hooks().forEach { runCatchingNonCancel { it.beforeSignOut() } }
        tokenStore.read()?.let { tokens ->
            apiCall {
                httpClient.post(api("auth/logout")) {
                    contentType(ContentType.Application.Json)
                    setBody(LogoutRequest(tokens.refreshToken))
                }
            }
        }
        clearLocalSession()
    }

    /** After the account was deleted on the server: only local cleanup. */
    override suspend fun clearLocalSession(wipeSettings: Boolean) {
        tokenStore.clear()
        httpClient.resetCachedTokens()
        cache.clear()
        outboxDao.clear()
        if (wipeSettings) settings.clear()
        mutableState.value = SessionState.SIGNED_OUT
    }

    private suspend fun runCatchingNonCancel(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            // Best effort: never block sign-out.
        }
    }
}
