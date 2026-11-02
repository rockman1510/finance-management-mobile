package com.financemanage.mobile.feature.auth.presentation

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.financemanage.mobile.feature.auth.di.AuthConfig
import com.financemanage.mobile.feature.auth.resources.Res
import com.financemanage.mobile.feature.auth.resources.reset_done
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject

@Serializable private data object WelcomeDest

@Serializable private data object SignInDest

@Serializable private data object SignUpDest

@Serializable private data object ForgotPasswordDest

@Serializable private data class TwoFactorDest(
    val challengeToken: String,
    val expiresInSeconds: Long,
)

/**
 * Signed-out flow: Welcome → Sign in / Sign up / Forgot password / 2FA. When sign-in succeeds the session
 * becomes SIGNED_IN and the app leaves this flow by itself.
 */
@Composable
fun AuthFlow() {
    val navController = rememberNavController()
    val config = koinInject<AuthConfig>()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    androidx.compose.foundation.layout.Box {
        NavHost(navController = navController, startDestination = WelcomeDest) {
            composable<WelcomeDest> {
                WelcomeScreen(
                    onSignIn = { navController.navigate(SignInDest) },
                    onSignUp = { navController.navigate(SignUpDest) },
                )
            }
            composable<SignInDest> {
                SignInScreen(
                    googleWebClientId = config.googleWebClientId,
                    onTwoFactorRequired = { token, expires -> navController.navigate(TwoFactorDest(token, expires)) },
                    onForgotPassword = { navController.navigate(ForgotPasswordDest) },
                    onSignUp = {
                        navController.navigate(SignUpDest) { popUpTo(WelcomeDest) }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<SignUpDest> {
                SignUpScreen(
                    onSignIn = { navController.navigate(SignInDest) { popUpTo(WelcomeDest) } },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<TwoFactorDest> { entry ->
                val dest = entry.toRoute<TwoFactorDest>()
                TwoFactorScreen(
                    challengeToken = dest.challengeToken,
                    expiresInSeconds = dest.expiresInSeconds,
                    onExpired = { navController.popBackStack(SignInDest, inclusive = false) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<ForgotPasswordDest> {
                ForgotPasswordScreen(
                    onDone = {
                        navController.popBackStack(SignInDest, inclusive = false)
                        scope.launch { snackbar.showSnackbar(getString(Res.string.reset_done)) }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
        SnackbarHost(
            snackbar,
            modifier =
                androidx.compose.ui.Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter),
        )
    }
}
