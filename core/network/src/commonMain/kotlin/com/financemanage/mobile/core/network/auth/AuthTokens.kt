package com.financemanage.mobile.core.network.auth

import kotlinx.serialization.Serializable

/** The pair stored on the device (Keystore / Keychain). The refresh token is single use (API §4.1). */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

/** `TokenPair` returned by `/auth/register`, `/auth/2fa/verify`, `/auth/refresh`. */
@Serializable
data class TokenPairDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long? = null,
) {
    fun toAuthTokens(): AuthTokens = AuthTokens(accessToken, refreshToken)
}

@Serializable
internal data class RefreshRequest(
    val refreshToken: String,
)
