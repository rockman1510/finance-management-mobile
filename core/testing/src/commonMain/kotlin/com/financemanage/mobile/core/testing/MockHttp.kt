package com.financemanage.mobile.core.testing

import com.financemanage.mobile.core.network.ApiConfig
import com.financemanage.mobile.core.network.auth.SessionEvents
import com.financemanage.mobile.core.network.auth.TokenStore
import com.financemanage.mobile.core.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

const val TEST_BASE_URL = "http://localhost:8080"

/** The app's real [HttpClient] configuration on top of a [MockEngine]. */
fun mockHttpClient(
    tokenStore: TokenStore = FakeTokenStore(),
    sessionEvents: SessionEvents = SessionEvents(),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): HttpClient =
    createHttpClient(
        engine = MockEngine(handler),
        config = ApiConfig(baseUrl = TEST_BASE_URL),
        tokenStore = tokenStore,
        sessionEvents = sessionEvents,
    )

fun MockRequestHandleScope.respondJson(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData =
    respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

fun MockRequestHandleScope.respondError(
    status: HttpStatusCode,
    code: String,
    message: String = code,
): HttpResponseData =
    respondJson("""{"code":"$code","message":"$message","timestamp":"2026-11-01T10:15:07Z"}""", status)
