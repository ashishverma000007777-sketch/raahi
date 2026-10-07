package `in`.raahi.app.data

data class VehicleVariant(
    val name: String,
)

data class VehicleModel(
    val name: String,
    val defaultFuelType: String = "PETROL",
    val variants: List<String> = emptyList(),
)

data class VehicleBrand(
    val name: String,
    val popular: Boolean = false,
    val models: List<VehicleModel> = emptyList(),
)

object VehicleCatalogue {

    val BRANDS: List<VehicleBrand> = listOf(
        VehicleBrand(
            name = "Hyundai",
            popular = true,
            models = listOf(
                VehicleModel("Grand i10 Nios", "PETROL", listOf("Sportz", "Magna", "Asta", "Era")),
                VehicleModel("Creta", "PETROL", listOf("SX(O)", "SX", "SX Tech", "S(O)", "S", "EX", "E")),
                VehicleModel("i20", "PETROL", listOf("Asta (O)", "Asta", "Sportz", "Magna", "N Line N8", "N Line N6")),
                VehicleModel("Venue", "PETROL", listOf("SX(O)", "SX", "S(O)", "S+", "S", "E")),
                VehicleModel("Verna", "PETROL", listOf("SX(O)", "SX", "S", "EX", "Turbo SX(O)")),
                VehicleModel("Exter", "PETROL", listOf("SX(O) Connect", "SX(O)", "SX", "S(O)", "S", "EX(O)", "EX")),
                VehicleModel("Aura", "CNG", listOf("SX(O)", "SX", "S", "E")),
                VehicleModel("Alcazar", "DIESEL", listOf("Signature", "Platinum", "Prestige")),
                VehicleModel("Tucson", "DIESEL", listOf("Signature", "Platinum")),
                VehicleModel("Ioniq 5", "ELECTRIC", listOf("RWD"))
            )
        ),
        VehicleBrand(
            name = "Maruti Suzuki",
            popular = true,
            models = listOf(
                VehicleModel("Swift", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi")),
                VehicleModel("Baleno", "PETROL", listOf("Alpha", "Zeta", "Delta", "Sigma")),
                VehicleModel("Brezza", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi")),
                VehicleModel("Dzire", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi")),
                VehicleModel("Fronx", "PETROL", listOf("Alpha", "Zeta", "Delta+", "Delta", "Sigma")),
                VehicleModel("Grand Vitara", "HYBRID", listOf("Alpha+", "Alpha", "Zeta+", "Zeta", "Delta", "Sigma")),
                VehicleModel("Ertiga", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi")),
                VehicleModel("Wagon R", "CNG", listOf("ZXi+", "ZXi", "VXi", "LXi")),
                VehicleModel("Alto K10", "PETROL", listOf("VXi+", "VXi", "LXi", "Std")),
                VehicleModel("Celerio", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi")),
                VehicleModel("Jimny", "PETROL", listOf("Alpha", "Zeta")),
                VehicleModel("XL6", "PETROL", listOf("Alpha+", "Alpha", "Zeta")),
                VehicleModel("Ignis", "PETROL", listOf("Alpha", "Zeta", "Delta", "Sigma")),
                VehicleModel("Invicto", "HYBRID", listOf("Alpha+", "Zeta+")),
                VehicleModel("Ciaz", "PETROL", listOf("Alpha", "Zeta", "Delta", "Sigma"))
            )
        ),
        VehicleBrand(
            name = "Tata",
            popular = true,
            models = listOf(
                VehicleModel("Nexon", "PETROL", listOf("Fearless+", "Fearless", "Creative+", "Creative", "Pure", "Smart")),
                VehicleModel("Punch", "PETROL", listOf("Creative Flagship", "Creative", "Accomplished Dazzle", "Accomplished", "Adventure", "Pure")),
                VehicleModel("Harrier", "DIESEL", listOf("Fearless+", "Fearless", "Adventure+", "Adventure", "Pure+", "Smart")),
                VehicleModel("Safari", "DIESEL", listOf("Accomplished+", "Accomplished", "Adventure+", "Adventure", "Pure+", "Smart")),
                VehicleModel("Altroz", "PETROL", listOf("XZ+ OS", "XZ+", "XZ", "XT", "XM+", "XE")),
                VehicleModel("Tiago", "PETROL", listOf("XZ+", "XZ", "XT", "XE")),
                VehicleModel("Tigor", "CNG", listOf("XZ+", "XZ", "XM", "XE")),
                VehicleModel("Curvv", "DIESEL", listOf("Accomplished+", "Accomplished", "Creative+", "Pure+", "Smart")),
                VehicleModel("Nexon EV", "ELECTRIC", listOf("Empowered+", "Empowered", "Fearless+", "Creative+")),
                VehicleModel("Punch EV", "ELECTRIC", listOf("Empowered+", "Empowered", "Adventure", "Smart+"))
            )
        ),
        VehicleBrand(
            name = "Mahindra",
            popular = true,
            models = listOf(
                VehicleModel("Thar", "DIESEL", listOf("LX Hard Top", "LX Convertible", "AX Opt")),
                VehicleModel("Thar Roxx", "DIESEL", listOf("AX7L", "AX5L", "AX3L", "MX5", "MX3", "MX1")),
                VehicleModel("XUV700", "DIESEL", listOf("AX7L", "AX7", "AX5", "AX3", "MX")),
                VehicleModel("Scorpio-N", "DIESEL", listOf("Z8L", "Z8 Select", "Z8", "Z6", "Z4", "Z2")),
                VehicleModel("Scorpio Classic", "DIESEL", listOf("S11", "S")),
                VehicleModel("XUV 3XO", "PETROL", listOf("AX7L", "AX7", "AX5L", "AX5", "MX3", "MX2", "MX1")),
                VehicleModel("Bolero", "DIESEL", listOf("B6 Opt", "B6", "B4")),
                VehicleModel("Bolero Neo", "DIESEL", listOf("N10 Opt", "N10", "N8", "N4")),
                VehicleModel("XUV400 EV", "ELECTRIC", listOf("EL Pro", "EC Pro"))
            )
        ),
        VehicleBrand(
            name = "Kia",
            popular = true,
            models = listOf(
                VehicleModel("Seltos", "PETROL", listOf("X-Line", "GTX+ (S)", "GTX+", "HTX+", "HTX", "HTK+", "HTK", "HTE")),
                VehicleModel("Sonet", "PETROL", listOf("X-Line", "GTX+", "HTX+", "HTX", "HTK+", "HTK", "HTE")),
                VehicleModel("Carens", "DIESEL", listOf("Luxury Plus", "Luxury", "Prestige Plus", "Prestige", "Premium")),
                VehicleModel("Carnival", "DIESEL", listOf("Limousine Plus", "Limousine")),
                VehicleModel("EV6", "ELECTRIC", listOf("GT-Line AWD", "GT-Line RWD"))
            )
        ),
        VehicleBrand(
            name = "Toyota",
            popular = true,
            models = listOf(
                VehicleModel("Innova Hycross", "HYBRID", listOf("ZX (O)", "ZX", "VX (O)", "VX", "GX (O)", "GX")),
                VehicleModel("Innova Crysta", "DIESEL", listOf("ZX", "VX", "GX Plus", "GX")),
                VehicleModel("Fortuner", "DIESEL", listOf("GR-S", "Legender 4x4", "Legender 4x2", "4x4 AT", "4x2 AT", "4x2 MT")),
                VehicleModel("Urban Cruiser Hyryder", "HYBRID", listOf("V Hybrid", "G Hybrid", "S Hybrid", "V", "G", "S", "E")),
                VehicleModel("Glanza", "PETROL", listOf("V", "G", "S", "E")),
                VehicleModel("Rumion", "PETROL", listOf("V", "G", "S")),
                VehicleModel("Hilux", "DIESEL", listOf("High AT", "High MT", "Standard MT")),
                VehicleModel("Camry", "HYBRID", listOf("Hybrid 2.5"))
            )
        ),
        VehicleBrand(
            name = "Honda",
            popular = true,
            models = listOf(
                VehicleModel("City", "PETROL", listOf("ZX", "VX", "V", "SV", "e:HEV ZX")),
                VehicleModel("Elevate", "PETROL", listOf("ZX", "VX", "V", "SV")),
                VehicleModel("Amaze", "PETROL", listOf("VX", "S", "E"))
            )
        ),
        VehicleBrand(
            name = "Volkswagen",
            popular = true,
            models = listOf(
                VehicleModel("Virtus", "PETROL", listOf("GT Plus", "GT Line", "Topline", "Highline", "Comfortline")),
                VehicleModel("Taigun", "PETROL", listOf("GT Plus Edge", "GT Plus", "GT Line", "Topline", "Highline", "Comfortline")),
                VehicleModel("Tiguan", "PETROL", listOf("Elegance")),
                VehicleModel("Polo", "PETROL", listOf("GT TSI", "Highline Plus", "Comfortline"))
            )
        ),
        VehicleBrand(
            name = "Skoda",
            popular = false,
            models = listOf(
                VehicleModel("Slavia", "PETROL", listOf("Monte Carlo", "Style", "Ambition", "Active")),
                VehicleModel("Kushaq", "PETROL", listOf("Monte Carlo", "Style", "Ambition", "Active")),
                VehicleModel("Kodiaq", "PETROL", listOf("L&K", "Sportline", "Style")),
                VehicleModel("Superb", "PETROL", listOf("L&K"))
            )
        ),
        VehicleBrand(
            name = "MG",
            popular = false,
            models = listOf(
                VehicleModel("Hector", "PETROL", listOf("Savvy Pro", "Sharp Pro", "Smart Pro", "Shine Pro", "Style")),
                VehicleModel("Astor", "PETROL", listOf("Savvy Pro", "Sharp Pro", "Select", "Shine", "Sprint")),
                VehicleModel("ZS EV", "ELECTRIC", listOf("Exclusive Plus", "Excite Pro", "Executive")),
                VehicleModel("Comet EV", "ELECTRIC", listOf("Plush", "Play", "Pace")),
                VehicleModel("Gloster", "DIESEL", listOf("Savvy 4x4", "Savvy 4x2", "Sharp 4x2"))
            )
        ),
        VehicleBrand(
            name = "Renault",
            popular = false,
            models = listOf(
                VehicleModel("Kwid", "PETROL", listOf("Climber", "RXT", "RXL(O)", "RXE")),
                VehicleModel("Kiger", "PETROL", listOf("RXZ", "RXT(O)", "RXL", "RXE")),
                VehicleModel("Triber", "PETROL", listOf("RXZ", "RXT", "RXL", "RXE"))
            )
        ),
        VehicleBrand(
            name = "Nissan",
            popular = false,
            models = listOf(
                VehicleModel("Magnite", "PETROL", listOf("Tekna+", "Tekna", "N-Connecta", "Acenta", "Visia"))
            )
        ),
        VehicleBrand(
            name = "Jeep",
            popular = false,
            models = listOf(
                VehicleModel("Compass", "DIESEL", listOf("Model S", "Black Shark", "Limited", "Longitude", "Sport")),
                VehicleModel("Meridian", "DIESEL", listOf("Overland", "Limited Plus", "Longitude")),
                VehicleModel("Wrangler", "PETROL", listOf("Rubicon", "Unlimited"))
            )
        ),
        VehicleBrand(
            name = "BMW",
            popular = false,
            models = listOf(
                VehicleModel("3 Series Gran Limousine", "PETROL", listOf("330Li M Sport", "320Ld M Sport")),
                VehicleModel("X1", "PETROL", listOf("sDrive18i M Sport", "sDrive18d M Sport")),
                VehicleModel("X3", "DIESEL", listOf("xDrive20d M Sport", "M40i")),
                VehicleModel("5 Series", "PETROL", listOf("530Li M Sport")),
                VehicleModel("X5", "DIESEL", listOf("xDrive30d M Sport", "xDrive40i M Sport"))
            )
        ),
        VehicleBrand(
            name = "Mercedes-Benz",
            popular = false,
            models = listOf(
                VehicleModel("C-Class", "PETROL", listOf("C200", "C220d", "C300 AMG Line")),
                VehicleModel("E-Class", "PETROL", listOf("E200 Exclusive", "E220d Exclusive", "E350d AMG Line")),
                VehicleModel("GLA", "PETROL", listOf("GLA 200", "GLA 220d 4MATIC AMG Line")),
                VehicleModel("GLC", "PETROL", listOf("GLC 300 4MATIC", "GLC 220d 4MATIC")),
                VehicleModel("GLE", "DIESEL", listOf("GLE 300d 4MATIC", "GLE 450d 4MATIC"))
            )
        ),
        VehicleBrand(
            name = "Audi",
            popular = false,
            models = listOf(
                VehicleModel("A4", "PETROL", listOf("Technology", "Premium Plus", "Premium")),
                VehicleModel("A6", "PETROL", listOf("Technology", "Premium Plus")),
                VehicleModel("Q3", "PETROL", listOf("Technology", "Premium Plus")),
                VehicleModel("Q5", "PETROL", listOf("Technology", "Premium Plus")),
                VehicleModel("Q7", "PETROL", listOf("Technology", "Premium Plus"))
            )
        )
    )

    fun findBrand(name: String): VehicleBrand? =
        BRANDS.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    fun findModels(brandName: String): List<VehicleModel> =
        findBrand(brandName)?.models ?: emptyList()

    fun findVariants(brandName: String, modelName: String): List<String> =
        findModels(brandName)
            .firstOrNull { it.name.equals(modelName.trim(), ignoreCase = true) }
            ?.variants ?: emptyList()
}
