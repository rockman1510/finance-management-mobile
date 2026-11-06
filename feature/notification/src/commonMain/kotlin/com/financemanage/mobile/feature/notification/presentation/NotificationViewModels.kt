package com.financemanage.mobile.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.financemanage.mobile.core.domain.usecase.GetCachedProfileUseCase
import com.financemanage.mobile.core.domain.usecase.ObservePushReceivedUseCase
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.model.onFailure
import com.financemanage.mobile.core.model.time.timeZoneOrDefault
import com.financemanage.mobile.core.model.time.today
import com.financemanage.mobile.core.ui.state.Resource
import com.financemanage.mobile.core.ui.state.ResourceHolder
import com.financemanage.mobile.feature.notification.domain.model.NotificationDto
import com.financemanage.mobile.feature.notification.domain.model.NotificationPreferences
import com.financemanage.mobile.feature.notification.domain.usecase.GetNotificationPreferencesUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.MarkAllNotificationsReadUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.MarkNotificationReadUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.ObserveNotificationsUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.RefreshNotificationsUseCase
import com.financemanage.mobile.feature.notification.domain.usecase.SaveNotificationPreferencesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Notifications of one day, newest first. `date` is null when the server sent no `createdAt`. */
data class NotificationDay(
    val date: LocalDate?,
    val items: List<NotificationDto>,
)

/** Groups a newest-first list by local day, keeping the order. */
fun groupByDay(
    notifications: List<NotificationDto>,
    zone: TimeZone,
): List<NotificationDay> {
    val groups = mutableListOf<NotificationDay>()
    notifications.forEach { notification ->
        val date = notification.createdAt?.toLocalDateTime(zone)?.date
        val last = groups.lastOrNull()
        if (last != null && last.date == date) {
            groups[groups.lastIndex] = last.copy(items = last.items + notification)
        } else {
            groups += NotificationDay(date, listOf(notification))
        }
    }
    return groups
}

data class NotificationsUiState(
    val days: Resource<List<NotificationDay>> = Resource(loading = true),
    val today: LocalDate? = null,
    val unreadCount: Int = 0,
    val actionError: AppError? = null,
)

class NotificationsViewModel(
    observeNotifications: ObserveNotificationsUseCase,
    refreshNotifications: RefreshNotificationsUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
    private val markAllNotificationsRead: MarkAllNotificationsReadUseCase,
    getCachedProfile: GetCachedProfileUseCase,
    observePushReceived: ObservePushReceivedUseCase,
) : ViewModel() {
    private val holder = ResourceHolder(viewModelScope, observeNotifications()) { refreshNotifications() }
    private val zoneId = MutableStateFlow<String?>(null)
    private val actionError = MutableStateFlow<AppError?>(null)

    val state: StateFlow<NotificationsUiState> =
        combine(holder.state, zoneId, actionError) { resource, zone, error ->
            val timeZone = timeZoneOrDefault(zone)
            NotificationsUiState(
                days = resource.map { groupByDay(it, timeZone) },
                today = today(zone),
                unreadCount = resource.data?.count { !it.read } ?: 0,
                actionError = error,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, NotificationsUiState())

    init {
        viewModelScope.launch { zoneId.value = getCachedProfile()?.timeZone }
        // A push arrived while the list is open: refresh it.
        viewModelScope.launch { observePushReceived().collect { refresh() } }
    }

    fun refresh() = holder.refresh()

    /** Tapping a notification marks it as read (best effort; the screen navigates right away). */
    fun open(notification: NotificationDto) {
        if (!notification.read) viewModelScope.launch { markNotificationRead(notification.id) }
    }

    fun markAllRead() {
        viewModelScope.launch { markAllNotificationsRead().onFailure { actionError.value = it } }
    }

    fun clearError() {
        actionError.value = null
    }
}

data class NotificationSettingsUiState(
    val loading: Boolean = true,
    val preferences: NotificationPreferences? = null,
    val loadError: AppError? = null,
    val saveError: AppError? = null,
)

/** Notification settings (F20): each change is saved at once and rolled back when the server refuses it. */
class NotificationSettingsViewModel(
    private val getNotificationPreferences: GetNotificationPreferencesUseCase,
    private val saveNotificationPreferences: SaveNotificationPreferencesUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(NotificationSettingsUiState())
    val state: StateFlow<NotificationSettingsUiState> = mutableState.asStateFlow()
    private var saveJob: Job? = null

    init {
        load()
    }

    fun load() {
        mutableState.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = getNotificationPreferences()) {
                is AppResult.Success -> mutableState.update { it.copy(loading = false, preferences = result.value) }
                is AppResult.Failure -> mutableState.update { it.copy(loading = false, loadError = result.error) }
            }
        }
    }

    fun setTypeEnabled(
        type: String,
        enabled: Boolean,
    ) = change { it.copy(disabledTypes = if (enabled) it.disabledTypes - type else it.disabledTypes + type) }

    fun setDailyReminder(enabled: Boolean) =
        change {
            it.copy(
                dailyReminderEnabled = enabled,
                dailyReminderTime = it.dailyReminderTime ?: DEFAULT_REMINDER_TIME,
                disabledTypes = if (enabled) it.disabledTypes - DAILY_REMINDER else it.disabledTypes,
            )
        }

    fun setDailyReminderTime(time: LocalTime) = change { it.copy(dailyReminderTime = time) }

    fun setQuietHours(enabled: Boolean) =
        change {
            if (enabled) {
                it.copy(quietStart = it.quietStart ?: DEFAULT_QUIET_START, quietEnd = it.quietEnd ?: DEFAULT_QUIET_END)
            } else {
                it.copy(quietStart = null, quietEnd = null)
            }
        }

    fun setQuietStart(time: LocalTime) = change { it.copy(quietStart = time) }

    fun setQuietEnd(time: LocalTime) = change { it.copy(quietEnd = time) }

    fun clearError() = mutableState.update { it.copy(saveError = null) }

    private fun change(transform: (NotificationPreferences) -> NotificationPreferences) {
        val previous = mutableState.value.preferences ?: return
        val updated = transform(previous)
        mutableState.update { it.copy(preferences = updated, saveError = null) }
        saveJob?.cancel()
        saveJob =
            viewModelScope.launch {
                when (val result = saveNotificationPreferences(updated)) {
                    is AppResult.Success -> mutableState.update { it.copy(preferences = result.value) }
                    is AppResult.Failure ->
                        mutableState.update {
                            it.copy(
                                preferences = previous,
                                saveError = result.error,
                            )
                        }
                }
            }
    }

    companion object {
        const val DAILY_REMINDER = "DAILY_REMINDER"
        private const val REMINDER_HOUR = 20
        private const val QUIET_START_HOUR = 22
        private const val QUIET_END_HOUR = 7
        val DEFAULT_REMINDER_TIME = LocalTime(REMINDER_HOUR, 0)
        val DEFAULT_QUIET_START = LocalTime(QUIET_START_HOUR, 0)
        val DEFAULT_QUIET_END = LocalTime(QUIET_END_HOUR, 0)
    }
}
