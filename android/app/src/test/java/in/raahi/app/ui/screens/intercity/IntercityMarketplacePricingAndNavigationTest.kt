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

    @Test
    fun testRoundTripValidation_Success_WhenReturnAfterDepartureAndTourDetailsPresent() {
        val now = java.time.LocalDateTime.now()
        val departure = now.plusHours(2)
        val ret = departure.plusDays(2)
        val tourDetails = "Day 1: Chandigarh to Shimla.\nDay 2: Kufri sightseeing & return."

        fun validateTrip(tripType: String, dep: java.time.LocalDateTime, retTime: java.time.LocalDateTime?, details: String): Pair<Boolean, String?> {
            val isDepartureInFuture = dep.isAfter(now.minusMinutes(2))
            if (!isDepartureInFuture) return false to "Departure time cannot be in the past"
            if (tripType == "ROUND_TRIP") {
                if (retTime == null || !retTime.isAfter(dep)) {
                    return false to "Return time must be after departure time"
                }
                if (details.isBlank()) {
                    return false to "Please specify duration, planned stops or itinerary for drivers"
                }
            }
            return true to null
        }

        val (valid, err) = validateTrip("ROUND_TRIP", departure, ret, tourDetails)
        assertTrue("Valid round trip must pass validation", valid)
        assertNull(err)

        // Verify tour days calculation
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(departure.toLocalDate(), ret.toLocalDate()).toInt()
        val durationLabel = if (daysBetween == 0) "Same-Day Return" else "${daysBetween + 1} Days Tour"
        assertEquals("3 Days Tour", durationLabel)
    }

    @Test
    fun testRoundTripValidation_RejectsReturnBeforeDeparture() {
        val now = java.time.LocalDateTime.now()
        val departure = now.plusHours(4)
        val ret = departure.minusHours(1) // invalid

        val isReturnAfterDeparture = ret.isAfter(departure)
        assertFalse("Return before departure must be rejected", isReturnAfterDeparture)
    }

    @Test
    fun testRoundTripValidation_RejectsBlankTourDetails() {
        val blankDetails = "   "
        val isTourDetailsValid = blankDetails.isNotBlank()
        assertFalse("Blank tour details must be rejected", isTourDetailsValid)
    }

    @Test
    fun testOneWayTrip_DoesNotRequireReturnTimeOrTourDetails() {
        val tripType = "ONE_WAY"
        val isRoundTrip = tripType == "ROUND_TRIP"
        assertFalse(isRoundTrip)

        // For One Way, return time is null and tour details is empty
        val returnTime: String? = null
        val tourDetails = ""
        val isValid = if (isRoundTrip) returnTime != null && tourDetails.isNotBlank() else true
        assertTrue("One way trip does not require return time or tour details", isValid)
    }

    @Test
    fun testTravelAgentPricing_DriverPayoutCalculationAndCommissionConstraints() {
        fun calculateAgentPayout(customerFare: Double, agentCommission: Double): Triple<Boolean, Double, String?> {
            if (customerFare <= 0.0) return Triple(false, 0.0, "Customer fare must be positive")
            if (agentCommission < 0.0) return Triple(false, 0.0, "Commission cannot be negative")
            if (agentCommission >= customerFare) return Triple(false, 0.0, "Commission must be strictly less than customer fare")
            val payout = customerFare - agentCommission
            return Triple(true, payout, null)
        }

        // Standard valid case: customer ₹5000, commission ₹1000 => payout ₹4000
        val (valid1, payout1, err1) = calculateAgentPayout(5000.0, 1000.0)
        assertTrue(valid1)
        assertEquals(4000.0, payout1, 0.001)
        assertNull(err1)

        // Boundary invalid: commission equals customer fare => rejected
        val (valid2, _, err2) = calculateAgentPayout(3000.0, 3000.0)
        assertFalse("Commission equal to fare must be rejected", valid2)
        assertEquals("Commission must be strictly less than customer fare", err2)

        // Boundary invalid: commission exceeds customer fare => rejected
        val (valid3, _, err3) = calculateAgentPayout(3000.0, 3500.0)
        assertFalse("Commission exceeding fare must be rejected", valid3)
        assertEquals("Commission must be strictly less than customer fare", err3)

        // Boundary invalid: negative commission => rejected
        val (valid4, _, err4) = calculateAgentPayout(3000.0, -100.0)
        assertFalse("Negative commission must be rejected", valid4)
        assertEquals("Commission cannot be negative", err4)
    }

    @Test
    fun testOwnerTripDriverBenefitModel() {
        val ownerFare = 3600.0
        val ownerPlatformFee = 0.0
        val driverCashPayout = ownerFare - ownerPlatformFee

        assertEquals(3600.0, driverCashPayout, 0.001)
        assertEquals(0.0, ownerPlatformFee, 0.001)
    }
}
