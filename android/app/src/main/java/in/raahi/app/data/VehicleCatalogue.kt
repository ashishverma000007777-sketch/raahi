package `in`.raahi.app.data

enum class VehicleCategory(val id: String, val label: String) {
    BIKE("BIKE", "Bike"),
    CAR("CAR", "Car"),
    VAN("VAN", "Van"),
    TOW_TRUCK("TOW_TRUCK", "Tow Truck");

    companion object {
        fun fromId(id: String?): VehicleCategory =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: CAR
    }
}

data class VehicleVariant(
    val name: String,
)

data class VehicleModel(
    val name: String,
    val defaultFuelType: String = "PETROL",
    val variants: List<String> = emptyList(),
    val category: VehicleCategory = VehicleCategory.CAR,
)

data class VehicleBrand(
    val name: String,
    val popular: Boolean = false,
    val models: List<VehicleModel> = emptyList(),
) {
    fun hasCategory(category: VehicleCategory): Boolean =
        models.any { it.category == category }

    fun modelsForCategory(category: VehicleCategory): List<VehicleModel> =
        models.filter { it.category == category }
}

object VehicleCatalogue {

    val ALL_BRANDS: List<VehicleBrand> = listOf(
        // ================================================================= CARS
        VehicleBrand(
            name = "Hyundai",
            popular = true,
            models = listOf(
                VehicleModel("Grand i10 Nios", "PETROL", listOf("Sportz", "Magna", "Asta", "Era"), VehicleCategory.CAR),
                VehicleModel("Creta", "PETROL", listOf("SX(O)", "SX", "SX Tech", "S(O)", "S", "EX", "E"), VehicleCategory.CAR),
                VehicleModel("i20", "PETROL", listOf("Asta (O)", "Asta", "Sportz", "Magna", "N Line N8", "N Line N6"), VehicleCategory.CAR),
                VehicleModel("Venue", "PETROL", listOf("SX(O)", "SX", "S(O)", "S+", "S", "E"), VehicleCategory.CAR),
                VehicleModel("Verna", "PETROL", listOf("SX(O)", "SX", "S", "EX", "Turbo SX(O)"), VehicleCategory.CAR),
                VehicleModel("Exter", "PETROL", listOf("SX(O) Connect", "SX(O)", "SX", "S(O)", "S", "EX(O)", "EX"), VehicleCategory.CAR),
                VehicleModel("Aura", "CNG", listOf("SX(O)", "SX", "S", "E"), VehicleCategory.CAR),
                VehicleModel("Alcazar", "DIESEL", listOf("Signature", "Platinum", "Prestige"), VehicleCategory.CAR),
                VehicleModel("Tucson", "DIESEL", listOf("Signature", "Platinum"), VehicleCategory.CAR),
                VehicleModel("Ioniq 5", "ELECTRIC", listOf("RWD"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Maruti Suzuki",
            popular = true,
            models = listOf(
                VehicleModel("Swift", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi"), VehicleCategory.CAR),
                VehicleModel("Baleno", "PETROL", listOf("Alpha", "Zeta", "Delta", "Sigma"), VehicleCategory.CAR),
                VehicleModel("Brezza", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi"), VehicleCategory.CAR),
                VehicleModel("Dzire", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi"), VehicleCategory.CAR),
                VehicleModel("Fronx", "PETROL", listOf("Alpha", "Zeta", "Delta+", "Delta", "Sigma"), VehicleCategory.CAR),
                VehicleModel("Grand Vitara", "HYBRID", listOf("Alpha+", "Alpha", "Zeta+", "Zeta", "Delta", "Sigma"), VehicleCategory.CAR),
                VehicleModel("Ertiga", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi"), VehicleCategory.CAR),
                VehicleModel("Wagon R", "CNG", listOf("ZXi+", "ZXi", "VXi", "LXi"), VehicleCategory.CAR),
                VehicleModel("Alto K10", "PETROL", listOf("VXi+", "VXi", "LXi", "Std"), VehicleCategory.CAR),
                VehicleModel("Celerio", "PETROL", listOf("ZXi+", "ZXi", "VXi", "LXi"), VehicleCategory.CAR),
                VehicleModel("Jimny", "PETROL", listOf("Alpha", "Zeta"), VehicleCategory.CAR),
                VehicleModel("XL6", "PETROL", listOf("Alpha+", "Alpha", "Zeta"), VehicleCategory.CAR),
                VehicleModel("Ignis", "PETROL", listOf("Alpha", "Zeta", "Delta", "Sigma"), VehicleCategory.CAR),
                VehicleModel("Invicto", "HYBRID", listOf("Alpha+", "Zeta+"), VehicleCategory.CAR),
                VehicleModel("Ciaz", "PETROL", listOf("Alpha", "Zeta", "Delta", "Sigma"), VehicleCategory.CAR),
                // Vans
                VehicleModel("Eeco", "CNG", listOf("5-Seater Standard", "7-Seater Standard", "5-Seater AC", "Cargo"), VehicleCategory.VAN),
                VehicleModel("Omni", "PETROL", listOf("8-Seater", "5-Seater", "Cargo"), VehicleCategory.VAN)
            )
        ),
        VehicleBrand(
            name = "Tata",
            popular = true,
            models = listOf(
                VehicleModel("Nexon", "PETROL", listOf("Fearless+", "Fearless", "Creative+", "Creative", "Pure", "Smart"), VehicleCategory.CAR),
                VehicleModel("Punch", "PETROL", listOf("Creative Flagship", "Creative", "Accomplished Dazzle", "Accomplished", "Adventure", "Pure"), VehicleCategory.CAR),
                VehicleModel("Harrier", "DIESEL", listOf("Fearless+", "Fearless", "Adventure+", "Adventure", "Pure+", "Smart"), VehicleCategory.CAR),
                VehicleModel("Safari", "DIESEL", listOf("Accomplished+", "Accomplished", "Adventure+", "Adventure", "Pure+", "Smart"), VehicleCategory.CAR),
                VehicleModel("Altroz", "PETROL", listOf("XZ+ OS", "XZ+", "XZ", "XT", "XM+", "XE"), VehicleCategory.CAR),
                VehicleModel("Tiago", "PETROL", listOf("XZ+", "XZ", "XT", "XE"), VehicleCategory.CAR),
                VehicleModel("Tigor", "CNG", listOf("XZ+", "XZ", "XM", "XE"), VehicleCategory.CAR),
                VehicleModel("Curvv", "DIESEL", listOf("Accomplished+", "Accomplished", "Creative+", "Pure+", "Smart"), VehicleCategory.CAR),
                VehicleModel("Nexon EV", "ELECTRIC", listOf("Empowered+", "Empowered", "Fearless+", "Creative+"), VehicleCategory.CAR),
                VehicleModel("Punch EV", "ELECTRIC", listOf("Empowered+", "Empowered", "Adventure", "Smart+"), VehicleCategory.CAR),
                // Vans
                VehicleModel("Winger", "DIESEL", listOf("Platinum", "Deluxe", "Skool", "Ambulance"), VehicleCategory.VAN),
                VehicleModel("Magic", "DIESEL", listOf("Express", "Mantra", "Gold"), VehicleCategory.VAN),
                // Tow Trucks
                VehicleModel("407 Recovery Truck", "DIESEL", listOf("Underlift Crane", "Flatbed", "Standard Tow"), VehicleCategory.TOW_TRUCK),
                VehicleModel("709 Flatbed Tow Truck", "DIESEL", listOf("Hydraulic Tilt Flatbed", "Heavy Underlift"), VehicleCategory.TOW_TRUCK),
                VehicleModel("Ultra Breakdown Truck", "DIESEL", listOf("T.7", "T.9", "T.11"), VehicleCategory.TOW_TRUCK)
            )
        ),
        VehicleBrand(
            name = "Mahindra",
            popular = true,
            models = listOf(
                VehicleModel("Thar", "DIESEL", listOf("LX Hard Top", "LX Convertible", "AX Opt"), VehicleCategory.CAR),
                VehicleModel("Thar Roxx", "DIESEL", listOf("AX7L", "AX5L", "AX3L", "MX5", "MX3", "MX1"), VehicleCategory.CAR),
                VehicleModel("XUV700", "DIESEL", listOf("AX7L", "AX7", "AX5", "AX3", "MX"), VehicleCategory.CAR),
                VehicleModel("Scorpio-N", "DIESEL", listOf("Z8L", "Z8 Select", "Z8", "Z6", "Z4", "Z2"), VehicleCategory.CAR),
                VehicleModel("Scorpio Classic", "DIESEL", listOf("S11", "S"), VehicleCategory.CAR),
                VehicleModel("XUV 3XO", "PETROL", listOf("AX7L", "AX7", "AX5L", "AX5", "MX3", "MX2", "MX1"), VehicleCategory.CAR),
                VehicleModel("Bolero", "DIESEL", listOf("B6 Opt", "B6", "B4"), VehicleCategory.CAR),
                VehicleModel("Bolero Neo", "DIESEL", listOf("N10 Opt", "N10", "N8", "N4"), VehicleCategory.CAR),
                VehicleModel("XUV400 EV", "ELECTRIC", listOf("EL Pro", "EC Pro"), VehicleCategory.CAR),
                // Vans & Utility
                VehicleModel("Bolero Maxi Truck Plus", "DIESEL", listOf("Standard", "Power Steering", "CBC"), VehicleCategory.VAN),
                VehicleModel("Supro Profit Truck", "DIESEL", listOf("Maxi", "Mini", "Van"), VehicleCategory.VAN),
                VehicleModel("Bolero Camper", "DIESEL", listOf("Gold ZX", "4WD", "Non-AC"), VehicleCategory.VAN)
            )
        ),
        VehicleBrand(
            name = "Kia",
            popular = true,
            models = listOf(
                VehicleModel("Seltos", "PETROL", listOf("X-Line", "GTX+ (S)", "GTX+", "HTX+", "HTX", "HTK+", "HTK", "HTE"), VehicleCategory.CAR),
                VehicleModel("Sonet", "PETROL", listOf("X-Line", "GTX+", "HTX+", "HTX", "HTK+", "HTK", "HTE"), VehicleCategory.CAR),
                VehicleModel("Carens", "DIESEL", listOf("Luxury Plus", "Luxury", "Prestige Plus", "Prestige", "Premium"), VehicleCategory.CAR),
                VehicleModel("Carnival", "DIESEL", listOf("Limousine Plus", "Limousine"), VehicleCategory.CAR),
                VehicleModel("EV6", "ELECTRIC", listOf("GT-Line AWD", "GT-Line RWD"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Toyota",
            popular = true,
            models = listOf(
                VehicleModel("Innova Hycross", "HYBRID", listOf("ZX (O)", "ZX", "VX (O)", "VX", "GX (O)", "GX"), VehicleCategory.CAR),
                VehicleModel("Innova Crysta", "DIESEL", listOf("ZX", "VX", "GX Plus", "GX"), VehicleCategory.CAR),
                VehicleModel("Fortuner", "DIESEL", listOf("GR-S", "Legender 4x4", "Legender 4x2", "4x4 AT", "4x2 AT", "4x2 MT"), VehicleCategory.CAR),
                VehicleModel("Urban Cruiser Hyryder", "HYBRID", listOf("V Hybrid", "G Hybrid", "S Hybrid", "V", "G", "S", "E"), VehicleCategory.CAR),
                VehicleModel("Glanza", "PETROL", listOf("V", "G", "S", "E"), VehicleCategory.CAR),
                VehicleModel("Rumion", "PETROL", listOf("V", "G", "S"), VehicleCategory.CAR),
                VehicleModel("Hilux", "DIESEL", listOf("High AT", "High MT", "Standard MT"), VehicleCategory.CAR),
                VehicleModel("Camry", "HYBRID", listOf("Hybrid 2.5"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Honda",
            popular = true,
            models = listOf(
                VehicleModel("City", "PETROL", listOf("ZX", "VX", "V", "SV", "e:HEV ZX"), VehicleCategory.CAR),
                VehicleModel("Elevate", "PETROL", listOf("ZX", "VX", "V", "SV"), VehicleCategory.CAR),
                VehicleModel("Amaze", "PETROL", listOf("VX", "S", "E"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Volkswagen",
            popular = true,
            models = listOf(
                VehicleModel("Virtus", "PETROL", listOf("GT Plus", "GT Line", "Topline", "Highline", "Comfortline"), VehicleCategory.CAR),
                VehicleModel("Taigun", "PETROL", listOf("GT Plus Edge", "GT Plus", "GT Line", "Topline", "Highline", "Comfortline"), VehicleCategory.CAR),
                VehicleModel("Tiguan", "PETROL", listOf("Elegance"), VehicleCategory.CAR),
                VehicleModel("Polo", "PETROL", listOf("GT TSI", "Highline Plus", "Comfortline"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Skoda",
            popular = false,
            models = listOf(
                VehicleModel("Slavia", "PETROL", listOf("Monte Carlo", "Style", "Ambition", "Active"), VehicleCategory.CAR),
                VehicleModel("Kushaq", "PETROL", listOf("Monte Carlo", "Style", "Ambition", "Active"), VehicleCategory.CAR),
                VehicleModel("Kodiaq", "PETROL", listOf("L&K", "Sportline", "Style"), VehicleCategory.CAR),
                VehicleModel("Superb", "PETROL", listOf("L&K"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "MG",
            popular = false,
            models = listOf(
                VehicleModel("Hector", "PETROL", listOf("Savvy Pro", "Sharp Pro", "Smart Pro", "Shine Pro", "Style"), VehicleCategory.CAR),
                VehicleModel("Astor", "PETROL", listOf("Savvy Pro", "Sharp Pro", "Select", "Shine", "Sprint"), VehicleCategory.CAR),
                VehicleModel("ZS EV", "ELECTRIC", listOf("Exclusive Plus", "Excite Pro", "Executive"), VehicleCategory.CAR),
                VehicleModel("Comet EV", "ELECTRIC", listOf("Plush", "Play", "Pace"), VehicleCategory.CAR),
                VehicleModel("Gloster", "DIESEL", listOf("Savvy 4x4", "Savvy 4x2", "Sharp 4x2"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Renault",
            popular = false,
            models = listOf(
                VehicleModel("Kwid", "PETROL", listOf("Climber", "RXT", "RXL(O)", "RXE"), VehicleCategory.CAR),
                VehicleModel("Kiger", "PETROL", listOf("RXZ", "RXT(O)", "RXL", "RXE"), VehicleCategory.CAR),
                VehicleModel("Triber", "PETROL", listOf("RXZ", "RXT", "RXL", "RXE"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Nissan",
            popular = false,
            models = listOf(
                VehicleModel("Magnite", "PETROL", listOf("Tekna+", "Tekna", "N-Connecta", "Acenta", "Visia"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Jeep",
            popular = false,
            models = listOf(
                VehicleModel("Compass", "DIESEL", listOf("Model S", "Black Shark", "Limited", "Longitude", "Sport"), VehicleCategory.CAR),
                VehicleModel("Meridian", "DIESEL", listOf("Overland", "Limited Plus", "Longitude"), VehicleCategory.CAR),
                VehicleModel("Wrangler", "PETROL", listOf("Rubicon", "Unlimited"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "BMW",
            popular = false,
            models = listOf(
                VehicleModel("3 Series Gran Limousine", "PETROL", listOf("330Li M Sport", "320Ld M Sport"), VehicleCategory.CAR),
                VehicleModel("X1", "PETROL", listOf("sDrive18i M Sport", "sDrive18d M Sport"), VehicleCategory.CAR),
                VehicleModel("X3", "DIESEL", listOf("xDrive20d M Sport", "M40i"), VehicleCategory.CAR),
                VehicleModel("5 Series", "PETROL", listOf("530Li M Sport"), VehicleCategory.CAR),
                VehicleModel("X5", "DIESEL", listOf("xDrive30d M Sport", "xDrive40i M Sport"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Mercedes-Benz",
            popular = false,
            models = listOf(
                VehicleModel("C-Class", "PETROL", listOf("C200", "C220d", "C300 AMG Line"), VehicleCategory.CAR),
                VehicleModel("E-Class", "PETROL", listOf("E200 Exclusive", "E220d Exclusive", "E350d AMG Line"), VehicleCategory.CAR),
                VehicleModel("GLA", "PETROL", listOf("GLA 200", "GLA 220d 4MATIC AMG Line"), VehicleCategory.CAR),
                VehicleModel("GLC", "PETROL", listOf("GLC 300 4MATIC", "GLC 220d 4MATIC"), VehicleCategory.CAR),
                VehicleModel("GLE", "DIESEL", listOf("GLE 300d 4MATIC", "GLE 450d 4MATIC"), VehicleCategory.CAR)
            )
        ),
        VehicleBrand(
            name = "Audi",
            popular = false,
            models = listOf(
                VehicleModel("A4", "PETROL", listOf("Technology", "Premium Plus", "Premium"), VehicleCategory.CAR),
                VehicleModel("A6", "PETROL", listOf("Technology", "Premium Plus"), VehicleCategory.CAR),
                VehicleModel("Q3", "PETROL", listOf("Technology", "Premium Plus"), VehicleCategory.CAR),
                VehicleModel("Q5", "PETROL", listOf("Technology", "Premium Plus"), VehicleCategory.CAR),
                VehicleModel("Q7", "PETROL", listOf("Technology", "Premium Plus"), VehicleCategory.CAR)
            )
        ),

        // ================================================================= BIKES (Two-Wheelers)
        VehicleBrand(
            name = "Hero",
            popular = true,
            models = listOf(
                VehicleModel("Splendor Plus", "PETROL", listOf("XTEC", "Self Start i3S", "Black and Accent"), VehicleCategory.BIKE),
                VehicleModel("HF Deluxe", "PETROL", listOf("Self Start", "Kick Start", "i3S"), VehicleCategory.BIKE),
                VehicleModel("Glamour", "PETROL", listOf("XTEC", "Disc", "Drum"), VehicleCategory.BIKE),
                VehicleModel("Passion Plus", "PETROL", listOf("Standard", "i3S"), VehicleCategory.BIKE),
                VehicleModel("Xpulse 200 4V", "PETROL", listOf("Standard", "Pro"), VehicleCategory.BIKE),
                VehicleModel("Xtreme 160R 4V", "PETROL", listOf("Connected", "Dual Disc"), VehicleCategory.BIKE),
                VehicleModel("Destini 125", "PETROL", listOf("XTEC", "Prime", "VX"), VehicleCategory.BIKE),
                VehicleModel("Pleasure Plus", "PETROL", listOf("XTEC", "LX", "VX"), VehicleCategory.BIKE)
            )
        ),
        VehicleBrand(
            name = "Honda (2-Wheelers)",
            popular = true,
            models = listOf(
                VehicleModel("Activa 6G", "PETROL", listOf("H-Smart", "Deluxe", "Standard"), VehicleCategory.BIKE),
                VehicleModel("Shine 125", "PETROL", listOf("Disc", "Drum"), VehicleCategory.BIKE),
                VehicleModel("SP 125", "PETROL", listOf("Disc", "Drum", "Sports Edition"), VehicleCategory.BIKE),
                VehicleModel("Dio 125", "PETROL", listOf("H-Smart", "Standard"), VehicleCategory.BIKE),
                VehicleModel("Unicorn", "PETROL", listOf("Standard"), VehicleCategory.BIKE),
                VehicleModel("Hornet 2.0", "PETROL", listOf("Standard", "Repsol"), VehicleCategory.BIKE),
                VehicleModel("CB350 H'ness", "PETROL", listOf("Legacy", "DLX Pro", "DLX"), VehicleCategory.BIKE)
            )
        ),
        VehicleBrand(
            name = "TVS",
            popular = true,
            models = listOf(
                VehicleModel("Jupiter", "PETROL", listOf("110 Standard", "110 ZX", "125 Disc", "125 SmartXonnect"), VehicleCategory.BIKE),
                VehicleModel("Apache RTR 160", "PETROL", listOf("4V Special Edition", "4V Disc", "2V RM"), VehicleCategory.BIKE),
                VehicleModel("Apache RTR 200", "PETROL", listOf("4V Dual Channel ABS", "4V Single Channel"), VehicleCategory.BIKE),
                VehicleModel("Raider 125", "PETROL", listOf("SmartXonnect", "Split Seat", "Single Seat"), VehicleCategory.BIKE),
                VehicleModel("Ntorq 125", "PETROL", listOf("XT", "Race XP", "Race Edition", "Standard"), VehicleCategory.BIKE),
                VehicleModel("Sport", "PETROL", listOf("Self Start", "Kick Start"), VehicleCategory.BIKE),
                VehicleModel("XL100 Heavy Duty", "PETROL", listOf("Comfort", "i-Touch Start", "Winner Edition"), VehicleCategory.BIKE),
                VehicleModel("iQube", "ELECTRIC", listOf("ST", "S", "Standard"), VehicleCategory.BIKE)
            )
        ),
        VehicleBrand(
            name = "Bajaj",
            popular = true,
            models = listOf(
                VehicleModel("Pulsar 150", "PETROL", listOf("Twin Disc", "Single Disc"), VehicleCategory.BIKE),
                VehicleModel("Pulsar NS200", "PETROL", listOf("Dual Channel ABS", "Standard"), VehicleCategory.BIKE),
                VehicleModel("Pulsar N160", "PETROL", listOf("Dual Channel ABS", "Single Channel ABS"), VehicleCategory.BIKE),
                VehicleModel("Platina 110", "PETROL", listOf("ABS", "Drum"), VehicleCategory.BIKE),
                VehicleModel("CT 125X", "PETROL", listOf("Disc", "Drum"), VehicleCategory.BIKE),
                VehicleModel("Avenger Cruise 220", "PETROL", listOf("Cruise 220", "Street 160"), VehicleCategory.BIKE),
                VehicleModel("Chetak", "ELECTRIC", listOf("Premium", "Urbane"), VehicleCategory.BIKE)
            )
        ),
        VehicleBrand(
            name = "Royal Enfield",
            popular = true,
            models = listOf(
                VehicleModel("Classic 350", "PETROL", listOf("Chrome", "Dark", "Signals", "Halcyon", "Redditch"), VehicleCategory.BIKE),
                VehicleModel("Bullet 350", "PETROL", listOf("Black Gold", "Standard", "Military"), VehicleCategory.BIKE),
                VehicleModel("Hunter 350", "PETROL", listOf("Metro Rebel", "Metro Dapper", "Retro Factory"), VehicleCategory.BIKE),
                VehicleModel("Meteor 350", "PETROL", listOf("Supernova", "Stellar", "Fireball"), VehicleCategory.BIKE),
                VehicleModel("Himalayan 450", "PETROL", listOf("Hanle Black", "Kamet White", "Slate", "Kaza Brown"), VehicleCategory.BIKE)
            )
        ),
        VehicleBrand(
            name = "Yamaha",
            popular = false,
            models = listOf(
                VehicleModel("FZ-S FI", "PETROL", listOf("Version 4.0 Deluxe", "Version 3.0"), VehicleCategory.BIKE),
                VehicleModel("MT-15 V2", "PETROL", listOf("Deluxe", "MotoGP"), VehicleCategory.BIKE),
                VehicleModel("YZF R15 V4", "PETROL", listOf("M", "Racing Blue", "Dark Knight", "Metallic Red"), VehicleCategory.BIKE),
                VehicleModel("RayZR 125 Fi", "PETROL", listOf("Street Rally", "Hybrid Disc", "Hybrid Drum"), VehicleCategory.BIKE),
                VehicleModel("Aerox 155", "PETROL", listOf("Version S", "Standard"), VehicleCategory.BIKE)
            )
        ),
        VehicleBrand(
            name = "Suzuki (2-Wheelers)",
            popular = false,
            models = listOf(
                VehicleModel("Access 125", "PETROL", listOf("Ride Connect Edition", "Special Edition", "Standard"), VehicleCategory.BIKE),
                VehicleModel("Burgman Street 125", "PETROL", listOf("EX", "Ride Connect Edition", "Standard"), VehicleCategory.BIKE),
                VehicleModel("Gixxer 150", "PETROL", listOf("Ride Connect", "Standard"), VehicleCategory.BIKE),
                VehicleModel("Avenis 125", "PETROL", listOf("Race Edition", "Standard"), VehicleCategory.BIKE)
            )
        ),
        VehicleBrand(
            name = "Ather",
            popular = false,
            models = listOf(
                VehicleModel("450X", "ELECTRIC", listOf("3.7 kWh Pro", "2.9 kWh"), VehicleCategory.BIKE),
                VehicleModel("450S", "ELECTRIC", listOf("Standard"), VehicleCategory.BIKE),
                VehicleModel("Rizta", "ELECTRIC", listOf("Z 3.7 kWh", "Z 2.9 kWh", "S 2.9 kWh"), VehicleCategory.BIKE)
            )
        ),
        VehicleBrand(
            name = "Ola Electric",
            popular = false,
            models = listOf(
                VehicleModel("S1 Pro", "ELECTRIC", listOf("Gen 2", "Gen 1"), VehicleCategory.BIKE),
                VehicleModel("S1 X", "ELECTRIC", listOf("4 kWh", "3 kWh", "2 kWh"), VehicleCategory.BIKE),
                VehicleModel("S1 Air", "ELECTRIC", listOf("3 kWh"), VehicleCategory.BIKE)
            )
        ),

        // ================================================================= VANS & COMMERCIAL
        VehicleBrand(
            name = "Force Motors",
            popular = true,
            models = listOf(
                VehicleModel("Trax Cruiser", "DIESEL", listOf("12-Seater", "9-Seater"), VehicleCategory.VAN),
                VehicleModel("Trax Toofan", "DIESEL", listOf("11-Seater", "9-Seater"), VehicleCategory.VAN),
                VehicleModel("Urbania", "DIESEL", listOf("Medium Wheelbase", "Long Wheelbase", "Short Wheelbase"), VehicleCategory.VAN),
                VehicleModel("Traveller", "DIESEL", listOf("3050", "3350", "4020"), VehicleCategory.VAN),
                // Tow Truck
                VehicleModel("Kargo King Recovery", "DIESEL", listOf("Underlift", "Flatbed"), VehicleCategory.TOW_TRUCK)
            )
        ),

        // ================================================================= TOW TRUCKS & RECOVERY
        VehicleBrand(
            name = "Ashok Leyland",
            popular = true,
            models = listOf(
                VehicleModel("Dost Recovery Crane", "DIESEL", listOf("Underlift Boom", "Twin Boom Breakdown"), VehicleCategory.TOW_TRUCK),
                VehicleModel("Bada Dost Breakdown", "DIESEL", listOf("i3+", "i4"), VehicleCategory.TOW_TRUCK),
                VehicleModel("Partner Recovery Truck", "DIESEL", listOf("6 Tyre Flatbed", "4 Tyre Underlift"), VehicleCategory.TOW_TRUCK)
            )
        ),
        VehicleBrand(
            name = "Eicher",
            popular = false,
            models = listOf(
                VehicleModel("Pro 2049 Tow Truck", "DIESEL", listOf("Hydraulic Underlift", "Wheel Lift"), VehicleCategory.TOW_TRUCK),
                VehicleModel("Pro 3015 Flatbed Recovery", "DIESEL", listOf("Tilt & Slide Flatbed", "Heavy Tow"), VehicleCategory.TOW_TRUCK)
            )
        ),
        VehicleBrand(
            name = "Isuzu",
            popular = false,
            models = listOf(
                VehicleModel("D-Max Flatbed Recovery", "DIESEL", listOf("Single Cab Flatbed", "Commercial Tilt Tray"), VehicleCategory.TOW_TRUCK),
                VehicleModel("S-CAB Breakdown Truck", "DIESEL", listOf("High-Ride 4x2", "Standard"), VehicleCategory.TOW_TRUCK)
            )
        )
    )

    // Backward-compat: BRANDS returns all car brands
    val BRANDS: List<VehicleBrand> = ALL_BRANDS.filter { it.hasCategory(VehicleCategory.CAR) }

    fun brandsFor(category: VehicleCategory): List<VehicleBrand> =
        ALL_BRANDS.filter { it.hasCategory(category) }

    fun findBrand(name: String): VehicleBrand? =
        ALL_BRANDS.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    fun findBrandFor(category: VehicleCategory, brandName: String): VehicleBrand? =
        brandsFor(category).firstOrNull { it.name.equals(brandName.trim(), ignoreCase = true) }

    fun findModels(brandName: String): List<VehicleModel> =
        findBrand(brandName)?.models ?: emptyList()

    fun modelsFor(category: VehicleCategory, brandName: String): List<VehicleModel> =
        findBrandFor(category, brandName)?.modelsForCategory(category) ?: emptyList()

    fun findVariants(brandName: String, modelName: String): List<String> =
        findModels(brandName)
            .firstOrNull { it.name.equals(modelName.trim(), ignoreCase = true) }
            ?.variants ?: emptyList()

    fun variantsFor(category: VehicleCategory, brandName: String, modelName: String): List<String> =
        modelsFor(category, brandName)
            .firstOrNull { it.name.equals(modelName.trim(), ignoreCase = true) }
            ?.variants ?: emptyList()

    fun isCompatible(category: VehicleCategory, brandName: String, modelName: String): Boolean =
        modelsFor(category, brandName).any { it.name.equals(modelName.trim(), ignoreCase = true) }
}
