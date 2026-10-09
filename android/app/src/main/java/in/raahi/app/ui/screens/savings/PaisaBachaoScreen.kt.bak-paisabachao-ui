package `in`.raahi.app.ui.screens.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.*
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.components.SectionLabel
import `in`.raahi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaisaBachaoScreen(
    onBack: () -> Unit,
    onAddFuel: () -> Unit,
    onFuelRates: () -> Unit,
    onServiceHistory: () -> Unit,
    onPlanTrip: () -> Unit,
    viewModel: SavingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Paisa Bachao",
                            color = RaahiText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = RaahiDisplayFont
                        )
                        Text(
                            text = "Measurable Fuel & Care Savings",
                            color = RaahiTextDim,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg)
            )
        },
        containerColor = RaahiBg
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is SavingsUiState.Loading -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = RaahiOrange, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Calculating Vehicle Savings...", color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Analyzing fuel economy, trips and maintenance spend", color = RaahiTextDim, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
                is SavingsUiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
                                .background(Color.White, RoundedCornerShape(20.dp))
                                .border(1.dp, RaahiBorderSoft, RoundedCornerShape(20.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(RaahiRed.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Unable to Load Savings", color = RaahiText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(s.message, color = RaahiTextDim, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::load,
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                                shape = RaahiShapeMedium
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is SavingsUiState.Loaded -> {
                    PaisaBachaoContent(
                        savings = s.savings,
                        onAddFuel = onAddFuel,
                        onFuelRates = onFuelRates,
                        onServiceHistory = onServiceHistory,
                        onPlanTrip = onPlanTrip,
                    )
                }
            }
        }
    }
}

