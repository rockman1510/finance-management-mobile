package com.financemanage.mobile.core.data

import com.financemanage.mobile.core.data.settings.AppSettingsRepositoryImpl
import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.model.UserPreferences
import com.financemanage.mobile.core.testing.InMemoryPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsRepositoryTest {
    @Test
    fun defaultsThenEveryValueIsReadBack() =
        runTest {
            val settings = AppSettingsRepositoryImpl(InMemoryPreferencesDataStore())
            assertEquals(UserPreferences(), settings.preferences.first())

            settings.setThemeMode(ThemeMode.DARK)
            settings.setHideBalances(true)
            settings.setOnboardingCompleted(true)
            settings.hideTwoFactorCardUntil(42L)
            settings.setAppLock(enabled = true, relockAfterMinutes = 15)
            settings.setLastWallet("w1")
            settings.setNotificationPermissionAsked()

            assertEquals(
                UserPreferences(
                    themeMode = ThemeMode.DARK,
                    hideBalances = true,
                    onboardingCompleted = true,
                    twoFactorCardHiddenUntil = 42L,
                    appLockEnabled = true,
                    relockAfterMinutes = 15,
                    lastWalletId = "w1",
                    notificationPermissionAsked = true,
                ),
                settings.preferences.first(),
            )

            settings.clear()
            assertEquals(UserPreferences(), settings.preferences.first())
        }

    @Test
    fun recentCategoriesAreMostRecentFirstWithoutDuplicatesAndCapped() =
        runTest {
            val settings = AppSettingsRepositoryImpl(InMemoryPreferencesDataStore())
            (1..10).forEach { settings.rememberCategory("c$it") }
            settings.rememberCategory("c5")

            val recent = settings.preferences.first().recentCategoryIds
            assertEquals(UserPreferences.MAX_RECENT_CATEGORIES, recent.size)
            assertEquals(listOf("c5", "c10", "c9", "c8", "c7", "c6", "c4", "c3"), recent)
        }
}
