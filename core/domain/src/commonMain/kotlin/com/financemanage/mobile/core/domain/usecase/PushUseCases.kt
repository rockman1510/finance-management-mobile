package com.financemanage.mobile.core.domain.usecase

import com.financemanage.mobile.core.domain.model.NotificationLink
import com.financemanage.mobile.core.domain.repository.AppSettingsRepository
import com.financemanage.mobile.core.domain.repository.PushRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/** Emits whenever a push arrives while the app is in the foreground (refresh lists and badges). */
class ObservePushReceivedUseCase(
    private val repository: PushRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.received
}

/** Asks for the notification permission (after the first budget or bill is created, F20). */
class RequestNotificationPermissionUseCase(
    private val repository: PushRepository,
) {
    operator fun invoke() = repository.requestPermission()
}

/** The notification / deep link waiting to be opened. */
class ObserveOpenedNotificationUseCase(
    private val repository: PushRepository,
) {
    operator fun invoke(): StateFlow<NotificationLink?> = repository.opened
}

class ConsumeOpenedNotificationUseCase(
    private val repository: PushRepository,
) {
    operator fun invoke(link: NotificationLink) = repository.consumeOpened(link)
}

/** Permission requests from the features. */
class ObserveNotificationPermissionRequestsUseCase(
    private val repository: PushRepository,
) {
    operator fun invoke(): Flow<Unit> = repository.permissionRequests
}

/**
 * Whether the notification permission should be requested now: only once per install, and never when it is already
 * granted. Records that it was asked.
 */
class ShouldAskNotificationPermissionUseCase(
    private val settings: AppSettingsRepository,
) {
    suspend operator fun invoke(alreadyGranted: Boolean): Boolean {
        if (alreadyGranted || settings.preferences.first().notificationPermissionAsked) return false
        settings.setNotificationPermissionAsked()
        return true
    }
}
