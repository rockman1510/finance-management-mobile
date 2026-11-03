package com.financemanage.mobile.core.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    var enabled by remember { mutableStateOf(false) }
    val refresh = {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            val allowed = status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional
            dispatch_async(dispatch_get_main_queue()) { enabled = allowed }
        }
    }
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }
    return remember(enabled) {
        object : NotificationPermissionController {
            override val enabled: Boolean = enabled

            override fun request() {
                val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
                UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                    options,
                ) { granted, _ ->
                    dispatch_async(dispatch_get_main_queue()) {
                        enabled = granted
                        if (granted) UIApplication.sharedApplication.registerForRemoteNotifications()
                    }
                }
            }

            override fun openSettings() {
                val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
                UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
            }
        }
    }
}
