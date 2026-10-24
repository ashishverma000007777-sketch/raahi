package `in`.raahi.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Wakeup receiver invoked by AlarmManager when the 15-minute continuous standstill rule expires.
 * Forwards the event to [DrivingTrackingService] with the active session ID.
 */
class AutoDriveAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val sessionId = intent?.getStringExtra("session_id")
        Log.i("RaahiDriving", "AutoDriveAlarmReceiver triggered for session: $sessionId")
        if (sessionId.isNullOrBlank()) return

        val serviceIntent = Intent(context, DrivingTrackingService::class.java).apply {
            action = DrivingTrackingService.ACTION_AUTO_COMPLETE_15MIN
            putExtra("session_id", sessionId)
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e("RaahiDriving", "Failed to start service from 15-min alarm receiver", e)
        }
    }
}
