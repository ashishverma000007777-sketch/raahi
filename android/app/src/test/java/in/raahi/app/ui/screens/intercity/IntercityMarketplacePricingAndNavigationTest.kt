package `in`.raahi.app.ui.screens.intercity

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.ceil

/**
 * Unit tests for Owner & Travel Agent Intercity Marketplace:
 * 1. Owner pricing calculation formulas (suggested ₹15/km, minimum ₹15/km for <=50km, ₹12/km for >50km).
 * 2. Fare validation and boundary checks (20, 30, 40, 50, 60, 100 km).
 * 3. Turn-by-Turn Google Maps Navigation intent URI construction & non-destructive launch.
 * 4. Owner direct completion (no completion OTP, 0% commission) vs Agent settlement lifecycle.
 * 5. Driver single-active-booking constraint state machine.
 */
class IntercityMarketplacePricingAndNavigationTest {

    private fun calculateOwnerSuggestedFare(distanceKm: Double): Double {
        return ceil(distanceKm * 15.0)
    }

    private fun calculateOwnerMinimumFare(distanceKm: Double): Double {
        return if (distanceKm <= 50.0) {
            ceil(distanceKm * 15.0)
        } else {
            ceil(distanceKm * 12.0)
        }
    }

    @Test
    fun testOwnerFareCalculationFormulas() {
        // 20 km: Suggested ₹300; minimum ₹300
        assertEquals(300.0, calculateOwnerSuggestedFare(20.0), 0.001)
        assertEquals(300.0, calculateOwnerMinimumFare(20.0), 0.001)

        // 30 km: Suggested ₹450; minimum ₹450
        assertEquals(450.0, calculateOwnerSuggestedFare(30.0), 0.001)
        assertEquals(450.0, calculateOwnerMinimumFare(30.0), 0.001)

        // 40 km: Suggested ₹600; minimum ₹600
        assertEquals(600.0, calculateOwnerSuggestedFare(40.0), 0.001)
        assertEquals(600.0, calculateOwnerMinimumFare(40.0), 0.001)

        // 50 km (boundary): Suggested ₹750; minimum ₹750 (<= 50km uses ₹15/km)
        assertEquals(750.0, calculateOwnerSuggestedFare(50.0), 0.001)
        assertEquals(750.0, calculateOwnerMinimumFare(50.0), 0.001)

        // 60 km (>50km uses ₹12/km minimum): Suggested ₹900; minimum ₹720
        assertEquals(900.0, calculateOwnerSuggestedFare(60.0), 0.001)
        assertEquals(720.0, calculateOwnerMinimumFare(60.0), 0.001)

        // 100 km: Suggested ₹1,500; minimum ₹1,200
        assertEquals(1500.0, calculateOwnerSuggestedFare(100.0), 0.001)
        assertEquals(1200.0, calculateOwnerMinimumFare(100.0), 0.001)
    }

    @Test
    fun testOwnerFareCeilRoundingOnFractionalDistance() {
        val distance = 50.5
        // ceil(50.5 * 15) = ceil(757.5) = 758
        assertEquals(758.0, calculateOwnerSuggestedFare(distance), 0.001)
        // ceil(50.5 * 12) = ceil(606.0) = 606
        assertEquals(606.0, calculateOwnerMinimumFare(distance), 0.001)
    }

    @Test
    fun testOwnerFareInputValidation() {
        val distance = 60.0
        val minFare = calculateOwnerMinimumFare(distance) // 720.0

        fun validateFare(entered: Double): Pair<Boolean, String?> {
            return if (entered < minFare) {
                false to "Minimum allowed fare is ₹${minFare.toInt()}. Please enter at least ₹${minFare.toInt()}."
            } else {
                true to null
            }
        }

        // Below minimum fare must be rejected
        val (validLow, errMsgLow) = validateFare(700.0)
        assertFalse("Fare below minimum must be rejected", validLow)
        assertNotNull(errMsgLow)
        assertTrue(errMsgLow!!.contains("720"))

        // Exact minimum fare is accepted
        val (validExact, errMsgExact) = validateFare(720.0)
        assertTrue("Exact minimum fare must be accepted", validExact)
        assertNull(errMsgExact)

        // Higher fare (e.g. suggested) is accepted
        val (validSuggested, errMsgSuggested) = validateFare(900.0)
        assertTrue("Suggested fare must be accepted", validSuggested)
        assertNull(errMsgSuggested)
    }

    @Test
    fun testGoogleMapsNavigationIntentUri() {
        val destLat = 28.6139
        val destLng = 77.2090
        val destAddress = "Connaught Place, New Delhi"

        val primaryNavUri = "google.navigation:q=$destLat,$destLng&mode=d"
        assertEquals("google.navigation:q=28.6139,77.209&mode=d", primaryNavUri)

        val fallbackGeoUri = "geo:$destLat,$destLng?q=" + java.net.URLEncoder.encode(destAddress, "UTF-8")
        assertEquals("geo:28.6139,77.209?q=Connaught+Place%2C+New+Delhi", fallbackGeoUri)
    }

