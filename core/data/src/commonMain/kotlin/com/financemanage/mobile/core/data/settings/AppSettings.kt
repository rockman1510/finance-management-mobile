package com.financemanage.mobile.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Local, non-secret preferences of the device. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hideBalances: Boolean = false,
    val onboardingCompleted: Boolean = false,
    /** Epoch ms until which the "turn on two-step verification" card stays hidden (F04). */
    val twoFactorCardHiddenUntil: Long? = null,
)

class AppSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<UserPreferences> =
        dataStore.data.map { prefs ->
            UserPreferences(
                themeMode =
                    prefs[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                        ?: ThemeMode.SYSTEM,
                hideBalances = prefs[HIDE_BALANCES] ?: false,
                onboardingCompleted = prefs[ONBOARDING_COMPLETED] ?: false,
                twoFactorCardHiddenUntil = prefs[TWO_FACTOR_CARD_HIDDEN_UNTIL],
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setHideBalances(hide: Boolean) {
        dataStore.edit { it[HIDE_BALANCES] = hide }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun hideTwoFactorCardUntil(epochMillis: Long) {
        dataStore.edit { it[TWO_FACTOR_CARD_HIDDEN_UNTIL] = epochMillis }
    }

    /** Wipes every preference (sign-out of a deleted account). */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TWO_FACTOR_CARD_HIDDEN_UNTIL = longPreferencesKey("two_factor_card_hidden_until")
    }
}

internal const val DATASTORE_FILE_NAME = "finance_manage.preferences_pb"

internal fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
