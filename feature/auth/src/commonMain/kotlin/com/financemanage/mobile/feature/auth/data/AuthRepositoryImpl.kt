package com.financemanage.mobile.feature.auth.data

import com.financemanage.mobile.core.data.platform.deviceName
import com.financemanage.mobile.core.data.session.SessionManager
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.auth.AuthTokens
import com.financemanage.mobile.core.network.auth.TokenPairDto
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.feature.auth.domain.AuthRepository
import com.financemanage.mobile.feature.auth.domain.LoginResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

private const val DEFAULT_CHALLENGE_SECONDS = 300L

internal class AuthRepositoryImpl(
    private val client: HttpClient,
    private val session: SessionManager,
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): AppResult<LoginResult> =
        handleLogin(
            apiCall { post("auth/login", LoginRequest(email.trim(), password, deviceName())).body<LoginResponseDto>() },
        )

    override suspend fun loginWithGoogle(idToken: String): AppResult<LoginResult> =
        handleLogin(apiCall { post("auth/google", GoogleLoginRequest(idToken, deviceName())).body<LoginResponseDto>() })

    override suspend fun verifyTwoFactor(
        challengeToken: String,
        code: String,
    ): AppResult<Unit> =
        signInWith(
            apiCall {
                post(
                    "auth/2fa/verify",
                    TwoFactorLoginRequest(challengeToken, code.trim()),
                ).body<TokenPairDto>()
            },
        )

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
    ): AppResult<Unit> =
        signInWith(
            apiCall {
                post(
                    "auth/register",
                    RegisterRequest(email.trim(), password, fullName.trim(), deviceName()),
                ).body<TokenPairDto>()
            },
        )

    override suspend fun forgotPassword(email: String): AppResult<Unit> =
        apiCall {
            post("auth/forgot-password", ForgotPasswordRequest(email.trim()))
            Unit
        }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
    ): AppResult<Unit> =
        apiCall {
            post("auth/reset-password", ResetPasswordRequest(email.trim(), code.trim(), newPassword))
            Unit
        }

    private suspend fun post(
        path: String,
        body: Any,
    ) = client.post(api(path)) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun handleLogin(result: AppResult<LoginResponseDto>): AppResult<LoginResult> =
        when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val dto = result.value
                val access = dto.accessToken
                val refresh = dto.refreshToken
                when {
                    dto.twoFactorRequired && dto.challengeToken != null ->
                        AppResult.Success(
                            LoginResult.TwoFactorRequired(
                                dto.challengeToken,
                                dto.challengeExpiresInSeconds ?: DEFAULT_CHALLENGE_SECONDS,
                            ),
                        )
                    access != null && refresh != null -> {
                        session.signIn(AuthTokens(access, refresh))
                        AppResult.Success(LoginResult.SignedIn)
                    }
                    else -> AppResult.Failure(AppError.Unexpected())
                }
            }
        }

    private suspend fun signInWith(result: AppResult<TokenPairDto>): AppResult<Unit> =
        when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                session.signIn(result.value.toAuthTokens())
                AppResult.Success(Unit)
            }
        }
}
