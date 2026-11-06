package com.financemanage.mobile.feature.settings.presentation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.financemanage.mobile.core.navigation.AppLockRoute
import com.financemanage.mobile.core.navigation.CategoriesRoute
import com.financemanage.mobile.core.navigation.DeleteAccountRoute
import com.financemanage.mobile.core.navigation.EmailVerificationRoute
import com.financemanage.mobile.core.navigation.ExchangeRatesRoute
import com.financemanage.mobile.core.navigation.NotificationSettingsRoute
import com.financemanage.mobile.core.navigation.OutboxRoute
import com.financemanage.mobile.core.navigation.PasswordRoute
import com.financemanage.mobile.core.navigation.ProfileEditRoute
import com.financemanage.mobile.core.navigation.RecurringListRoute
import com.financemanage.mobile.core.navigation.SessionsRoute
import com.financemanage.mobile.core.navigation.SettingsRoute
import com.financemanage.mobile.core.navigation.TwoFactorRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

/** Opens the 2FA screen directly on the setup step (suggestion card). */
@Serializable data object TwoFactorSetupRoute

/** Settings destinations (F04). */
fun NavGraphBuilder.settingsGraph(navController: NavController) {
    composable<SettingsRoute> {
        SettingsScreen(
            onNavigate = { destination ->
                navController.navigate(
                    when (destination) {
                        SettingsDestination.PROFILE -> ProfileEditRoute
                        SettingsDestination.EMAIL -> EmailVerificationRoute
                        SettingsDestination.PASSWORD -> PasswordRoute
                        SettingsDestination.TWO_FACTOR -> TwoFactorRoute
                        SettingsDestination.TWO_FACTOR_SETUP -> TwoFactorSetupRoute
                        SettingsDestination.SESSIONS -> SessionsRoute
                        SettingsDestination.NOTIFICATIONS -> NotificationSettingsRoute
                        SettingsDestination.EXCHANGE_RATES -> ExchangeRatesRoute
                        SettingsDestination.CATEGORIES -> CategoriesRoute
                        SettingsDestination.RECURRING -> RecurringListRoute
                        SettingsDestination.OUTBOX -> OutboxRoute
                        SettingsDestination.APP_LOCK -> AppLockRoute
                        SettingsDestination.DELETE_ACCOUNT -> DeleteAccountRoute
                    },
                )
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable<ProfileEditRoute> { ProfileEditScreen(onBack = { navController.popBackStack() }) }
    composable<EmailVerificationRoute> { EmailVerificationScreen(onBack = { navController.popBackStack() }) }
    composable<PasswordRoute> { PasswordScreen(onBack = { navController.popBackStack() }) }
    composable<TwoFactorRoute> { TwoFactorScreen(onFinished = { navController.popBackStack() }) }
    composable<TwoFactorSetupRoute> {
        TwoFactorScreen(onFinished = { navController.popBackStack() }, startSetupImmediately = true)
    }
    composable<SessionsRoute> { SessionsScreen(onBack = { navController.popBackStack() }) }
    composable<DeleteAccountRoute> { DeleteAccountScreen(onBack = { navController.popBackStack() }) }
    composable<AppLockRoute> {
        val viewModel = koinViewModel<AppLockViewModel>()
        val preferences by viewModel.preferences.collectAsStateWithLifecycle()
        AppLockScreen(
            preferences = preferences,
            onChange = viewModel::setAppLock,
            onBack = { navController.popBackStack() },
        )
    }
}
