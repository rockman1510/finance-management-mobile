package com.financemanage.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.data.settings.ThemeMode
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.navigation.MainScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val viewModel = koinViewModel<AppViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val darkTheme =
        when (state.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    FinanceTheme(darkTheme = darkTheme, hideBalances = state.hideBalances) {
        MainScaffold(isOnline = state.isOnline)
    }
}