@Composable
fun PaisaBachaoContent(
    savings: SavingsSummaryDto,
    onAddFuel: () -> Unit,
    onFuelRates: () -> Unit,
    onServiceHistory: () -> Unit,
    onPlanTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
        }

        // 1. Savings Hero Banner
        item {
            SavingsHeroCard(savings)
        }

        // 2. Month-over-Month Fuel Comparison
        item {
            SectionLabel("Monthly Fuel Comparison")
        }
        item {
            MonthlySpendComparisonCard(savings = savings, onAddFuel = onAddFuel)
        }

        // 3. Mileage & Efficiency Trend
        item {
            SectionLabel("Mileage & Efficiency Trend")
        }
        item {
            MileageTrendCard(savings = savings, onAddFuel = onAddFuel)
        }

        // 4. Result-based Streaks
        item {
            SectionLabel("Result-Based Streaks", trailing = "Active Badges")
        }
        item {
            StreaksSection(streaks = savings.streaks)
        }

        // 5. Fuel Price Radar
        if (savings.fuelPriceRadar != null) {
            item {
                SectionLabel("Fuel Price Radar", trailing = "Compare States", onTrailingClick = onFuelRates)
            }
            item {
                FuelPriceRadarCard(radar = savings.fuelPriceRadar, onFuelRates = onFuelRates)
            }
        }

        // 6. Maintenance Spending
        item {
            SectionLabel("Maintenance Spend", trailing = "History", onTrailingClick = onServiceHistory)
        }
        item {
            MaintenanceSpendCard(savings = savings, onServiceHistory = onServiceHistory)
        }

        // 7. Anonymized Community Benchmarking
        if (savings.communityBenchmark != null) {
            item {
                SectionLabel("Community Benchmark")
            }
            item {
                CommunityBenchmarkCard(benchmark = savings.communityBenchmark)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SavingsHeroCard(savings: SavingsSummaryDto) {
    val hasMoM = savings.hasMoMComparison
    val savingsAmount = savings.fuelSpendSavings

    val cardColor = if (hasMoM && savingsAmount != null && savingsAmount > 0) RaahiGreen
    else if (hasMoM && savingsAmount != null && savingsAmount < 0) RaahiAmber
    else RaahiCyan

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        background = cardColor.copy(alpha = 0.10f),
        borderColor = cardColor.copy(alpha = 0.28f)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gaadi Ki Bachat",
                    color = RaahiTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .background(cardColor.copy(alpha = 0.16f), RaahiShapePill)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (hasMoM && savingsAmount != null && savingsAmount > 0) "Bachat Active" else "Monthly Tracker",
                        color = cardColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (hasMoM && savingsAmount != null && savingsAmount > 0) {
                Text(
                    text = "₹${savingsAmount.toInt()}",
                    color = RaahiGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    fontFamily = RaahiDisplayFont
                )
                Text(
                    text = "Saved this month compared to previous month",
                    color = RaahiText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            } else if (hasMoM && savingsAmount != null && savingsAmount < 0) {
                Text(
                    text = "₹${(savings.currentMonthFuelSpend ?: 0.0).toInt()}",
                    color = RaahiText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    fontFamily = RaahiDisplayFont
                )
                Text(
                    text = "Current month fuel spend (₹${(-savingsAmount).toInt()} higher than last month)",
                    color = RaahiTextDim,
                    fontSize = 12.5.sp
                )
            } else {
                Text(
                    text = "₹${(savings.currentMonthFuelSpend ?: 0.0).toInt()}",
                    color = RaahiCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    fontFamily = RaahiDisplayFont
                )
                Text(
                    text = "Total recorded fuel spend this month",
                    color = RaahiText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = RaahiBorderSoft)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasMoM) Icons.Outlined.CheckCircle else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = cardColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = savings.fuelSpendSavingsMessage ?: "Log refills to track financial savings.",
                    color = RaahiTextDim,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun MonthlySpendComparisonCard(savings: SavingsSummaryDto, onAddFuel: () -> Unit) {
    val currentSpend = savings.currentMonthFuelSpend ?: 0.0
    val prevSpend = savings.previousMonthFuelSpend

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This Month vs Last Month",
                    color = RaahiText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = RaahiDisplayFont
                )
                Text(
                    text = "+ Log Refill",
                    color = RaahiOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onAddFuel)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Current Month Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("This Month", color = RaahiTextDim, fontSize = 12.sp)
                    Text("₹${currentSpend.toInt()}", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = RaahiDisplayFont)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(RaahiCyan)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Previous Month Bar
            if (prevSpend != null) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Previous Month", color = RaahiTextDim, fontSize = 12.sp)
                        Text("₹${prevSpend.toInt()}", color = RaahiTextDim, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = RaahiDisplayFont)
                    }
                    Spacer(Modifier.height(4.dp))
                    val ratio = if (currentSpend + prevSpend > 0) (prevSpend / (currentSpend + prevSpend)).toFloat().coerceIn(0.1f, 1f) else 0.5f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(RaahiBorderSoft.copy(alpha = 0.5f))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RaahiBorderSoft.copy(alpha = 0.25f), RaahiShapeSmall)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "No recorded fuel refills in previous month. Month-over-month comparison will appear automatically once two months are logged.",
                        color = RaahiTextDim,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            if (savings.currentMonthLitres != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = RaahiBorderSoft)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volume Filled This Month", color = RaahiTextDim, fontSize = 12.sp)
                    Text("${savings.currentMonthLitres} Litres", color = RaahiCyan, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
        }
    }
}

@Composable
private fun MileageTrendCard(savings: SavingsSummaryDto, onAddFuel: () -> Unit) {
    val currMileage = savings.currentMonthMileageKmPerLitre
    val trendDiff = savings.mileageTrendKmPerLitre

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(RaahiCyan.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Speed, contentDescription = null, tint = RaahiCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Recorded Mileage", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
                        Text(
                            text = if (currMileage != null) "$currMileage km/L" else "Unavailable",
                            color = if (currMileage != null) RaahiCyan else RaahiTextDim,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = RaahiDisplayFont
                        )
                    }
                }

                if (trendDiff != null) {
                    val trendColor = if (trendDiff > 0) RaahiGreen else if (trendDiff < 0) RaahiRed else RaahiCyan
                    Box(
                        modifier = Modifier.background(trendColor.copy(alpha = 0.16f), RaahiShapePill).padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (trendDiff > 0) "+$trendDiff km/L" else "$trendDiff km/L",
                            color = trendColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = savings.mileageTrendText ?: "Log 2+ refills with odometer readings to calculate real-world mileage.",
                color = RaahiTextDim,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (currMileage == null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onAddFuel,
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiCyan.copy(alpha = 0.15f), contentColor = RaahiCyan),
                    shape = RaahiShapeSmall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Refill with Odometer Reading", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StreaksSection(streaks: List<StreakBadgeDto>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        streaks.forEach { streak ->
            StreakBadgeTile(streak)
        }
    }
}

