package `in`.raahi.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.raahi.app.MainActivity
import `in`.raahi.app.R
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.network.RaahiWebSocketClient
import `in`.raahi.app.network.WsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingJobAlertManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val webSocketClient: RaahiWebSocketClient,
    private val jobsRepository: JobsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _incomingJob = MutableStateFlow<JobDto?>(null)
    val incomingJob: StateFlow<JobDto?> = _incomingJob.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "raahi_incoming_requests"
        const val CHANNEL_NAME = "Incoming Roadside Requests"
        const val NOTIFICATION_ID = 9001
    }

    init {
        createNotificationChannel()
        listenToWebSocket()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority full-screen alerts for roadside assistance requests"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun listenToWebSocket() {
        scope.launch {
            webSocketClient.events.collect { event ->
                when (event) {
                    is WsEvent.JobNewRequest -> {
                        triggerAlert(event.job)
                    }
                    is WsEvent.JobStatusChange -> {
                        if (_incomingJob.value?.id == event.jobId && event.status != "PENDING") {
                            dismissAlert()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun triggerAlert(job: JobDto) {
        if (_incomingJob.value?.id == job.id) return // already alerting for this job
        _incomingJob.value = job
        startRingtone()
        postNotification(job)
    }

    fun dismissAlert() {
        _incomingJob.value = null
        stopRingtone()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    suspend fun declineJob(): Boolean {
        val job = _incomingJob.value ?: return false
        dismissAlert()
        return runCatching {
            jobsRepository.declineJob(job.id)
            true
        }.getOrDefault(false)
    }

    suspend fun acceptJob(): JobDto? {
        val job = _incomingJob.value ?: return null
        dismissAlert()
        return runCatching {
            jobsRepository.acceptJob(job.id)
        }.getOrNull()
    }

    private fun startRingtone() {
        try {
            stopRingtone()
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w("IncomingJobAlert", "Could not start ringtone: ${e.message}")
        }
    }

    private fun stopRingtone() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Log.w("IncomingJobAlert", "Could not stop ringtone: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    private fun postNotification(job: JobDto) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("jobId", job.id)
            putExtra("type", "incoming_request")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val problem = job.problemType.replace("_", " ")
        val rewardText = "₹${job.rewardAmount.toInt()}"
        val distanceText = job.distanceKm?.let { " · ${"%.1f".format(it)} km away" } ?: ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("New Roadside Request · $rewardText")
            .setContentText("$problem$distanceText")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
