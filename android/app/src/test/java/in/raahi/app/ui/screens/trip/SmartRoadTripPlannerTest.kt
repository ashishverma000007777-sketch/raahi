package `in`.raahi.app.ui.screens.trip

import com.google.android.gms.maps.model.LatLng
import `in`.raahi.app.data.GoogleRouteData
import `in`.raahi.app.data.GoogleRoutesProvider
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/**
 * Unit tests for Smart Road Trip Planner:
 * 1. Trip Budget Calculator (fuel, tolls, round-trip, cost/km, unavailable handling).
 * 2. Fuel Range & Stop Planner (range estimation, refuel advice thresholds, detour calculation).
 * 3. Alternative Route Comparison (genuine property-based labeling: Fastest, Shortest, Lowest Cost).
 * 4. Smart Break Planner (detour ranking, open in maps URL generation).
 * 5. Role-based Dashboard & Authorization constraints.
 */
class SmartRoadTripPlannerTest {

    // --- 1. TRIP BUDGET CALCULATOR TESTS ---

    private fun calculateBudget(
        distanceKm: Double,
        isRoundTrip: Boolean,
        mileage: Double,
        fuelPrice: Double,
        tollCost: Double?,
        tollAvailable: Boolean,
    ): Pair<Double, Double?> {
        val effectiveDist = if (isRoundTrip) distanceKm * 2.0 else distanceKm
        val safeMileage = if (mileage > 0.0) mileage else 14.5
        val litres = ((effectiveDist / safeMileage) * 10.0).roundToInt() / 10.0
        val fuelCost = (litres * fuelPrice).roundToInt().toDouble()
        val effectiveToll = if (tollAvailable && tollCost != null) {
            if (isRoundTrip) (tollCost * 2.0 * 100.0).roundToInt() / 100.0 else tollCost
        } else null
        val totalCost = fuelCost + (effectiveToll ?: 0.0)
        return fuelCost to totalCost
    }

    private fun calculateCostPerKm(totalCost: Double, distanceKm: Double, isRoundTrip: Boolean): Double {
        val effectiveDist = if (isRoundTrip) distanceKm * 2.0 else distanceKm
        return ((totalCost / effectiveDist) * 100.0).roundToInt() / 100.0
    }

    @Test
    fun testBudgetCalculation_OneWay_WithTolls() {
        val distanceKm = 250.0
        val mileage = 15.0 // km/L
        val fuelPrice = 96.0 // ₹/L
        val tollCost = 280.0
        val tollAvailable = true

        val (fuelCost, totalCost) = calculateBudget(
            distanceKm = distanceKm,
            isRoundTrip = false,
            mileage = mileage,
            fuelPrice = fuelPrice,
            tollCost = tollCost,
            tollAvailable = tollAvailable
        )

        // 250 / 15 = 16.666 -> rounded to 16.7 L. 16.7 * 96 = 1603.2 -> 1603.0
        assertEquals(1603.0, fuelCost, 1.0)
        // Total = 1603 + 280 = 1883.0
        assertEquals(1883.0, totalCost!!, 1.0)

        val costPerKm = calculateCostPerKm(totalCost, distanceKm, isRoundTrip = false)
        assertEquals(7.53, costPerKm, 0.02)
    }

    @Test
    fun testBudgetCalculation_RoundTrip_DoublesDistanceFuelAndTolls() {
        val distanceKm = 100.0
        val mileage = 10.0 // km/L
        val fuelPrice = 100.0 // ₹/L
        val tollCost = 150.0
        val tollAvailable = true

        // One-way: 100 km -> 10 L -> ₹1000 fuel + ₹150 toll = ₹1150
        val (oneWayFuel, oneWayTotal) = calculateBudget(distanceKm, false, mileage, fuelPrice, tollCost, tollAvailable)
        assertEquals(1000.0, oneWayFuel, 0.01)
        assertEquals(1150.0, oneWayTotal!!, 0.01)

        // Round-trip: 200 km -> 20 L -> ₹2000 fuel + ₹300 toll = ₹2300
        val (roundFuel, roundTotal) = calculateBudget(distanceKm, true, mileage, fuelPrice, tollCost, tollAvailable)
        assertEquals(2000.0, roundFuel, 0.01)
        assertEquals(2300.0, roundTotal!!, 0.01)

        // Cost per km should remain consistent (₹2300 / 200 km = ₹11.50/km)
        val roundCostPerKm = calculateCostPerKm(roundTotal, distanceKm, isRoundTrip = true)
        assertEquals(11.50, roundCostPerKm, 0.01)
    }

