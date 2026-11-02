package com.financemanage.mobile.feature.auth.presentation

import androidx.compose.runtime.Composable

// No Google button on iOS (App Store guideline 4.8 would require Sign in with Apple too) – docs/Claude.md F02.
@Composable
actual fun rememberGoogleIdTokenRequest(webClientId: String?): (suspend () -> String?)? = null
