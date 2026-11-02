package com.financemanage.mobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import kotlin.coroutines.resume

private class IosBiometricAuthenticator : BiometricAuthenticator {
    @OptIn(ExperimentalForeignApi::class)
    override val isAvailable: Boolean
        get() = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)

    override suspend fun authenticate(reason: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            LAContext().evaluatePolicy(LAPolicyDeviceOwnerAuthentication, localizedReason = reason) { success, _ ->
                if (continuation.isActive) continuation.resume(success)
            }
        }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator = remember { IosBiometricAuthenticator() }
