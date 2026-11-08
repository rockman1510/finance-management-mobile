package com.financemanage.mobile.feature.settings.domain.repository

import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.settings.domain.model.DeviceSession
import com.financemanage.mobile.feature.settings.domain.model.TwoFactorSetup

/** Account security: e-mail verification, password, two-step verification, sessions, deletion (API §4). */
interface SecurityRepository {
    suspend fun sendVerificationCode(): AppResult<Unit>

    suspend fun verifyEmail(code: String): AppResult<Unit>

    suspend fun changePassword(
        currentPassword: String?,
        newPassword: String,
    ): AppResult<Unit>

    suspend fun setupTwoFactor(): AppResult<TwoFactorSetup>

    suspend fun enableTwoFactor(code: String): AppResult<List<String>>

    suspend fun disableTwoFactor(code: String): AppResult<Unit>

    suspend fun sessions(): AppResult<List<DeviceSession>>

    suspend fun revokeSession(id: String): AppResult<Unit>

    suspend fun revokeOtherSessions(): AppResult<Unit>

    suspend fun deleteAccount(
        password: String?,
        confirmEmail: String,
    ): AppResult<Unit>
}
