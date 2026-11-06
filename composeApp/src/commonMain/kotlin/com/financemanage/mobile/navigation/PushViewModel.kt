package com.financemanage.mobile.navigation

import androidx.lifecycle.ViewModel
import com.financemanage.mobile.core.domain.model.NotificationLink
import com.financemanage.mobile.core.domain.usecase.ConsumeOpenedNotificationUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveNotificationPermissionRequestsUseCase
import com.financemanage.mobile.core.domain.usecase.ObserveOpenedNotificationUseCase
import com.financemanage.mobile.core.domain.usecase.ShouldAskNotificationPermissionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Signed-in push glue (F20): the notification / deep link to open and the permission requests of the features. */
class PushViewModel(
    observeOpenedNotification: ObserveOpenedNotificationUseCase,
    private val consumeOpenedNotification: ConsumeOpenedNotificationUseCase,
    observePermissionRequests: ObserveNotificationPermissionRequestsUseCase,
    private val shouldAskNotificationPermission: ShouldAskNotificationPermissionUseCase,
) : ViewModel() {
    val opened: StateFlow<NotificationLink?> = observeOpenedNotification()

    val permissionRequests: Flow<Unit> = observePermissionRequests()

    /** The UI navigated to [link]. */
    fun consume(link: NotificationLink) = consumeOpenedNotification(link)

    /** Whether to show the system permission prompt now (once per install, never when already granted). */
    suspend fun shouldAskPermission(alreadyGranted: Boolean): Boolean = shouldAskNotificationPermission(alreadyGranted)
}
