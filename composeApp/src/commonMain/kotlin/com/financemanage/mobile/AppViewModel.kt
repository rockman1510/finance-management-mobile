package com.financemanage.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.model.SessionState
import com.financemanage.mobile.core.domain.model.ThemeMode
import com.financemanage.mobile.core.domain.usecase.ObserveConnectivityUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePreferencesUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveSessionStateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hideBalances: Boolean = false,
    val isOnline: Boolean = true,
    val session: SessionState = SessionState.UNKNOWN,
    val onboardingCompleted: Boolean = false,
    val appLockEnabled: Boolean = false,
    val relockAfterMinutes: Int = 5,
)

/** App-wide UI state: theme, hidden balances, connectivity, session and lock settings. */
class AppViewModel(
    observePreferences: ObservePreferencesUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
    observeSessionState: ObserveSessionStateUseCase,
) : ViewModel() {
    val uiState: StateFlow<AppUiState> =
        combine(
            observePreferences(),
            observeConnectivity(),
            observeSessionState(),
        ) { preferences, online, sessionState ->
            AppUiState(
                themeMode = preferences.themeMode,
                hideBalances = preferences.hideBalances,
                isOnline = online,
                session = sessionState,
                onboardingCompleted = preferences.onboardingCompleted,
                appLockEnabled = preferences.appLockEnabled,
                relockAfterMinutes = preferences.relockAfterMinutes,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
