package com.financemanage.mobile.feature.auth.presentation

import androidx.compose.runtime.Composable

/**
 * Returns a function that asks the platform for a Google **ID token**, or `null` when Google sign-in is not
 * offered on this platform (iOS, or no client id configured). The function returns `null` when the user cancels.
 */
@Composable
expect fun rememberGoogleIdTokenRequest(webClientId: String?): (suspend () -> String?)?
