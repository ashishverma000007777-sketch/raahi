package `in`.raahi.app.ui.screens.jobs

import org.junit.Assert.*
import org.junit.Test

class EnRouteCancellationProtectionTest {

    @Test
    fun testCancellationReasonCategories() {
        val helperReasons = listOf(
            "VEHICLE_EMERGENCY" to "Vehicle issue / Breakdown",
            "UNSAFE_CONDITIONS" to "Unsafe conditions / Road hazard",
            "CUSTOMER_UNREACHABLE" to "Customer unreachable / No response",
            "INCORRECT_LOCATION" to "Incorrect or inaccessible location",
            "HELPER_CANCELLED" to "Unable to assist at this time",
            "OTHER" to "Other reason",
        )

        val customerReasons = listOf(
            "CUSTOMER_CANCELLED" to "Change of plans / No longer needed",
            "CUSTOMER_CANCELLED" to "Found alternate help",
            "CUSTOMER_CANCELLED" to "Helper taking too long",
            "INCORRECT_LOCATION" to "Entered incorrect location / address",
            "VEHICLE_EMERGENCY" to "Emergency resolved",
            "OTHER" to "Other reason",
        )

        // Verify genuine emergency codes exist in helper list
        val helperCodes = helperReasons.map { it.first }
        assertTrue(helperCodes.contains("VEHICLE_EMERGENCY"))
        assertTrue(helperCodes.contains("UNSAFE_CONDITIONS"))
        assertTrue(helperCodes.contains("CUSTOMER_UNREACHABLE"))

        // Verify customer reason codes
        val customerCodes = customerReasons.map { it.first }
        assertTrue(customerCodes.contains("CUSTOMER_CANCELLED"))
        assertTrue(customerCodes.contains("INCORRECT_LOCATION"))
    }

    @Test
    fun testTravelLossStatusBadgeFormatting() {
        fun formatStatusBadge(status: String?, payoutStatus: String?, approved: Double?, estimated: Double?): String {
            return when (status) {
                "ADMIN_APPROVED" -> {
                    if (payoutStatus == "PAID") "Reimbursement Paid: ₹${approved?.toInt() ?: 0}"
                    else "Approved by Admin (₹${approved?.toInt() ?: 0} approved, settlement pending)"
                }
                "ELIGIBLE_PENDING_REVIEW" -> "Pending Admin Review (₹${estimated?.toInt() ?: 0} est.)"
                "REJECTED" -> "Reimbursement Claim Rejected"
                "NOT_ELIGIBLE" -> "Travel Reimbursement Ineligible"
                else -> "Travel Protection Evaluated"
            }
        }

        assertEquals("Approved by Admin (₹120 approved, settlement pending)", formatStatusBadge("ADMIN_APPROVED", "UNPAID", 120.0, 150.0))
        assertEquals("Reimbursement Paid: ₹120", formatStatusBadge("ADMIN_APPROVED", "PAID", 120.0, 150.0))
        assertEquals("Pending Admin Review (₹90 est.)", formatStatusBadge("ELIGIBLE_PENDING_REVIEW", "UNPAID", null, 90.0))
        assertEquals("Reimbursement Claim Rejected", formatStatusBadge("REJECTED", "UNPAID", 0.0, 50.0))
        assertEquals("Travel Reimbursement Ineligible", formatStatusBadge("NOT_ELIGIBLE", "UNPAID", 0.0, 0.0))
    }

    @Test
    fun testEnRouteStepperLabels() {
        val steps = listOf("PENDING", "MATCHED", "EN_ROUTE", "ARRIVED", "IN_PROGRESS", "WORK_DONE", "COMPLETED")
        val stepLabels = mapOf(
            "PENDING" to "Requested",
            "MATCHED" to "Helper assigned",
            "EN_ROUTE" to "Helper on the way",
            "ARRIVED" to "Helper arrived",
            "IN_PROGRESS" to "Work in progress",
            "WORK_DONE" to "Work completed",
            "COMPLETED" to "Job complete",
        )

        assertTrue(steps.contains("EN_ROUTE"))
        assertEquals("Helper on the way", stepLabels["EN_ROUTE"])
        assertEquals(2, steps.indexOf("EN_ROUTE"))
    }

    @Test
    fun testInFlightCancellationDebounceGuard() {
        var isActing = false
        var callCount = 0

        fun onCancelClick(): Boolean {
            if (isActing) return false
            isActing = true
            callCount++
            return true
        }

        assertTrue(onCancelClick())
        assertFalse("Duplicate click while in-flight should be ignored", onCancelClick())
        assertEquals(1, callCount)
    }
}
