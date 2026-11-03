package com.financemanage.mobile.core.network

import com.financemanage.mobile.core.network.auth.SessionEvents
import com.financemanage.mobile.core.network.auth.TokenRefresher
import com.financemanage.mobile.core.network.auth.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val REQUEST_TIMEOUT_MS = 30_000L
private const val CONNECT_TIMEOUT_MS = 15_000L

/**
 * The single [HttpClient] of the app.
 *
 * - `expectSuccess = true`: any non-2xx response throws; wrap calls in [apiCall] to get an `AppError`.
 * - Bearer auth on every endpoint except the `auth/` ones, with single-use refresh token rotation.
 */
fun createHttpClient(
    engine: HttpClientEngine,
    config: ApiConfig,
    tokenStore: TokenStore,
    sessionEvents: SessionEvents,
    json: Json = NetworkJson,
): HttpClient {
    val refresher = TokenRefresher(tokenStore, sessionEvents)
    return HttpClient(engine) {
        expectSuccess = true

        defaultRequest {
            url(config.baseUrl.trimEnd('/') + "/")
            accept(ContentType.Application.Json)
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }

        // Ktor's Auth plugin re-sends a request that got a 401 without a token. For /auth/* a 401 means wrong
        // credentials or code: sending it again would count twice towards the lock-out (API §4.1).
        install(AuthEndpointsNoRetry)

        install(Auth) {
            bearer {
                loadTokens {
                    tokenStore.read()?.let { BearerTokens(it.accessToken, it.refreshToken) }
                }
                refreshTokens {
                    // A 401 from /auth/* (wrong password, wrong 2FA code…) is a credentials error, not an expired token.
                    if (response.call.request.url.encodedPath
                            .isAuthEndpoint()
                    ) {
                        null
                    } else {
                        refresher.refresh(client, oldTokens) { markAsRefreshTokenRequest() }
                    }
                }
                sendWithoutRequest { request -> !request.url.encodedPath.isAuthEndpoint() }
            }
        }

        if (config.enableLogging) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
        }
    }
}

private fun String.isAuthEndpoint(): Boolean = contains("${API_PREFIX}auth/")

private val AuthEndpointsNoRetry =
    createClientPlugin("AuthEndpointsNoRetry") {
        onRequest { request, _ ->
            if (request.url.encodedPath.isAuthEndpoint()) request.attributes.put(AuthCircuitBreaker, Unit)
        }
    }

/**
 * Drops the bearer tokens cached in memory by Ktor. Call after signing in (new tokens saved in the
 * [TokenStore]) and after signing out.
 */
fun HttpClient.resetCachedTokens() {
    authProvider<BearerAuthProvider>()?.clearToken()
}
