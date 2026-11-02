package com.financemanage.mobile.core.network.auth

import com.financemanage.mobile.core.network.api
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException

/**
 * Exchanges the single-use refresh token for a new pair (API §4.1).
 *
 * - Success: the new pair is persisted before it is used.
 * - The backend refuses the token (401/400/422): tokens are cleared and [SessionEvent.SignedOut] is emitted.
 * - Network failure: nothing is cleared, the original request simply fails.
 *
 * Ktor's bearer provider guarantees that only one refresh runs at a time.
 */
internal class TokenRefresher(
    private val tokenStore: TokenStore,
    private val sessionEvents: SessionEvents,
) {
    suspend fun refresh(
        client: HttpClient,
        oldTokens: BearerTokens?,
        markAsRefreshRequest: HttpRequestBuilder.() -> Unit,
    ): BearerTokens? {
        val refreshToken = oldTokens?.refreshToken ?: tokenStore.read()?.refreshToken
        if (refreshToken == null) {
            signOut()
            return null
        }
        return try {
            val pair =
                client
                    .post(api("auth/refresh")) {
                        markAsRefreshRequest()
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(refreshToken))
                    }.body<TokenPairDto>()
            tokenStore.save(pair.toAuthTokens())
            BearerTokens(pair.accessToken, pair.refreshToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            if (e.response.status.isRejection()) signOut()
            null
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            null
        }
    }

    private suspend fun signOut() {
        tokenStore.clear()
        sessionEvents.emit(SessionEvent.SignedOut)
    }

    private fun HttpStatusCode.isRejection(): Boolean =
        this == HttpStatusCode.Unauthorized ||
            this == HttpStatusCode.BadRequest ||
            this == HttpStatusCode.Forbidden ||
            this == HttpStatusCode.UnprocessableEntity
}
