package `in`.raahi.app.ui.screens.jobs

data class ProblemType(
    val id: String,
    val label: String,
    val emoji: String,
    val suggestedReward: Int
)

val PROBLEM_TYPES = listOf(
    ProblemType("puncture", "Flat Tyre", "\uD83D\uDEE2\uFE0F", 150),
    ProblemType("battery", "Battery Issue", "\uD83D\uDD0B", 200),
    ProblemType("breakdown", "Engine Problem", "\u2699\uFE0F", 300),
    ProblemType("fuel", "Fuel Delivery", "\u26FD", 200),
    ProblemType("towing", "Towing", "\uD83D\uDE9B", 400),
    ProblemType("other", "Other", "\u2753", 100),
)

val QUICK_PRICES = listOf(50, 100, 150, 200, 300, 500)
