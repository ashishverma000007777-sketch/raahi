package `in`.raahi.app.ui.screens.intercity

import `in`.raahi.app.data.CityCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferredCitySelectorLogicTest {

    @Test
    fun testInitialCitySelectionResolution() {
        // Null or blank defaults to All Cities
        val nullInitial: String? = null
        val canonicalNull = if (!nullInitial.isNullOrBlank()) CityCatalogue.canonicalName(nullInitial) ?: nullInitial else "All Cities"
        assertEquals("All Cities", canonicalNull)

        val blankInitial = "   "
        val canonicalBlank = if (blankInitial.isNotBlank()) CityCatalogue.canonicalName(blankInitial) ?: blankInitial else "All Cities"
        assertEquals("All Cities", canonicalBlank)

        // Valid canonical city resolves as-is
        val delhiInitial = "Delhi"
        val canonicalDelhi = if (delhiInitial.isNotBlank()) CityCatalogue.canonicalName(delhiInitial) ?: delhiInitial else "All Cities"
        assertEquals("Delhi", canonicalDelhi)

        // Alias resolves to canonical name
        val gurugramInitial = "Gurugram"
        val canonicalGurgaon = if (gurugramInitial.isNotBlank()) CityCatalogue.canonicalName(gurugramInitial) ?: gurugramInitial else "All Cities"
        assertEquals("Gurgaon", canonicalGurgaon)
    }

    @Test
    fun testSaveCanonicalCityConversion() {
        fun resolveForSave(selected: String): String? {
            return if (selected.equals("All Cities", ignoreCase = true) || selected.equals("All", ignoreCase = true)) {
                null
            } else {
                CityCatalogue.canonicalName(selected) ?: selected.trim()
            }
        }

        // Selecting "All Cities" must persist null to backend
        assertNull(resolveForSave("All Cities"))
        assertNull(resolveForSave("all cities"))
        assertNull(resolveForSave("All"))

        // Selecting a specific city persists canonical name
        assertEquals("Delhi", resolveForSave("Delhi"))
        assertEquals("Gurgaon", resolveForSave("Gurugram"))
        assertEquals("Chandigarh", resolveForSave("CHD"))
        assertEquals("Jaipur", resolveForSave("Pink City"))
    }

    @Test
    fun testUnsupportedCityValidation() {
        val unsupported = "UnsupportedMetropolis"
        val isSupported = CityCatalogue.isSupported(unsupported)
        assertFalse(isSupported)

        val canonical = CityCatalogue.canonicalName(unsupported)
        assertNull(canonical)

        // Verifying error message formatting
        val errorMessage = if (canonical == null) {
            "Selected city '$unsupported' is not currently supported in the intercity network."
        } else null

        assertEquals("Selected city 'UnsupportedMetropolis' is not currently supported in the intercity network.", errorMessage)
    }

    @Test
    fun testMatchesAllCitiesOptionLogic() {
        fun matchesAllOption(query: String): Boolean {
            val q = query.trim().lowercase()
            return q.isEmpty() || "all cities".contains(q) || "all".contains(q) || "any".contains(q)
        }

        assertTrue(matchesAllOption(""))
        assertTrue(matchesAllOption("all"))
        assertTrue(matchesAllOption("cities"))
        assertTrue(matchesAllOption("any"))
        assertFalse(matchesAllOption("delhi"))
        assertFalse(matchesAllOption("jaipur"))
    }
}
