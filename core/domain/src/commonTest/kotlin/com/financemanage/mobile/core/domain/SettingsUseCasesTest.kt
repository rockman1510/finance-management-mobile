package com.financemanage.mobile.core.domain

import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.model.UserPreferences
import com.financemanage.mobile.core.domain.repository.AppSettingsRepository
import com.financemanage.mobile.core.domain.usecase.RememberFormChoicesUseCase
import com.financemanage.mobile.core.domain.usecase.ShouldAskNotificationPermissionUseCase
import com.financemanage.mobile.core.domain.usecase.ToggleHideBalancesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeSettings : AppSettingsRepository {
    val state = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = state

    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }

    override suspend fun setHideBalances(hide: Boolean) = state.update { it.copy(hideBalances = hide) }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        state.update { it.copy(onboardingCompleted = completed) }

    override suspend fun hideTwoFactorCardUntil(epochMillis: Long) =
        state.update { it.copy(twoFactorCardHiddenUntil = epochMillis) }

    override suspend fun setAppLock(
        enabled: Boolean,
        relockAfterMinutes: Int,
    ) = state.update { it.copy(appLockEnabled = enabled, relockAfterMinutes = relockAfterMinutes) }

    override suspend fun rememberCategory(categoryId: String) =
        state.update { it.copy(recentCategoryIds = listOf(categoryId) + (it.recentCategoryIds - categoryId)) }

    override suspend fun setLastWallet(walletId: String) = state.update { it.copy(lastWalletId = walletId) }

    override suspend fun setNotificationPermissionAsked() = state.update { it.copy(notificationPermissionAsked = true) }

    override suspend fun clear() {
        state.value = UserPreferences()
    }
}

class SettingsUseCasesTest {
    @Test
    fun notificationPermissionIsAskedOnceAndNeverWhenAlreadyGranted() =
        runTest {
            val settings = FakeSettings()
            val shouldAsk = ShouldAskNotificationPermissionUseCase(settings)

            assertFalse(shouldAsk(alreadyGranted = true))
            assertFalse(settings.state.value.notificationPermissionAsked)

            assertTrue(shouldAsk(alreadyGranted = false))
            assertTrue(settings.state.value.notificationPermissionAsked)
            assertFalse(shouldAsk(alreadyGranted = false))
        }

    @Test
    fun toggleHideBalancesFlipsThePreference() =
        runTest {
            val settings = FakeSettings()
            val toggle = ToggleHideBalancesUseCase(settings)
            toggle()
            assertTrue(settings.state.value.hideBalances)
            toggle()
            assertFalse(settings.state.value.hideBalances)
        }

    @Test
    fun rememberFormChoicesReturnsTheRecentCategories() =
        runTest {
            val settings = FakeSettings()
            val remember = RememberFormChoicesUseCase(settings)
            remember("food", "cash")
            assertEquals(listOf("cafe", "food"), remember("cafe", null))
            assertEquals("cash", settings.state.value.lastWalletId)
        }
}