@Composable
private fun StreakBadgeTile(streak: StreakBadgeDto) {
    val isAchieved = streak.isAchieved
    val accent = if (isAchieved) RaahiGreen else RaahiOrange

    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).background(accent.copy(alpha = 0.14f), RaahiShapeMedium),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (streak.icon) {
                    "FUEL" -> Icons.Outlined.LocalGasStation
                    "MILEAGE" -> Icons.Outlined.Speed
                    else -> Icons.Outlined.Verified
                }
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(streak.title, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    Spacer(Modifier.width(6.dp))
                    if (isAchieved) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RaahiGreen, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(streak.badgeText, color = accent, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(streak.progressText, color = RaahiTextDim, fontSize = 11.sp)
            }

            Box(
                modifier = Modifier
                    .background(if (isAchieved) RaahiGreen.copy(alpha = 0.15f) else RaahiBorderSoft, RaahiShapePill)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isAchieved) "Achieved" else "In Progress",
                    color = if (isAchieved) RaahiGreen else RaahiTextDim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FuelPriceRadarCard(radar: FuelPriceRadarDto, onFuelRates: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Radar, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Fuel Price Radar", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
                }

                if (radar.priceDifferencePerLitre != null && radar.priceDifferencePerLitre > 0) {
                    Box(Modifier.background(RaahiGreen.copy(alpha = 0.16f), RaahiShapePill).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("Save ₹${radar.priceDifferencePerLitre}/L", color = RaahiGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(radar.radarAdvice ?: "Compare state fuel prices to save on highway trips.", color = RaahiTextDim, fontSize = 12.sp, lineHeight = 16.sp)

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = RaahiBorderSoft)
            Spacer(Modifier.height(10.dp))

            // State comparison preview
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Home (${radar.homeState ?: "Delhi"})", color = RaahiTextDim, fontSize = 11.sp)
                    Text(radar.homeStatePetrol?.let { "₹$it/L" } ?: "--", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Cheapest (${radar.cheapestNearbyState ?: "State"})", color = RaahiGreen, fontSize = 11.sp)
                    Text(radar.cheapestStatePetrol?.let { "₹$it/L" } ?: "--", color = RaahiGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Border Difference", color = RaahiTextDim, fontSize = 11.sp)
                    Text(radar.priceDifferencePerLitre?.let { "-₹$it/L" } ?: "0.00", color = RaahiCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MaintenanceSpendCard(savings: SavingsSummaryDto, onServiceHistory: () -> Unit) {
    val currentYearSpend = savings.currentYearMaintenanceSpend ?: 0.0
    val totalSpend = savings.totalMaintenanceSpend ?: 0.0

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Maintenance & Service Spending", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
                Text("${savings.maintenanceRecordsCount} records", color = RaahiTextDim, fontSize = 11.5.sp)
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("2026 Year-to-Date", color = RaahiTextDim, fontSize = 11.5.sp)
                    Text("₹${currentYearSpend.toInt()}", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = RaahiDisplayFont)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Lifetime Recorded", color = RaahiTextDim, fontSize = 11.5.sp)
                    Text("₹${totalSpend.toInt()}", color = RaahiCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = RaahiDisplayFont)
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = RaahiBorderSoft)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onServiceHistory),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View Detailed Service Invoices", color = RaahiOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun CommunityBenchmarkCard(benchmark: CommunityBenchmarkDto) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Peer Comparison",
                    color = RaahiText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = RaahiDisplayFont
                )
                Box(
                    modifier = Modifier
                        .background(if (benchmark.available) RaahiCyan.copy(alpha = 0.15f) else RaahiBorderSoft, RaahiShapePill)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (benchmark.available) "Active" else "Awaiting Data",
                        color = if (benchmark.available) RaahiCyan else RaahiTextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = benchmark.comparisonSummary ?: "Community benchmarking compares real-world driving efficiency across similar vehicles.",
                color = if (benchmark.available) RaahiText else RaahiTextDim,
                fontSize = 12.5.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = benchmark.note ?: "",
                color = RaahiTextFaint,
                fontSize = 11.sp
            )
        }
    }
}
