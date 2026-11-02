package com.financemanage.mobile.core.network

import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.ErrorCodes
import com.financemanage.mobile.core.network.error.apiCall
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ErrorMapperTest {
    private suspend fun callWith(
        status: HttpStatusCode,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): AppError {
        val client = testClient { respondJson(body, status, headers) }
        val result = apiCall { client.get(api("wallets")).bodyAsText() }
        return assertIs<AppResult.Failure>(result).error
    }

    @Test
    fun validationErrorKeepsFieldErrors() =
        runTest {
            val error =
                callWith(
                    HttpStatusCode.BadRequest,
                    """{"code":"VALIDATION_FAILED","message":"Request validation failed",
               "fieldErrors":{"name":"must not be blank","type":"must not be null"},
               "timestamp":"2026-11-01T10:15:07.798400582Z"}""",
                )
            val api = assertIs<AppError.Api>(error)
            assertEquals(400, api.status)
            assertEquals(ErrorCodes.VALIDATION_FAILED, api.code)
            assertEquals(mapOf("name" to "must not be blank", "type" to "must not be null"), api.fieldErrors)
            assertTrue(api.isValidation)
        }

    @Test
    fun emptyUnauthorizedBody() =
        runTest {
            // No tokens stored: the refresh gives up and the 401 is reported as is.
            val client = testClient { respond("", HttpStatusCode.Unauthorized) }
            val result = apiCall { client.get(api("me")).bodyAsText() }
            val api = assertIs<AppError.Api>(assertIs<AppResult.Failure>(result).error)
            assertEquals(401, api.status)
            assertNull(api.code)
            assertTrue(api.fieldErrors.isEmpty())
        }

    @Test
    fun businessRuleError() =
        runTest {
            val api =
                assertIs<AppError.Api>(
                    callWith(
                        HttpStatusCode.UnprocessableEntity,
                        """{"code":"WALLET_ARCHIVED","message":"Wallet is archived"}""",
                    ),
                )
            assertEquals(422, api.status)
            assertEquals("WALLET_ARCHIVED", api.code)
            assertEquals("Wallet is archived", api.serverMessage)
            assertTrue(api.isBusinessRule)
        }

    @Test
    fun tooManyRequestsWithRetryAfter() =
        runTest {
            val api =
                assertIs<AppError.Api>(
                    callWith(
                        HttpStatusCode.TooManyRequests,
                        """{"code":"ACCOUNT_LOCKED","message":"Account locked"}""",
                        mapOf("Retry-After" to "900"),
                    ),
                )
            assertEquals(ErrorCodes.ACCOUNT_LOCKED, api.code)
            assertEquals(900L, api.retryAfterSeconds)
            assertTrue(api.isTooManyRequests)
        }

    @Test
    fun nonJsonBodyStillMapsTheStatus() =
        runTest {
            val api = assertIs<AppError.Api>(callWith(HttpStatusCode.BadGateway, "<html>Bad gateway</html>"))
            assertEquals(502, api.status)
            assertNull(api.code)
        }

    @Test
    fun ioFailureIsANetworkError() =
        runTest {
            val client = testClient { throw IOException("offline") }
            val result = apiCall { client.get(api("wallets")).bodyAsText() }
            assertIs<AppError.Network>(assertIs<AppResult.Failure>(result).error)
        }

    @Test
    fun successIsReturned() =
        runTest {
            val client = testClient { respondJson("""{"ok":true}""") }
            val result = apiCall { client.get(api("wallets")).bodyAsText() }
            assertEquals("""{"ok":true}""", assertIs<AppResult.Success<String>>(result).value)
        }
}
