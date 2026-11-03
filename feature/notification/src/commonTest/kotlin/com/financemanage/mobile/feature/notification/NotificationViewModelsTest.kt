package com.financemanage.mobile.feature.notification

import com.financemanage.mobile.core.data.push.PushBridge
import com.financemanage.mobile.core.data.shared.ProfileRepository
import com.financemanage.mobile.core.model.AppError
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.SampleJson
import com.financemanage.mobile.core.testing.awaitCondition
import com.financemanage.mobile.core.testing.awaitValue
import com.financemanage.mobile.core.testing.bodyJson
import com.financemanage.mobile.core.testing.respondJson
import com.financemanage.mobile.core.testing.runViewModelTest
import com.financemanage.mobile.core.testing.testJsonCache
import com.financemanage.mobile.core.testing.tracked
import com.financemanage.mobile.feature.notification.data.NotificationDto
import com.financemanage.mobile.feature.notification.data.NotificationRepository
import com.financemanage.mobile.feature.notification.presentation.NotificationSettingsViewModel
import com.financemanage.mobile.feature.notification.presentation.NotificationsViewModel
import com.financemanage.mobile.feature.notification.presentation.groupByDay
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val LIST =
    """[{"id":"n1","type":"BILL_DUE","title":"Hoá đơn","body":"Tiền điện","referenceType":"BILL","referenceId":"b1",
    "createdAt":"2026-11-02T18:00:00Z","read":false},
    {"id":"n2","type":"BUDGET_WARNING","title":"Ngân sách","body":"80%","createdAt":"2026-11-02T01:00:00Z","read":false},
    {"id":"n3","type":"DAILY_REMINDER","title":"Nhắc","body":"","createdAt":"2026-11-01T12:00:00Z","read":true}]"""

private const val PREFS =
    """{"disabledTypes":["BUDGET_WARNING"],"dailyReminderEnabled":false,"dailyReminderTime":null,
    "quietStart":null,"quietEnd":null,"timeZone":"Asia/Ho_Chi_Minh"}"""

class NotificationViewModelsTest {
    private val backend = MockBackend()
    private val cache = testJsonCache()
    private val repository = NotificationRepository(backend.client, cache)
    private val profiles = ProfileRepository(backend.client, cache)

    @Test
    fun groupsByLocalDay() {
        fun n(
            id: String,
            at: String?,
        ) = NotificationDto(id, createdAt = at?.let(Instant::parse))
        val days =
            groupByDay(
                listOf(n("a", "2026-11-02T18:00:00Z"), n("b", "2026-11-02T01:00:00Z"), n("c", null)),
                TimeZone.of("Asia/Ho_Chi_Minh"),
            )
        // 18:00Z on the 2nd is already the 3rd in Vietnam.
        assertEquals(listOf(LocalDate(2026, 11, 3), LocalDate(2026, 11, 2), null), days.map { it.date })
    }

    @Test
    fun listCountsUnreadAndMarksReadOptimistically() =
        runViewModelTest {
            backend.get("me", SampleJson.ME)
            profiles.refresh()
            backend.get("notifications", LIST)
            backend.post("notifications/n1/read", "", HttpStatusCode.NoContent)
            val viewModel = NotificationsViewModel(repository, profiles).tracked()
            viewModel.refresh()
            val loaded = viewModel.state.awaitValue { it.days.data != null }
            assertEquals(2, loaded.unreadCount)
            assertEquals(3, loaded.days.data!!.size)

            viewModel.open(
                loaded.days.data!!
                    .first()
                    .items
                    .first(),
            )
            viewModel.state.awaitValue { it.unreadCount == 1 }
            backend.await(HttpMethod.Post, "notifications/n1/read")

            viewModel.open(
                loaded.days.data!!
                    .last()
                    .items
                    .first(),
            )
            assertTrue(backend.requests(HttpMethod.Post, "notifications/n3/read").isEmpty(), "already read")
        }

    @Test
    fun markAllReadAndAPushRefreshesTheList() =
        runViewModelTest {
            backend.get("notifications", LIST)
            backend.post("notifications/read-all", "", HttpStatusCode.NoContent)
            val viewModel = NotificationsViewModel(repository, profiles).tracked()
            viewModel.refresh()
            viewModel.state.awaitValue { it.unreadCount == 2 }

            viewModel.markAllRead()
            viewModel.state.awaitValue { it.unreadCount == 0 }

            PushBridge.onMessageReceived()
            backend.await(HttpMethod.Get, "notifications", count = 2)
            viewModel.state.awaitValue { !it.days.loading }
        }

    @Test
    fun markAllReadFailureIsShown() =
        runViewModelTest {
            backend.get("notifications", LIST)
            backend.offline(HttpMethod.Post, "notifications/read-all")
            val viewModel = NotificationsViewModel(repository, profiles).tracked()
            viewModel.refresh()
            viewModel.state.awaitValue { it.unreadCount == 2 }
            viewModel.markAllRead()
            assertTrue(viewModel.state.awaitValue { it.actionError != null }.actionError is AppError.Network)
            assertEquals(2, viewModel.state.value.unreadCount)
            viewModel.clearError()
            viewModel.state.awaitValue { it.actionError == null }
        }

    @Test
    fun settingsSaveEachChangeAndUseDefaults() =
        runViewModelTest {
            backend.get("notifications/preferences", PREFS)
            backend.on(HttpMethod.Put, "notifications/preferences") { request ->
                respondJson(request.bodyJson().toString())
            }
            val viewModel = NotificationSettingsViewModel(repository)
            viewModel.state.awaitValue { it.preferences != null }

            viewModel.setTypeEnabled("BUDGET_WARNING", true)
            viewModel.state.awaitValue { it.preferences!!.disabledTypes.isEmpty() }

            viewModel.setDailyReminder(true)
            val reminder = viewModel.state.awaitValue { it.preferences!!.dailyReminderEnabled }
            assertEquals(LocalTime(20, 0), reminder.preferences!!.dailyReminderTime)

            viewModel.setQuietHours(true)
            val quiet = viewModel.state.awaitValue { it.preferences!!.quietStart != null }
            assertEquals(LocalTime(22, 0), quiet.preferences!!.quietStart)
            assertEquals(LocalTime(7, 0), quiet.preferences!!.quietEnd)
            // Each change cancels a save still running, so only the last state is guaranteed to be sent.
            awaitCondition("last save") {
                backend
                    .requests(
                        HttpMethod.Put,
                        "notifications/preferences",
                    ).lastOrNull()
                    ?.bodyJson()
                    ?.get("quietStart") !=
                    null
            }
            val body = backend.requests(HttpMethod.Put, "notifications/preferences").last().bodyJson()
            assertEquals("\"22:00\"", body["quietStart"].toString())
            assertEquals("true", body["dailyReminderEnabled"].toString())
        }

    @Test
    fun aRefusedChangeIsRolledBack() =
        runViewModelTest {
            backend.get("notifications/preferences", PREFS)
            backend.fail(HttpMethod.Put, "notifications/preferences", HttpStatusCode.BadRequest, "VALIDATION_FAILED")
            val viewModel = NotificationSettingsViewModel(repository)
            viewModel.state.awaitValue { it.preferences != null }
            viewModel.setTypeEnabled("BILL_DUE", false)
            val failed = viewModel.state.awaitValue { it.saveError != null }
            assertEquals(setOf("BUDGET_WARNING"), failed.preferences!!.disabledTypes)
            assertFalse("BILL_DUE" in failed.preferences!!.disabledTypes)
            viewModel.clearError()
            assertNull(viewModel.state.value.saveError)
        }
}
