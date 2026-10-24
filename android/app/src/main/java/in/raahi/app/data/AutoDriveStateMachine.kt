package `in`.raahi.app.data

import java.util.UUID

/**
 * Validated states for automatic driving detection and trip lifecycle.
 */
enum class AutoDriveState {
    PERMISSION_REQUIRED,
    MONITORING,
    DRIVE_DETECTED,
    AWAITING_USER_CONFIRMATION,
    ACTIVE_DRIVE,
    STOPPED_PENDING_COMPLETION,
    COMPLETED,
    ERROR;

    companion object {
        fun fromString(value: String?): AutoDriveState {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MONITORING
        }
    }
}

/**
 * Pure state machine governing automatic drive detection, prompt dispatch,
 * active drive tracking, 15-minute continuous stop auto-completion, and manual stop reset.
 *
 * Designed to be deterministic and fully unit-testable without Android framework mocks.
 */
class AutoDriveStateMachine(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val speedThresholdMps: Float = 2.78f, // > 10 km/h
    private val detectDurationMs: Long = 60_000L, // 60s continuous movement
    private val declineCooldownMs: Long = 2 * 60 * 60 * 1000L, // 2 hours
    private val stopCooldownMs: Long = 60_000L, // 1 min buffer after manual stop
    private val stopAutoCompletionThresholdMs: Long = 15 * 60 * 1000L, // 15 continuous minutes
    private val onStateChanged: (oldState: AutoDriveState, newState: AutoDriveState) -> Unit = { _, _ -> },
    private val onPromptUser: (detectionId: String) -> Unit = {},
    private val onTripCompleted: (sessionId: String, durationMs: Long, reason: String) -> Unit = { _, _, _ -> },
    private val onSchedule15MinTimer: (sessionId: String, delayMs: Long) -> Unit = { _, _ -> },
    private val onCancel15MinTimer: (sessionId: String) -> Unit = {},
) {
    var state: AutoDriveState = AutoDriveState.MONITORING
        private set

    var currentSessionId: String? = null
        private set

    var currentDetectionId: String? = null
        private set

    val isConfirmedDriving: Boolean
        get() = state == AutoDriveState.ACTIVE_DRIVE || state == AutoDriveState.STOPPED_PENDING_COMPLETION

    var movingSince: Long = 0L
        private set

    var stationarySince: Long = 0L
        private set

    var declinedAt: Long = 0L
        private set

    var stoppedAt: Long = 0L
        private set

    var stopStandstillRequired: Boolean = false
        private set

    var tripStartedAt: Long = 0L
        private set

    private fun transitionTo(newState: AutoDriveState) {
        if (state == newState) return
        val old = state
        state = newState
        onStateChanged(old, newState)
    }

    /**
     * Restore state machine state from persistent storage (e.g. SharedPreferences).
     */
    fun restore(
        persistedState: AutoDriveState,
        sessionId: String?,
        detectionId: String?,
        tripStart: Long,
        stationaryTime: Long,
        declineTime: Long,
        stopTime: Long,
        standstillReq: Boolean,
        hasPermission: Boolean
    ) {
        if (!hasPermission) {
            transitionTo(AutoDriveState.PERMISSION_REQUIRED)
            return
        }
        currentSessionId = sessionId
        currentDetectionId = detectionId
        tripStartedAt = tripStart
        stationarySince = stationaryTime
        declinedAt = declineTime
        stoppedAt = stopTime
        stopStandstillRequired = standstillReq

        val now = clock()
        if ((persistedState == AutoDriveState.ACTIVE_DRIVE || persistedState == AutoDriveState.STOPPED_PENDING_COMPLETION) && !sessionId.isNullOrBlank()) {
            if (stationaryTime > 0L && (now - stationaryTime) >= stopAutoCompletionThresholdMs) {
                // Device rebooted or process died while stopped, and 15 min has elapsed: complete immediately!
                transitionTo(AutoDriveState.STOPPED_PENDING_COMPLETION)
                completeCurrentTrip(reason = "15_MIN_IDLE_RESTORE")
                return
            }
            if (stationaryTime > 0L) {
                transitionTo(AutoDriveState.STOPPED_PENDING_COMPLETION)
                val remaining = (stopAutoCompletionThresholdMs - (now - stationaryTime)).coerceAtLeast(1000L)
                onSchedule15MinTimer(sessionId, remaining)
            } else {
                transitionTo(AutoDriveState.ACTIVE_DRIVE)
            }
        } else if (persistedState == AutoDriveState.AWAITING_USER_CONFIRMATION && !detectionId.isNullOrBlank()) {
            transitionTo(AutoDriveState.AWAITING_USER_CONFIRMATION)
        } else {
            transitionTo(AutoDriveState.MONITORING)
        }
    }

    /**
     * Update permission status.
     */
    fun onPermissionStatusChanged(hasPermission: Boolean) {
        if (!hasPermission) {
            transitionTo(AutoDriveState.PERMISSION_REQUIRED)
        } else if (state == AutoDriveState.PERMISSION_REQUIRED) {
            transitionTo(AutoDriveState.MONITORING)
        }
    }

    fun onPermissionChanged(hasPermission: Boolean) = onPermissionStatusChanged(hasPermission)

    /**
     * Process new speed fix from GPS.
     */
    fun onSpeedUpdate(speedMps: Float) {
        if (state == AutoDriveState.PERMISSION_REQUIRED) return

        val now = clock()

        when (state) {
            AutoDriveState.MONITORING, AutoDriveState.COMPLETED -> {
                // If manual stop occurred while moving, wait until car drops below speed threshold
                // before recognizing new movement as an independent drive.
                if (stopStandstillRequired) {
                    if (speedMps < speedThresholdMps) {
                        stopStandstillRequired = false
                    } else {
                        // Still within the same moving leg that was stopped
                        return
                    }
                }

                // Check decline cooldown
                if (declinedAt > 0L && (now - declinedAt) < declineCooldownMs) {
                    return
                }

                // Check minimum safety buffer after manual stop
                if (stoppedAt > 0L && (now - stoppedAt) < stopCooldownMs) {
                    return
                }

                if (speedMps >= speedThresholdMps) {
                    if (movingSince == 0L) {
                        movingSince = now
                    } else if (now - movingSince >= detectDurationMs) {
                        // Continuous motion detected! Prompt the user.
                        val detectionId = UUID.randomUUID().toString()
                        currentDetectionId = detectionId
                        transitionTo(AutoDriveState.AWAITING_USER_CONFIRMATION)
                        onPromptUser(detectionId)
                    }
                } else {
                    movingSince = 0L
                }
            }

            AutoDriveState.AWAITING_USER_CONFIRMATION -> {
                // Already prompting user: do not send duplicate prompts for the same detection event!
            }

            AutoDriveState.ACTIVE_DRIVE, AutoDriveState.STOPPED_PENDING_COMPLETION -> {
                if (speedMps >= 2.2f) {
                    // Movement resumed! Cancel pending stop completion.
                    if (stationarySince > 0L || state == AutoDriveState.STOPPED_PENDING_COMPLETION) {
                        currentSessionId?.let { onCancel15MinTimer(it) }
                        stationarySince = 0L
                    }
                    transitionTo(AutoDriveState.ACTIVE_DRIVE)
                } else if (speedMps < 0.8f) {
                    // Vehicle is stopped / stationary
                    if (stationarySince == 0L) {
                        stationarySince = now
                        transitionTo(AutoDriveState.STOPPED_PENDING_COMPLETION)
                        currentSessionId?.let { onSchedule15MinTimer(it, stopAutoCompletionThresholdMs) }
                    } else if (now - stationarySince >= stopAutoCompletionThresholdMs) {
                        // 15 continuous minutes stationary threshold reached!
                        completeCurrentTrip(reason = "15_MIN_IDLE")
                    }
                }
                // Speeds between 0.8 and 2.2 m/s are treated as creeping in traffic (do not complete trip).
            }

            AutoDriveState.PERMISSION_REQUIRED, AutoDriveState.ERROR, AutoDriveState.DRIVE_DETECTED -> {
                // Inactive, transient or blocked states
            }
        }
    }

    /**
     * User accepts driving prompt ("Yes, I'm driving").
     * Idempotent: exactly one active trip session is created.
     */
    fun confirmDrive(detectionId: String? = null): String? {
        if (state == AutoDriveState.ACTIVE_DRIVE) {
            // Already active; return existing session
            return currentSessionId
        }

        // If a specific detectionId is supplied, verify it matches the current detection event
        if (detectionId != null && currentDetectionId != null && detectionId != currentDetectionId) {
            // Stale detection prompt: ignore
            return null
        }

        val sessionId = UUID.randomUUID().toString()
        currentSessionId = sessionId
        currentDetectionId = null
        movingSince = 0L
        stationarySince = 0L
        tripStartedAt = clock()
        stopStandstillRequired = false

        transitionTo(AutoDriveState.ACTIVE_DRIVE)
        return sessionId
    }

    /**
     * User dismisses or declines driving prompt ("No, not now").
     * Idempotent: does NOT create a trip.
     */
    fun declineDrive(detectionId: String? = null): Boolean {
        if (detectionId != null && currentDetectionId != null && detectionId != currentDetectionId) {
            // Stale detection prompt
            return false
        }

        currentDetectionId = null
        movingSince = 0L
        declinedAt = clock()

        transitionTo(AutoDriveState.MONITORING)
        return true
    }

    /**
     * User manually presses "Stop Drive".
     * Completes current trip, resets session, and stays in MONITORING mode for future drives.
     */
    fun stopDrive(currentSpeedMps: Float = 0f): Boolean {
        val sessionId = currentSessionId ?: return false

        currentSessionId?.let { onCancel15MinTimer(it) }

        val duration = if (tripStartedAt > 0L) clock() - tripStartedAt else 0L
        onTripCompleted(sessionId, duration, "MANUAL_STOP")

        stoppedAt = clock()
        // If vehicle is moving when user stopped drive, require standstill before next detection
        stopStandstillRequired = currentSpeedMps >= speedThresholdMps

        currentSessionId = null
        currentDetectionId = null
        stationarySince = 0L
        tripStartedAt = 0L
        movingSince = 0L

        transitionTo(AutoDriveState.COMPLETED)
        transitionTo(AutoDriveState.MONITORING)
        return true
    }

    /**
     * 15-minute timer fired (from AlarmManager or coroutine timer).
     * Validates that the timer matches the current session ID so old timers cannot complete newer trips.
     */
    fun on15MinTimeout(targetSessionId: String): Boolean {
        if (currentSessionId == null || currentSessionId != targetSessionId) {
            // Old or stale timer: ignore!
            return false
        }

        val now = clock()
        if (stationarySince == 0L || (now - stationarySince) < stopAutoCompletionThresholdMs - 2000L) {
            // Movement resumed or not yet 15 min: do not complete
            return false
        }

        completeCurrentTrip(reason = "15_MIN_IDLE")
        return true
    }

    private fun completeCurrentTrip(reason: String) {
        val sId = currentSessionId ?: return
        onCancel15MinTimer(sId)

        val duration = if (tripStartedAt > 0L) clock() - tripStartedAt else 0L
        onTripCompleted(sId, duration, reason)

        currentSessionId = null
        currentDetectionId = null
        stationarySince = 0L
        tripStartedAt = 0L
        movingSince = 0L
        stoppedAt = clock()
        stopStandstillRequired = false

        transitionTo(AutoDriveState.COMPLETED)
        transitionTo(AutoDriveState.MONITORING)
    }
}
