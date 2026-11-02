package com.financemanage.mobile.core.network

import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.network.auth.SessionEvent
import com.financemanage.mobile.core.network.auth.SessionEvents
import com.financemanage.mobile.core.network.error.apiCall
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRefreshTest {
    private val tokenPairJson = """{"accessToken":"access-2","refreshToken":"refresh-2","expiresInSeconds":900}"""

    @Test
    fun sendsTheBearerTokenOnBusinessEndpoints() =
        runTest {
            val store = InMemoryTokenStore(AuthTokens("access-1", "refresh-1"))
            var authorization: String? = null
            val client =
                testClient(store) { request ->
                    authorization = request.headers[HttpHeaders.Authorization]
                    respondJson("{}")
                }
            client.get(api("me"))
            assertEquals("Bearer access-1", authorization)
        }

    @Test
    fun doesNotSendTheTokenToAuthEndpoints() =
        runTest {
            val store = InMemoryTokenStore(AuthTokens("access-1", "refresh-1"))
            var authorization: String? = "unset"
            val client =
                testClient(store) { request ->
                    authorization = request.headers[HttpHeaders.Authorization]
                    respondJson(tokenPairJson)
                }
            client.post(api("auth/login"))
            assertNull(authorization)
        }

    @Test
    fun refreshesOnceAndRetriesWithTheNewToken() =
        runTest {
            val store = InMemoryTokenStore(AuthTokens("access-1", "refresh-1"))
            val refreshBodies = mutableListOf<String>()
            val client =
                testClient(store) { request ->
                    when {
                        request.url.encodedPath.endsWith("auth/refresh") -> {
                            refreshBodies += request.body.toByteArray().decodeToString()
                            respondJson(tokenPairJson)
                        }
                        request.hasToken("access-2") ->
                            respondJson(
                                """{"id":"me"}""",
                            )
                        else -> respond("", HttpStatusCode.Unauthorized)
                    }
                }

            assertEquals("""{"id":"me"}""", client.get(api("me")).bodyAsText())
            assertEquals(listOf("""{"refreshToken":"refresh-1"}"""), refreshBodies)
            assertEquals(AuthTokens("access-2", "refresh-2"), store.tokens)
        }

    @Test
    fun concurrentRequestsShareASingleRefresh() =
        runTest {
            val store = InMemoryTokenStore(AuthTokens("access-1", "refresh-1"))
            var refreshCount = 0
            val client =
                testClient(store) { request ->
                    when {
                        request.url.encodedPath.endsWith("auth/refresh") -> {
                            refreshCount++
                            respondJson(tokenPairJson)
                        }
                        request.hasToken("access-2") -> respondJson("{}")
                        else -> respond("", HttpStatusCode.Unauthorized)
                    }
                }

            List(5) { async { client.get(api("wallets")).bodyAsText() } }.awaitAll()
            assertEquals(1, refreshCount)
        }

    @Test
    fun rejectedRefreshClearsTokensAndSignsOut() =
        runTest(UnconfinedTestDispatcher()) {
            val store = InMemoryTokenStore(AuthTokens("access-1", "refresh-old"))
            val events = SessionEvents()
            val signedOut = async { events.events.first() }
            var refreshCount = 0
            val client =
                testClient(store, events) { request ->
                    if (request.url.encodedPath.endsWith("auth/refresh")) refreshCount++
                    respond("", HttpStatusCode.Unauthorized)
                }

            val result = apiCall { client.get(api("me")).bodyAsText() }

            val error = assertIs<AppError.Api>(assertIs<AppResult.Failure>(result).error)
            assertEquals(401, error.status)
            assertEquals(1, refreshCount, "no refresh loop")
            assertNull(store.tokens)
            assertEquals(SessionEvent.SignedOut, signedOut.await())
        }

    @Test
    fun wrongPasswordDoesNotTriggerARefresh() =
        runTest {
            val store = InMemoryTokenStore(AuthTokens("access-1", "refresh-1"))
            var refreshCount = 0
            val client =
                testClient(store) { request ->
                    if (request.url.encodedPath.endsWith("auth/refresh")) refreshCount++
                    respondJson(
                        """{"code":"INVALID_CREDENTIALS","message":"Invalid credentials"}""",
                        HttpStatusCode.Unauthorized,
                    )
                }

            val result = apiCall { client.post(api("auth/login")).bodyAsText() }

            val error = assertIs<AppError.Api>(assertIs<AppResult.Failure>(result).error)
            assertEquals("INVALID_CREDENTIALS", error.code)
            assertEquals(0, refreshCount)
            assertEquals(0, store.clearCount)
            assertTrue(store.tokens != null)
        }

    @Test
    fun networkFailureDuringRefreshKeepsTheSession() =
        runTest {
            val store = InMemoryTokenStore(AuthTokens("access-1", "refresh-1"))
            val client =
                testClient(store) { request ->
                    if (request.url.encodedPath.endsWith("auth/refresh")) throw kotlinx.io.IOException("offline")
                    respond("", HttpStatusCode.Unauthorized)
                }

            apiCall { client.get(api("me")).bodyAsText() }

            assertEquals(AuthTokens("access-1", "refresh-1"), store.tokens)
            assertEquals(0, store.clearCount)
        }
}

private fun io.ktor.client.request.HttpRequestData.hasToken(token: String): Boolean =
    headers[HttpHeaders.Authorization] == "Bearer $token"
