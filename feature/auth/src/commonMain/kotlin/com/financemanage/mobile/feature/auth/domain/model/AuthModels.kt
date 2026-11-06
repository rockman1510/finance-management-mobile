package com.financemanage.mobile.feature.auth.domain.model

sealed interface LoginResult {
    data object SignedIn : LoginResult

    /** The account has 2FA: verify a code with [challengeToken] within [expiresInSeconds]. */
    data class TwoFactorRequired(
        val challengeToken: String,
        val expiresInSeconds: Long,
    ) : LoginResult
}

/** Local validation rules (API §4.1). */
object AuthRules {
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 72
    const val CODE_LENGTH = 6

    fun isEmailValid(email: String): Boolean {
        val trimmed = email.trim()
        val at = trimmed.indexOf('@')
        return at > 0 && trimmed.indexOf('.', at) > at + 1 && !trimmed.endsWith(".")
    }

    fun isPasswordValid(password: String): Boolean = password.length in PASSWORD_MIN..PASSWORD_MAX
}
