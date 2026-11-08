package com.financemanage.mobile.feature.notification.domain.repository

import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.feature.notification.domain.model.NotificationDto
import com.financemanage.mobile.feature.notification.domain.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow

/** In-app notifications and notification preferences (API §14). */
interface NotificationRepository {
    val notifications: Flow<Cached<List<NotificationDto>>?>

    suspend fun refresh(): AppResult<List<NotificationDto>>

    /** Marks one notification as read; the cache is updated first so the list reacts at once. */
    suspend fun markRead(id: String): AppResult<Unit>

    suspend fun markAllRead(): AppResult<Unit>

    suspend fun preferences(): AppResult<NotificationPreferences>

    suspend fun savePreferences(preferences: NotificationPreferences): AppResult<NotificationPreferences>
}
