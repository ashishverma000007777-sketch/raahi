package `in`.raahi.app.data

/**
 * Curated centralized city catalogue for Raahi Intercity Marketplace.
 *
 * Provides canonical city representations, verified region groupings,
 * alternate spelling aliases (e.g. Gurugram -> Gurgaon, Allahabad -> Prayagraj),
 * and query filtering for pickup hubs and driver push-alert preferences.
 */
data class CityEntry(
    val name: String,
    val region: String,
    val state: String = region,
    val aliases: List<String> = emptyList(),
    val isPopular: Boolean = false,
)

object CityCatalogue {

    const val REGION_TRICITY = "Chandigarh Tricity"
    const val REGION_PUNJAB = "Punjab"
    const val REGION_HARYANA = "Haryana"
    const val REGION_DELHI_NCR = "Delhi NCR"
    const val REGION_HIMACHAL = "Himachal Pradesh"
    const val REGION_RAJASTHAN = "Rajasthan"
    const val REGION_UTTAR_PRADESH = "Uttar Pradesh"
    const val REGION_UTTARAKHAND = "Uttarakhand"
    const val REGION_OTHER = "Other major cities"

    val REGIONS: List<String> = listOf(
        REGION_TRICITY,
        REGION_PUNJAB,
        REGION_HARYANA,
        REGION_DELHI_NCR,
        REGION_HIMACHAL,
        REGION_RAJASTHAN,
        REGION_UTTAR_PRADESH,
        REGION_UTTARAKHAND,
        REGION_OTHER,
    )

