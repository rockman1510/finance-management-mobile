package com.financemanage.mobile.feature.auth.domain.usecase

import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.auth.domain.model.LoginResult
import com.financemanage.mobile.feature.auth.domain.repository.AuthRepository

/** Signs in with e-mail and password; may ask for a two-step verification code. */
class SignInUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
    ): AppResult<LoginResult> = repository.login(email, password)
}

/** Signs in with a Google ID token; may ask for a two-step verification code. */
class SignInWithGoogleUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String): AppResult<LoginResult> = repository.loginWithGoogle(idToken)
}

/** Completes a sign-in that requires two-step verification. */
class VerifyTwoFactorUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        challengeToken: String,
        code: String,
    ): AppResult<Unit> = repository.verifyTwoFactor(challengeToken, code)
}

/** Creates an account; the user then verifies the e-mail and signs in. */
class SignUpUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        fullName: String,
        email: String,
        password: String,
    ): AppResult<Unit> = repository.register(fullName, email, password)
}

/** Sends a password reset code by e-mail. */
class RequestPasswordResetUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String): AppResult<Unit> = repository.forgotPassword(email)
}

/** Sets a new password with the code received by e-mail. */
class ResetPasswordUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        code: String,
        newPassword: String,
    ): AppResult<Unit> = repository.resetPassword(email, code, newPassword)
}
