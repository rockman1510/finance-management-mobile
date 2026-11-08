package com.financemanage.mobile.feature.notification.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

/** One notification (API §14). `type` stays a string so new backend types never break the list. */
@Serializable
data class NotificationDto(
    val id: String,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdAt: Instant? = null,
    val read: Boolean = false,
)

/** Notification preferences: `GET/PUT /notifications/preferences` (`timeZone` is read-only). */
@Serializable
data class NotificationPreferences(
    val disabledTypes: Set<String> = emptySet(),
    val dailyReminderEnabled: Boolean = false,
    val dailyReminderTime: LocalTime? = null,
    val quietStart: LocalTime? = null,
    val quietEnd: LocalTime? = null,
    val timeZone: String? = null,
)
