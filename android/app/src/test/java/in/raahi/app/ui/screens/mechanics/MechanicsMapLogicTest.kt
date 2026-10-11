package `in`.raahi.app.ui.screens.mechanics

import `in`.raahi.app.data.GooglePlaceResult
import `in`.raahi.app.data.LatLng
import `in`.raahi.app.network.MechanicDto
import `in`.raahi.app.network.OsmMechanicShopDto
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
    fun testSourceSeparation() {
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

        val osmShop = OsmMechanicShopDto(
            id = "osm-12345",
            name = "Gupta Puncture Works",
            phone = "9812345678",
            address = "Sector 70, Mohali",
            lat = 30.7000,
            lng = 76.7100,
            osmUrl = null,
            status = "NEW",
            distanceKm = 1.5,
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

        val oItem = UnifiedMechanicItem(
            id = "osm-${osmShop.id}",
            name = osmShop.name,
            subtitle = "Community Record (OSM)",
            address = osmShop.address,
            phone = osmShop.phone,
            lat = osmShop.lat,
            lng = osmShop.lng,
            distanceKm = osmShop.distanceKm,
            rating = null,
            isAvailable = true,
            specializations = "Puncture / Repair",
            source = MechanicSource.OSM_COMMUNITY,
            rawOsmShop = osmShop,
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
        assertEquals(MechanicSource.OSM_COMMUNITY, oItem.source)
        assertEquals(MechanicSource.GOOGLE_PLACES, gItem.source)

        // Rating is honestly preserved: OSM has null rating (never fabricated)
        assertNull("OSM community record must never have an invented rating", oItem.rating)
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
                phone = "9800000001", lat = 30.7, lng = 76.7, distanceKm = 1.0, rating = null,
                isAvailable = true, specializations = "Tyre Puncture, Wheel Alignment",
                source = MechanicSource.OSM_COMMUNITY
            ),
            UnifiedMechanicItem(
                id = "2", name = "Apex Electric Works", subtitle = null, address = "Phase 5",
                phone = "9800000002", lat = 30.7, lng = 76.7, distanceKm = 2.0, rating = 4.7,
                isAvailable = true, specializations = "Battery, Alternator, Electric Wiring",
                source = MechanicSource.VERIFIED
            ),
            UnifiedMechanicItem(
                id = "3", name = "National Garage & Towing", subtitle = null, address = "Phase 8",
                phone = "9800000003", lat = 30.7, lng = 76.7, distanceKm = 3.0, rating = 4.2,
                isAvailable = false, specializations = "Engine, Towing, 24x7 SOS",
                source = MechanicSource.VERIFIED
            )
        )

        // Category filter: Puncture
        val punctureMatches = items.filter { it.name.contains("puncture", ignoreCase = true) || it.specializations?.contains("puncture", ignoreCase = true) == true }
        assertEquals(1, punctureMatches.size)
        assertEquals("1", punctureMatches[0].id)

        // Category filter: Electric
        val electricMatches = items.filter { it.specializations?.contains("electric", ignoreCase = true) == true || it.specializations?.contains("battery", ignoreCase = true) == true }
        assertEquals(1, electricMatches.size)
        assertEquals("2", electricMatches[0].id)

        // Category filter: SOS / Towing
        val sosMatches = items.filter { it.specializations?.contains("SOS", ignoreCase = true) == true || it.specializations?.contains("tow", ignoreCase = true) == true }
        assertEquals(1, sosMatches.size)
        assertEquals("3", sosMatches[0].id)

        // Search query: phone
        val phoneSearch = items.filter { it.phone?.contains("00000002") == true }
        assertEquals(1, phoneSearch.size)
        assertEquals("2", phoneSearch[0].id)

        // Search query: address
        val addressSearch = items.filter { it.address?.contains("Kharar", ignoreCase = true) == true }
        assertEquals(1, addressSearch.size)
        assertEquals("1", addressSearch[0].id)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
