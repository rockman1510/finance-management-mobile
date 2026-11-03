package com.financemanage.mobile.core.testing

import com.financemanage.mobile.core.network.auth.SessionEvents
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

typealias MockResponder = suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData

/**
 * A fake backend for repository / ViewModel tests: register answers per method + path (relative to `/api/v1/`, query
 * string excluded) and inspect the requests the app sent. The last matching registration wins, so a test can change
 * an answer half-way. Unknown routes answer `404 NOT_FOUND`.
 */
class MockBackend(
    val tokenStore: FakeTokenStore = FakeTokenStore(),
    val sessionEvents: SessionEvents = SessionEvents(),
) {
    private class Route(
        val method: HttpMethod,
        val path: Regex,
        val responder: MockResponder,
    )

    private val routes = MutableStateFlow<List<Route>>(emptyList())
    private val log = MutableStateFlow<List<HttpRequestData>>(emptyList())

    /** Every request received so far, oldest first. */
    val requests: List<HttpRequestData> get() = log.value

    val client: HttpClient =
        mockHttpClient(tokenStore, sessionEvents) { request ->
            log.update { it + request }
            val route = routes.value.lastOrNull { it.method == request.method && it.path.matches(request.apiPath) }
            route?.responder?.invoke(this, request) ?: respondError(HttpStatusCode.NotFound, "NOT_FOUND")
        }

    /** [path] is a regex over the relative path, e.g. `"wallets"` or `"loans/[^/]+/repayments"`. */
    fun on(
        method: HttpMethod,
        path: String,
        responder: MockResponder,
    ) {
        routes.update { it + Route(method, Regex(path), responder) }
    }

    fun get(
        path: String,
        body: String,
    ) = on(HttpMethod.Get, path) { respondJson(body) }

    fun post(
        path: String,
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = on(HttpMethod.Post, path) { respondJson(body, status) }

    fun patch(
        path: String,
        body: String,
    ) = on(HttpMethod.Patch, path) { respondJson(body) }

    fun delete(path: String) = on(HttpMethod.Delete, path) { respondJson("", HttpStatusCode.NoContent) }

    fun fail(
        method: HttpMethod,
        path: String,
        status: HttpStatusCode,
        code: String,
    ) = on(method, path) { respondError(status, code) }

    fun requests(
        method: HttpMethod,
        path: String,
    ): List<HttpRequestData> {
        val regex = Regex(path)
        return requests.filter { it.method == method && regex.matches(it.apiPath) }
    }

    /** The single request sent to [method] [path]; fails when there is none or more than one. */
    fun single(
        method: HttpMethod,
        path: String,
    ): HttpRequestData = requests(method, path).single()

    /** Waits until at least [count] requests matched [method] [path] (work may run on Ktor's dispatcher). */
    suspend fun await(
        method: HttpMethod,
        path: String,
        count: Int = 1,
    ): HttpRequestData {
        awaitCondition("$method $path") { requests(method, path).size >= count }
        return requests(method, path).last()
    }
}
