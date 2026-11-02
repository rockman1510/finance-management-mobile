package com.financemanage.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.data.connectivity.NetworkMonitor
import com.financemanage.mobile.core.data.settings.AppSettingsRepository
import com.financemanage.mobile.core.data.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hideBalances: Boolean = false,
    val isOnline: Boolean = true,
)

/** App-wide UI state: theme, hidden balances and connectivity. */
class AppViewModel(
    settings: AppSettingsRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {
    val uiState: StateFlow<AppUiState> =
        combine(settings.preferences, networkMonitor.isOnline) { preferences, online ->
            AppUiState(
                themeMode = preferences.themeMode,
                hideBalances = preferences.hideBalances,
                isOnline = online,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