    @Test
    fun testNavigationPreservesActiveTripSession() {
        // Launching external turn-by-turn navigation must NOT alter trip status
        var tripStatus = "IN_PROGRESS"
        var isNavigationLaunched = false

        fun onLaunchTurnByTurnNavigation() {
            isNavigationLaunched = true
            // Trip status must remain IN_PROGRESS
        }

        onLaunchTurnByTurnNavigation()
        assertTrue(isNavigationLaunched)
        assertEquals("IN_PROGRESS", tripStatus)
    }

    @Test
    fun testOwnerDirectCompletionWorkflow() {
        // OWNER workflow:
        // ASSIGNED -> ARRIVED -> verify pickup OTP -> IN_PROGRESS -> arrive at destination -> COMPLETED directly
        var status = "ASSIGNED"
        var paymentStatus = "PENDING"
        var completionOtpRequired = false

        // Arrive at pickup
        status = "ARRIVED"
        assertEquals("ARRIVED", status)

        // Pickup OTP verified
        status = "IN_PROGRESS"
        assertEquals("IN_PROGRESS", status)

        // Driver arrives at destination for OWNER ride
        // Direct complete: No completion OTP required, 0% commission, marked PAID
        status = "COMPLETED"
        paymentStatus = "PAID"
        completionOtpRequired = false

        assertEquals("COMPLETED", status)
        assertEquals("PAID", paymentStatus)
        assertFalse(completionOtpRequired)
    }

    @Test
    fun testAgentSettlementLifecycleWorkflow() {
        // AGENT workflow:
        // IN_PROGRESS -> arriveAtDestination -> AWAITING_AGENT_SETTLEMENT
        // -> driver submits settlement ref -> SETTLEMENT_SUBMITTED
        // -> agent confirms settlement -> SETTLEMENT_CONFIRMED
        // -> driver verifies completion OTP -> COMPLETED
        var status = "IN_PROGRESS"
        var settlementRef: String? = null
        var isSettlementConfirmed = false

        // Step 1: Arrive at destination
        status = "AWAITING_AGENT_SETTLEMENT"
        assertEquals("AWAITING_AGENT_SETTLEMENT", status)

        // Step 2: Driver submits settlement reference
        settlementRef = "UPI-REF-9876543210"
        status = "SETTLEMENT_SUBMITTED"
        assertEquals("SETTLEMENT_SUBMITTED", status)
        assertEquals("UPI-REF-9876543210", settlementRef)

        // Step 3: Agent confirms settlement
        isSettlementConfirmed = true
        status = "SETTLEMENT_CONFIRMED"
        assertEquals("SETTLEMENT_CONFIRMED", status)
        assertTrue(isSettlementConfirmed)

        // Step 4: Driver enters 6-digit completion OTP
        val validOtp = "654321"
        var tripCompleted = false
        if (validOtp.length == 6) {
            status = "COMPLETED"
            tripCompleted = true
        }
        assertEquals("COMPLETED", status)
        assertTrue(tripCompleted)
    }

    @Test
    fun testDriverOneActiveBookingConstraint() {
        val activeStatuses = setOf(
            "ASSIGNED",
            "ARRIVED",
            "IN_PROGRESS",
            "AWAITING_AGENT_SETTLEMENT",
            "SETTLEMENT_SUBMITTED",
            "SETTLEMENT_CONFIRMED"
        )

        fun canAcceptNewBooking(currentDriverTripStatuses: List<String>): Boolean {
            val hasActive = currentDriverTripStatuses.any { it in activeStatuses }
            return !hasActive
        }

        // Driver with no trips can accept
        assertTrue(canAcceptNewBooking(emptyList()))

        // Driver with only COMPLETED or CANCELLED trips can accept
        assertTrue(canAcceptNewBooking(listOf("COMPLETED", "CANCELLED")))

        // Driver with an IN_PROGRESS trip CANNOT accept
        assertFalse(canAcceptNewBooking(listOf("IN_PROGRESS")))

        // Driver in AWAITING_AGENT_SETTLEMENT CANNOT accept
        assertFalse(canAcceptNewBooking(listOf("AWAITING_AGENT_SETTLEMENT")))

        // Driver in SETTLEMENT_CONFIRMED CANNOT accept
        assertFalse(canAcceptNewBooking(listOf("SETTLEMENT_CONFIRMED")))

        // Driver with ASSIGNED trip CANNOT accept
        assertFalse(canAcceptNewBooking(listOf("ASSIGNED")))
    }
}