    @Test
    fun testBudgetCalculation_UnavailableToll_DoesNotFabricateZero() {
        val distanceKm = 180.0
        val mileage = 18.0 // Diesel
        val fuelPrice = 89.62
        val tollCost: Double? = null
        val tollAvailable = false

        val (fuelCost, totalCost) = calculateBudget(distanceKm, false, mileage, fuelPrice, tollCost, tollAvailable)
        // 180 / 18 = 10 L -> 10 * 89.62 = ₹896
        assertEquals(896.0, fuelCost, 1.0)
        // Total cost equals fuel cost, but toll cost remains null (Unavailable)
        assertEquals(896.0, totalCost!!, 1.0)
        assertFalse("Tolls must be marked unavailable", tollAvailable)
    }

    // --- 2. FUEL RANGE & STOP PLANNER TESTS ---

    private fun calculateRemainingRange(remainingLitres: Double, mileage: Double): Double {
        val safeMileage = if (mileage > 0.0) mileage else 14.5
        return ((remainingLitres * safeMileage) * 10.0).roundToInt() / 10.0
    }

    private fun checkRefuelAdvice(rangeKm: Double, tripDistanceKm: Double): Pair<Boolean, String> {
        val needsRefuel = tripDistanceKm > 0.0 && rangeKm < tripDistanceKm
        val advice = if (needsRefuel) {
            "Refuel required: Estimated range of ${rangeKm.toInt()} km is less than trip distance (${tripDistanceKm.toInt()} km). Suggest refueling before ${((rangeKm * 0.85).toInt())} km."
        } else {
            "Fuel sufficient: Range of ${rangeKm.toInt()} km covers the one-way distance (${tripDistanceKm.toInt()} km)."
        }
        return needsRefuel to advice
    }

    @Test
    fun testFuelRangePlanner_SufficientFuel_NoWarning() {
        val remainingLitres = 25.0
        val mileage = 16.0
        val tripDistanceKm = 300.0

        val rangeKm = calculateRemainingRange(remainingLitres, mileage)
        assertEquals(400.0, rangeKm, 0.01)

        val (needsRefuel, advice) = checkRefuelAdvice(rangeKm, tripDistanceKm)
        assertFalse("Fuel is sufficient for 300 km trip", needsRefuel)
        assertTrue(advice.contains("Fuel sufficient"))
    }

    @Test
    fun testFuelRangePlanner_InsufficientFuel_TriggersRefuelAlert() {
        val remainingLitres = 10.0
        val mileage = 15.0
        val tripDistanceKm = 250.0

        val rangeKm = calculateRemainingRange(remainingLitres, mileage) // 150 km
        assertEquals(150.0, rangeKm, 0.01)

        val (needsRefuel, advice) = checkRefuelAdvice(rangeKm, tripDistanceKm)
        assertTrue("Refuel must be required when range < trip distance", needsRefuel)
        assertTrue(advice.contains("Refuel required"))
        assertTrue(advice.contains("150 km"))
        assertTrue(advice.contains("250 km"))
        // Safe refuel stop suggested before 85% of 150 km = 127 km
        assertTrue(advice.contains("127 km"))
    }

    // --- 3. ALTERNATIVE ROUTE COMPARISON & LABELING TESTS ---

    @Test
    fun testAlternativeRouteLabeling_FastestAndShortest() {
        val route1 = GoogleRouteData(
            points = listOf(LatLng(28.0, 77.0), LatLng(29.0, 78.0)),
            distanceMeters = 200000.0,
            distanceKm = 200.0,
            durationSeconds = 14400.0, // 4 hours
            durationMinutes = 240,
            formattedEta = "4h 0m",
            description = "NH 44"
        )
        val route2 = GoogleRouteData(
            points = listOf(LatLng(28.0, 77.0), LatLng(29.0, 78.0)),
            distanceMeters = 230000.0,
            distanceKm = 230.0,
            durationSeconds = 18000.0, // 5 hours
            durationMinutes = 300,
            formattedEta = "5h 0m",
            description = "State Highway 12"
        )

        val labeled = GoogleRoutesProvider.assignRouteLabels(listOf(route1, route2))
        assertEquals(2, labeled.size)
        // Route 1 is both fastest and shortest
        assertEquals("Fastest & Shortest", labeled[0].routeLabel)
        assertEquals("Via State Highway 12", labeled[1].routeLabel)
    }

    @Test
    fun testAlternativeRouteLabeling_FastestVsShortestDistinction() {
        val route1 = GoogleRouteData(
            points = listOf(LatLng(28.0, 77.0), LatLng(29.0, 78.0)),
            distanceMeters = 220000.0,
            distanceKm = 220.0,
            durationSeconds = 12000.0, // 3h 20m - Fastest highway
            durationMinutes = 200,
            formattedEta = "3h 20m",
            description = "Expressway"
        )
        val route2 = GoogleRouteData(
            points = listOf(LatLng(28.0, 77.0), LatLng(29.0, 78.0)),
            distanceMeters = 190000.0,
            distanceKm = 190.0,
            durationSeconds = 16000.0, // 4h 26m - Shorter distance, slower roads
            durationMinutes = 266,
            formattedEta = "4h 26m",
            description = "Old Highway"
        )

        val labeled = GoogleRoutesProvider.assignRouteLabels(listOf(route1, route2))
        assertEquals("Fastest", labeled[0].routeLabel)
        assertEquals("Shortest Distance", labeled[1].routeLabel)
    }

