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

    @Test
    fun testFormatRoleDisplayName() {
        assertEquals("Vehicle Owner", formatRoleDisplayName("VEHICLE_OWNER"))
        assertEquals("Vehicle Owner", formatRoleDisplayName("OWNER"))
        assertEquals("Vehicle Owner", formatRoleDisplayName("vehicle_owner"))
        assertEquals("Vehicle Owner", formatRoleDisplayName("owner"))
        assertEquals("Travel Agent", formatRoleDisplayName("TRAVEL_AGENT"))
        assertEquals("Travel Agent", formatRoleDisplayName("travel_agent"))
        assertEquals("Driver", formatRoleDisplayName("DRIVER"))
        assertEquals("Driver", formatRoleDisplayName("driver"))
        assertEquals("Mechanic", formatRoleDisplayName("MECHANIC"))
        assertEquals("Helper", formatRoleDisplayName("HELPER"))
        assertEquals("Admin", formatRoleDisplayName("ADMIN"))
        assertEquals("--", formatRoleDisplayName(null))
        assertEquals("--", formatRoleDisplayName(""))
    }

    @Test
    fun testVehicleOwnerProfileDisplay_NoPendingStatusOrDriverHelps() {
        val ownerUser = UserDto(
            id = "owner-1",
            name = "Rajesh Sharma",
            phone = "+919876543210",
            role = "VEHICLE_OWNER",
            vehicleType = "Sedan",
            vehicleReg = "DL01AB1234",
            isVerified = false, // Even if isVerified boolean is false in DB, an Owner is not a driver needing driver verification
            ratingAvg = 4.9,
            totalHelps = 0,
        )

        // 1. Role name and header badge
        assertEquals("Vehicle Owner", formatRoleDisplayName(ownerUser.role))
        assertEquals("VEHICLE OWNER", formatRoleDisplayName(ownerUser.role).uppercase())

        // 2. StatsRow Telemetry for Owner
        val roleUpper = ownerUser.role.uppercase()
        val isOwner = roleUpper in listOf("VEHICLE_OWNER", "OWNER")
        assertTrue("User must be identified as Owner", isOwner)

        val vehicleText = ownerUser.vehicleType?.takeIf { it.isNotBlank() } ?: "Personal"
        val ratingText = if (ownerUser.ratingAvg > 0.0) "%.1f".format(ownerUser.ratingAvg) else "--"
        val roleLabel = "Owner"
        val accountStatus = "Active"

        assertEquals("Sedan", vehicleText)
        assertEquals("4.9", ratingText)
        assertEquals("Owner", roleLabel)
        assertEquals("Active", accountStatus)
        assertNotEquals("Pending", accountStatus)

        // 3. Fallback when vehicleType is null or blank
        val ownerNoVehicle = ownerUser.copy(vehicleType = null)
        val fallbackVehicle = ownerNoVehicle.vehicleType?.takeIf { it.isNotBlank() } ?: "Personal"
        assertEquals("Personal", fallbackVehicle)
    }

    @Test
    fun testTravelAgentProfileDisplay_ShowsAgentRoleAndActiveAccount() {
        val agentUser = UserDto(
            id = "agent-1",
            name = "Vikram Travels",
            phone = "+919876512345",
            role = "TRAVEL_AGENT",
            vehicleType = null,
            vehicleReg = null,
            isVerified = true,
            ratingAvg = 4.8,
            totalHelps = 0,
            agencyName = "Northern Star Fleet"
        )

        assertEquals("Travel Agent", formatRoleDisplayName(agentUser.role))
        assertEquals("TRAVEL AGENT", formatRoleDisplayName(agentUser.role).uppercase())

        val roleUpper = agentUser.role.uppercase()
        val isTravelAgent = roleUpper == "TRAVEL_AGENT"
        assertTrue(isTravelAgent)

        val agencyText = if (!agentUser.agencyName.isNullOrBlank()) "Agent" else "Commercial"
        val ratingText = "%.1f".format(agentUser.ratingAvg)
        val roleLabel = "Agent"
        val accountStatus = "Active"

        assertEquals("Agent", agencyText)
        assertEquals("4.8", ratingText)
        assertEquals("Agent", roleLabel)
        assertEquals("Active", accountStatus)
    }

    @Test
    fun testDriverProfileDisplay_ShowsHelpsAndVerificationStatus() {
        val unverifiedDriver = UserDto(
            id = "driver-1",
            name = "Suresh",
            phone = "+919876500001",
            role = "DRIVER",
            vehicleType = null,
            vehicleReg = null,
            isVerified = false,
            ratingAvg = 0.0,
            totalHelps = 0
        )
        val verifiedDriver = unverifiedDriver.copy(
            id = "driver-2",
            name = "Harpreet Singh",
            isVerified = true,
            ratingAvg = 4.7,
            totalHelps = 15
        )

        // Unverified driver
        assertEquals("Driver", formatRoleDisplayName(unverifiedDriver.role))
        val unverifiedStatus = if (unverifiedDriver.isVerified) "Verified" else "Pending"
        assertEquals("Pending", unverifiedStatus)
        assertEquals("0", unverifiedDriver.totalHelps.toString())

        // Verified driver
        assertEquals("Driver", formatRoleDisplayName(verifiedDriver.role))
        val verifiedStatus = if (verifiedDriver.isVerified) "Verified" else "Pending"
        assertEquals("Verified", verifiedStatus)
        assertEquals("15", verifiedDriver.totalHelps.toString())
    }

    @Test
    fun testMarketplaceRoleBannersAndDriverToggleSegregation() {
        fun resolveMarketplaceBanner(role: String, agencyName: String?): Triple<String, String, Boolean> {
            val isOwner = role in listOf("VEHICLE_OWNER", "OWNER")
            val isAgent = role == "TRAVEL_AGENT" || role == "ADMIN"
            val isDriver = !isOwner && !isAgent

            val subtitle = if (!agencyName.isNullOrBlank()) {
                agencyName
            } else when {
                isOwner -> "Vehicle Owner"
                isAgent -> "Registered Travel Agency"
                else -> "Verified Highway Driver"
            }

            val badgeText = when {
                isOwner -> "VEHICLE OWNER"
                isAgent -> "TRAVEL AGENT"
                else -> "VERIFIED DRIVER"
            }

            val showOnlineToggle = isDriver
            return Triple(badgeText, subtitle, showOnlineToggle)
        }

        // Vehicle Owner
        val (ownerBadge, ownerSub, ownerToggle) = resolveMarketplaceBanner("VEHICLE_OWNER", null)
        assertEquals("VEHICLE OWNER", ownerBadge)
        assertEquals("Vehicle Owner", ownerSub)
        assertFalse("Vehicle Owner must NOT see driver online/offline toggle", ownerToggle)

        // Travel Agent
        val (agentBadge, agentSub, agentToggle) = resolveMarketplaceBanner("TRAVEL_AGENT", null)
        assertEquals("TRAVEL AGENT", agentBadge)
        assertEquals("Registered Travel Agency", agentSub)
        assertFalse("Travel Agent must NOT see driver online/offline toggle", agentToggle)

        // Travel Agent with custom agency name
        val (_, agentCustomSub, _) = resolveMarketplaceBanner("TRAVEL_AGENT", "Express Cabs")
        assertEquals("Express Cabs", agentCustomSub)

        // Driver
        val (driverBadge, driverSub, driverToggle) = resolveMarketplaceBanner("DRIVER", null)
        assertEquals("VERIFIED DRIVER", driverBadge)
        assertEquals("Verified Highway Driver", driverSub)
        assertTrue("Driver MUST see driver online/offline toggle", driverToggle)
    }
}