    val CITIES: List<CityEntry> = listOf(
        // Chandigarh Tricity
        CityEntry("Chandigarh", REGION_TRICITY, state = "Chandigarh", aliases = listOf("CHD"), isPopular = true),
        CityEntry("Mohali", REGION_TRICITY, state = "Punjab", aliases = listOf("SAS Nagar", "Sahibzada Ajit Singh Nagar"), isPopular = true),
        CityEntry("Panchkula", REGION_TRICITY, state = "Haryana", aliases = listOf("Panchkula Tricity"), isPopular = false),
        CityEntry("Zirakpur", REGION_TRICITY, state = "Punjab", aliases = listOf("Zirakpur Punjab"), isPopular = false),

        // Punjab
        CityEntry("Ludhiana", REGION_PUNJAB, state = "Punjab", aliases = listOf("Ludhiana Punjab"), isPopular = true),
        CityEntry("Amritsar", REGION_PUNJAB, state = "Punjab", aliases = listOf("Amritsar Sahib", "Golden Temple City"), isPopular = true),
        CityEntry("Jalandhar", REGION_PUNJAB, state = "Punjab", aliases = listOf("Jullundur"), isPopular = false),
        CityEntry("Patiala", REGION_PUNJAB, state = "Punjab", aliases = emptyList(), isPopular = false),
        CityEntry("Bathinda", REGION_PUNJAB, state = "Punjab", aliases = listOf("Bhatinda"), isPopular = false),
        CityEntry("Pathankot", REGION_PUNJAB, state = "Punjab", aliases = emptyList(), isPopular = false),

        // Haryana
        CityEntry("Gurgaon", REGION_HARYANA, state = "Haryana", aliases = listOf("Gurugram"), isPopular = true),
        CityEntry("Faridabad", REGION_HARYANA, state = "Haryana", aliases = emptyList(), isPopular = false),
        CityEntry("Panipat", REGION_HARYANA, state = "Haryana", aliases = emptyList(), isPopular = false),
        CityEntry("Ambala", REGION_HARYANA, state = "Haryana", aliases = listOf("Ambala Cantt", "Ambala City"), isPopular = false),
        CityEntry("Karnal", REGION_HARYANA, state = "Haryana", aliases = emptyList(), isPopular = false),
        CityEntry("Sonipat", REGION_HARYANA, state = "Haryana", aliases = listOf("Sonepat"), isPopular = false),
        CityEntry("Rohtak", REGION_HARYANA, state = "Haryana", aliases = emptyList(), isPopular = false),
        CityEntry("Hisar", REGION_HARYANA, state = "Haryana", aliases = listOf("Hissar"), isPopular = false),

        // Delhi NCR
        CityEntry("Delhi", REGION_DELHI_NCR, state = "Delhi", aliases = listOf("New Delhi", "Delhi NCR", "Dilli"), isPopular = true),
        CityEntry("Noida", REGION_DELHI_NCR, state = "Uttar Pradesh", aliases = listOf("Gautam Buddha Nagar", "Noida UP"), isPopular = true),
        CityEntry("Greater Noida", REGION_DELHI_NCR, state = "Uttar Pradesh", aliases = listOf("Gr Noida"), isPopular = false),
        CityEntry("Ghaziabad", REGION_DELHI_NCR, state = "Uttar Pradesh", aliases = listOf("Ghaziabad UP"), isPopular = false),

        // Himachal Pradesh
        CityEntry("Shimla", REGION_HIMACHAL, state = "Himachal Pradesh", aliases = listOf("Simla"), isPopular = true),
        CityEntry("Manali", REGION_HIMACHAL, state = "Himachal Pradesh", aliases = listOf("Kullu Manali"), isPopular = true),
        CityEntry("Dharamshala", REGION_HIMACHAL, state = "Himachal Pradesh", aliases = listOf("Dharamsala", "McLeod Ganj", "Mcleodganj"), isPopular = false),
        CityEntry("Solan", REGION_HIMACHAL, state = "Himachal Pradesh", aliases = emptyList(), isPopular = false),
        CityEntry("Mandi", REGION_HIMACHAL, state = "Himachal Pradesh", aliases = emptyList(), isPopular = false),
        CityEntry("Kasauli", REGION_HIMACHAL, state = "Himachal Pradesh", aliases = emptyList(), isPopular = false),

        // Rajasthan
        CityEntry("Jaipur", REGION_RAJASTHAN, state = "Rajasthan", aliases = listOf("Pink City"), isPopular = true),
        CityEntry("Udaipur", REGION_RAJASTHAN, state = "Rajasthan", aliases = listOf("Lake City"), isPopular = true),
        CityEntry("Jodhpur", REGION_RAJASTHAN, state = "Rajasthan", aliases = listOf("Sun City", "Blue City"), isPopular = false),
        CityEntry("Ajmer", REGION_RAJASTHAN, state = "Rajasthan", aliases = listOf("Ajmer Sharif", "Pushkar"), isPopular = false),
        CityEntry("Kota", REGION_RAJASTHAN, state = "Rajasthan", aliases = emptyList(), isPopular = false),
        CityEntry("Bikaner", REGION_RAJASTHAN, state = "Rajasthan", aliases = emptyList(), isPopular = false),

        // Uttar Pradesh
        CityEntry("Agra", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", aliases = listOf("Taj City"), isPopular = true),
        CityEntry("Lucknow", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", aliases = listOf("Nawab City", "LKO"), isPopular = true),
        CityEntry("Kanpur", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", aliases = listOf("Cawnpore"), isPopular = false),
        CityEntry("Varanasi", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", aliases = listOf("Banaras", "Benares", "Kashi"), isPopular = false),
        CityEntry("Prayagraj", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", aliases = listOf("Allahabad"), isPopular = false),
        CityEntry("Mathura", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", aliases = listOf("Vrindavan", "Mathura Vrindavan"), isPopular = false),
        CityEntry("Meerut", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", aliases = emptyList(), isPopular = false),
        CityEntry("Ayodhya", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", aliases = listOf("Faizabad"), isPopular = false),

        // Uttarakhand
        CityEntry("Dehradun", REGION_UTTARAKHAND, state = "Uttarakhand", aliases = listOf("Doon"), isPopular = true),
        CityEntry("Rishikesh", REGION_UTTARAKHAND, state = "Uttarakhand", aliases = listOf("Hrishikesh"), isPopular = true),
        CityEntry("Haridwar", REGION_UTTARAKHAND, state = "Uttarakhand", aliases = listOf("Hardwar"), isPopular = false),
        CityEntry("Nainital", REGION_UTTARAKHAND, state = "Uttarakhand", aliases = emptyList(), isPopular = false),
        CityEntry("Mussoorie", REGION_UTTARAKHAND, state = "Uttarakhand", aliases = listOf("Mussourie"), isPopular = false),
        CityEntry("Haldwani", REGION_UTTARAKHAND, state = "Uttarakhand", aliases = listOf("Kathgodam"), isPopular = false),

        // Other major cities
        CityEntry("Mumbai", REGION_OTHER, state = "Maharashtra", aliases = listOf("Bombay", "Maharashtra"), isPopular = true),
        CityEntry("Pune", REGION_OTHER, state = "Maharashtra", aliases = listOf("Poona", "Maharashtra"), isPopular = true),
        CityEntry("Bengaluru", REGION_OTHER, state = "Karnataka", aliases = listOf("Bangalore", "Karnataka"), isPopular = true),
        CityEntry("Ahmedabad", REGION_OTHER, state = "Gujarat", aliases = listOf("Amdavad", "Gujarat"), isPopular = false),
        CityEntry("Hyderabad", REGION_OTHER, state = "Telangana", aliases = listOf("Secunderabad", "Telangana"), isPopular = false),
    )

    /** Returns quick-pick popular cities */
    fun popularCities(): List<CityEntry> = CITIES.filter { it.isPopular }

    /** Finds a city by canonical name or alias (case-insensitive, trimmed) */
    fun findCity(nameOrAlias: String?): CityEntry? {
        if (nameOrAlias.isNullOrBlank()) return null
        val query = nameOrAlias.trim().lowercase()
        return CITIES.firstOrNull { city ->
            city.name.lowercase() == query ||
            city.aliases.any { it.lowercase() == query }
        }
    }

    /** Resolves canonical name for a given input or alias */
    fun canonicalName(nameOrAlias: String?): String? {
        return findCity(nameOrAlias)?.name
    }

    /** Returns true if the city is supported by the curated catalogue */
    fun isSupported(nameOrAlias: String?): Boolean {
        return findCity(nameOrAlias) != null
    }

    /** Filter cities matching query across canonical name, region, state, or aliases */
    fun filter(query: String?): List<CityEntry> {
        val clean = query?.trim()?.lowercase().orEmpty()
        if (clean.isBlank()) return CITIES
        return CITIES.filter { city ->
            city.name.lowercase().contains(clean) ||
            city.region.lowercase().contains(clean) ||
            city.state.lowercase().contains(clean) ||
            city.aliases.any { it.lowercase().contains(clean) }
        }
    }

    /**
     * Groups filtered results by canonical region in the defined order.
     * Empty regions are omitted from the map.
     */
    fun grouped(query: String? = null): Map<String, List<CityEntry>> {
        val filtered = filter(query)
        val map = linkedMapOf<String, List<CityEntry>>()
        for (region in REGIONS) {
            val inRegion = filtered.filter { it.region == region }
            if (inRegion.isNotEmpty()) {
                map[region] = inRegion
            }
        }
        return map
    }
}
