package com.financemanage.mobile.core.ui.platform

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

private class AndroidBiometricAuthenticator(
    private val activity: FragmentActivity?,
) : BiometricAuthenticator {
    override val isAvailable: Boolean
        get() =
            activity != null &&
                BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun authenticate(reason: String): Boolean {
        val host = activity ?: return false
        return suspendCancellableCoroutine { continuation ->
            val prompt =
                BiometricPrompt(
                    host,
                    ContextCompat.getMainExecutor(host),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    },
                )
            val info =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(reason)
                    .setAllowedAuthenticators(AUTHENTICATORS)
                    .build()
            prompt.authenticate(info)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val context = LocalContext.current
    return remember(context) { AndroidBiometricAuthenticator(context as? FragmentActivity) }
}
