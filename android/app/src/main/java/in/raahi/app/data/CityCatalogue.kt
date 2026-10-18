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
    val lat: Double = 0.0,
    val lng: Double = 0.0,
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
        CityEntry("Chandigarh", REGION_TRICITY, state = "Chandigarh", lat = 30.7333, lng = 76.7794, aliases = listOf("CHD"), isPopular = true),
        CityEntry("Mohali", REGION_TRICITY, state = "Punjab", lat = 30.7046, lng = 76.7179, aliases = listOf("SAS Nagar", "Sahibzada Ajit Singh Nagar"), isPopular = true),
        CityEntry("Panchkula", REGION_TRICITY, state = "Haryana", lat = 30.6942, lng = 76.8606, aliases = listOf("Panchkula Tricity"), isPopular = false),
        CityEntry("Zirakpur", REGION_TRICITY, state = "Punjab", lat = 30.6425, lng = 76.8173, aliases = listOf("Zirakpur Punjab"), isPopular = false),
        CityEntry("Kharar", REGION_TRICITY, state = "Punjab", lat = 30.7499, lng = 76.6491, aliases = listOf("Kharar Punjab"), isPopular = false),
        CityEntry("New Chandigarh", REGION_TRICITY, state = "Punjab", lat = 30.7900, lng = 76.7300, aliases = listOf("Mullanpur"), isPopular = false),
        CityEntry("Pinjore", REGION_TRICITY, state = "Haryana", lat = 30.7956, lng = 76.9189, aliases = emptyList(), isPopular = false),
        CityEntry("Kalka", REGION_TRICITY, state = "Haryana", lat = 30.8344, lng = 76.9350, aliases = emptyList(), isPopular = false),

        // Punjab (Comprehensive & Prioritized)
        CityEntry("Abohar", REGION_PUNJAB, state = "Punjab", lat = 30.1453, lng = 74.1993, aliases = listOf("Abha Nagari", "Fazilka Abohar", "Abohar Punjab"), isPopular = true),
        CityEntry("Ludhiana", REGION_PUNJAB, state = "Punjab", lat = 30.9010, lng = 75.8573, aliases = listOf("Ludhiana Punjab"), isPopular = true),
        CityEntry("Amritsar", REGION_PUNJAB, state = "Punjab", lat = 31.6340, lng = 74.8723, aliases = listOf("Amritsar Sahib", "Golden Temple City"), isPopular = true),
        CityEntry("Jalandhar", REGION_PUNJAB, state = "Punjab", lat = 31.3260, lng = 75.5762, aliases = listOf("Jullundur"), isPopular = true),
        CityEntry("Bathinda", REGION_PUNJAB, state = "Punjab", lat = 30.2110, lng = 74.9455, aliases = listOf("Bhatinda"), isPopular = true),
        CityEntry("Patiala", REGION_PUNJAB, state = "Punjab", lat = 30.3398, lng = 76.3869, aliases = emptyList(), isPopular = true),
        CityEntry("Fazilka", REGION_PUNJAB, state = "Punjab", lat = 30.4037, lng = 74.0254, aliases = emptyList(), isPopular = false),
        CityEntry("Firozpur", REGION_PUNJAB, state = "Punjab", lat = 30.9237, lng = 74.6119, aliases = listOf("Ferozepur", "Ferozepur Cantt"), isPopular = false),
        CityEntry("Sri Muktsar Sahib", REGION_PUNJAB, state = "Punjab", lat = 30.4762, lng = 74.5164, aliases = listOf("Muktsar", "Mukatsar"), isPopular = false),
        CityEntry("Malout", REGION_PUNJAB, state = "Punjab", lat = 30.1915, lng = 74.4934, aliases = emptyList(), isPopular = false),
        CityEntry("Moga", REGION_PUNJAB, state = "Punjab", lat = 30.8165, lng = 75.1717, aliases = emptyList(), isPopular = false),
        CityEntry("Faridkot", REGION_PUNJAB, state = "Punjab", lat = 30.6769, lng = 74.7583, aliases = emptyList(), isPopular = false),
        CityEntry("Kotkapura", REGION_PUNJAB, state = "Punjab", lat = 30.5828, lng = 74.8276, aliases = listOf("Kot Kapura"), isPopular = false),
        CityEntry("Hoshiarpur", REGION_PUNJAB, state = "Punjab", lat = 31.5273, lng = 75.9149, aliases = emptyList(), isPopular = false),
        CityEntry("Pathankot", REGION_PUNJAB, state = "Punjab", lat = 32.2684, lng = 75.6529, aliases = emptyList(), isPopular = false),
        CityEntry("Gurdaspur", REGION_PUNJAB, state = "Punjab", lat = 32.0419, lng = 75.4053, aliases = emptyList(), isPopular = false),
        CityEntry("Batala", REGION_PUNJAB, state = "Punjab", lat = 31.8186, lng = 75.2028, aliases = emptyList(), isPopular = false),
        CityEntry("Kapurthala", REGION_PUNJAB, state = "Punjab", lat = 31.3800, lng = 75.3800, aliases = emptyList(), isPopular = false),
        CityEntry("Phagwara", REGION_PUNJAB, state = "Punjab", lat = 31.2240, lng = 75.7708, aliases = emptyList(), isPopular = false),
        CityEntry("Khanna", REGION_PUNJAB, state = "Punjab", lat = 30.7071, lng = 76.2167, aliases = emptyList(), isPopular = false),
        CityEntry("Barnala", REGION_PUNJAB, state = "Punjab", lat = 30.3819, lng = 75.5471, aliases = emptyList(), isPopular = false),
        CityEntry("Mansa", REGION_PUNJAB, state = "Punjab", lat = 29.9984, lng = 75.3949, aliases = emptyList(), isPopular = false),
        CityEntry("Sangrur", REGION_PUNJAB, state = "Punjab", lat = 30.2458, lng = 75.8421, aliases = emptyList(), isPopular = false),
        CityEntry("Malerkotla", REGION_PUNJAB, state = "Punjab", lat = 30.5256, lng = 75.8901, aliases = emptyList(), isPopular = false),
        CityEntry("Rajpura", REGION_PUNJAB, state = "Punjab", lat = 30.4842, lng = 76.5939, aliases = emptyList(), isPopular = false),
        CityEntry("Rupnagar", REGION_PUNJAB, state = "Punjab", lat = 30.9664, lng = 76.5331, aliases = listOf("Ropar"), isPopular = false),
        CityEntry("Nangal", REGION_PUNJAB, state = "Punjab", lat = 31.3678, lng = 76.3789, aliases = emptyList(), isPopular = false),
        CityEntry("Fatehgarh Sahib", REGION_PUNJAB, state = "Punjab", lat = 30.6431, lng = 76.3882, aliases = listOf("Sirhind"), isPopular = false),
        CityEntry("Mandi Gobindgarh", REGION_PUNJAB, state = "Punjab", lat = 30.6656, lng = 76.3072, aliases = listOf("Gobindgarh"), isPopular = false),
        CityEntry("Nawanshahr", REGION_PUNJAB, state = "Punjab", lat = 31.1256, lng = 76.1187, aliases = listOf("SBS Nagar", "Shaheed Bhagat Singh Nagar"), isPopular = false),
        CityEntry("Tarn Taran", REGION_PUNJAB, state = "Punjab", lat = 31.4518, lng = 74.9255, aliases = listOf("Tarn Taran Sahib"), isPopular = false),
        CityEntry("Jalalabad", REGION_PUNJAB, state = "Punjab", lat = 30.6080, lng = 74.2550, aliases = emptyList(), isPopular = false),
        CityEntry("Jagraon", REGION_PUNJAB, state = "Punjab", lat = 30.7844, lng = 75.4770, aliases = emptyList(), isPopular = false),
        CityEntry("Sunam", REGION_PUNJAB, state = "Punjab", lat = 30.1306, lng = 75.8014, aliases = emptyList(), isPopular = false),
        CityEntry("Dhuri", REGION_PUNJAB, state = "Punjab", lat = 30.3694, lng = 75.8672, aliases = emptyList(), isPopular = false),
        CityEntry("Nabha", REGION_PUNJAB, state = "Punjab", lat = 30.3744, lng = 76.1528, aliases = emptyList(), isPopular = false),
        CityEntry("Samana", REGION_PUNJAB, state = "Punjab", lat = 30.1544, lng = 76.1928, aliases = emptyList(), isPopular = false),
        CityEntry("Nakodar", REGION_PUNJAB, state = "Punjab", lat = 31.1256, lng = 75.4744, aliases = emptyList(), isPopular = false),
        CityEntry("Patti", REGION_PUNJAB, state = "Punjab", lat = 31.2800, lng = 74.8600, aliases = emptyList(), isPopular = false),
        CityEntry("Morinda", REGION_PUNJAB, state = "Punjab", lat = 30.7931, lng = 76.4950, aliases = emptyList(), isPopular = false),
        CityEntry("Kurali", REGION_PUNJAB, state = "Punjab", lat = 30.8247, lng = 76.5744, aliases = emptyList(), isPopular = false),
        CityEntry("Dera Bassi", REGION_PUNJAB, state = "Punjab", lat = 30.5847, lng = 76.8444, aliases = listOf("Derabassi"), isPopular = false),

        // Haryana
        CityEntry("Gurgaon", REGION_HARYANA, state = "Haryana", lat = 28.4595, lng = 77.0266, aliases = listOf("Gurugram"), isPopular = true),
        CityEntry("Faridabad", REGION_HARYANA, state = "Haryana", lat = 28.4089, lng = 77.3178, aliases = emptyList(), isPopular = false),
        CityEntry("Panipat", REGION_HARYANA, state = "Haryana", lat = 29.3909, lng = 76.9635, aliases = emptyList(), isPopular = false),
        CityEntry("Ambala", REGION_HARYANA, state = "Haryana", lat = 30.3782, lng = 76.7767, aliases = listOf("Ambala Cantt", "Ambala City"), isPopular = false),
        CityEntry("Karnal", REGION_HARYANA, state = "Haryana", lat = 29.6857, lng = 76.9905, aliases = emptyList(), isPopular = false),
        CityEntry("Sonipat", REGION_HARYANA, state = "Haryana", lat = 28.9931, lng = 77.0151, aliases = listOf("Sonepat"), isPopular = false),
        CityEntry("Rohtak", REGION_HARYANA, state = "Haryana", lat = 28.8955, lng = 76.6066, aliases = emptyList(), isPopular = false),
        CityEntry("Hisar", REGION_HARYANA, state = "Haryana", lat = 29.1492, lng = 75.7217, aliases = listOf("Hissar"), isPopular = false),
        CityEntry("Sirsa", REGION_HARYANA, state = "Haryana", lat = 29.5349, lng = 75.0298, aliases = emptyList(), isPopular = false),
        CityEntry("Fatehabad", REGION_HARYANA, state = "Haryana", lat = 29.5147, lng = 75.4526, aliases = emptyList(), isPopular = false),
        CityEntry("Jind", REGION_HARYANA, state = "Haryana", lat = 29.3140, lng = 76.3140, aliases = emptyList(), isPopular = false),
        CityEntry("Kaithal", REGION_HARYANA, state = "Haryana", lat = 29.8015, lng = 76.4022, aliases = emptyList(), isPopular = false),
        CityEntry("Kurukshetra", REGION_HARYANA, state = "Haryana", lat = 29.9695, lng = 76.8783, aliases = listOf("Thanesar"), isPopular = false),
        CityEntry("Yamunanagar", REGION_HARYANA, state = "Haryana", lat = 30.1290, lng = 77.2674, aliases = listOf("Jagadhri"), isPopular = false),
        CityEntry("Bhiwani", REGION_HARYANA, state = "Haryana", lat = 28.7932, lng = 76.1390, aliases = emptyList(), isPopular = false),
        CityEntry("Rewari", REGION_HARYANA, state = "Haryana", lat = 28.1837, lng = 76.6202, aliases = emptyList(), isPopular = false),
        CityEntry("Narnaul", REGION_HARYANA, state = "Haryana", lat = 28.0439, lng = 76.1077, aliases = emptyList(), isPopular = false),
        CityEntry("Palwal", REGION_HARYANA, state = "Haryana", lat = 28.1487, lng = 77.3320, aliases = emptyList(), isPopular = false),
        CityEntry("Bahadurgarh", REGION_HARYANA, state = "Haryana", lat = 28.6925, lng = 76.9240, aliases = emptyList(), isPopular = false),
        CityEntry("Jhajjar", REGION_HARYANA, state = "Haryana", lat = 28.6080, lng = 76.6570, aliases = emptyList(), isPopular = false),
        CityEntry("Mandi Dabwali", REGION_HARYANA, state = "Haryana", lat = 29.9578, lng = 74.7256, aliases = listOf("Dabwali"), isPopular = false),
        CityEntry("Tohana", REGION_HARYANA, state = "Haryana", lat = 29.7042, lng = 75.9036, aliases = emptyList(), isPopular = false),
        CityEntry("Narwana", REGION_HARYANA, state = "Haryana", lat = 29.5960, lng = 76.1190, aliases = emptyList(), isPopular = false),
        CityEntry("Hansi", REGION_HARYANA, state = "Haryana", lat = 29.1000, lng = 75.9600, aliases = emptyList(), isPopular = false),
        CityEntry("Pehowa", REGION_HARYANA, state = "Haryana", lat = 29.9800, lng = 76.5800, aliases = emptyList(), isPopular = false),
        CityEntry("Shahbad", REGION_HARYANA, state = "Haryana", lat = 30.1680, lng = 76.8700, aliases = listOf("Shahbad Markanda"), isPopular = false),

        // Delhi NCR
        CityEntry("Delhi", REGION_DELHI_NCR, state = "Delhi", lat = 28.6139, lng = 77.2090, aliases = listOf("New Delhi", "Delhi NCR", "Dilli"), isPopular = true),
        CityEntry("Noida", REGION_DELHI_NCR, state = "Uttar Pradesh", lat = 28.5355, lng = 77.3910, aliases = listOf("Gautam Buddha Nagar", "Noida UP"), isPopular = true),
        CityEntry("Greater Noida", REGION_DELHI_NCR, state = "Uttar Pradesh", lat = 28.4744, lng = 77.5040, aliases = listOf("Gr Noida"), isPopular = false),
        CityEntry("Ghaziabad", REGION_DELHI_NCR, state = "Uttar Pradesh", lat = 28.6692, lng = 77.4538, aliases = listOf("Ghaziabad UP"), isPopular = false),

        // Himachal Pradesh
        CityEntry("Shimla", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 31.1048, lng = 77.1734, aliases = listOf("Simla"), isPopular = true),
        CityEntry("Manali", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 32.2396, lng = 77.1887, aliases = listOf("Kullu Manali"), isPopular = true),
        CityEntry("Dharamshala", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 32.2190, lng = 76.3234, aliases = listOf("Dharamsala", "McLeod Ganj", "Mcleodganj"), isPopular = false),
        CityEntry("Solan", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 30.9045, lng = 77.0967, aliases = emptyList(), isPopular = false),
        CityEntry("Kasauli", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 30.9013, lng = 76.9649, aliases = emptyList(), isPopular = false),
        CityEntry("Baddi", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 30.9578, lng = 76.7914, aliases = emptyList(), isPopular = false),
        CityEntry("Nalagarh", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 31.0425, lng = 76.7167, aliases = emptyList(), isPopular = false),
        CityEntry("Mandi", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 31.7087, lng = 76.9320, aliases = emptyList(), isPopular = false),
        CityEntry("Kullu", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 31.9579, lng = 77.1095, aliases = emptyList(), isPopular = false),
        CityEntry("Una", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 31.4685, lng = 76.2708, aliases = emptyList(), isPopular = false),
        CityEntry("Hamirpur", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 31.6862, lng = 76.5213, aliases = emptyList(), isPopular = false),
        CityEntry("Bilaspur", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 31.3325, lng = 76.7589, aliases = emptyList(), isPopular = false),
        CityEntry("Kangra", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 32.0998, lng = 76.2691, aliases = emptyList(), isPopular = false),
        CityEntry("Palampur", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 32.1109, lng = 76.5363, aliases = emptyList(), isPopular = false),
        CityEntry("Paonta Sahib", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 30.4439, lng = 77.6200, aliases = emptyList(), isPopular = false),
        CityEntry("Nahan", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 30.5599, lng = 77.2955, aliases = emptyList(), isPopular = false),
        CityEntry("Chamba", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 32.5534, lng = 76.1258, aliases = emptyList(), isPopular = false),
        CityEntry("Dalhousie", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 32.5387, lng = 75.9710, aliases = emptyList(), isPopular = false),
        CityEntry("Parwanoo", REGION_HIMACHAL, state = "Himachal Pradesh", lat = 30.8350, lng = 76.9600, aliases = emptyList(), isPopular = false),

        // Rajasthan
        CityEntry("Jaipur", REGION_RAJASTHAN, state = "Rajasthan", lat = 26.9124, lng = 75.7873, aliases = listOf("Pink City"), isPopular = true),
        CityEntry("Sri Ganganagar", REGION_RAJASTHAN, state = "Rajasthan", lat = 29.9038, lng = 73.8772, aliases = listOf("Ganganagar"), isPopular = true),
        CityEntry("Hanumangarh", REGION_RAJASTHAN, state = "Rajasthan", lat = 29.5819, lng = 74.3294, aliases = listOf("Hanumangarh Town", "Hanumangarh Jn"), isPopular = true),
        CityEntry("Suratgarh", REGION_RAJASTHAN, state = "Rajasthan", lat = 29.3190, lng = 73.9010, aliases = emptyList(), isPopular = false),
        CityEntry("Anupgarh", REGION_RAJASTHAN, state = "Rajasthan", lat = 29.1911, lng = 73.2086, aliases = emptyList(), isPopular = false),
        CityEntry("Bikaner", REGION_RAJASTHAN, state = "Rajasthan", lat = 28.0229, lng = 73.3119, aliases = emptyList(), isPopular = false),
        CityEntry("Churu", REGION_RAJASTHAN, state = "Rajasthan", lat = 28.2900, lng = 74.9600, aliases = emptyList(), isPopular = false),
        CityEntry("Sikar", REGION_RAJASTHAN, state = "Rajasthan", lat = 27.6094, lng = 75.1399, aliases = emptyList(), isPopular = false),
        CityEntry("Jhunjhunu", REGION_RAJASTHAN, state = "Rajasthan", lat = 28.1289, lng = 75.3995, aliases = emptyList(), isPopular = false),
        CityEntry("Alwar", REGION_RAJASTHAN, state = "Rajasthan", lat = 27.5530, lng = 76.6346, aliases = emptyList(), isPopular = false),
        CityEntry("Bharatpur", REGION_RAJASTHAN, state = "Rajasthan", lat = 27.2152, lng = 77.5030, aliases = emptyList(), isPopular = false),
        CityEntry("Dausa", REGION_RAJASTHAN, state = "Rajasthan", lat = 26.8930, lng = 76.3370, aliases = emptyList(), isPopular = false),
        CityEntry("Kotputli", REGION_RAJASTHAN, state = "Rajasthan", lat = 27.7040, lng = 76.2000, aliases = emptyList(), isPopular = false),
        CityEntry("Behror", REGION_RAJASTHAN, state = "Rajasthan", lat = 27.8900, lng = 76.2800, aliases = emptyList(), isPopular = false),
        CityEntry("Neemrana", REGION_RAJASTHAN, state = "Rajasthan", lat = 27.9900, lng = 76.3800, aliases = emptyList(), isPopular = false),
        CityEntry("Bhiwadi", REGION_RAJASTHAN, state = "Rajasthan", lat = 28.2100, lng = 76.8600, aliases = emptyList(), isPopular = false),
        CityEntry("Ajmer", REGION_RAJASTHAN, state = "Rajasthan", lat = 26.4499, lng = 74.6399, aliases = listOf("Ajmer Sharif"), isPopular = false),
        CityEntry("Pushkar", REGION_RAJASTHAN, state = "Rajasthan", lat = 26.4897, lng = 74.5511, aliases = emptyList(), isPopular = false),
        CityEntry("Kishangarh", REGION_RAJASTHAN, state = "Rajasthan", lat = 26.5700, lng = 74.8600, aliases = emptyList(), isPopular = false),
        CityEntry("Beawar", REGION_RAJASTHAN, state = "Rajasthan", lat = 26.1014, lng = 74.3214, aliases = emptyList(), isPopular = false),
        CityEntry("Jodhpur", REGION_RAJASTHAN, state = "Rajasthan", lat = 26.2389, lng = 73.0243, aliases = listOf("Sun City", "Blue City"), isPopular = false),
        CityEntry("Udaipur", REGION_RAJASTHAN, state = "Rajasthan", lat = 24.5854, lng = 73.7125, aliases = listOf("Lake City"), isPopular = true),
        CityEntry("Kota", REGION_RAJASTHAN, state = "Rajasthan", lat = 25.2138, lng = 75.8648, aliases = emptyList(), isPopular = false),
        CityEntry("Bhilwara", REGION_RAJASTHAN, state = "Rajasthan", lat = 25.3407, lng = 74.6313, aliases = emptyList(), isPopular = false),
        CityEntry("Pali", REGION_RAJASTHAN, state = "Rajasthan", lat = 25.7711, lng = 73.3234, aliases = emptyList(), isPopular = false),
        CityEntry("Chittorgarh", REGION_RAJASTHAN, state = "Rajasthan", lat = 24.8887, lng = 74.6269, aliases = emptyList(), isPopular = false),
        CityEntry("Mount Abu", REGION_RAJASTHAN, state = "Rajasthan", lat = 24.5926, lng = 72.7156, aliases = emptyList(), isPopular = false),

        // Uttar Pradesh
        CityEntry("Agra", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", lat = 27.1767, lng = 78.0081, aliases = listOf("Taj City"), isPopular = true),
        CityEntry("Lucknow", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", lat = 26.8467, lng = 80.9462, aliases = listOf("Nawab City", "LKO"), isPopular = true),
        CityEntry("Kanpur", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", lat = 26.4499, lng = 80.3319, aliases = listOf("Cawnpore"), isPopular = false),
        CityEntry("Varanasi", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", lat = 25.3176, lng = 82.9739, aliases = listOf("Banaras", "Benares", "Kashi"), isPopular = false),
        CityEntry("Prayagraj", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", lat = 25.4358, lng = 81.8463, aliases = listOf("Allahabad"), isPopular = false),
        CityEntry("Mathura", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", lat = 27.4924, lng = 77.6737, aliases = listOf("Vrindavan", "Mathura Vrindavan"), isPopular = false),
        CityEntry("Meerut", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", lat = 28.9845, lng = 77.7064, aliases = emptyList(), isPopular = false),
        CityEntry("Ayodhya", REGION_UTTAR_PRADESH, state = "Uttar Pradesh", lat = 26.7922, lng = 82.1998, aliases = listOf("Faizabad"), isPopular = false),

        // Uttarakhand
        CityEntry("Dehradun", REGION_UTTARAKHAND, state = "Uttarakhand", lat = 30.3165, lng = 78.0322, aliases = listOf("Doon"), isPopular = true),
        CityEntry("Rishikesh", REGION_UTTARAKHAND, state = "Uttarakhand", lat = 30.0869, lng = 78.2676, aliases = listOf("Hrishikesh"), isPopular = true),
        CityEntry("Haridwar", REGION_UTTARAKHAND, state = "Uttarakhand", lat = 29.9457, lng = 78.1642, aliases = listOf("Hardwar"), isPopular = false),
        CityEntry("Nainital", REGION_UTTARAKHAND, state = "Uttarakhand", lat = 29.3919, lng = 79.4542, aliases = emptyList(), isPopular = false),
        CityEntry("Mussoorie", REGION_UTTARAKHAND, state = "Uttarakhand", lat = 30.4598, lng = 78.0644, aliases = listOf("Mussourie"), isPopular = false),
        CityEntry("Haldwani", REGION_UTTARAKHAND, state = "Uttarakhand", lat = 29.2183, lng = 79.5130, aliases = listOf("Kathgodam"), isPopular = false),

        // Other major cities
        CityEntry("Mumbai", REGION_OTHER, state = "Maharashtra", lat = 19.0760, lng = 72.8777, aliases = listOf("Bombay", "Maharashtra"), isPopular = true),
        CityEntry("Pune", REGION_OTHER, state = "Maharashtra", lat = 18.5204, lng = 73.8567, aliases = listOf("Poona", "Maharashtra"), isPopular = true),
        CityEntry("Bengaluru", REGION_OTHER, state = "Karnataka", lat = 12.9716, lng = 77.5946, aliases = listOf("Bangalore", "Karnataka"), isPopular = true),
        CityEntry("Ahmedabad", REGION_OTHER, state = "Gujarat", lat = 23.0225, lng = 72.5714, aliases = listOf("Amdavad", "Gujarat"), isPopular = false),
        CityEntry("Hyderabad", REGION_OTHER, state = "Telangana", lat = 17.3850, lng = 78.4867, aliases = listOf("Secunderabad", "Telangana"), isPopular = false),
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
        } ?: CITIES.firstOrNull { city ->
            // Substring / prefix fallback (e.g. "Abohar, Punjab" -> Abohar)
            query.startsWith(city.name.lowercase()) ||
            city.aliases.any { query.startsWith(it.lowercase()) }
        }
    }

    /** Resolves canonical name for a given input or alias */
    fun canonicalName(nameOrAlias: String?): String? {
        return findCity(nameOrAlias)?.name
    }

    /** Resolves coordinates for a known city or alias */
    fun getCoordinates(nameOrAlias: String?): Pair<Double, Double>? {
        val entry = findCity(nameOrAlias) ?: return null
        if (entry.lat != 0.0 && entry.lng != 0.0) {
            return entry.lat to entry.lng
        }
        return null
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
