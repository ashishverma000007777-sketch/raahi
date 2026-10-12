package `in`.raahi.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import kotlinx.coroutines.withContext
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

    private var ringtone: Ringtone? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val TAG = "RAAHI_INCOMING_ALERT"
        const val CHANNEL_ID = "raahi_incoming_requests_v2"
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
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created: id=$CHANNEL_ID, importance=${channel.importance}")
        }
    }

    private fun listenToWebSocket() {
        scope.launch {
            webSocketClient.events.collect { event ->
                when (event) {
                    is WsEvent.JobNewRequest -> {
                        Log.d(TAG, "WebSocket JobNewRequest received: jobId=${event.job.id}, status=${event.job.status}")
                        triggerAlert(event.job)
                    }
                    is WsEvent.JobStatusChange -> {
                        Log.d(TAG, "WebSocket JobStatusChange received: jobId=${event.jobId}, status=${event.status}")
                        if (_incomingJob.value?.id == event.jobId && event.status != "PENDING") {
                            Log.d(TAG, "Dismissing alert because active job ${event.jobId} is no longer PENDING (status=${event.status})")
                            dismissAlert()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun fetchAndTrigger(jobId: String, fallbackJob: JobDto? = null) {
        if (_incomingJob.value?.id == jobId) {
            Log.d(TAG, "fetchAndTrigger: already alerting for jobId=$jobId, skipping duplicate fetch")
            return
        }
        if (fallbackJob != null) {
            Log.d(TAG, "fetchAndTrigger: displaying immediate alert from FCM payload for jobId=$jobId")
            triggerAlert(fallbackJob)
        }
        scope.launch(Dispatchers.IO) {
            Log.d(TAG, "fetchAndTrigger: fetching fresh authoritative job details for jobId=$jobId")
            val job = runCatching { jobsRepository.getJob(jobId) }.getOrNull()
            withContext(Dispatchers.Main) {
                if (job != null) {
                    if (job.status == "PENDING") {
                        triggerAlert(job)
                    } else {
                        Log.d(TAG, "Job $jobId is no longer PENDING (status=${job.status}), dismissing stale alert")
                        if (_incomingJob.value?.id == jobId) {
                            dismissAlert()
                        }
                    }
                } else if (fallbackJob == null) {
                    Log.w(TAG, "fetchAndTrigger: unable to fetch job $jobId and no fallback job available")
                }
            }
        }
    }

    fun triggerAlert(job: JobDto) {
        Log.d(TAG, "triggerAlert invoked for jobId=${job.id}, status=${job.status}")
        if (job.status != "PENDING") {
            Log.d(TAG, "Ignoring alert for job ${job.id}: status is ${job.status}, expected PENDING")
            return
        }
        if (_incomingJob.value?.id == job.id) {
            Log.d(TAG, "Ignoring duplicate alert for job ${job.id}: already active")
            return
        }
        _incomingJob.value = job
        startRingtoneAndVibration()
        postNotification(job)
    }

    fun dismissAlert() {
        val currentId = _incomingJob.value?.id
        Log.d(TAG, "dismissAlert called (current job: $currentId)")
        _incomingJob.value = null
        stopRingtoneAndVibration()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    suspend fun declineJob(): Boolean {
        val job = _incomingJob.value ?: return false
        Log.d(TAG, "declineJob invoked by helper for jobId=${job.id}")
        dismissAlert()
        return runCatching {
            jobsRepository.declineJob(job.id)
            true
        }.getOrDefault(false)
    }

    suspend fun acceptJob(): JobDto? {
        val job = _incomingJob.value ?: return null
        Log.d(TAG, "acceptJob invoked by helper for jobId=${job.id}")
        dismissAlert()
        return runCatching {
            jobsRepository.acceptJob(job.id)
        }.getOrNull()
    }

    private fun startRingtoneAndVibration() {
        stopRingtoneAndVibration()
        startRingtone()
        startVibration()
    }

    private fun startRingtone() {
        // 1. Try RingtoneManager first: accurately resolves system alarm/ringtone URIs
        try {
            val ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            if (ringtoneUri != null) {
                val r = RingtoneManager.getRingtone(context, ringtoneUri)
                if (r != null) {
                    r.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        r.isLooping = true
                    }
                    r.play()
                    ringtone = r
                    Log.d(TAG, "Ringtone started via RingtoneManager ($ringtoneUri)")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "RingtoneManager start exception: ${e.message}, falling back to MediaPlayer", e)
        }

        // 2. Fallback to MediaPlayer
        try {
            val fallbackUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, fallbackUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d(TAG, "Ringtone started via MediaPlayer ($fallbackUri)")
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer start exception: ${e.message}", e)
        }
    }

    private fun startVibration() {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator = vib
            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(pattern, 0)
            }
            Log.d(TAG, "Continuous vibration started")
        } catch (e: Exception) {
            Log.w(TAG, "Could not start vibration: ${e.message}")
        }
    }

    private fun stopRingtoneAndVibration() {
        try {
            ringtone?.let {
                if (it.isPlaying) it.stop()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception stopping Ringtone: ${e.message}")
        } finally {
            ringtone = null
        }

        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception stopping MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Exception stopping vibrator: ${e.message}")
        } finally {
            vibrator = null
        }
        Log.d(TAG, "Ringtone and vibration stopped")
    }

    private fun postNotification(job: JobDto) {
        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
        Log.d(TAG, "postNotification: CHANNEL_ID=$CHANNEL_ID, canUseFullScreenIntent=$canUseFullScreen")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification $NOTIFICATION_ID posted successfully for job ${job.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post notification for job ${job.id}: ${e.message}", e)
        }
    }
}

