package com.financemanage.mobile.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.financemanage.mobile.core.ui.platform.rememberBiometricAuthenticator
import com.financemanage.mobile.core.ui.resources.Res
import com.financemanage.mobile.core.ui.resources.app_locked
import com.financemanage.mobile.core.ui.resources.unlock
import com.financemanage.mobile.core.ui.resources.unlock_reason
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * App lock (F04): when enabled, the app is locked on cold start and after [relockAfterMinutes] in the background.
 * Unlocking uses fingerprint / Face ID with the device passcode as fallback.
 */
@Composable
fun AppLockGate(
    enabled: Boolean,
    relockAfterMinutes: Int,
    content: @Composable () -> Unit,
) {
    var locked by rememberSaveable { mutableStateOf(enabled) }
    var backgroundedAt by remember { mutableLongStateOf(0L) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { backgroundedAt = Clock.System.now().toEpochMilliseconds() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        val away = Clock.System.now().toEpochMilliseconds() - backgroundedAt
        if (enabled && backgroundedAt > 0 && away >= relockAfterMinutes * MILLIS_PER_MINUTE) locked = true
    }
    LaunchedEffect(enabled) { if (!enabled) locked = false }

    if (enabled && locked) LockScreen(onUnlocked = { locked = false }) else content()
}

@Composable
private fun LockScreen(onUnlocked: () -> Unit) {
    val authenticator = rememberBiometricAuthenticator()
    val scope = rememberCoroutineScope()
    val reason = stringResource(Res.string.unlock_reason)
    val unlock: () -> Unit = {
        scope.launch {
            // Without any biometric/passcode on the device the lock cannot be enforced: let the user in.
            if (!authenticator.isAvailable || authenticator.authenticate(reason)) onUnlocked()
        }
    }
    LaunchedEffect(Unit) { unlock() }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(Res.string.app_locked), style = MaterialTheme.typography.titleMedium)
        Button(onClick = unlock) { Text(stringResource(Res.string.unlock)) }
    }
}
