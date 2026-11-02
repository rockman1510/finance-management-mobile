package com.financemanage.mobile.feature.settings.data

import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.map
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
private data class CodeRequest(
    val code: String,
)

@Serializable
private data class ChangePasswordRequest(
    val currentPassword: String? = null,
    val newPassword: String,
)

@Serializable
private data class DeleteAccountRequest(
    val password: String? = null,
    val confirmEmail: String,
)

@Serializable
data class TwoFactorSetup(
    val secret: String = "",
    val otpauthUri: String = "",
)

@Serializable
private data class RecoveryCodesDto(
    val recoveryCodes: List<String> = emptyList(),
)

@Serializable
data class DeviceSession(
    val id: String,
    val deviceName: String? = null,
    val lastActiveAt: Instant? = null,
    val expiresAt: Instant? = null,
    val current: Boolean = false,
)

/** Account security calls (API §4.2). Profile changes go through the shared [ProfileRepository]. */
class SecurityRepository(
    private val client: HttpClient,
    private val profiles: ProfileRepository,
) {
    suspend fun sendVerificationCode(): AppResult<Unit> =
        apiCall {
            client.post(api("me/email/verification-code")).ignore()
        }

    suspend fun verifyEmail(code: String): AppResult<Unit> =
        apiCall {
            client
                .post(
                    api("me/email/verify"),
                ) { json(CodeRequest(code)) }
                .ignore()
        }.onSuccess { profiles.refresh() }

    suspend fun changePassword(
        currentPassword: String?,
        newPassword: String,
    ): AppResult<Unit> =
        apiCall {
            client
                .post(
                    api("me/password"),
                ) { json(ChangePasswordRequest(currentPassword, newPassword)) }
                .ignore()
        }.onSuccess { profiles.refresh() }

    suspend fun setupTwoFactor(): AppResult<TwoFactorSetup> =
        apiCall {
            client.post(api("me/2fa/setup")).body<TwoFactorSetup>()
        }

    suspend fun enableTwoFactor(code: String): AppResult<List<String>> =
        apiCall { client.post(api("me/2fa/enable")) { json(CodeRequest(code)) }.body<RecoveryCodesDto>() }
            .onSuccess { profiles.refresh() }
            .map { it.recoveryCodes }

    suspend fun disableTwoFactor(code: String): AppResult<Unit> =
        apiCall {
            client
                .post(
                    api("me/2fa/disable"),
                ) { json(CodeRequest(code)) }
                .ignore()
        }.onSuccess { profiles.refresh() }

    suspend fun sessions(): AppResult<List<DeviceSession>> =
        apiCall {
            client.get(api("me/sessions")).body<List<DeviceSession>>()
        }

    suspend fun revokeSession(id: String): AppResult<Unit> = apiCall { client.delete(api("me/sessions/$id")).ignore() }

    suspend fun revokeOtherSessions(): AppResult<Unit> = apiCall { client.delete(api("me/sessions")).ignore() }

    suspend fun deleteAccount(
        password: String?,
        confirmEmail: String,
    ): AppResult<Unit> =
        apiCall {
            client
                .delete(api("me")) {
                    json(DeleteAccountRequest(password?.takeIf { it.isNotEmpty() }, confirmEmail.trim()))
                }.ignore()
        }

    private fun io.ktor.client.request.HttpRequestBuilder.json(body: Any) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    @Suppress("UnusedReceiverParameter")
    private fun io.ktor.client.statement.HttpResponse.ignore() = Unit
}
