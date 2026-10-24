package `in`.raahi.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import `in`.raahi.app.MainActivity
import `in`.raahi.app.R

private const val CHANNEL_ID = "raahi_default"
private const val CHANNEL_NAME = "Raahi Updates"

const val CHANNEL_INTERCITY_ALERTS = "intercity_ride_alerts"
const val CHANNEL_INTERCITY_NAME = "Intercity Ride Alerts"

object RaahiNotifications {

    /** Called once from RaahiApp.onCreate — channels must exist before the first
     * notification is posted, and creating one that already exists is a safe no-op. */
    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Job status, SOS alerts, and other real-time updates"
        }
        manager.createNotificationChannel(channel)

        val intercityChannel = NotificationChannel(
            CHANNEL_INTERCITY_ALERTS, CHANNEL_INTERCITY_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for new intercity trips matching your preferred routes"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 150, 100, 150)
            setShowBadge(true)
        }
        manager.createNotificationChannel(intercityChannel)
    }

    /** Posts a real notification via NotificationManager — this is what was missing before:
     * onMessageReceived only logged the payload, so a message arriving while the app was in
     * the foreground (where the OS does NOT auto-display notification-payload messages,
     * unlike when backgrounded) was silently dropped from the user's point of view. */
    fun show(context: Context, title: String, body: String, data: Map<String, String>) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Posts a dedicated intercity ride alert with deduplication by tripId and
     * deep-link intent routing directly to the trip detail screen.
     */
    fun showIntercityAlert(context: Context, title: String, body: String, tripId: String, data: Map<String, String>) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "intercity_detail/$tripId")
            putExtra("tripId", tripId)
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val notificationId = if (tripId.isNotBlank()) tripId.hashCode() else System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_INTERCITY_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}
