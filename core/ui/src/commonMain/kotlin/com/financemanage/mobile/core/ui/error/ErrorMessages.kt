package com.financemanage.mobile.core.ui.error

import androidx.compose.runtime.Composable
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.ErrorCodes
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.error_account_locked
import com.financemanage.mobile.core.ui.resources.error_conflict
import com.financemanage.mobile.core.ui.resources.error_currency_mismatch
import com.financemanage.mobile.core.ui.resources.error_exchange_rate_missing
import com.financemanage.mobile.core.ui.resources.error_generic
import com.financemanage.mobile.core.ui.resources.error_invalid_amount
import com.financemanage.mobile.core.ui.resources.error_invalid_credentials
import com.financemanage.mobile.core.ui.resources.error_network
import com.financemanage.mobile.core.ui.resources.error_not_found
import com.financemanage.mobile.core.ui.resources.error_resend_too_soon
import com.financemanage.mobile.core.ui.resources.error_session_expired
import com.financemanage.mobile.core.ui.resources.error_too_many_requests
import com.financemanage.mobile.core.ui.resources.error_too_many_requests_generic
import com.financemanage.mobile.core.ui.resources.error_validation
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val SECONDS_PER_MINUTE = 60

/**
 * The text shown to the user for an error, chosen from the error `code` (API §3).
 * Features handle their own codes first and fall back to this function.
 */
@Composable
fun AppError.userMessage(): String {
    val api = this as? AppError.Api
    val retryAfter = api?.retryAfterSeconds
    if (api?.isTooManyRequests == true && retryAfter != null && api.code != ErrorCodes.ACCOUNT_LOCKED) {
        val minutes = ((retryAfter + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE).toInt().coerceAtLeast(1)
        return stringResource(Res.string.error_too_many_requests, minutes)
    }
    return stringResource(messageResource())
}

/** Resource for an error, without formatting arguments. */
fun AppError.messageResource(): StringResource =
    when (this) {
        is AppError.Network -> Res.string.error_network
        is AppError.Unexpected -> Res.string.error_generic
        is AppError.Api -> codeResource(code) ?: statusResource()
    }

private fun codeResource(code: String?): StringResource? =
    when (code) {
        ErrorCodes.INVALID_CREDENTIALS -> Res.string.error_invalid_credentials
        ErrorCodes.ACCOUNT_LOCKED -> Res.string.error_account_locked
        ErrorCodes.RESEND_TOO_SOON -> Res.string.error_resend_too_soon
        ErrorCodes.CONCURRENT_MODIFICATION -> Res.string.error_conflict
        ErrorCodes.NOT_FOUND -> Res.string.error_not_found
        ErrorCodes.VALIDATION_FAILED -> Res.string.error_validation
        ErrorCodes.INVALID_AMOUNT -> Res.string.error_invalid_amount
        ErrorCodes.CURRENCY_MISMATCH -> Res.string.error_currency_mismatch
        ErrorCodes.EXCHANGE_RATE_MISSING -> Res.string.error_exchange_rate_missing
        else -> null
    }

private fun AppError.Api.statusResource(): StringResource =
    when {
        isUnauthorized -> Res.string.error_session_expired
        isNotFound -> Res.string.error_not_found
        isConflict -> Res.string.error_conflict
        isValidation -> Res.string.error_validation
        isTooManyRequests -> Res.string.error_too_many_requests_generic
        else -> Res.string.error_generic
    }
