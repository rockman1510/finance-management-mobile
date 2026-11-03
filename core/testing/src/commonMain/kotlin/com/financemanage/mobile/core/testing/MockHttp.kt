package com.financemanage.mobile.core.testing

import com.financemanage.mobile.core.network.ApiConfig
import com.financemanage.mobile.core.network.auth.SessionEvents
import com.financemanage.mobile.core.network.auth.TokenStore
import com.financemanage.mobile.core.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val TEST_BASE_URL = "http://localhost:8080"

/** The app's real [HttpClient] configuration on top of a [MockEngine]. */
fun mockHttpClient(
    tokenStore: TokenStore = FakeTokenStore(),
    sessionEvents: SessionEvents = SessionEvents(),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): HttpClient =
    createHttpClient(
        engine =
            MockEngine(
                MockEngineConfig().apply {
                    dispatcher = TestEngineDispatcher
                    addHandler(handler)
                },
            ),
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

/** The JSON (or text) body the app sent. */
fun HttpRequestData.bodyText(): String =
    when (val content = body) {
        is TextContent -> content.text
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        else -> ""
    }

/** `/api/v1/...` path of the request without the prefix, e.g. `wallets/123`. */
val HttpRequestData.apiPath: String
    get() = url.encodedPath.removePrefix("/api/v1/")

/** The JSON body the app sent, as an object. */
fun HttpRequestData.bodyJson(): JsonObject = Json.parseToJsonElement(bodyText()).jsonObject

/** A top-level field of the JSON body as text (`null` when absent or JSON null): numbers keep their exact form. */
fun HttpRequestData.bodyField(name: String): String? =
    (bodyJson()[name] as? JsonPrimitive)?.takeUnless { it.toString() == "null" }?.jsonPrimitive?.content
