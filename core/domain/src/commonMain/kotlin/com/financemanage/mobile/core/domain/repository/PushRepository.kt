package com.financemanage.mobile.core.domain.repository

import com.financemanage.mobile.core.domain.model.NotificationLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Push notifications as seen by the shared code (F20). */
interface PushRepository {
    /** Notification tapped / deep link opened, waiting to be handled by the UI. */
    val opened: StateFlow<NotificationLink?>

    /** A push arrived while the app is in the foreground. */
    val received: Flow<Unit>

    /** A feature asked for the notification permission. */
    val permissionRequests: Flow<Unit>

    fun consumeOpened(link: NotificationLink)

    fun requestPermission()
}
