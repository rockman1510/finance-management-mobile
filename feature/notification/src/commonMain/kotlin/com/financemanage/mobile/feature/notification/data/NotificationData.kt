package com.financemanage.mobile.feature.notification.data

import com.financemanage.mobile.core.data.cache.JsonCache
import com.financemanage.mobile.core.domain.model.Cached
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.onSuccess
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import com.financemanage.mobile.feature.notification.domain.model.NotificationDto
import com.financemanage.mobile.feature.notification.domain.model.NotificationPreferences
import com.financemanage.mobile.feature.notification.domain.repository.NotificationRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
private data class PreferencesRequest(
    val disabledTypes: Set<String>,
    val dailyReminderEnabled: Boolean,
    val dailyReminderTime: LocalTime?,
    val quietStart: LocalTime?,
    val quietEnd: LocalTime?,
)

class NotificationRepositoryImpl(
    private val client: HttpClient,
    private val cache: JsonCache,
) : NotificationRepository {
    private val listSerializer = ListSerializer(NotificationDto.serializer())

    override val notifications: Flow<Cached<List<NotificationDto>>?> = cache.observe(KEY, listSerializer)

    override suspend fun refresh(): AppResult<List<NotificationDto>> =
        apiCall { client.get(api("notifications")).body<List<NotificationDto>>() }
            .onSuccess { cache.put(KEY, listSerializer, it) }

    /** Marks one notification as read; the cache is updated first so the list reacts at once. */
    override suspend fun markRead(id: String): AppResult<Unit> {
        updateCached { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
        return apiCall {
            client.post(api("notifications/$id/read"))
            Unit
        }
    }

    override suspend fun markAllRead(): AppResult<Unit> {
        val result =
            apiCall {
                client.post(api("notifications/read-all"))
                Unit
            }
        result.onSuccess { updateCached { list -> list.map { it.copy(read = true) } } }
        return result
    }

    override suspend fun preferences(): AppResult<NotificationPreferences> =
        apiCall { client.get(api("notifications/preferences")).body<NotificationPreferences>() }

    override suspend fun savePreferences(preferences: NotificationPreferences): AppResult<NotificationPreferences> =
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
