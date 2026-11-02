package com.financemanage.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.financemanage.mobile.core.data.session.SessionState
import com.financemanage.mobile.core.data.settings.ThemeMode
import com.financemanage.mobile.core.ui.theme.FinanceTheme
import com.financemanage.mobile.feature.auth.presentation.AuthFlow
import com.financemanage.mobile.feature.onboarding.presentation.OnboardingFlow
import com.financemanage.mobile.feature.settings.presentation.TwoFactorScreen
import com.financemanage.mobile.lock.AppLockGate
import com.financemanage.mobile.navigation.MainScaffold
import org.koin.compose.viewmodel.koinViewModel

/** Root: Splash → (signed out) Auth → (first run) Onboarding → app lock → main app. */
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
        Surface(Modifier.fillMaxSize()) {
            when (state.session) {
                SessionState.UNKNOWN ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                SessionState.SIGNED_OUT -> AuthFlow()
                SessionState.SIGNED_IN ->
                    if (!state.onboardingCompleted) {
                        OnboardingFlow(
                            twoFactorSetup = { onFinished ->
                                TwoFactorScreen(onFinished = onFinished, startSetupImmediately = true)
                            },
                            // Completion is stored in the settings; this composable switches by itself.
                            onFinished = {},
                        )
                    } else {
                        AppLockGate(enabled = state.appLockEnabled, relockAfterMinutes = state.relockAfterMinutes) {
                            MainScaffold(isOnline = state.isOnline)
                        }
                    }
            }
        }
    }
}
