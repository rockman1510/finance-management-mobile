package com.financemanage.mobile.feature.auth.domain.repository

import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.auth.domain.model.LoginResult

/** Sign-in, sign-up and password reset (API §4). A successful sign-in stores the tokens and starts the session. */
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
