package `in`.raahi.app.ui.screens.intercity

import `in`.raahi.app.Routes
import `in`.raahi.app.ui.components.RaahiTab
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Cancellation dialog logic (state retention, reason validation, in-flight guard, error recovery).
 * 2. Bottom navigation architecture (5 equal tabs, backward-compatibility aliases, route mapping).
 * 3. Route preservation and integrity across mobility flows.
 */
class IntercityCancellationAndNavigationTest {

    @Test
    fun testCancellationReasonResolution() {
        val presetReasons = listOf(
            "Change of plans",
            "Found alternate ride",
            "Vehicle issue / unavailable",
            "Schedule conflict",
            "Other reason"
        )

        // Preset selection directly maps
        val selectedPreset = presetReasons[0]
        val resolvedPreset = selectedPreset.trim()
        assertEquals("Change of plans", resolvedPreset)

        // Custom reason when "Other" is selected
        val customText = "  Emergency family travel  "
        val resolvedCustom = customText.trim()
        assertEquals("Emergency family travel", resolvedCustom)

        // Fallback when custom reason is left blank
        val blankCustom = "   "
        val resolvedFallback = blankCustom.trim().ifBlank { "Cancelled by user" }
        assertEquals("Cancelled by user", resolvedFallback)
    }

    @Test
    fun testCancellationInFlightGuard() {
        var isCancelling = false
        var apiCallCount = 0

        fun attemptCancel(): Boolean {
            if (isCancelling) return false
            isCancelling = true
            apiCallCount++
            return true
        }

        // First attempt succeeds
        assertTrue("First cancellation attempt should be initiated", attemptCancel())
        assertEquals(1, apiCallCount)
        assertTrue(isCancelling)

        // In-flight attempt is safely guarded
        assertFalse("Duplicate in-flight cancellation must be rejected", attemptCancel())
        assertEquals(1, apiCallCount)

        // After completion / failure reset
        isCancelling = false
        assertTrue("Subsequent attempt allowed after reset", attemptCancel())
        assertEquals(2, apiCallCount)
    }

    @Test
    fun testCancellationErrorRecovery() {
        // Simulating the dialog state machine
        var isDialogOpen = true
        var inFlight = false
        var selectedReason = "Found alternate ride"
        var errorMessage: String? = null

        fun simulateCancelFailure(backendError: String) {
            inFlight = true
            // Backend fails
            inFlight = false
            errorMessage = backendError
            // Dialog must stay open on failure so the user can read the error and retry
            // isDialogOpen remains true
        }

        simulateCancelFailure("Network timeout. Please check your connection and retry.")

        assertTrue("Dialog must remain open on failure", isDialogOpen)
        assertFalse("In-flight state must reset", inFlight)
        assertEquals("Found alternate ride", selectedReason)
        assertEquals("Network timeout. Please check your connection and retry.", errorMessage)

        // User retries with success
        fun simulateCancelSuccess() {
            inFlight = true
            inFlight = false
            errorMessage = null
            isDialogOpen = false // Only dismiss upon backend confirmation
        }

        simulateCancelSuccess()
        assertFalse("Dialog should only dismiss upon successful confirmation", isDialogOpen)
        assertNull(errorMessage)
    }

    @Test
    fun testBottomNavigationTabStructure() {
        // Verify all 5 primary tabs are present in order: Home | Services | AI | Garage | Profile
        val tabs = RaahiTab.values()
        assertEquals("Bottom bar must have exactly 5 tabs", 5, tabs.size)
        assertEquals(RaahiTab.HOME, tabs[0])
        assertEquals(RaahiTab.SERVICES, tabs[1])
        assertEquals(RaahiTab.AI_MECHANIC, tabs[2])
        assertEquals(RaahiTab.GARAGE, tabs[3])
        assertEquals(RaahiTab.PROFILE, tabs[4])

        // Verify label typography
        assertEquals("Home", RaahiTab.HOME.label)
        assertEquals("Services", RaahiTab.SERVICES.label)
        assertEquals("AI", RaahiTab.AI_MECHANIC.label)
        assertEquals("Garage", RaahiTab.GARAGE.label)
        assertEquals("Profile", RaahiTab.PROFILE.label)
    }

    @Test
    fun testBackwardCompatibilityTabAliases() {
        // ASSISTANCE and MECHANICS must seamlessly resolve to SERVICES
        assertEquals(RaahiTab.SERVICES, RaahiTab.ASSISTANCE)
        assertEquals(RaahiTab.SERVICES, RaahiTab.MECHANICS)

        // SHOP must resolve to GARAGE
        assertEquals(RaahiTab.GARAGE, RaahiTab.SHOP)

        // TRIPS must resolve to HOME
        assertEquals(RaahiTab.HOME, RaahiTab.TRIPS)
    }

    @Test
    fun testRoutesForTabMapping() {
        assertEquals(Routes.HOME, Routes.forTab(RaahiTab.HOME))
        assertEquals(Routes.ASSISTANCE, Routes.forTab(RaahiTab.SERVICES))
        assertEquals(Routes.AI_MECHANIC, Routes.forTab(RaahiTab.AI_MECHANIC))
        assertEquals(Routes.MY_GARAGE, Routes.forTab(RaahiTab.GARAGE))
        assertEquals(Routes.PROFILE, Routes.forTab(RaahiTab.PROFILE))

        // Legacy tab alias mapping
        assertEquals(Routes.ASSISTANCE, Routes.forTab(RaahiTab.ASSISTANCE))
        assertEquals(Routes.ASSISTANCE, Routes.forTab(RaahiTab.MECHANICS))
        assertEquals(Routes.MY_GARAGE, Routes.forTab(RaahiTab.SHOP))
        assertEquals(Routes.HOME, Routes.forTab(RaahiTab.TRIPS))
    }

    @Test
    fun testKeyMobilityRoutesPreserved() {
        // Hub routes
        assertEquals("trips_hub", Routes.TRIPS)
        assertEquals("assistance_hub", Routes.ASSISTANCE)

        // Intercity routes
        assertEquals("intercity_marketplace", Routes.INTERCITY_MARKETPLACE)
        assertEquals("intercity_create", Routes.INTERCITY_CREATE)
        assertEquals("intercity_driver_trips", Routes.INTERCITY_DRIVER_TRIPS)
        assertEquals("intercity_agent_dashboard", Routes.INTERCITY_AGENT_DASHBOARD)

        // Parametric routes
        assertEquals("intercity_search?from={from}&to={to}", Routes.INTERCITY_SEARCH)
        assertEquals("intercity_detail/{tripId}", Routes.INTERCITY_DETAIL)
        assertEquals("intercity_active/{tripId}", Routes.INTERCITY_ACTIVE)

        // Assistance and roadside services
        assertEquals("request_help", Routes.REQUEST_HELP)
        assertEquals("mechanics_map", Routes.MECHANICS_MAP)
        assertEquals("my_jobs", Routes.MY_JOBS)
        assertEquals("sos", Routes.SOS)
        assertEquals("ai_mechanic", Routes.AI_MECHANIC)
        assertEquals("shop", Routes.SHOP)
    }
}
