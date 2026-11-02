package com.financemanage.mobile.feature.auth.data

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequest(
    val email: String,
    val password: String,
    val deviceName: String? = null,
)

@Serializable
internal data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val deviceName: String? = null,
)

@Serializable
internal data class GoogleLoginRequest(
    val idToken: String,
    val deviceName: String? = null,
)

@Serializable
internal data class TwoFactorLoginRequest(
    val challengeToken: String,
    val code: String,
)

@Serializable
internal data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
internal data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String,
)

/** `LoginResponse`: either tokens, or a 2FA challenge (API §4.1). */
@Serializable
internal data class LoginResponseDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresInSeconds: Long? = null,
    val twoFactorRequired: Boolean = false,
    val challengeToken: String? = null,
    val challengeExpiresInSeconds: Long? = null,
)
