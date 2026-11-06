package com.financemanage.mobile.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.model.UserPreferences
import com.financemanage.mobile.core.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

class AppSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : AppSettingsRepository {
    override val preferences: Flow<UserPreferences> =
        dataStore.data.map { prefs ->
            UserPreferences(
                themeMode =
                    prefs[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                        ?: ThemeMode.SYSTEM,
                hideBalances = prefs[HIDE_BALANCES] ?: false,
                onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false,
                twoFactorCardHiddenUntil = prefs[TWO_FACTOR_CARD_HIDDEN_UNTIL],
                appLockEnabled = prefs[APP_LOCK_ENABLED] ?: false,
                relockAfterMinutes = prefs[RELOCK_AFTER_MINUTES] ?: UserPreferences.DEFAULT_RELOCK_MINUTES,
                recentCategoryIds = prefs[RECENT_CATEGORIES]?.split(',')?.filter { it.isNotBlank() }.orEmpty(),
                lastWalletId = prefs[LAST_WALLET_ID],
                notificationPermissionAsked = prefs[NOTIFICATION_PERMISSION_ASKED] ?: false,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun setHideBalances(hide: Boolean) {
        dataStore.edit { it[HIDE_BALANCES] = hide }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun hideTwoFactorCardUntil(epochMillis: Long) {
        dataStore.edit { it[TWO_FACTOR_CARD_HIDDEN_UNTIL] = epochMillis }
    }

    override suspend fun setAppLock(
        enabled: Boolean,
        relockAfterMinutes: Int,
    ) {
        dataStore.edit {
            it[APP_LOCK_ENABLED] = enabled
            it[RELOCK_AFTER_MINUTES] = relockAfterMinutes
        }
    }

    override suspend fun rememberCategory(categoryId: String) {
        dataStore.edit { prefs ->
            val current = prefs[RECENT_CATEGORIES]?.split(',')?.filter { it.isNotBlank() }.orEmpty()
            val updated =
                (
                    listOf(
                        categoryId,
                    ) + current.filter { it != categoryId }
                ).take(UserPreferences.MAX_RECENT_CATEGORIES)
            prefs[RECENT_CATEGORIES] = updated.joinToString(",")
        }
    }

    override suspend fun setLastWallet(walletId: String) {
        dataStore.edit { it[LAST_WALLET_ID] = walletId }
    }

    override suspend fun setNotificationPermissionAsked() {
        dataStore.edit { it[NOTIFICATION_PERMISSION_ASKED] = true }
    }

    /** Wipes every preference (sign-out of a deleted account). */
    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TWO_FACTOR_CARD_HIDDEN_UNTIL = longPreferencesKey("two_factor_card_hidden_until")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val RELOCK_AFTER_MINUTES = intPreferencesKey("relock_after_minutes")
        val RECENT_CATEGORIES = stringPreferencesKey("recent_categories")
        val LAST_WALLET_ID = stringPreferencesKey("last_wallet_id")
        val NOTIFICATION_PERMISSION_ASKED = booleanPreferencesKey("notification_permission_asked")
    }
}

internal const val DATASTORE_FILE_NAME = "finance_manage.preferences_pb"

internal fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
