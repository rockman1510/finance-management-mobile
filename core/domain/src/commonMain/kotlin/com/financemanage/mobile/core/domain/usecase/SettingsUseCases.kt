package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.model.UserPreferences
import com.financemanage.mobile.core.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ObservePreferencesUseCase(
    private val repository: AppSettingsRepository,
) {
    operator fun invoke(): Flow<UserPreferences> = repository.preferences
}

/** The preferences right now. */
class GetPreferencesUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(): UserPreferences = repository.preferences.first()
}

class SetThemeModeUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class SetHideBalancesUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(hide: Boolean) = repository.setHideBalances(hide)
}

class SetOnboardingCompletedUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(completed: Boolean = true) = repository.setOnboardingCompleted(completed)
}

/** Hides the "turn on two-step verification" card until [epochMillis] (F04). */
class HideTwoFactorCardUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(epochMillis: Long) = repository.hideTwoFactorCardUntil(epochMillis)
}

class SetAppLockUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(
        enabled: Boolean,
        relockAfterMinutes: Int,
    ) = repository.setAppLock(enabled, relockAfterMinutes)
}

/**
 * Remembers the category and wallet last used in a form, for the next one (F06/F07). Returns the recently used
 * category ids, most recent first.
 */
class RememberFormChoicesUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke(
        categoryId: String?,
        walletId: String?,
    ): List<String> {
        categoryId?.let { repository.rememberCategory(it) }
        walletId?.let { repository.setLastWallet(it) }
        return repository.preferences.first().recentCategoryIds
    }
}

/** Shows / hides the balances everywhere (eye icon). */
class ToggleHideBalancesUseCase(
    private val repository: AppSettingsRepository,
) {
    suspend operator fun invoke() = repository.setHideBalances(!repository.preferences.first().hideBalances)
}
