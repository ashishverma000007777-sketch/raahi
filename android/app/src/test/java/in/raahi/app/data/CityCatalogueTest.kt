package `in`.raahi.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CityCatalogueTest {

    @Test
    fun testAllRequiredRegionsArePresent() {
        val requiredRegions = listOf(
            CityCatalogue.REGION_TRICITY,
            CityCatalogue.REGION_PUNJAB,
            CityCatalogue.REGION_HARYANA,
            CityCatalogue.REGION_DELHI_NCR,
            CityCatalogue.REGION_HIMACHAL,
            CityCatalogue.REGION_RAJASTHAN,
            CityCatalogue.REGION_UTTAR_PRADESH,
            CityCatalogue.REGION_UTTARAKHAND,
            CityCatalogue.REGION_OTHER,
        )

        assertEquals(requiredRegions, CityCatalogue.REGIONS)

        // Ensure every region has at least one city in the catalogue
        for (region in requiredRegions) {
            val cities = CityCatalogue.CITIES.filter { it.region == region }
            assertTrue("Region '$region' should have curated cities", cities.isNotEmpty())
        }
    }

    @Test
    fun testCanonicalResolutionForAliases() {
        // Gurgaon / Gurugram
        assertEquals("Gurgaon", CityCatalogue.canonicalName("Gurgaon"))
        assertEquals("Gurgaon", CityCatalogue.canonicalName("Gurugram"))
        assertEquals("Gurgaon", CityCatalogue.canonicalName("  gurugram  "))

        // Mohali / SAS Nagar
        assertEquals("Mohali", CityCatalogue.canonicalName("Mohali"))
        assertEquals("Mohali", CityCatalogue.canonicalName("SAS Nagar"))

        // Varanasi / Banaras / Kashi
        assertEquals("Varanasi", CityCatalogue.canonicalName("Banaras"))
        assertEquals("Varanasi", CityCatalogue.canonicalName("Kashi"))

        // Prayagraj / Allahabad
        assertEquals("Prayagraj", CityCatalogue.canonicalName("Allahabad"))

        // Delhi / New Delhi
        assertEquals("Delhi", CityCatalogue.canonicalName("New Delhi"))

        // Mumbai / Bombay
        assertEquals("Mumbai", CityCatalogue.canonicalName("Bombay"))

        // Bengaluru / Bangalore
        assertEquals("Bengaluru", CityCatalogue.canonicalName("Bangalore"))

        // Shimla / Simla
        assertEquals("Shimla", CityCatalogue.canonicalName("Simla"))
    }

    @Test
    fun testCaseInsensitiveAndWhitespaceTrimmedSearch() {
        val r1 = CityCatalogue.filter("   chandigarh   ")
        assertTrue(r1.any { it.name == "Chandigarh" })

        val r2 = CityCatalogue.filter("cHaNdIgArH")
        assertTrue(r2.any { it.name == "Chandigarh" })

        val r3 = CityCatalogue.filter("DELHI")
        assertTrue(r3.any { it.name == "Delhi" })
    }

    @Test
    fun testFilteringByRegionName() {
        val uttarakhandResults = CityCatalogue.filter("Uttarakhand")
        assertTrue(uttarakhandResults.any { it.name == "Dehradun" })
        assertTrue(uttarakhandResults.any { it.name == "Rishikesh" })
        assertTrue(uttarakhandResults.any { it.name == "Haridwar" })

        val hpResults = CityCatalogue.filter("Himachal")
        assertTrue(hpResults.any { it.name == "Shimla" })
        assertTrue(hpResults.any { it.name == "Manali" })

        val tricityResults = CityCatalogue.filter("Tricity")
        assertTrue(tricityResults.any { it.name == "Chandigarh" })
        assertTrue(tricityResults.any { it.name == "Mohali" })
        assertTrue(tricityResults.any { it.name == "Panchkula" })
        assertTrue(tricityResults.any { it.name == "Zirakpur" })
    }

    @Test
    fun testFilteringByAliasMatchesEntry() {
        val results = CityCatalogue.filter("Gurugram")
        assertTrue(results.any { it.name == "Gurgaon" })

        val banarasResults = CityCatalogue.filter("Banaras")
        assertTrue(banarasResults.any { it.name == "Varanasi" })
    }

    @Test
    fun testUnsupportedCityReturnsEmptyAndNull() {
        assertNull(CityCatalogue.findCity("Honolulu"))
        assertNull(CityCatalogue.canonicalName("RandomInvalidCity123"))
        assertFalse(CityCatalogue.isSupported("Atlantis"))

        val filterResults = CityCatalogue.filter("NonExistentCityXYZ")
        assertTrue(filterResults.isEmpty())
    }

    @Test
    fun testGroupedMaintainsOrderAndOmitsEmpty() {
        val groupedAll = CityCatalogue.grouped()
        assertEquals(CityCatalogue.REGIONS, groupedAll.keys.toList())

        val groupedFiltered = CityCatalogue.grouped("Jaipur")
        assertEquals(listOf(CityCatalogue.REGION_RAJASTHAN), groupedFiltered.keys.toList())
        assertEquals(listOf("Jaipur"), groupedFiltered[CityCatalogue.REGION_RAJASTHAN]?.map { it.name })
    }

    @Test
    fun testPopularCitiesAreCurated() {
        val popular = CityCatalogue.popularCities()
        assertTrue(popular.isNotEmpty())
        assertTrue(popular.any { it.name == "Chandigarh" })
        assertTrue(popular.any { it.name == "Delhi" })
        assertTrue(popular.any { it.name == "Jaipur" })
        assertTrue(popular.any { it.name == "Agra" })
        assertTrue(popular.all { it.isPopular })
    }

    @Test
    fun testNoDuplicateCanonicalNames() {
        val names = CityCatalogue.CITIES.map { it.name }
        val duplicates = names.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("Found duplicate canonical city names: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun testAboharCoordinatesAndNonDelhiDefault() {
        // Abohar must resolve to exact coordinates (30.1453, 74.1993)
        val aboharCoords = CityCatalogue.getCoordinates("Abohar")
        assertNotNull("Abohar coordinates must not be null", aboharCoords)
        assertEquals(30.1453, aboharCoords!!.first, 0.001)
        assertEquals(74.1993, aboharCoords.second, 0.001)

        // Abohar MUST NOT default to Delhi coordinates (28.6139, 77.2090)
        assertFalse(
            "Abohar must not resolve to Delhi coordinates",
            aboharCoords.first == 28.6139 && aboharCoords.second == 77.2090
        )

        // Test alias and prefix resolution for Abohar
        val aliasCoords = CityCatalogue.getCoordinates("Abohar Punjab")
        assertNotNull(aliasCoords)
        assertEquals(30.1453, aliasCoords!!.first, 0.001)

        val abhaNagari = CityCatalogue.findCity("Abha Nagari")
        assertNotNull(abhaNagari)
        assertEquals("Abohar", abhaNagari!!.name)
    }

    @Test
    fun testAboharToJaipurAndChandigarhRoutingCoordinates() {
        val abohar = CityCatalogue.getCoordinates("Abohar")!!
        val jaipur = CityCatalogue.getCoordinates("Jaipur")!!
        val chandigarh = CityCatalogue.getCoordinates("Chandigarh")!!
        val delhi = CityCatalogue.getCoordinates("Delhi")!!

        // Verify independent, correct coordinates
        assertEquals(26.9124, jaipur.first, 0.001)
        assertEquals(75.7873, jaipur.second, 0.001)

        assertEquals(30.7333, chandigarh.first, 0.001)
        assertEquals(76.7794, chandigarh.second, 0.001)

        // Confirm distinct coordinates for all destinations
        assertFalse("Abohar and Jaipur coordinates must differ", abohar == jaipur)
        assertFalse("Abohar and Chandigarh coordinates must differ", abohar == chandigarh)
        assertFalse("Abohar and Delhi coordinates must differ", abohar == delhi)

        // Abohar -> Jaipur latitude delta is southward (~3.23 degrees)
        val latDeltaJaipur = abohar.first - jaipur.first
        assertTrue("Jaipur should be south of Abohar", latDeltaJaipur > 3.0)

        // Abohar -> Chandigarh latitude is almost same level (~30 deg N), but eastward (~2.58 deg)
        val lngDeltaChd = chandigarh.second - abohar.second
        assertTrue("Chandigarh should be east of Abohar", lngDeltaChd > 2.0)
    }

    @Test
    fun testPunjabCitiesCoverage() {
        val punjabCities = listOf(
            "Abohar", "Fazilka", "Firozpur", "Sri Muktsar Sahib", "Malout",
            "Bathinda", "Moga", "Faridkot", "Kotkapura", "Ludhiana",
            "Amritsar", "Jalandhar", "Patiala", "Hoshiarpur", "Pathankot"
        )
        for (cityName in punjabCities) {
            val city = CityCatalogue.findCity(cityName)
            assertNotNull("Punjab city '$cityName' must exist in catalogue", city)
            assertEquals("Punjab", city!!.state)
            val coords = CityCatalogue.getCoordinates(cityName)
            assertNotNull("Coordinates for '$cityName' must not be null", coords)
            assertTrue("Latitude for '$cityName' must be valid", coords!!.first in 29.0..33.0)
            assertTrue("Longitude for '$cityName' must be valid", coords.second in 73.0..77.0)
        }
    }
}
