package com.financemanage.mobile.feature.auth.domain

import com.financemanage.mobile.core.model.AppResult

sealed interface LoginResult {
    data object SignedIn : LoginResult

    /** The account has 2FA: verify a code with [challengeToken] within [expiresInSeconds]. */
    data class TwoFactorRequired(
        val challengeToken: String,
        val expiresInSeconds: Long,
    ) : LoginResult
}

interface AuthRepository {
    suspend fun login(
        email: String,
        password: String,
    ): AppResult<LoginResult>

    suspend fun loginWithGoogle(idToken: String): AppResult<LoginResult>

    suspend fun verifyTwoFactor(
        challengeToken: String,
        code: String,
    ): AppResult<Unit>

    suspend fun register(
        fullName: String,
        email: String,
        password: String,
    ): AppResult<Unit>

    suspend fun forgotPassword(email: String): AppResult<Unit>

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): AppResult<Unit>
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
