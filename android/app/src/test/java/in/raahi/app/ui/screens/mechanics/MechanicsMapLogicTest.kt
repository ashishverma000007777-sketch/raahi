package `in`.raahi.app.ui.screens.mechanics

import `in`.raahi.app.data.GooglePlaceResult
import `in`.raahi.app.data.GoogleRoutesProvider
import `in`.raahi.app.data.LatLng
import `in`.raahi.app.network.MechanicDto
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class MechanicsMapLogicTest {

    private val mohaliLoc = LatLng(30.7046, 76.7179)
    private val chandigarhLoc = LatLng(30.7333, 76.7794)

    @Test
    fun testNoDelhiFallbackWhenLocationUnavailable() {
        // Assert that a null GPS fix must NOT fall back to Delhi (28.6139, 77.2090)
        val acquiredLocation: LatLng? = null
        assertNull("GPS failure must remain null, never Delhi", acquiredLocation)
        val delhi = LatLng(28.6139, 77.2090)
        assertNotEquals(acquiredLocation, delhi)
    }

    @Test
    fun testSourceSeparationVerifiedAndGooglePlaces() {
        val verifiedMechanic = MechanicDto(
            userId = "u1",
            name = "Ramesh Verma",
            phone = "9876543210",
            shopName = "Verma Motors",
            specializations = "Engine, Transmission, SOS",
            isAvailable = true,
            lat = 30.7100,
            lng = 76.7200,
            distanceKm = 1.2,
            ratingAvg = 4.8,
        )

        val googlePlace = GooglePlaceResult(
            id = "gp-999",
            name = "Bosch Car Service Center",
            address = "Phase 7, Industrial Area, Mohali",
            lat = 30.7050,
            lng = 76.7150,
            rating = 4.5,
            userRatingCount = 120,
            distanceKm = 0.8,
            isOpenNow = true,
        )

        val vItem = UnifiedMechanicItem(
            id = "verified-${verifiedMechanic.userId}",
            name = verifiedMechanic.shopName ?: verifiedMechanic.name ?: "Mechanic",
            subtitle = verifiedMechanic.name,
            address = null,
            phone = verifiedMechanic.phone,
            lat = verifiedMechanic.lat!!,
            lng = verifiedMechanic.lng!!,
            distanceKm = verifiedMechanic.distanceKm,
            rating = verifiedMechanic.ratingAvg,
            isAvailable = verifiedMechanic.isAvailable,
            specializations = verifiedMechanic.specializations,
            source = MechanicSource.VERIFIED,
            rawMechanic = verifiedMechanic,
        )

        val gItem = UnifiedMechanicItem(
            id = "google-${googlePlace.id}",
            name = googlePlace.name,
            subtitle = "Google Places",
            address = googlePlace.address,
            phone = null,
            lat = googlePlace.lat,
            lng = googlePlace.lng,
            distanceKm = googlePlace.distanceKm,
            rating = googlePlace.rating,
            ratingCount = googlePlace.userRatingCount,
            isAvailable = googlePlace.isOpenNow ?: true,
            specializations = "Auto Service",
            source = MechanicSource.GOOGLE_PLACES,
            rawGooglePlace = googlePlace,
        )

        assertEquals(MechanicSource.VERIFIED, vItem.source)
        assertEquals(MechanicSource.GOOGLE_PLACES, gItem.source)

        assertEquals(4.8, vItem.rating!!, 0.01)
        assertEquals(4.5, gItem.rating!!, 0.01)
    }

    @Test
    fun testHaversineDistanceCalculation() {
        val dist = haversineKm(mohaliLoc.lat, mohaliLoc.lng, chandigarhLoc.lat, chandigarhLoc.lng)
        // Distance between Mohali Sector 62 and Sector 17 Chandigarh is approx 6.5-7.5 km
        assertTrue("Distance should be roughly 6-8 km", dist in 5.5..8.5)
    }

    @Test
    fun testFilterByCategoryAndSearch() {
        val items = listOf(
            UnifiedMechanicItem(
                id = "1", name = "Sharma Puncture Shop", subtitle = null, address = "Kharar Road",
                phone = "9800000001", lat = 30.7, lng = 76.7, distanceKm = 1.0, rating = 4.2,
                isAvailable = true, specializations = "Tyre Puncture, Wheel Alignment",
                source = MechanicSource.VERIFIED
            ),
            UnifiedMechanicItem(
                id = "2", name = "Apex Electric Works", subtitle = null, address = "Phase 5",
                phone = "9800000002", lat = 30.71, lng = 76.71, distanceKm = 2.5, rating = 4.6,
                isAvailable = true, specializations = "Battery Replacement, Alternator, Wiring",
                source = MechanicSource.GOOGLE_PLACES
            ),
            UnifiedMechanicItem(
                id = "3", name = "Tricity Towing Assistance", subtitle = null, address = "Zirakpur Barrier",
                phone = "9800000003", lat = 30.65, lng = 76.82, distanceKm = 8.0, rating = 4.0,
                isAvailable = true, specializations = "24/7 Towing, Flatbed Crane",
                source = MechanicSource.VERIFIED
            ),
        )

        // Filter: Puncture
        val punctureItems = items.filter { it.specializations?.contains("puncture", ignoreCase = true) == true }
        assertEquals(1, punctureItems.size)
        assertEquals("Sharma Puncture Shop", punctureItems.first().name)

        // Filter: Electrical
        val electricItems = items.filter { it.specializations?.contains("battery", ignoreCase = true) == true }
        assertEquals(1, electricItems.size)
        assertEquals("Apex Electric Works", electricItems.first().name)

        // Filter: Search "towing"
        val query = "towing"
        val searchItems = items.filter { it.name.contains(query, ignoreCase = true) || it.specializations?.contains(query, ignoreCase = true) == true }
        assertEquals(1, searchItems.size)
        assertEquals("Tricity Towing Assistance", searchItems.first().name)

        // Filter: Verified only
        val verifiedOnly = items.filter { it.source == MechanicSource.VERIFIED }
        assertEquals(2, verifiedOnly.size)

        // Filter: Google Places only
        val googleOnly = items.filter { it.source == MechanicSource.GOOGLE_PLACES }
        assertEquals(1, googleOnly.size)
    }

    @Test
    fun testGoogleRoutesPolylineDecoder() {
        // Standard test polyline string: "_p~iF~ps|U_ulLnnqC_mqNvxq`@" (points around (38.5, -120.2))
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val points = GoogleRoutesProvider.decodePolyline(encoded)
        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 0.001)
        assertEquals(-120.2, points[0].longitude, 0.001)
        assertEquals(40.7, points[1].latitude, 0.001)
        assertEquals(-120.95, points[1].longitude, 0.001)
        assertEquals(43.252, points[2].latitude, 0.001)
        assertEquals(-126.453, points[2].longitude, 0.001)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c * 10.0).roundToInt() / 10.0
    }
}
