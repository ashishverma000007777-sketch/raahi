package `in`.raahi.app.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.IncomingJobAlertManager
import `in`.raahi.app.network.JobDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RaahiFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var incomingJobAlertManager: IncomingJobAlertManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Fires on first install and whenever FCM rotates the token — the backend must be
        // told every time, not just once at sign-in, or delivery silently breaks after a
        // rotation. If the user isn't signed in yet, AuthRepository.registerFcmToken just
        // no-ops (no auth token to call the API with) and the real one is sent right after
        // sign-in instead (see AuthViewModel).
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { authRepository.registerFcmToken(token) }
                .onFailure { Log.w("RaahiFCM", "Could not register FCM token: ${it.message}") }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"]
        val jobId = data["jobId"]
        Log.d("RAAHI_INCOMING_ALERT", "FCM onMessageReceived: type=$type, jobId=$jobId, dataKeys=${data.keys}")

        if (type == "job_new_request" || (!jobId.isNullOrBlank() && type != "job_status_change" && type != "job_accepted")) {
            Log.d("RAAHI_INCOMING_ALERT", "Routing incoming Help Request FCM to IncomingJobAlertManager (jobId=$jobId)")
            if (!jobId.isNullOrBlank()) {
                val fallbackJob = runCatching {
                    val problemType = data["problemType"] ?: "ROADSIDE_ASSISTANCE"
                    val rewardAmount = data["rewardAmount"]?.toDoubleOrNull() ?: 0.0
                    val lat = data["lat"]?.toDoubleOrNull() ?: 0.0
                    val lng = data["lng"]?.toDoubleOrNull() ?: 0.0
                    JobDto(
                        id = jobId,
                        status = "PENDING",
                        problemType = problemType,
                        problemDesc = null,
                        lat = lat,
                        lng = lng,
                        rewardAmount = rewardAmount,
                        helperOtp = null,
                        requesterName = "Nearby User",
                        requesterPhone = null,
                        helperName = null,
                        helperPhone = null,
                        helperRatingAvg = null,
                        helperTotalHelps = null,
                        helperVerified = null,
                        viewerRole = "HELPER"
                    )
                }.getOrNull()

                incomingJobAlertManager.fetchAndTrigger(jobId, fallbackJob)
            }
            return
        }

        if (type == "intercity_new_trip" || (!data["tripId"].isNullOrBlank() && type != "job_status_change")) {
            val tripId = data["tripId"].orEmpty()
            val title = message.notification?.title ?: data["title"] ?: "New Intercity Trip Available"
            val body = message.notification?.body ?: data["body"] ?: "A new intercity trip has been posted. Tap to view details and apply."
            RaahiNotifications.showIntercityAlert(applicationContext, title, body, tripId, data)
            return
        }

        // Notification-only payloads are shown automatically by the system when the app is
        // backgrounded, but NOT when foregrounded — that gap is what RaahiNotifications.show
        // closes. Every push this backend actually sends (see NotificationService.java) sets
        // both notification.title/body AND a data payload, so falling back to the data map's
        // own title/body (when a message is data-only) covers both real shapes without
        // fabricating content that wasn't sent.
        val title = message.notification?.title ?: data["title"] ?: "Raahi"
        val body = message.notification?.body ?: data["body"] ?: return
        RaahiNotifications.show(applicationContext, title, body, data)
    }
}
