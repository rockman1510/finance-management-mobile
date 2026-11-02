package com.financemanage.mobile.core.network

import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.network.auth.SessionEvents
import com.financemanage.mobile.core.network.auth.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

internal class InMemoryTokenStore(
    var tokens: AuthTokens? = null,
) : TokenStore {
    var clearCount = 0

    override suspend fun read(): AuthTokens? = tokens

    override suspend fun save(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        clearCount++
        tokens = null
    }
}

internal fun MockRequestHandleScope.respondJson(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    extraHeaders: Map<String, String> = emptyMap(),
): HttpResponseData =
    respond(
        content = body,
        status = status,
        headers =
            headersOf(
                *(mapOf(HttpHeaders.ContentType to "application/json") + extraHeaders)
                    .map { (name, value) -> name to listOf(value) }
                    .toTypedArray(),
            ),
    )

internal fun testClient(
    tokenStore: TokenStore = InMemoryTokenStore(),
    sessionEvents: SessionEvents = SessionEvents(),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): HttpClient =
    createHttpClient(
        engine = MockEngine(handler),
        config = ApiConfig(baseUrl = "http://localhost:8080"),
        tokenStore = tokenStore,
        sessionEvents = sessionEvents,
    )