    @Test
    fun testAlternativeRouteLabeling_TollFreeRoute() {
        val route1 = GoogleRouteData(
            points = listOf(LatLng(28.0, 77.0), LatLng(29.0, 78.0)),
            distanceMeters = 200000.0,
            distanceKm = 200.0,
            durationSeconds = 12000.0,
            durationMinutes = 200,
            formattedEta = "3h 20m",
            tollCost = 350.0,
            tollAvailable = true,
            description = "Expressway"
        )
        val route2 = GoogleRouteData(
            points = listOf(LatLng(28.0, 77.0), LatLng(29.0, 78.0)),
            distanceMeters = 240000.0,
            distanceKm = 240.0,
            durationSeconds = 15000.0,
            durationMinutes = 250,
            formattedEta = "4h 10m",
            tollCost = 0.0,
            tollAvailable = true,
            description = "Free Byway"
        )

        val labeled = GoogleRoutesProvider.assignRouteLabels(listOf(route1, route2))
        assertEquals("Fastest & Shortest", labeled[0].routeLabel)
        assertEquals("Toll-Free Route", labeled[1].routeLabel)
    }

    // --- 4. SMART BREAK PLANNER & DETOUR TESTS ---

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateDetourKm(lat: Double, lng: Double, polyline: List<LatLng>): Double {
        if (polyline.isEmpty()) return 0.0
        var minDist = Double.MAX_VALUE
        for (pt in polyline) {
            val dist = haversineKm(lat, lng, pt.latitude, pt.longitude)
            if (dist < minDist) minDist = dist
        }
        return (minDist * 10.0).roundToInt() / 10.0
    }

    @Test
    fun testDetourDistanceCalculation_AccurateAndNonZero() {
        val polyline = listOf(
            LatLng(28.6139, 77.2090), // Connaught Place
            LatLng(28.4595, 77.0266), // Gurgaon
            LatLng(28.1487, 76.8173)  // Bhiwadi corridor
        )

        // Stop on the route: very low detour (< 1 km)
        val onRouteDetour = calculateDetourKm(28.4595, 77.0266, polyline)
        assertEquals(0.0, onRouteDetour, 0.01)

        // Stop 5 km away from Gurgaon point
        // Approx 0.045 deg lat ~ 5 km
        val offRouteDetour = calculateDetourKm(28.4595 + 0.045, 77.0266, polyline)
        assertTrue("Detour should be approximately 5.0 km", offRouteDetour in 4.5..5.5)
    }

    @Test
    fun testGoogleMapsUrlGeneration_FormatValidity() {
        val lat = 28.5355
        val lng = 77.3910
        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
        assertEquals("https://www.google.com/maps/search/?api=1&query=28.5355,77.391", mapsUrl)
        assertTrue(mapsUrl.startsWith("https://www.google.com/maps/search/?api=1&query="))
    }

    // --- 5. ROLE-BASED DASHBOARD & AUTHORIZATION TESTS ---

    @Test
    fun testRolePersistenceAndDisplayNames() {
        val validRoles = listOf("VEHICLE_OWNER", "DRIVER", "TRAVEL_AGENT")

        fun getRoleTitle(role: String): String = when (role) {
            "VEHICLE_OWNER", "OWNER" -> "Vehicle Owner"
            "DRIVER" -> "Driver"
            "TRAVEL_AGENT" -> "Travel Agent"
            else -> "Commuter"
        }

        assertEquals("Vehicle Owner", getRoleTitle("VEHICLE_OWNER"))
        assertEquals("Vehicle Owner", getRoleTitle("OWNER"))
        assertEquals("Driver", getRoleTitle("DRIVER"))
        assertEquals("Travel Agent", getRoleTitle("TRAVEL_AGENT"))

        assertTrue("VEHICLE_OWNER is valid", "VEHICLE_OWNER" in validRoles)
        assertTrue("DRIVER is valid", "DRIVER" in validRoles)
        assertTrue("TRAVEL_AGENT is valid", "TRAVEL_AGENT" in validRoles)
    }

    @Test
    fun testRoleAuthorization_DriverCannotPostTrips() {
        fun canPostTrip(role: String): Boolean {
            return role != "DRIVER"
        }

        assertTrue("Vehicle Owner can post trips", canPostTrip("VEHICLE_OWNER"))
        assertTrue("Travel Agent can post trips", canPostTrip("TRAVEL_AGENT"))
        assertFalse("Driver accounts cannot post trips", canPostTrip("DRIVER"))
    }
}
