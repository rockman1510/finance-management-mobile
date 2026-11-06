package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Local, non-secret preferences of the device. */
interface AppSettingsRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setHideBalances(hide: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun hideTwoFactorCardUntil(epochMillis: Long)

    suspend fun setAppLock(
        enabled: Boolean,
        relockAfterMinutes: Int,
    )

    suspend fun rememberCategory(categoryId: String)

    suspend fun setLastWallet(walletId: String)

    suspend fun setNotificationPermissionAsked()

    /** Wipes every preference (sign-out of a deleted account). */
    suspend fun clear()
}
