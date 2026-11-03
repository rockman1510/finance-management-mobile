package com.financemanage.mobile.core.ui.platform

import androidx.compose.runtime.Composable

/** Notification permission helpers (F20): Android 13+ runtime permission, iOS `UNUserNotificationCenter`. */
interface NotificationPermissionController {
    /** Whether the system currently lets the app show notifications (re-read on resume). */
    val enabled: Boolean

    /** Shows the system permission prompt (no-op when it was already granted or permanently denied). */
    fun request()

    /** Opens the app's notification settings in the system settings app. */
    fun openSettings()
}

@Composable
expect fun rememberNotificationPermissionController(): NotificationPermissionController
