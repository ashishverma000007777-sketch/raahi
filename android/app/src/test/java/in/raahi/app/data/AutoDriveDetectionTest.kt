package `in`.raahi.app.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit test suite for Auto Drive Detection state machine and lifecycle.
 * Tests cover:
 * 1. Initial monitoring state and permission gating.
 * 2. Continuous movement detection prompting user.
 * 3. Idempotent movement detection while awaiting confirmation.
 * 4. User acceptance activating exactly one unique driving session.
 * 5. User decline dismissing prompt without starting a trip or corrupting state.
 * 6. Manual stop completing the session without disabling auto monitoring.
 * 7. Standstill requirement preventing prompt-stop loops when stopped while moving.
 * 8. 15-minute continuous stationary rule completing the trip and scheduling alarm.
 * 9. Resumed movement cancelling the pending 15-minute auto-complete timer.
 * 10. Session ID isolation preventing old timer actions from completing newer trips.
 */
class AutoDriveDetectionTest {

    private var currentTime = 1_000_000L
    private var promptCount = 0
    private var lastDetectionId: String? = null
    private var completedTripReason: String? = null
    private var completedTripSessionId: String? = null
    private var scheduledTimerSession: String? = null
    private var cancelledTimerSession: String? = null

    private lateinit var stateMachine: AutoDriveStateMachine

    @Before
    fun setup() {
        currentTime = 1_000_000L
        promptCount = 0
        lastDetectionId = null
        completedTripReason = null
        completedTripSessionId = null
        scheduledTimerSession = null
        cancelledTimerSession = null

        stateMachine = AutoDriveStateMachine(
            speedThresholdMps = 2.8f,
            detectDurationMs = 8000L,
            declineCooldownMs = 10 * 60_000L,
            stopCooldownMs = 60_000L,
            stopAutoCompletionThresholdMs = 15 * 60_000L,
            clock = { currentTime },
            onPromptUser = { id ->
                promptCount++
                lastDetectionId = id
            },
            onTripCompleted = { sId, _, reason ->
                completedTripSessionId = sId
                completedTripReason = reason
            },
            onSchedule15MinTimer = { sId, _ -> scheduledTimerSession = sId },
            onCancel15MinTimer = { sId -> cancelledTimerSession = sId },
        )
    }

    @Test
    fun testInitialStateIsMonitoringWhenPermissionGranted() {
        assertEquals(AutoDriveState.MONITORING, stateMachine.state)
        assertNull(stateMachine.currentSessionId)
        assertFalse(stateMachine.isConfirmedDriving)
    }

    @Test
    fun testPermissionRequiredGating() {
        stateMachine.onPermissionChanged(false)
        assertEquals(AutoDriveState.PERMISSION_REQUIRED, stateMachine.state)

        // Movement detected while permission revoked should be rejected
        stateMachine.onSpeedUpdate(5.0f)
        currentTime += 10000L
        stateMachine.onSpeedUpdate(5.0f)
        assertEquals(0, promptCount)
        assertEquals(AutoDriveState.PERMISSION_REQUIRED, stateMachine.state)

        // Permission restored
        stateMachine.onPermissionChanged(true)
        assertEquals(AutoDriveState.MONITORING, stateMachine.state)
    }

    @Test
    fun testPromptTriggeredOnContinuousMovement() {
        // First speed fix starts movement timer
        stateMachine.onSpeedUpdate(4.0f)
        assertEquals(0, promptCount)
        assertEquals(AutoDriveState.MONITORING, stateMachine.state)

        // 8 seconds of continuous motion threshold reached
        currentTime += 8500L
        stateMachine.onSpeedUpdate(4.5f)

        assertEquals("Prompt must fire after continuous movement", 1, promptCount)
        assertNotNull(lastDetectionId)
        assertEquals(AutoDriveState.AWAITING_USER_CONFIRMATION, stateMachine.state)
    }

    @Test
    fun testIdempotentPromptingWhileAwaitingConfirmation() {
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 8500L
        stateMachine.onSpeedUpdate(4.5f)
        val initialDetectionId = lastDetectionId

        // Subsequent movement events while awaiting confirmation must not create new prompts
        currentTime += 5000L
        stateMachine.onSpeedUpdate(6.0f)
        assertEquals("Duplicate movement events must not re-prompt", 1, promptCount)
        assertEquals(initialDetectionId, stateMachine.currentDetectionId)
        assertEquals(AutoDriveState.AWAITING_USER_CONFIRMATION, stateMachine.state)
    }

    @Test
    fun testUserAcceptStartsExactlyOneSession() {
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 8500L
        stateMachine.onSpeedUpdate(4.5f)
        val detectionId = lastDetectionId

        val sessionId = stateMachine.confirmDrive(detectionId)
        assertNotNull("Session ID must be created on confirmation", sessionId)
        assertEquals(AutoDriveState.ACTIVE_DRIVE, stateMachine.state)
        assertTrue(stateMachine.isConfirmedDriving)
        assertEquals(sessionId, stateMachine.currentSessionId)
        assertNull("Pending detection ID must be cleared after confirmation", stateMachine.currentDetectionId)

        // Idempotent confirm must return existing session and not duplicate
        val duplicateSessionId = stateMachine.confirmDrive(detectionId)
        assertEquals(sessionId, duplicateSessionId)
    }

