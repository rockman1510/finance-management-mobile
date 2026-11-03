package com.financemanage.mobile.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.financemanage.mobile.MainActivity
import com.financemanage.mobile.R
import com.financemanage.mobile.core.data.push.PushBridge
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** FCM entry point (F20, API §14.1). */
class FinanceMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        PushBridge.onNewToken(token)
    }

    /** Called for messages received while the app is in the foreground: FCM shows nothing itself then. */
    override fun onMessageReceived(message: RemoteMessage) {
        PushBridge.onMessageReceived()
        val title = message.notification?.title ?: return
        Push.show(this, title, message.notification?.body.orEmpty(), message.data)
    }
}

object Push {
    const val CHANNEL_ID = "default"
    const val EXTRA_REFERENCE_TYPE = "referenceType"
    const val EXTRA_REFERENCE_ID = "referenceId"
    private const val EXTRA_NOTIFICATION_ID = "notificationId"

    /** Creates the `default` channel the backend targets (Android 8+), then fetches the FCM token if configured. */
    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_default),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        // No google-services.json → Firebase is not initialised and push stays off.
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> PushBridge.onNewToken(token) }
    }

    /** Reads the notification `data` carried by an Activity intent (tap on a push) and hands it to the UI. */
    fun handleIntent(intent: Intent?) {
        intent ?: return
        intent.data?.toString()?.let { url -> if (PushBridge.onDeepLink(url)) return }
        val extras = intent.extras ?: return
        if (!extras.containsKey(EXTRA_NOTIFICATION_ID) && !extras.containsKey(EXTRA_REFERENCE_TYPE)) return
        PushBridge.onNotificationOpened(extras.getString(EXTRA_REFERENCE_TYPE), extras.getString(EXTRA_REFERENCE_ID))
        intent.removeExtra(EXTRA_NOTIFICATION_ID)
        intent.removeExtra(EXTRA_REFERENCE_TYPE)
    }

    fun show(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String>,
    ) {
        val granted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { data.forEach { (key, value) -> putExtra(key, value) } }
        val requestCode = data[EXTRA_NOTIFICATION_ID]?.hashCode() ?: title.hashCode()
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        NotificationManagerCompat.from(context).notify(requestCode, notification)
    }
}
