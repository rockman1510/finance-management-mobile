package com.financemanage.mobile.feature.settings.domain.usecase

import com.financemanage.mobile.core.domain.repository.SessionRepository
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.settings.domain.model.DeviceSession
import com.financemanage.mobile.feature.settings.domain.model.TwoFactorSetup
import com.financemanage.mobile.feature.settings.domain.repository.SecurityRepository

/** Sends a code to verify the e-mail address. */
class SendEmailVerificationCodeUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.sendVerificationCode()
}

class VerifyEmailUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(code: String): AppResult<Unit> = repository.verifyEmail(code)
}

/** Changes the password (or sets one for Google-only accounts, without the current one). */
class ChangePasswordUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(
        currentPassword: String?,
        newPassword: String,
    ): AppResult<Unit> = repository.changePassword(currentPassword, newPassword)
}

/** Starts the two-step verification setup: secret + QR code. */
class SetupTwoFactorUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(): AppResult<TwoFactorSetup> = repository.setupTwoFactor()
}

/** Turns two-step verification on; returns the recovery codes. */
class EnableTwoFactorUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(code: String): AppResult<List<String>> = repository.enableTwoFactor(code)
}

class DisableTwoFactorUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(code: String): AppResult<Unit> = repository.disableTwoFactor(code)
}

/** The devices signed in to the account. */
class GetDeviceSessionsUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(): AppResult<List<DeviceSession>> = repository.sessions()
}

class RevokeDeviceSessionUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.revokeSession(id)
}

/** Signs out every other device. */
class RevokeOtherDeviceSessionsUseCase(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.revokeOtherSessions()
}

/**
 * Deletes the account (the password is required unless the account is Google-only), then wipes every local trace,
 * preferences included, which ends the session (F04).
 */
class DeleteAccountUseCase(
    private val repository: SecurityRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(
        password: String?,
        confirmEmail: String,
    ): AppResult<Unit> =
        repository.deleteAccount(password, confirmEmail).also {
            if (it is AppResult.Success) session.clearLocalSession(wipeSettings = true)
        }
}