    @Test
    fun testUserDeclineDismissesPromptWithoutTrip() {
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 8500L
        stateMachine.onSpeedUpdate(4.5f)
        val detectionId = lastDetectionId

        val declined = stateMachine.declineDrive(detectionId)
        assertTrue(declined)
        assertEquals(AutoDriveState.MONITORING, stateMachine.state)
        assertFalse(stateMachine.isConfirmedDriving)
        assertNull(stateMachine.currentSessionId)
        assertNull(stateMachine.currentDetectionId)
    }

    @Test
    fun testManualStopEndsDriveAndPreservesMonitoring() {
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 8500L
        stateMachine.onSpeedUpdate(4.5f)
        val sessionId = stateMachine.confirmDrive(lastDetectionId)

        // Vehicle was in motion (5.0 m/s) when stop was pressed
        val stopped = stateMachine.stopDrive(currentSpeedMps = 5.0f)
        assertTrue(stopped)
        assertEquals("MANUAL_STOP", completedTripReason)
        assertEquals(sessionId, completedTripSessionId)
        assertEquals(AutoDriveState.MONITORING, stateMachine.state)
        assertFalse("Driving confirmed flag must be cleared", stateMachine.isConfirmedDriving)
        assertNull("Active session ID must be reset", stateMachine.currentSessionId)

        // Standstill requirement must prevent prompt while still in same movement
        currentTime += 70_000L // Past stop cooldown
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 9000L
        stateMachine.onSpeedUpdate(4.0f)
        assertEquals("Must not prompt while vehicle continues same motion after stop", 1, promptCount)

        // Vehicle stops (drops below threshold)
        stateMachine.onSpeedUpdate(0.5f)

        // Now subsequent drive begins
        stateMachine.onSpeedUpdate(3.5f)
        currentTime += 9000L
        stateMachine.onSpeedUpdate(4.0f)
        assertEquals("Subsequent independent drive must prompt user", 2, promptCount)
        assertEquals(AutoDriveState.AWAITING_USER_CONFIRMATION, stateMachine.state)
    }

    @Test
    fun testFifteenMinuteContinuousStationaryRule() {
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 8500L
        stateMachine.onSpeedUpdate(4.5f)
        val sessionId = stateMachine.confirmDrive(lastDetectionId)

        // Vehicle comes to a stop (speed < 0.8 m/s)
        stateMachine.onSpeedUpdate(0.2f)
        assertEquals(AutoDriveState.STOPPED_PENDING_COMPLETION, stateMachine.state)
        assertEquals(sessionId, scheduledTimerSession)

        // 5 minutes later: still stopped (< 15 min threshold)
        currentTime += 5 * 60_000L
        stateMachine.onSpeedUpdate(0.1f)
        assertEquals(AutoDriveState.STOPPED_PENDING_COMPLETION, stateMachine.state)
        assertNull("Trip must not complete at 5 minutes", completedTripReason)

        // 15 continuous minutes standstill reached
        currentTime += 10 * 60_000L + 1000L
        stateMachine.onSpeedUpdate(0.0f)

        assertEquals("15_MIN_IDLE", completedTripReason)
        assertEquals(sessionId, completedTripSessionId)
        assertEquals(AutoDriveState.MONITORING, stateMachine.state)
        assertFalse(stateMachine.isConfirmedDriving)
        assertNull(stateMachine.currentSessionId)
    }

    @Test
    fun testMovementResumesBeforeFifteenMinutesCancelsStopTimer() {
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 8500L
        stateMachine.onSpeedUpdate(4.5f)
        val sessionId = stateMachine.confirmDrive(lastDetectionId)

        // Vehicle stops
        stateMachine.onSpeedUpdate(0.0f)
        assertEquals(AutoDriveState.STOPPED_PENDING_COMPLETION, stateMachine.state)

        // Movement resumes after 6 minutes (speed >= 2.2 m/s)
        currentTime += 6 * 60_000L
        stateMachine.onSpeedUpdate(5.0f)
        assertEquals(AutoDriveState.ACTIVE_DRIVE, stateMachine.state)
        assertEquals(sessionId, cancelledTimerSession)

        // Alarm firing after 15 minutes from original stop must be rejected
        currentTime += 10 * 60_000L
        val timedOut = stateMachine.on15MinTimeout(sessionId!!)
        assertFalse("Cancelled timer must not complete the trip", timedOut)
        assertEquals(AutoDriveState.ACTIVE_DRIVE, stateMachine.state)
    }

    @Test
    fun testOldTimerCannotCompleteNewerTrip() {
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 8500L
        stateMachine.onSpeedUpdate(4.5f)
        val session1 = stateMachine.confirmDrive(lastDetectionId)

        // Trip 1 stopped manually
        stateMachine.stopDrive(currentSpeedMps = 0.0f)

        // Cooldown passed
        currentTime += 70_000L

        // Trip 2 begins later
        stateMachine.onSpeedUpdate(4.0f)
        currentTime += 8500L
        stateMachine.onSpeedUpdate(5.0f)
        val session2 = stateMachine.confirmDrive(lastDetectionId)
        assertNotEquals(session1, session2)

        // Old alarm from Trip 1 arrives targeting session1
        val timeoutTrip1 = stateMachine.on15MinTimeout(session1!!)
        assertFalse("Alarm targeting old session must not affect active session", timeoutTrip1)
        assertEquals(AutoDriveState.ACTIVE_DRIVE, stateMachine.state)
        assertEquals(session2, stateMachine.currentSessionId)
    }
}
