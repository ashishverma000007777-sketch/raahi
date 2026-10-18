package `in`.raahi.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import androidx.core.app.NotificationCompat
import `in`.raahi.app.R
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class DrivingTrackingService : Service() {
    @Inject lateinit var vehicleRepository: VehicleRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_START = "in.raahi.app.driving.START"
        const val ACTION_CONFIRM = "in.raahi.app.driving.CONFIRM"
        const val ACTION_DECLINE = "in.raahi.app.driving.DECLINE"
        const val ACTION_STOP = "in.raahi.app.driving.STOP"
        const val ACTION_AUTO_COMPLETE_15MIN = "in.raahi.app.driving.AUTO_COMPLETE_15MIN"
        const val EVENT_DRIVING_DETECTED = "in.raahi.app.driving.DETECTED"
        const val EVENT_DISTANCE_UPDATED = "in.raahi.app.driving.DISTANCE_UPDATED"
        const val EVENT_STATUS_CHANGED = "in.raahi.app.driving.STATUS_CHANGED"

        private const val CHANNEL_ID = "raahi_driving_tracking_silent_v3"
        private const val ALERT_CHANNEL_ID = "raahi_driving_alerts_high"
        private const val NOTIFICATION_ID = 7401
        private const val ALERT_NOTIFICATION_ID = 7402
        private const val SUMMARY_NOTIFICATION_ID = 7403
        private const val PREFS = "raahi_driving_tracking"
        private const val SPEED_THRESHOLD_MPS = 2.78f // > 10 km/h (2.78 m/s)
        private const val DETECT_DURATION_MS = 60_000L // continuous for ~60s
        private const val DECLINE_COOLDOWN_MS = 2 * 60 * 60 * 1000L
        private const val STOP_COOLDOWN_MS = 60_000L
        private const val SYNC_INTERVAL_MS = 10 * 60 * 1000L
        private const val STOP_AUTO_COMPLETE_THRESHOLD_MS = 15 * 60 * 1000L // 15 continuous minutes

        @Volatile
        var isConfirmedDriving: Boolean = false
            internal set

        @Volatile
        var currentDriveState: AutoDriveState = AutoDriveState.MONITORING
            internal set
    }

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var callback: LocationCallback? = null
    private var lastFix: Location? = null
    private var distanceSinceLastFix = 0f
    private var lastSyncAt = 0L
    private var syncInProgress = false
    private var vehicleLockInProgress = false
    private var trackingPaused = false
    private var lowSpeedSince = 0L
    private var alertSoundJob: Job? = null
    private var periodicCheckJob: Job? = null

    internal val stateMachine = AutoDriveStateMachine(
        clock = { System.currentTimeMillis() },
        speedThresholdMps = SPEED_THRESHOLD_MPS,
        detectDurationMs = DETECT_DURATION_MS,
        declineCooldownMs = DECLINE_COOLDOWN_MS,
        stopCooldownMs = STOP_COOLDOWN_MS,
        stopAutoCompletionThresholdMs = STOP_AUTO_COMPLETE_THRESHOLD_MS,
        onStateChanged = { oldState, newState ->
            handleStateTransition(oldState, newState)
        },
        onPromptUser = { detectionId ->
            postDrivingAlert(detectionId)
            sendBroadcast(
                Intent(EVENT_DRIVING_DETECTED)
                    .setPackage(packageName)
                    .putExtra("detection_id", detectionId)
            )
        },
        onTripCompleted = { sessionId, durationMs, reason ->
            Log.i("RaahiDriving", "Trip completed for session $sessionId ($reason, duration: ${durationMs / 1000}s)")
            postTripSummaryNotification()
        },
        onSchedule15MinTimer = { sessionId, delayMs ->
            schedule15MinAlarm(sessionId, delayMs)
        },
        onCancel15MinTimer = { sessionId ->
            cancel15MinAlarm(sessionId)
        }
    )

    override fun onCreate() {
        super.onCreate()
        setupNotificationChannels()
        restorePersistentState()
        startPeriodicCheckLoop()
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Raahi background tracking",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Silent notification while auto drive detection is active"
                    setSound(null, null)
                    enableVibration(false)
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Raahi driving alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sound and vibration for driving alerts and trip summaries"
                    enableVibration(true)
                    setSound(
                        android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
            )
        }
    }

    private fun restorePersistentState() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val persistedStateName = prefs.getString("drive_state", null)
        val persistedState = AutoDriveState.fromString(persistedStateName)
        val sessionId = prefs.getString("current_session_id", null)
        val detectionId = prefs.getString("pending_detection_id", null)
        val tripStart = prefs.getLong("trip_started_at", 0L)
        val stationaryTime = prefs.getLong("stationary_since", 0L)
        val declineTime = prefs.getLong("declined_at", 0L)
        val stopTime = prefs.getLong("stopped_at", 0L)
        val standstillReq = prefs.getBoolean("stop_standstill_required", false)
        val hasPerm = hasLocationPermission()

        stateMachine.restore(
            persistedState = persistedState,
            sessionId = sessionId,
            detectionId = detectionId,
            tripStart = tripStart,
            stationaryTime = stationaryTime,
            declineTime = declineTime,
            stopTime = stopTime,
            standstillReq = standstillReq,
            hasPermission = hasPerm
        )

        currentDriveState = stateMachine.state
        isConfirmedDriving = stateMachine.state == AutoDriveState.ACTIVE_DRIVE ||
            stateMachine.state == AutoDriveState.STOPPED_PENDING_COMPLETION
    }

    private fun startPeriodicCheckLoop() {
        periodicCheckJob?.cancel()
        periodicCheckJob = serviceScope.launch {
            while (true) {
                delay(15_000L)
                checkStationaryTimeoutInScope()
            }
        }
    }

    private fun checkStationaryTimeoutInScope() {
        if (stateMachine.state == AutoDriveState.STOPPED_PENDING_COMPLETION) {
            val sId = stateMachine.currentSessionId ?: return
            val statSince = stateMachine.stationarySince
            if (statSince > 0L && (System.currentTimeMillis() - statSince) >= STOP_AUTO_COMPLETE_THRESHOLD_MS) {
                Log.i("RaahiDriving", "Periodic check detected 15 min standstill; triggering auto-complete")
                stateMachine.on15MinTimeout(sId)
            }
        }
    }

    private fun handleStateTransition(oldState: AutoDriveState, newState: AutoDriveState) {
        currentDriveState = newState
        val isConfirmed = newState == AutoDriveState.ACTIVE_DRIVE || newState == AutoDriveState.STOPPED_PENDING_COMPLETION
        isConfirmedDriving = isConfirmed

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString("drive_state", newState.name)
            .putBoolean("is_driving_confirmed", isConfirmed)
            .putBoolean("is_service_running", newState != AutoDriveState.PERMISSION_REQUIRED)
            .putLong("declined_at", stateMachine.declinedAt)
            .putLong("stopped_at", stateMachine.stoppedAt)
            .putBoolean("stop_standstill_required", stateMachine.stopStandstillRequired)
            .putLong("stationary_since", stateMachine.stationarySince)

        if (stateMachine.currentSessionId != null) {
            prefs.putString("current_session_id", stateMachine.currentSessionId)
        } else {
            prefs.remove("current_session_id")
        }

        if (stateMachine.currentDetectionId != null) {
            prefs.putString("pending_detection_id", stateMachine.currentDetectionId)
        } else {
            prefs.remove("pending_detection_id")
        }

        prefs.apply()

        updateForegroundNotification()
        sendBroadcast(Intent(EVENT_STATUS_CHANGED).setPackage(packageName))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlertSound()
                getSystemService(NotificationManager::class.java).cancel(ALERT_NOTIFICATION_ID)
                val currentSpeed = lastFix?.let { if (it.hasSpeed()) it.speed else 0f } ?: 0f
                stateMachine.stopDrive(currentSpeed)

                // Clear GPS accumulation for next trip
                distanceSinceLastFix = 0f
                lastFix = null
                trackingPaused = false
                lowSpeedSince = 0L

                // Service remains running to continue auto detection for subsequent drives!
                updateForegroundNotification()
                sendBroadcast(Intent(EVENT_STATUS_CHANGED).setPackage(packageName))
                return START_STICKY
            }

            ACTION_DECLINE -> {
                stopAlertSound()
                getSystemService(NotificationManager::class.java).cancel(ALERT_NOTIFICATION_ID)
                val detectionId = intent.getStringExtra("detection_id")
                stateMachine.declineDrive(detectionId)

                distanceSinceLastFix = 0f
                lastFix = null
                trackingPaused = false
                lowSpeedSince = 0L

                updateForegroundNotification()
                sendBroadcast(Intent(EVENT_STATUS_CHANGED).setPackage(packageName))
                return START_STICKY
            }

            ACTION_CONFIRM -> {
                stopAlertSound()
                getSystemService(NotificationManager::class.java).cancel(ALERT_NOTIFICATION_ID)
                val detectionId = intent.getStringExtra("detection_id")
                val sessionId = stateMachine.confirmDrive(detectionId)

                if (sessionId != null) {
                    val tripPrefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                    tripPrefs.edit()
                        .remove("trip_vehicle_id")
                        .putLong("trip_started_at", stateMachine.tripStartedAt)
                        .putLong("trip_start_distance_m", tripPrefs.getLong("confirmed_distance_m", 0L))
                        .putLong("odometer_synced_distance_m", tripPrefs.getLong("confirmed_distance_m", 0L))
                        .putBoolean("trip_summary_pending", true)
                        .apply()

                    distanceSinceLastFix = 0f
                    lastFix = null
                    trackingPaused = false
                    lowSpeedSince = 0L

                    lockVehicleForSession(sessionId, stateMachine.tripStartedAt)
                }

                updateForegroundNotification()
                sendBroadcast(Intent(EVENT_STATUS_CHANGED).setPackage(packageName))
                return START_STICKY
            }

            ACTION_AUTO_COMPLETE_15MIN -> {
                val sessionId = intent.getStringExtra("session_id")
                Log.i("RaahiDriving", "ACTION_AUTO_COMPLETE_15MIN received for session: $sessionId")
                if (!sessionId.isNullOrBlank()) {
                    stateMachine.on15MinTimeout(sessionId)
                }
                updateForegroundNotification()
                return START_STICKY
            }

            ACTION_START -> {
                if (!hasLocationPermission()) {
                    stateMachine.onPermissionStatusChanged(false)
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean("is_service_running", false)
                        .putBoolean("is_driving_confirmed", false)
                        .putString("drive_state", AutoDriveState.PERMISSION_REQUIRED.name)
                        .apply()
                    sendBroadcast(Intent(EVENT_STATUS_CHANGED).setPackage(packageName))
                    stopSelf()
                    return START_NOT_STICKY
                }

                stateMachine.onPermissionStatusChanged(true)
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("is_service_running", true)
                    .apply()
                sendBroadcast(Intent(EVENT_STATUS_CHANGED).setPackage(packageName))
                startForegroundCompat()
                beginLocationUpdates()
                return START_STICKY
            }

            else -> {
                return START_STICKY
            }
        }
    }

    private fun startForegroundCompat() {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateForegroundNotification() {
        try {
            val notification = buildForegroundNotification()
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w("RaahiDriving", "Could not update foreground notification", e)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val isDriving = isConfirmedDriving
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val contentPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                7400,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
        }

        builder.setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)

        if (isDriving) {
            val stopIntent = Intent(this, DrivingTrackingService::class.java).setAction(ACTION_STOP)
            val stopPendingIntent = PendingIntent.getService(
                this,
                7404,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            builder.setContentTitle("Raahi trip in progress")
                .setContentText("Recording trip distance • Odometer sync active")
                .addAction(
                    0,
                    "Stop Drive",
                    stopPendingIntent
                )
        } else {
            builder.setContentTitle("Raahi auto drive detection")
                .setContentText("Monitoring for your next drive")
        }

        return builder.build()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private fun beginLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w("RaahiDriving", "Location permission missing; stopping GPS updates safely")
            stopLocationUpdates()
            stateMachine.onPermissionStatusChanged(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        stopLocationUpdates()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach(::processLocation)
            }
        }

        try {
            fused.requestLocationUpdates(request, callback!!, mainLooper)
                .addOnFailureListener { error ->
                    Log.e("RaahiDriving", "Location updates failed", error)
                    stopLocationUpdates()
                    if (error is SecurityException || !hasLocationPermission()) {
                        stateMachine.onPermissionStatusChanged(false)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
        } catch (error: SecurityException) {
            Log.e("RaahiDriving", "Location permission revoked during GPS startup", error)
            stopLocationUpdates()
            stateMachine.onPermissionStatusChanged(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun processLocation(location: Location) {
        if (!location.hasAccuracy() || location.accuracy > 25f) return
        if (System.currentTimeMillis() - location.time > 15_000L) return
        if (location.hasSpeed() && location.speed > 55f) return

        val now = System.currentTimeMillis()
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val previous = lastFix
        lastFix = location

        val speedMps = if (location.hasSpeed()) location.speed else {
            if (previous != null && location.time > previous.time) {
                previous.distanceTo(location) / ((location.time - previous.time) / 1000f)
            } else 0f
        }

        // Forward speed fix to state machine
        stateMachine.onSpeedUpdate(speedMps)

        // If not in an active drive state, don't accumulate trip distance
        if (!isConfirmedDriving) return

        // Pause accumulation for prolonged walking / low speed crawl
        if (speedMps >= 2.2f) {
            lowSpeedSince = 0L
            trackingPaused = false
        } else {
            if (lowSpeedSince == 0L) lowSpeedSince = now
            if (now - lowSpeedSince >= 3 * 60 * 1000L) {
                trackingPaused = true
            }
        }

        if (trackingPaused) return

        // Accumulate GPS distance
        if (previous != null && location.time > previous.time) {
            val deltaMs = location.time - previous.time
            val deltaMeters = previous.distanceTo(location)
            val segmentSpeed = deltaMeters / (deltaMs / 1000f)
            if (deltaMs <= 15_000L && deltaMeters in 1f..200f && segmentSpeed <= 55f) {
                distanceSinceLastFix += deltaMeters
                val wholeMeters = distanceSinceLastFix.toInt()
                if (wholeMeters >= 100) {
                    val total = prefs.getLong("confirmed_distance_m", 0L) + wholeMeters
                    prefs.edit().putLong("confirmed_distance_m", total).apply()
                    distanceSinceLastFix -= wholeMeters
                    sendBroadcast(
                        Intent(EVENT_DISTANCE_UPDATED)
                            .setPackage(packageName)
                            .putExtra("distance_meters", total)
                    )
                    maybeSyncOdometer(total)
                }
            }
        }
    }

    private fun postTripSummaryNotification() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val startedAt = prefs.getLong("trip_started_at", 0L)
        if (startedAt <= 0L) return
        if (!prefs.getBoolean("trip_summary_pending", false)) return

        val tripStartDistance = prefs.getLong("trip_start_distance_m", 0L)
        val distanceKm = (
            prefs.getLong("confirmed_distance_m", 0L) - tripStartDistance
        ).coerceAtLeast(0L) / 1000.0

        val elapsedMinutes = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L) / 60_000L
        val duration = if (elapsedMinutes >= 60L) {
            "${elapsedMinutes / 60}h ${elapsedMinutes % 60}m"
        } else {
            "$elapsedMinutes min"
        }

        val manager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Raahi trip completed")
            .setContentText(String.format(java.util.Locale.getDefault(), "%.2f km • %s", distanceKm, duration))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    String.format(java.util.Locale.getDefault(), "Trip distance: %.2f km\nDuration: %s", distanceKm, duration)
                )
            )
            .setAutoCancel(true)
            .build()

        manager.notify(SUMMARY_NOTIFICATION_ID, notification)
        prefs.edit()
            .putLong("trip_started_at", 0L)
            .putBoolean("trip_summary_pending", false)
            .apply()
    }

    private fun maybeSyncOdometer(totalDistanceMeters: Long) {
        val now = System.currentTimeMillis()
        if (syncInProgress || now - lastSyncAt < SYNC_INTERVAL_MS) return

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val syncedMeters = prefs.getLong("odometer_synced_distance_m", 0L)
        val unsyncedMeters = totalDistanceMeters - syncedMeters
        if (unsyncedMeters < 1000L) return

        syncInProgress = true
        serviceScope.launch {
            try {
                val vehicle = vehicleRepository.myVehicle() ?: return@launch
                val lockedVehicleId = prefs.getString("trip_vehicle_id", null)
                if (lockedVehicleId.isNullOrBlank()) {
                    Log.d("RaahiDriving", "Odometer sync deferred: waiting for current trip vehicle lock")
                    return@launch
                }
                if (vehicle.id != lockedVehicleId) {
                    Log.w("RaahiDriving", "Vehicle lock mismatch; refusing odometer update")
                    return@launch
                }

                val incrementKm = (unsyncedMeters / 1000L).toInt()
                if (incrementKm <= 0) return@launch

                val latestSynced = prefs.getLong("odometer_synced_distance_m", 0L)
                if (latestSynced != syncedMeters) return@launch

                val targetKm = vehicle.odometerKm + incrementKm
                lastSyncAt = System.currentTimeMillis()
                vehicleRepository.updateOdometer(targetKm)

                prefs.edit()
                    .putLong("odometer_synced_distance_m", syncedMeters + incrementKm.toLong() * 1000L)
                    .putInt("odometer_last_confirmed_reading_km", targetKm)
                    .apply()

                Log.i("RaahiDriving", "Odometer sync confirmed: +$incrementKm km")
            } catch (e: Exception) {
                Log.w("RaahiDriving", "Odometer sync failed; pending distance retained", e)
            } finally {
                syncInProgress = false
            }
        }
    }

    private fun lockVehicleForSession(sessionId: String, tripStartedAt: Long) {
        if (vehicleLockInProgress) return
        vehicleLockInProgress = true
        val tripPrefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        serviceScope.launch {
            try {
                repeat(3) { attempt ->
                    if (stateMachine.currentSessionId != sessionId || tripPrefs.getLong("trip_started_at", 0L) != tripStartedAt) {
                        return@launch
                    }
                    try {
                        val vehicle = vehicleRepository.myVehicle()
                        if (vehicle == null) {
                            Log.w("RaahiDriving", "No vehicle configured; distance tracking only")
                            return@launch
                        }
                        if (stateMachine.currentSessionId != sessionId || tripPrefs.getLong("trip_started_at", 0L) != tripStartedAt) {
                            return@launch
                        }
                        tripPrefs.edit()
                            .putString("trip_vehicle_id", vehicle.id)
                            .putInt("trip_start_odometer_km", vehicle.odometerKm)
                            .apply()
                        Log.i("RaahiDriving", "Trip vehicle locked: ${vehicle.id}")
                        return@launch
                    } catch (e: Exception) {
                        Log.w("RaahiDriving", "Vehicle lock attempt ${attempt + 1}/3 failed", e)
                        if (attempt < 2) delay(1500L * (attempt + 1))
                    }
                }
            } finally {
                vehicleLockInProgress = false
            }
        }
    }

    private fun schedule15MinAlarm(sessionId: String, delayMs: Long) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(this, AutoDriveAlarmReceiver::class.java).apply {
                action = ACTION_AUTO_COMPLETE_15MIN
                putExtra("session_id", sessionId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                7415,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            val triggerAt = System.currentTimeMillis() + delayMs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Log.i("RaahiDriving", "Scheduled 15-min auto-complete alarm for session $sessionId in ${delayMs / 1000}s")
        } catch (e: Exception) {
            Log.w("RaahiDriving", "Failed to schedule 15-min alarm", e)
        }
    }

    private fun cancel15MinAlarm(sessionId: String?) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(this, AutoDriveAlarmReceiver::class.java).apply {
                action = ACTION_AUTO_COMPLETE_15MIN
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                7415,
                intent,
                PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.i("RaahiDriving", "Cancelled 15-min auto-complete alarm for session $sessionId")
            }
        } catch (e: Exception) {
            Log.w("RaahiDriving", "Failed to cancel 15-min alarm", e)
        }
    }

    private fun startAlertSound() {
        stopAlertSound()
        alertSoundJob = serviceScope.launch {
            val toneGenerator = runCatching {
                android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 80)
            }.getOrNull()
            try {
                repeat(10) {
                    if (stateMachine.state != AutoDriveState.AWAITING_USER_CONFIRMATION) return@launch
                    toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 350)
                    delay(3000L)
                }
            } finally {
                toneGenerator?.release()
            }
        }
    }

    private fun stopAlertSound() {
        alertSoundJob?.cancel()
        alertSoundJob = null
    }

    private fun postDrivingAlert(detectionId: String) {
        val yesIntent = Intent(this, DrivingTrackingService::class.java).apply {
            action = ACTION_CONFIRM
            putExtra("detection_id", detectionId)
        }
        val yesPendingIntent = PendingIntent.getService(
            this,
            7405,
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val noIntent = Intent(this, DrivingTrackingService::class.java).apply {
            action = ACTION_DECLINE
            putExtra("detection_id", detectionId)
        }
        val noPendingIntent = PendingIntent.getService(
            this,
            7406,
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            putExtra("show_driving_confirmation", true)
            putExtra("detection_id", detectionId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                ALERT_NOTIFICATION_ID,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
        }

        val builder = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)

        val notification = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Are you driving your car?")
            .setContentText("Raahi detected vehicle motion. Confirm to track distance.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }
            .addAction(
                0,
                "Yes, I'm driving",
                yesPendingIntent
            )
            .addAction(
                0,
                "No, not now",
                noPendingIntent
            )
            .build()

        getSystemService(NotificationManager::class.java).notify(ALERT_NOTIFICATION_ID, notification)
        startAlertSound()
    }

    private fun stopLocationUpdates() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
    }

    override fun onDestroy() {
        stopAlertSound()
        periodicCheckJob?.cancel()
        stopLocationUpdates()
        serviceScope.cancel()
        isConfirmedDriving = false
        currentDriveState = AutoDriveState.MONITORING
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean("is_service_running", false)
            .putBoolean("is_driving_confirmed", false)
            .apply()
        sendBroadcast(Intent(EVENT_STATUS_CHANGED).setPackage(packageName))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
