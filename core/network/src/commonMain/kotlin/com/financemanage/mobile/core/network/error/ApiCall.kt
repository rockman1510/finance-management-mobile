package com.financemanage.mobile.core.network.error

import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.network.NetworkJson
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/** Error body of the backend (API §3). */
@Serializable
internal data class ErrorBody(
    val code: String? = null,
    val message: String? = null,
    val fieldErrors: Map<String, String>? = null,
)

object ErrorMapper {
    /** Maps a non-2xx response to [AppError.Api]; the body may be empty (e.g. `401`). */
    suspend fun fromResponse(
        response: HttpResponse,
        json: Json = NetworkJson,
    ): AppError.Api {
        val text =
            try {
                response.bodyAsText()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                ""
            }
        val body =
            if (text.isBlank()) {
                null
            } else {
                try {
                    json.decodeFromString(ErrorBody.serializer(), text)
                } catch (e: SerializationException) {
                    null
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        return AppError.Api(
            status = response.status.value,
            code = body?.code,
            serverMessage = body?.message,
            fieldErrors = body?.fieldErrors.orEmpty(),
            retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull(),
        )
    }

    /** Maps anything thrown by a call that is not an HTTP response. */
    fun fromThrowable(throwable: Throwable): AppError =
        when (throwable) {
            is AppError -> throwable
            is IOException -> AppError.Network(throwable)
            else -> AppError.Unexpected(throwable)
        }
}

/**
 * Runs an HTTP call and turns every failure into an [AppError]:
 * non-2xx -> [AppError.Api], no connection / timeout -> [AppError.Network], anything else -> [AppError.Unexpected].
 * Coroutine cancellation is never swallowed.
 */
suspend fun <T> apiCall(
    json: Json = NetworkJson,
    block: suspend () -> T,
): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        AppResult.Failure(ErrorMapper.fromResponse(e.response, json))
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        AppResult.Failure(ErrorMapper.fromThrowable(e))
    }
