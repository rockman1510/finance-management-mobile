package com.financemanage.mobile.feature.notification.data

import com.financemanage.mobile.core.data.cache.Cached
import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

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

@Serializable
private data class PreferencesRequest(
    val disabledTypes: Set<String>,
    val dailyReminderEnabled: Boolean,
    val dailyReminderTime: LocalTime?,
    val quietStart: LocalTime?,
    val quietEnd: LocalTime?,
)

class NotificationRepository(
    private val client: HttpClient,
    private val cache: JsonCache,
) {
    private val listSerializer = ListSerializer(NotificationDto.serializer())

    val notifications: Flow<Cached<List<NotificationDto>>?> = cache.observe(KEY, listSerializer)

    suspend fun refresh(): AppResult<List<NotificationDto>> =
        apiCall { client.get(api("notifications")).body<List<NotificationDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }

    /** Marks one notification as read; the cache is updated first so the list reacts at once. */
    suspend fun markRead(id: String): AppResult<Unit> {
        updateCached { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
        return apiCall {
            client.post(api("notifications/$id/read"))
            Unit
        }
    }

    suspend fun markAllRead(): AppResult<Unit> {
        val result =
            apiCall {
                client.post(api("notifications/read-all"))
                Unit
            }
        result.onSuccess { updateCached { list -> list.map { it.copy(read = true) } } }
        return result
    }

    suspend fun preferences(): AppResult<NotificationPreferences> =
        apiCall { client.get(api("notifications/preferences")).body<NotificationPreferences>() }

    suspend fun savePreferences(preferences: NotificationPreferences): AppResult<NotificationPreferences> =
        apiCall {
            client
                .put(api("notifications/preferences")) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        PreferencesRequest(
                            disabledTypes = preferences.disabledTypes,
                            dailyReminderEnabled = preferences.dailyReminderEnabled,
                            dailyReminderTime = preferences.dailyReminderTime,
                            quietStart = preferences.quietStart,
                            quietEnd = preferences.quietEnd,
                        ),
                    )
                }.body<NotificationPreferences>()
        }

    private suspend fun updateCached(transform: (List<NotificationDto>) -> List<NotificationDto>) {
        val current = cache.get(KEY, listSerializer)?.value ?: return
        cache.put(KEY, listSerializer, transform(current))
    }

    private companion object {
        const val KEY = "notifications"
    }
}
