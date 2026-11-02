package com.financemanage.mobile.core.ui.platform

import androidx.compose.runtime.Composable

/** Fingerprint / Face ID with the device passcode as fallback. */
interface BiometricAuthenticator {
    /** `true` when the device has a biometric or a passcode set up. */
    val isAvailable: Boolean

    /** Shows the system prompt; returns `true` when the user authenticated. */
    suspend fun authenticate(reason: String): Boolean
}

@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator
