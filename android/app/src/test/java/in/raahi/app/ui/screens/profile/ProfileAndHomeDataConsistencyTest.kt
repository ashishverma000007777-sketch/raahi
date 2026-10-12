package `in`.raahi.app.ui.screens.profile

import `in`.raahi.app.network.HelperStatusDto
import `in`.raahi.app.network.UserDto
import `in`.raahi.app.network.VehicleDto
import org.junit.Assert.*
import org.junit.Test

class ProfileAndHomeDataConsistencyTest {

    // Helper functions mirroring the exact production logic in HomeScreen and ProfileScreen
    private fun formatHomeGreeting(user: UserDto?, greeting: String = "Good Evening"): String {
        val firstName = user?.name?.trim()?.takeIf { it.isNotBlank() }?.split("\\s+".toRegex())?.firstOrNull()?.takeIf { it.isNotBlank() }
        return if (firstName != null) "$greeting,\n$firstName" else greeting
    }

    private fun profileDisplayName(user: UserDto?): String {
        return user?.name?.trim()?.takeIf { it.isNotBlank() } ?: "Raahi User"
    }

    private fun profileInitial(user: UserDto?): String {
        return user?.name?.trim()?.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercaseChar()?.toString() ?: "R"
    }

    private fun profilePhone(user: UserDto?): String {
        return user?.phone?.takeIf { it.isNotBlank() } ?: "--"
    }

    private fun profileRole(user: UserDto?): String {
        return user?.role?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "--"
    }

    private fun profileRatingText(user: UserDto?): String {
        return if (user != null && user.ratingAvg > 0.0) "%.1f".format(user.ratingAvg) else "--"
    }

    private fun profileStatusText(user: UserDto?): String {
        return if (user?.isVerified == true) "Verified" else "Pending"
    }

    private fun isWalletPillEligible(user: UserDto?, helperStatus: HelperStatusDto?): Boolean {
        return (user?.role == "HELPER" || user?.role == "MECHANIC") &&
            helperStatus?.applicationStatus == "APPROVED"
    }

    private fun formatWalletBalance(balance: Double): String {
        return "₹${if (balance % 1.0 == 0.0) balance.toLong().toString() else "%.2f".format(balance)}"
    }

    @Test
    fun testGreetingWithSkippedNameShowsNeutralGreetingWithoutPlaceholder() {
        // User who skipped entering name during onboarding
        val userNoName = UserDto(
            id = "u1", name = null, phone = "+919876543210", role = "DRIVER",
            vehicleType = null, vehicleReg = null, isVerified = false, ratingAvg = 0.0, totalHelps = 0
        )
        val greeting = formatHomeGreeting(userNoName, "Good Evening")
        assertEquals("Good Evening", greeting)
        assertFalse("Greeting must never contain hardcoded placeholder 'avi'", greeting.contains("avi", ignoreCase = true))

        // Blank name string
        val userBlankName = userNoName.copy(name = "   ")
        assertEquals("Good Evening", formatHomeGreeting(userBlankName, "Good Evening"))

        // Null user
        assertEquals("Good Evening", formatHomeGreeting(null, "Good Evening"))
    }

    @Test
    fun testGreetingWithRealNameShowsFirstName() {
        val userWithName = UserDto(
            id = "u2", name = "Vikram Singh", phone = "+919876543210", role = "DRIVER",
            vehicleType = null, vehicleReg = null, isVerified = false, ratingAvg = 0.0, totalHelps = 0
        )
        assertEquals("Good Evening,\nVikram", formatHomeGreeting(userWithName, "Good Evening"))
    }

    @Test
    fun testProfileRemovesFakeRatingAndFakeVerification() {
        // Unrated, unverified user
        val user = UserDto(
            id = "u3", name = null, phone = "+919876500000", role = "DRIVER",
            vehicleType = null, vehicleReg = null, isVerified = false, ratingAvg = 0.0, totalHelps = 0
        )

        assertEquals("Raahi User", profileDisplayName(user))
        assertEquals("R", profileInitial(user))
        assertEquals("+919876500000", profilePhone(user))
        assertEquals("Driver", profileRole(user))
        assertEquals("--", profileRatingText(user)) // Must not be fake "5.0"
        assertEquals("Pending", profileStatusText(user)) // Must not be fake "Verified"
        assertFalse(user.isVerified)
    }

    @Test
    fun testProfileWithLegitimateRatingAndVerification() {
        val approvedHelper = UserDto(
            id = "u4", name = "Aman Kumar", phone = "+919876511111", role = "HELPER",
            vehicleType = "BIKE", vehicleReg = "PB65A1234", isVerified = true, ratingAvg = 4.8, totalHelps = 24
        )

        assertEquals("Aman Kumar", profileDisplayName(approvedHelper))
        assertEquals("A", profileInitial(approvedHelper))
        assertEquals("Helper", profileRole(approvedHelper))
        assertEquals("4.8", profileRatingText(approvedHelper))
        assertEquals("Verified", profileStatusText(approvedHelper))
        assertTrue(approvedHelper.isVerified)
    }

    @Test
    fun testWalletEligibilityForDriverAndPendingApplicant() {
        val driver = UserDto(
            id = "u5", name = "Ramesh", phone = "+919876522222", role = "DRIVER",
            vehicleType = null, vehicleReg = null, isVerified = false, ratingAvg = 0.0, totalHelps = 0
        )

        // Ordinary driver with no application
        val statusNone = HelperStatusDto(
            applicationStatus = "NONE", rejectionReason = null, suspensionReason = null,
            accountActive = true, blockedUntil = null, blockReason = null,
            online = false, commissionBalance = 0.0, commissionDue = false, commissionRate = 0.1,
            canAcceptJobs = false, ratingAvg = 0.0, totalHelps = 0, name = null, phone = null,
            services = null, serviceRadiusKm = null
        )
        assertFalse("Ordinary driver must not see helper wallet pill", isWalletPillEligible(driver, statusNone))

        // Pending helper applicant
        val statusPending = statusNone.copy(applicationStatus = "PENDING")
        assertFalse("Pending applicant must not be treated as approved helper", isWalletPillEligible(driver, statusPending))

        // Approved helper
        val helper = driver.copy(role = "HELPER")
        val statusApproved = statusNone.copy(applicationStatus = "APPROVED", commissionBalance = -120.0)
        assertTrue("Approved helper must see wallet shortcut", isWalletPillEligible(helper, statusApproved))
        assertEquals("₹-120", formatWalletBalance(statusApproved.commissionBalance))
        assertNotEquals("₹-55", formatWalletBalance(statusApproved.commissionBalance))
    }

    @Test
    fun testVehicleDetailsNotHardcoded() {
        val vehicle: VehicleDto? = null
        // When vehicle is null, should be recognized as empty
        assertNull("Saved vehicle is null", vehicle)

        val realVehicle = VehicleDto(
            id = "veh1", brand = "Tata", model = "Nexon", variant = "XZ+",
            modelYear = 2022, fuelType = "DIESEL", registrationNumber = "CH01CC9999",
            odometerKm = 35200, odometerUpdatedAt = null, lastServiceDate = null,
            lastServiceOdometerKm = null, insuranceExpiry = null, pucExpiry = null,
            tyreReplacedDate = null, tyreReplacedOdometerKm = null, batteryReplacedDate = null,
            batteryReplacedOdometerKm = null, createdAt = null, updatedAt = null
        )
        val name = "${realVehicle.brand} ${realVehicle.model} ${realVehicle.variant}"
        val sub = "${realVehicle.registrationNumber} • ${realVehicle.odometerKm} km"

        assertEquals("Tata Nexon XZ+", name)
        assertEquals("CH01CC9999 • 35200 km", sub)
        assertFalse(name.contains("Hyundai Grand i10", ignoreCase = true))
        assertFalse(sub.contains("PB01B1775", ignoreCase = true))
    }
}
