package com.financemanage.mobile.feature.notification.domain.usecase

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.notification.domain.model.NotificationDto
import com.financemanage.mobile.feature.notification.domain.model.NotificationPreferences
import com.financemanage.mobile.feature.notification.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

/** Observes the cached notifications. */
class ObserveNotificationsUseCase(
    private val repository: NotificationRepository,
) {
    operator fun invoke(): Flow<Cached<List<NotificationDto>>?> = repository.notifications
}

class RefreshNotificationsUseCase(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(): AppResult<List<NotificationDto>> = repository.refresh()
}

class MarkNotificationReadUseCase(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.markRead(id)
}

class MarkAllNotificationsReadUseCase(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.markAllRead()
}

class GetNotificationPreferencesUseCase(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(): AppResult<NotificationPreferences> = repository.preferences()
}

class SaveNotificationPreferencesUseCase(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(preferences: NotificationPreferences): AppResult<NotificationPreferences> =
        repository.savePreferences(preferences)
}
