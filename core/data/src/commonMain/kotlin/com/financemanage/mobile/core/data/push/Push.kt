package com.financemanage.mobile.core.data.push

import com.financemanage.mobile.core.data.platform.deviceName
import com.financemanage.mobile.core.data.platform.platformName
import com.financemanage.mobile.core.data.session.SessionState
import com.financemanage.mobile.core.data.session.SignOutHook
import com.financemanage.mobile.core.model.AppResult
import com.financemanage.mobile.core.network.api
import com.financemanage.mobile.core.network.error.apiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Where a notification points to (API §14): `referenceType` (`BILL`, `BUDGET`, `SAVING_GOAL`, `DEPOSIT`, `LOAN`) and
 * `referenceId`. Both are null for `DAILY_REMINDER` (→ Quick add).
 */
data class NotificationLink(
    val referenceType: String?,
    val referenceId: String?,
) {
    companion object {
        private const val SCHEME = "financemanage://"

        /** Parses `financemanage://<referenceType>/<id>` (also `financemanage://quick-add`). */
        fun parse(url: String): NotificationLink? {
            if (!url.startsWith(SCHEME, ignoreCase = true)) return null
            val parts =
                url
                    .substring(SCHEME.length)
                    .trim('/')
                    .split('/')
                    .filter { it.isNotBlank() }
            val type = parts.getOrNull(0)?.uppercase()?.replace('-', '_')
            return when {
                type == null || type == "QUICK_ADD" -> NotificationLink(null, null)
                else -> NotificationLink(type, parts.getOrNull(1))
            }
        }
    }
}

/**
 * Bridge between the platform push SDK (FCM on Android, the Firebase iOS SDK called from Swift) and the shared code.
 * A process-wide object so that platform callbacks (services, app delegates) can reach it before Koin or the UI.
 */
object PushBridge {
    private val mutableToken = MutableStateFlow<String?>(null)
    private val mutableOpened = MutableStateFlow<NotificationLink?>(null)
    private val mutableReceived = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val mutablePermissionRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Current push token, null until the SDK reports one. */
    val token: StateFlow<String?> = mutableToken.asStateFlow()

    /** Notification tapped / deep link opened, waiting to be handled by the UI (survives a cold start). */
    val opened: StateFlow<NotificationLink?> = mutableOpened.asStateFlow()

    /** A push arrived while the app is in the foreground: refresh the list and the badge. */
    val received: SharedFlow<Unit> = mutableReceived.asSharedFlow()

    /** A feature asks for the notification permission (after the first budget or bill is created). */
    val permissionRequests: SharedFlow<Unit> = mutablePermissionRequests.asSharedFlow()

    fun onNewToken(token: String) {
        if (token.isNotBlank()) mutableToken.value = token
    }

    fun onNotificationOpened(
        referenceType: String?,
        referenceId: String?,
    ) {
        mutableOpened.value =
            NotificationLink(referenceType?.takeIf { it.isNotBlank() }, referenceId?.takeIf { it.isNotBlank() })
    }

    /** Handles `financemanage://…` URLs; returns false when the URL is not ours. */
    fun onDeepLink(url: String): Boolean {
        val link = NotificationLink.parse(url) ?: return false
        mutableOpened.value = link
        return true
    }

    fun onMessageReceived() {
        mutableReceived.tryEmit(Unit)
    }

    fun requestPermission() {
        mutablePermissionRequests.tryEmit(Unit)
    }

    /** The UI navigated to [link]. */
    fun consumeOpened(link: NotificationLink) {
        mutableOpened.compareAndSet(link, null)
    }
}

@Serializable
private data class RegisterDeviceRequest(
    val token: String,
    val provider: String,
    val platform: String,
    val deviceName: String,
)

/**
 * Registers the push token with the backend after sign-in and whenever it changes
 * (`POST /notifications/devices`), and unregisters it on sign-out (`DELETE /notifications/devices/{token}`).
 */
class PushRegistrar(
    private val client: HttpClient,
    private val sessionState: Flow<SessionState>,
    private val tokens: Flow<String?>,
    private val scope: CoroutineScope,
) : SignOutHook {
    private var registered: String? = null
    private var currentToken: String? = null

    fun start() {
        scope.launch {
            combine(sessionState, tokens) { state, token -> state to token }
                .distinctUntilChanged()
                .collect { (state, token) ->
                    currentToken = token
                    if (state != SessionState.SIGNED_IN) {
                        registered = null
                    } else if (token != null && token != registered) {
                        if (register(token) is AppResult.Success) registered = token
                    }
                }
        }
    }

    private suspend fun register(token: String): AppResult<Unit> =
        apiCall {
            client.post(api("notifications/devices")) {
                contentType(ContentType.Application.Json)
                setBody(RegisterDeviceRequest(token, PROVIDER, platformName(), deviceName()))
            }
            Unit
        }

    override suspend fun beforeSignOut() {
        val token = currentToken ?: return
        apiCall { client.delete(api("notifications/devices/${token.encodeURLPathPart()}")) }
        registered = null
    }

    private companion object {
        const val PROVIDER = "FCM"
    }
}
