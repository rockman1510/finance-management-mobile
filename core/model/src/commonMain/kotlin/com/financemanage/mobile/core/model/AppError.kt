package com.financemanage.mobile.core.model

/**
 * Every failure the data layer can report. The UI decides what to show from [Api.code] (see API §3),
 * never from the English `message`.
 */
sealed class AppError(
    message: String?,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** A non-2xx HTTP response. `code` is `null` when the body is empty (e.g. an expired access token). */
    class Api(
        val status: Int,
        val code: String?,
        val serverMessage: String? = null,
        val fieldErrors: Map<String, String> = emptyMap(),
        val retryAfterSeconds: Long? = null,
    ) : AppError(serverMessage ?: "HTTP $status${code?.let { " $it" }.orEmpty()}") {
        val isValidation: Boolean get() = status == 400
        val isUnauthorized: Boolean get() = status == 401
        val isNotFound: Boolean get() = status == 404
        val isConflict: Boolean get() = status == 409
        val isBusinessRule: Boolean get() = status == 422
        val isTooManyRequests: Boolean get() = status == 429
    }

    /** No connection, DNS failure, timeout… – the request may be retried with the same Idempotency-Key. */
    class Network(
        cause: Throwable? = null,
    ) : AppError("Network error", cause)

    /** Anything else (unexpected payload, programming error). */
    class Unexpected(
        cause: Throwable? = null,
    ) : AppError(cause?.message ?: "Unexpected error", cause)
}

/** Error codes returned by the backend (API §3). Features add the codes they handle. */
object ErrorCodes {
    // General
    const val VALIDATION_FAILED = "VALIDATION_FAILED"
    const val NOT_FOUND = "NOT_FOUND"
    const val CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION"
    const val INVALID_AMOUNT = "INVALID_AMOUNT"
    const val INVALID_CURRENCY = "INVALID_CURRENCY"
    const val CURRENCY_MISMATCH = "CURRENCY_MISMATCH"
    const val IDEMPOTENCY_KEY_REUSED = "IDEMPOTENCY_KEY_REUSED"
    const val EXCHANGE_RATE_MISSING = "EXCHANGE_RATE_MISSING"

    // Account
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val RESEND_TOO_SOON = "RESEND_TOO_SOON"
    const val EMAIL_TAKEN = "EMAIL_TAKEN"
    const val WRONG_PASSWORD = "WRONG_PASSWORD"
    const val INVALID_CODE = "INVALID_CODE"
    const val GOOGLE_SIGN_IN_DISABLED = "GOOGLE_SIGN_IN_DISABLED"
}
