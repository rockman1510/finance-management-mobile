package com.financemanage.mobile

import com.financemanage.mobile.core.data.push.PushBridge

/**
 * Push entry points for Swift (F20, API §14.1): `IosPush.shared.onNewToken(token:)` from the Firebase iOS SDK's
 * `messaging(_:didReceiveRegistrationToken:)`, and the notification/URL callbacks of the app delegate.
 */
object IosPush {
    fun onNewToken(token: String) = PushBridge.onNewToken(token)

    fun onNotificationOpened(
        referenceType: String?,
        referenceId: String?,
    ) = PushBridge.onNotificationOpened(referenceType, referenceId)

    fun onDeepLink(url: String): Boolean = PushBridge.onDeepLink(url)

    fun onMessageReceived() = PushBridge.onMessageReceived()
}
