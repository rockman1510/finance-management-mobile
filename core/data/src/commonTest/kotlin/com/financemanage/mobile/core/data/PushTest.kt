package com.financemanage.mobile.core.data

import com.financemanage.mobile.core.data.push.NotificationLink
import com.financemanage.mobile.core.data.push.PushBridge
import com.financemanage.mobile.core.data.push.PushRegistrar
import com.financemanage.mobile.core.data.session.SessionState
import com.financemanage.mobile.core.testing.MockBackend
import com.financemanage.mobile.core.testing.apiPath
import com.financemanage.mobile.core.testing.bodyText
import com.financemanage.mobile.core.testing.respondJson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PushTest {
    @Test
    fun parsesDeepLinks() {
        assertEquals(NotificationLink("BILL", "b1"), NotificationLink.parse("financemanage://bill/b1"))
        assertEquals(NotificationLink("SAVING_GOAL", "g1"), NotificationLink.parse("financemanage://saving-goal/g1/"))
        assertEquals(NotificationLink(null, null), NotificationLink.parse("financemanage://quick-add"))
        assertEquals(NotificationLink(null, null), NotificationLink.parse("FinanceManage://"))
        assertNull(NotificationLink.parse("https://example.com/bill/1"))
    }

    @Test
    fun bridgeKeepsTheOpenedLinkUntilConsumed() {
        assertFalse(PushBridge.onDeepLink("https://example.com"))
        assertTrue(PushBridge.onDeepLink("financemanage://loan/l1"))
        val link = PushBridge.opened.value!!
        assertEquals(NotificationLink("LOAN", "l1"), link)
        PushBridge.consumeOpened(link)
        assertNull(PushBridge.opened.value)

        PushBridge.onNotificationOpened("", " ".trim())
        assertEquals(NotificationLink(null, null), PushBridge.opened.value)
        PushBridge.consumeOpened(PushBridge.opened.value!!)
    }

    @Test
    fun registersTheTokenWhenSignedInAndUnregistersOnSignOut() =
        runTest(UnconfinedTestDispatcher()) {
            val backend = MockBackend()
            backend.on(HttpMethod.Post, "notifications/devices") { respondJson("", HttpStatusCode.NoContent) }
            backend.delete("notifications/devices/.+")
            val session = MutableStateFlow(SessionState.SIGNED_OUT)
            val token = MutableStateFlow<String?>(null)
            val registrar =
                PushRegistrar(
                    client = backend.client,
                    sessionState = session,
                    tokens = token,
                    scope = backgroundScope,
                )
            registrar.start()

            token.value = "tok/1"
            assertTrue(backend.requests.isEmpty())

            session.value = SessionState.SIGNED_IN
            val register = backend.await(HttpMethod.Post, "notifications/devices")
            assertTrue(register.bodyText().contains("\"token\":\"tok/1\""))
            assertTrue(register.bodyText().contains("\"provider\":\"FCM\""))

            registrar.beforeSignOut()
            val unregister = backend.single(HttpMethod.Delete, "notifications/devices/.+")
            assertEquals("notifications/devices/tok%2F1", unregister.apiPath)
            assertEquals(1, backend.requests(HttpMethod.Post, "notifications/devices").size)
        }
}
