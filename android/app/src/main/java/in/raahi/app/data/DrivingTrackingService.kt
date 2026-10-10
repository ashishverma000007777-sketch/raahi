package `in`.raahi.app.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@AndroidEntryPoint
class DrivingTrackingService : Service() {
    @Inject lateinit var vehicleRepository: VehicleRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_START = "in.raahi.app.driving.START"
        const val ACTION_CONFIRM = "in.raahi.app.driving.CONFIRM"
        const val ACTION_DECLINE = "in.raahi.app.driving.DECLINE"
        const val ACTION_STOP = "in.raahi.app.driving.STOP"
        const val EVENT_DRIVING_DETECTED = "in.raahi.app.driving.DETECTED"
        const val EVENT_DISTANCE_UPDATED = "in.raahi.app.driving.DISTANCE_UPDATED"

        private const val CHANNEL_ID = "raahi_driving_tracking_silent_v3"
        private const val ALERT_CHANNEL_ID = "raahi_driving_alerts_high"
        private const val NOTIFICATION_ID = 7401
        private const val PREFS = "raahi_driving_tracking"
        private const val SPEED_THRESHOLD_MPS = 3.3f // approximately 12 km/h
        private const val DETECT_DURATION_MS = 30_000L
        private const val DECLINE_COOLDOWN_MS = 2 * 60 * 60 * 1000L
        private const val SYNC_INTERVAL_MS = 10 * 60 * 1000L
    }

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var callback: LocationCallback? = null
    private var lastFix: Location? = null
    private var movingSince = 0L
    private var confirmed = false
    private var promptSent = false
    private var distanceSinceLastFix = 0f
    private var lastSyncAt = 0L
    private var syncInProgress = false
    private var vehicleLockInProgress = false
    private var trackingPaused = false
    private var lowSpeedSince = 0L
    private var stationarySince = 0L
    private var stationaryAnchor: Location? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Raahi background tracking",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Silent notification while GPS tracking is active"
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                // TRIP_SUMMARY_GUARD_PATCH
                postTripSummaryNotification()
                // TRIP_STOP_STATE_RESET_PATCH
                confirmed = false
                promptSent = false
                distanceSinceLastFix = 0f
                lastFix = null
                movingSince = 0L
                trackingPaused = false
                lowSpeedSince = 0L
                stationarySince = 0L
                stationaryAnchor = null
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_DECLINE -> {
                // DECLINE_REARM_PATCH: honour cooldown, then allow future detection again.
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putLong("declined_at", System.currentTimeMillis()).apply()
                confirmed = false
                promptSent = false
                movingSince = 0L
                distanceSinceLastFix = 0f
                lastFix = null
                return START_NOT_STICKY
            }
            ACTION_CONFIRM -> {
                // TRIP_START_CHECKPOINT_PATCH
                val tripPrefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                tripPrefs.edit()
                    .remove("trip_vehicle_id")
                    .putLong("trip_started_at", System.currentTimeMillis())
                    .putLong(
                        "trip_start_distance_m",
                        tripPrefs.getLong("confirmed_distance_m", 0L)
                    )
                    // TRIP_SYNC_CHECKPOINT_PATCH: keep the global distance checkpoint.
                    .putLong(
                        "odometer_synced_distance_m",
                        tripPrefs.getLong("confirmed_distance_m", 0L)
                    )
                    .putBoolean("trip_summary_pending", true)
                    .apply()

                // TRIP_DISTANCE_RESET_PATCH
                // Prevent unfinished GPS distance from leaking into the next trip.
                distanceSinceLastFix = 0f
                movingSince = 0L
                confirmed = true
                promptSent = true
                lastFix = null
                trackingPaused = false
                lowSpeedSince = 0L
                stationarySince = 0L

                // Lock the vehicle selected at trip confirmation time.
                val confirmedTripStartedAt = tripPrefs.getLong("trip_started_at", 0L)
                if (!vehicleLockInProgress) {
                    vehicleLockInProgress = true
                    serviceScope.launch {
                        try {
                            repeat(3) { attempt ->
                                if (!confirmed || tripPrefs.getLong("trip_started_at", 0L) != confirmedTripStartedAt) {
                                    return@launch
                                }
                                try {
                                    val vehicle = vehicleRepository.myVehicle()
                                    if (vehicle == null) {
                                        Log.w("RaahiDriving", "No vehicle configured; distance tracking only")
                                        return@launch
                                    }
                                    if (!confirmed || tripPrefs.getLong("trip_started_at", 0L) != confirmedTripStartedAt) {
                                        return@launch
                                    }
                                    tripPrefs.edit()
                                        .putString("trip_vehicle_id", vehicle.id)
                                        .putInt("trip_start_odometer_km", vehicle.odometerKm)
                                        .apply()
                                    Log.i("RaahiDriving", "Trip vehicle locked")
                                    return@launch
                                } catch (e: Exception) {
                                    Log.w("RaahiDriving", "Vehicle lock attempt ${attempt + 1}/3 failed", e)
                                    if (attempt < 2) kotlinx.coroutines.delay(1500L * (attempt + 1))
                                }
                            }
                        } finally {
                            vehicleLockInProgress = false
                        }
                    }
                }
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (!hasLocationPermission()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundCompat()
                beginLocationUpdates()
                return START_NOT_STICKY
            }
            else -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
    }

    private fun startForegroundCompat() {
        val stopIntent = Intent(this, DrivingTrackingService::class.java)
            .setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            7404,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // ONGOING_STOP_CONTROL_PATCH
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("Raahi driving detection")
            .setContentText("GPS monitoring active. Use Stop tracking to turn it off.")
            .addAction(
                android.app.Notification.Action.Builder(
                    null,
                    "Stop tracking",
                    stopPendingIntent
                ).build()
            )
            .setOngoing(true)
            .build()

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

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private fun beginLocationUpdates() {
        // LOCATION_PERMISSION_SAFETY_PATCH
        if (!hasLocationPermission()) {
            Log.w("RaahiDriving", "Location permission missing; stopping GPS updates safely")
            stopLocationUpdates()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
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
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
        } catch (error: SecurityException) {
            Log.e("RaahiDriving", "Location permission revoked during GPS startup", error)
            stopLocationUpdates()
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

        if (!confirmed) {
            val cooldownActive =
                now - prefs.getLong("declined_at", 0L) < DECLINE_COOLDOWN_MS
            if (cooldownActive || promptSent) return

            if (speedMps >= SPEED_THRESHOLD_MPS) {
                if (movingSince == 0L) movingSince = now
                if (now - movingSince >= DETECT_DURATION_MS) {
                    promptSent = true
                    sendBroadcast(
                        Intent(EVENT_DRIVING_DETECTED).setPackage(packageName)
                    )
                    postDrivingAlert()
                }
            } else {
                movingSince = 0L
            }
            return
        }

        // Driving has resumed: clear pause/stationary timers.
        if (speedMps >= 2.2f) {
            lowSpeedSince = 0L
            stationarySince = 0L
            stationaryAnchor = null
            trackingPaused = false
        } else {
            if (lowSpeedSince == 0L) lowSpeedSince = now

            // Walking/very slow movement pauses accumulation after 3 minutes.
            if (now - lowSpeedSince >= 3 * 60 * 1000L) {
                trackingPaused = true
            }

            // End only when GPS shows almost no displacement for 5 minutes.
            if (speedMps < 0.8f) {
                if (stationarySince == 0L) {
                    stationarySince = now
                    stationaryAnchor = location
                }
                val anchor = stationaryAnchor
                val displacement = anchor?.distanceTo(location) ?: Float.MAX_VALUE
                if (now - stationarySince >= 10 * 60 * 1000L && displacement < 25f) {
                    finishStationarySession()
                    return
                }
            } else {
                stationarySince = 0L
                stationaryAnchor = null
            }
        }

        // Do not accumulate while paused, or from implausible GPS jumps.
        if (trackingPaused) return
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


    // TRIP_SUMMARY_NOTIFICATION_PATCH
    private fun postTripSummaryNotification() {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        // TRIP_SUMMARY_ONCE_PATCH
        val startedAt = prefs.getLong("trip_started_at", 0L)
        if (startedAt <= 0L) return
        if (!prefs.getBoolean("trip_summary_pending", false)) return
        val tripStartDistance = prefs.getLong("trip_start_distance_m", 0L)
        val distanceKm = (
            prefs.getLong("confirmed_distance_m", 0L) - tripStartDistance
        ).coerceAtLeast(0L) / 1000.0
        val elapsedMinutes = if (startedAt > 0L) {
            (System.currentTimeMillis() - startedAt).coerceAtLeast(0L) / 60_000L
        } else 0L

        val duration = if (elapsedMinutes >= 60L) {
            "${elapsedMinutes / 60}h ${elapsedMinutes % 60}m"
        } else {
            "$elapsedMinutes min"
        }

        val manager = getSystemService(NotificationManager::class.java)
        val notification = Notification.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)

            .setContentTitle("Raahi trip completed")
            .setContentText(
                String.format(
                    java.util.Locale.getDefault(),
                    "%.2f km • %s",
                    distanceKm,
                    duration
                )
            )
            .setStyle(
                Notification.BigTextStyle().bigText(
                    String.format(
                        java.util.Locale.getDefault(),
                        "Trip distance: %.2f km\\nDuration: %s",
                        distanceKm,
                        duration
                    )
                )
            )
            .setAutoCancel(true)
            .build()

        manager.notify(7403, notification)
        prefs.edit()
            .putLong("trip_started_at", 0L)
            .putBoolean("trip_summary_pending", false)
            .apply()
    }

    private fun finishStationarySession() {
        confirmed = false
        trackingPaused = false
        lowSpeedSince = 0L
        stationarySince = 0L
        stationaryAnchor = null
        lastFix = null
        stopLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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

                // Re-read the persisted checkpoint immediately before the write.
                // Do not retry by blindly incrementing a previously submitted value.
                val latestSynced = prefs.getLong("odometer_synced_distance_m", 0L)
                if (latestSynced != syncedMeters) return@launch

                val targetKm = vehicle.odometerKm + incrementKm
                // Start the retry cooldown only when an actual API write is about to happen.
                lastSyncAt = System.currentTimeMillis()
                vehicleRepository.updateOdometer(targetKm)

                // Persist only after the API call returns successfully.
                prefs.edit()
                    .putLong(
                        "odometer_synced_distance_m",
                        syncedMeters + incrementKm.toLong() * 1000L
                    )
                    .putInt("odometer_last_confirmed_reading_km", targetKm)
                    .apply()

                Log.i("RaahiDriving", "Odometer sync confirmed: +$incrementKm km")
            } catch (e: Exception) {
                // Keep pending distance for retry; never advance the local checkpoint on failure.
                Log.w("RaahiDriving", "Odometer sync failed; pending distance retained", e)
            } finally {
                syncInProgress = false
            }
        }
    }

    private fun postDrivingAlert() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?.putExtra("show_driving_confirmation", true)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            ?: return

        val pendingIntent = PendingIntent.getActivity(
            this,
            7402,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle("Are you driving your car?")
            .setContentText("Tap to confirm. Raahi will track distance only after you agree.")
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java).notify(7402, notification)
    }

    private fun stopLocationUpdates() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
    }

    override fun onDestroy() {
        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
