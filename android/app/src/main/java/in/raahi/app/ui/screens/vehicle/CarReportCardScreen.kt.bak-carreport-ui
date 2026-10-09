package `in`.raahi.app.ui.screens.vehicle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.network.CarHealthDto
import `in`.raahi.app.network.MaintenanceItemDto
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.network.WeeklyReportDto
import `in`.raahi.app.network.YearEndReportDto
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.components.SectionLabel
import `in`.raahi.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarReportCardScreen(
    onBack: () -> Unit,
    onSetupVehicle: () -> Unit,
    onEditVehicle: () -> Unit,
    onServiceHistory: () -> Unit,
    onAddFuel: () -> Unit,
    onPlanTrip: () -> Unit,
    viewModel: CarHealthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val odometerError by viewModel.odometerUpdateError.collectAsState()
    var showOdometerDialog by remember { mutableStateOf(false) }

    // Hoisted so the artwork layer can follow the report's scroll position.
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Box(Modifier.fillMaxSize().background(RaahiBg)) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gaadi ka Report Card", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = RaahiDisplayFont)
                        Text("Vehicle Intelligence & Diagnostics", color = RaahiTextDim, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is CarHealthUiState.Loading -> {
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
                            Text("Loading Vehicle Diagnostics...", color = RaahiText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Retrieving telemetry, service intervals and vehicle health", color = RaahiTextDim, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
                is CarHealthUiState.Error -> {
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
                            Text("Vehicle Data Unavailable", color = RaahiText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                is CarHealthUiState.Loaded -> {
                    val vehicle = s.vehicle
                    val health = s.health
                    if (vehicle == null) {
                        NoVehicleContent(onSetupVehicle)
                    } else {
                        CarReportCardContent(
                            vehicle = vehicle,
                            health = health,
                            onEditVehicle = onEditVehicle,
                            onUpdateOdometer = { showOdometerDialog = true },
                            onServiceHistory = onServiceHistory,
                            onAddFuel = onAddFuel,
                            onPlanTrip = onPlanTrip,
                            listState = listState,
                        )
                    }
                }
            }
        }
    }
    }

    if (showOdometerDialog) {
        OdometerUpdateDialog(
            errorMessage = odometerError,
            onDismiss = {
                showOdometerDialog = false
                viewModel.clearOdometerError()
            },
            onSave = { km ->
                viewModel.updateOdometer(km) { showOdometerDialog = false }
            }
        )
    }
}

@Composable
fun CarReportCardContent(
    vehicle: VehicleDto,
    health: CarHealthDto?,
    onEditVehicle: () -> Unit,
    onUpdateOdometer: () -> Unit,
    onServiceHistory: () -> Unit,
    onAddFuel: () -> Unit,
    onPlanTrip: () -> Unit,
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { Spacer(Modifier.height(56.dp)) }

        // 1. Vehicle Identity Card
        item {
            VehicleIdentityCard(
                vehicle = vehicle,
                onEdit = onEditVehicle,
                onUpdateOdometer = onUpdateOdometer
            )
        }

        // 2. Report Card Hero (Score / 100, Grade, Weekly Trend)
        item {
            ReportCardHero(health)
        }

        // 3. Radar Triad Visualizer (Care, Efficiency, Safety)
        item {
            ReportCardRadarCard(health)
        }

        // 4. Sub-scores Triad Breakdown
        item {
            SectionLabel("Report Card Breakdown")
        }
        item {
            SubScoresTriad(
                health = health,
                onAddFuel = onAddFuel,
                onPlanTrip = onPlanTrip,
                onServiceHistory = onServiceHistory
            )
        }

        // 5. Weekly Report ("Your car this week")
        if (health?.weeklyReport != null) {
            item {
                SectionLabel("Weekly Insights")
            }
            item {
                WeeklyReportCard(health.weeklyReport)
            }
        }

        // 6. Year-End Report
        if (health?.yearEndReport != null) {
            item {
                SectionLabel("Annual Summary")
            }
            item {
                YearEndReportCard(health.yearEndReport)
            }
        }

        // 7. Maintenance & Quick Diagnostics
        item {
            SectionLabel(
                text = "Quick Diagnostics",
                trailing = "All Records",
                onTrailingClick = onServiceHistory
            )
        }
        val items = health?.maintenanceItems.orEmpty()
        if (items.isEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Add service, insurance or PUC dates to view active diagnostics.",
                        color = RaahiTextDim,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(items) { item: MaintenanceItemDto ->
                MaintenanceStatusRow(item)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun VehicleIdentityCard(
    vehicle: VehicleDto,
    onEdit: () -> Unit,
    onUpdateOdometer: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = listOfNotNull(vehicle.brand, vehicle.model, vehicle.variant).joinToString(" "),
                        color = RaahiText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        fontFamily = RaahiDisplayFont
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${vehicle.modelYear}", color = RaahiTextDim, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.background(RaahiCyan.copy(alpha = 0.16f), RaahiShapeSmall)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                vehicle.fuelType.lowercase().replaceFirstChar { it.uppercase() },
                                color = RaahiCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(vehicle.registrationNumber, color = RaahiTextDim, fontSize = 12.sp)
                    }
                }
                Box(
                    modifier = Modifier.size(48.dp).background(RaahiOrange.copy(alpha = 0.12f), RaahiShapeMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = RaahiBorderSoft)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Speed, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${vehicle.odometerKm} km",
                    color = RaahiText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    fontFamily = RaahiDisplayFont
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Update odometer",
                    color = RaahiOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onUpdateOdometer)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Edit vehicle details",
                color = RaahiCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }
    }
}

@Composable
private fun ReportCardHero(health: CarHealthDto?) {
    val score = health?.score
    if (score == null) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(10.dp))
                Text("Report Card Incomplete", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    health?.message ?: "Complete your vehicle information (service, insurance, fuel) to calculate your report card.",
                    color = RaahiTextDim,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val grade = health.grade ?: when {
        score >= 90 -> "A+"
        score >= 80 -> "A"
        score >= 70 -> "B+"
        score >= 60 -> "B"
        score >= 50 -> "C"
        else -> "D"
    }

    val primaryColor = when {
        score >= 80 -> RaahiGreen
        score >= 60 -> RaahiCyan
        score >= 40 -> RaahiAmber
        else -> RaahiRed
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        background = primaryColor.copy(alpha = 0.08f),
        borderColor = primaryColor.copy(alpha = 0.25f)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Gaadi ka Report Card", color = RaahiTextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("Overall Vehicle Health", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = RaahiDisplayFont)
                }

                // Grade Badge
                Box(
                    modifier = Modifier
                        .background(primaryColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Grade $grade",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = RaahiDisplayFont
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circular Score Display
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                    val animatedScore by animateFloatAsState(targetValue = score / 100f, tween(1000), label = "scoreAnim")
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 8.dp.toPx()
                        drawArc(
                            color = primaryColor.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedScore,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$score", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 28.sp, fontFamily = RaahiDisplayFont)
                        Text("/ 100", color = RaahiTextFaint, fontSize = 10.sp)
                    }
                }

                Spacer(Modifier.width(20.dp))

                Column(Modifier.weight(1f)) {
                    // Weekly Trend
                    val trend = health.weeklyTrend ?: 0
                    val trendText = health.weeklyTrendText ?: "Stable this week"
                    val trendColor = if (trend > 0) RaahiGreen else if (trend < 0) RaahiRed else RaahiCyan

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(trendColor.copy(alpha = 0.14f), RaahiShapePill)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (trend > 0) Icons.Default.TrendingUp else if (trend < 0) Icons.Default.TrendingDown else Icons.Default.TrendingFlat,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(trendText, color = trendColor, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (score >= 80) "Vehicle is in prime operational condition."
                        else if (score >= 60) "Vehicle is in fair condition with minor actions due."
                        else "Vehicle requires scheduled maintenance and document renewals.",
                        color = RaahiTextDim,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportCardRadarCard(health: CarHealthDto?) {
    val care = (health?.careScore ?: 70).toFloat()
    val eff = (health?.efficiencyScore ?: 60).toFloat()
    val safety = (health?.safetyScore ?: 50).toFloat()
    val hasSafety = health?.safetyScore != null

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Performance Balance", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
                Text("Triad View", color = RaahiCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))

            // Radar visualization canvas
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width * 0.42f

                    // Draw outer guide triangle
                    val p1 = Offset(center.x, center.y - radius) // Care (Top)
                    val p2 = Offset(center.x + radius * cos(Math.toRadians(30.0)).toFloat(), center.y + radius * sin(Math.toRadians(30.0)).toFloat()) // Efficiency (Bottom-Right)
                    val p3 = Offset(center.x - radius * cos(Math.toRadians(30.0)).toFloat(), center.y + radius * sin(Math.toRadians(30.0)).toFloat()) // Safety (Bottom-Left)

                    val outerPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    }
                    drawPath(outerPath, color = Color.White.copy(alpha = 0.08f), style = Stroke(width = 1.5.dp.toPx()))

                    // Draw axes from center
                    drawLine(Color.White.copy(alpha = 0.12f), center, p1, strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.12f), center, p2, strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.12f), center, p3, strokeWidth = 1.dp.toPx())

                    // Calculate value points
                    val v1 = Offset(center.x, center.y - (radius * (care / 100f)))
                    val v2 = Offset(center.x + (radius * (eff / 100f)) * cos(Math.toRadians(30.0)).toFloat(), center.y + (radius * (eff / 100f)) * sin(Math.toRadians(30.0)).toFloat())
                    val v3 = Offset(center.x - (radius * (safety / 100f)) * cos(Math.toRadians(30.0)).toFloat(), center.y + (radius * (safety / 100f)) * sin(Math.toRadians(30.0)).toFloat())

                    val dataPath = Path().apply {
                        moveTo(v1.x, v1.y)
                        lineTo(v2.x, v2.y)
                        lineTo(v3.x, v3.y)
                        close()
                    }

                    // Filled radar polygon
                    drawPath(dataPath, color = RaahiCyan.copy(alpha = 0.28f))
                    drawPath(dataPath, color = RaahiCyan, style = Stroke(width = 2.dp.toPx()))

                    // Vertex dots
                    drawCircle(RaahiGreen, radius = 4.dp.toPx(), center = v1)
                    drawCircle(RaahiCyan, radius = 4.dp.toPx(), center = v2)
                    drawCircle(if (hasSafety) RaahiVioletAccent else Color.Gray, radius = 4.dp.toPx(), center = v3)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Legend Row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RadarLegendItem(color = RaahiGreen, label = "Care", score = health?.careScore?.let { "$it" } ?: "--")
                RadarLegendItem(color = RaahiCyan, label = "Efficiency", score = health?.efficiencyScore?.let { "$it" } ?: "--")
                RadarLegendItem(color = if (hasSafety) RaahiVioletAccent else RaahiTextDim, label = "Safety", score = health?.safetyScore?.let { "$it" } ?: "Pending")
            }
        }
    }
}

@Composable
private fun RadarLegendItem(color: Color, label: String, score: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, color = RaahiTextDim, fontSize = 10.5.sp)
            Text(score, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SubScoresTriad(
    health: CarHealthDto?,
    onAddFuel: () -> Unit,
    onPlanTrip: () -> Unit,
    onServiceHistory: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Care Sub-Score
        SubScoreDetailCard(
            icon = Icons.Outlined.Build,
            iconTint = RaahiGreen,
            title = "Care & Maintenance",
            score = health?.careScore,
            grade = health?.careGrade,
            summary = health?.careSummary ?: "Service, Insurance, and PUC health factors",
            detailText = "Tracked ${health?.careFactorsConsidered ?: 0} of ${health?.careFactorsTotal ?: 5} factors",
            actionLabel = "Service Log",
            onAction = onServiceHistory
        )

        // 2. Efficiency Sub-Score
        SubScoreDetailCard(
            icon = Icons.Outlined.LocalGasStation,
            iconTint = RaahiCyan,
            title = "Fuel & Mileage Efficiency",
            score = health?.efficiencyScore,
            grade = health?.efficiencyGrade,
            summary = health?.efficiencySummary ?: "Real-world fuel consumption and km/L tracking",
            detailText = health?.efficiencyTrendText ?: "Log refills with odometer reading to calculate mileage",
            actionLabel = "Log Refill",
            onAction = onAddFuel
        )

        // 3. Safety Sub-Score (Honest rule: never fake)
        val safetyScore = health?.safetyScore
        SubScoreDetailCard(
            icon = Icons.Outlined.Shield,
            iconTint = if (safetyScore != null) RaahiVioletAccent else RaahiTextDim,
            title = "Driving Safety Score",
            score = safetyScore,
            grade = health?.safetyGrade,
            summary = health?.safetySummary ?: "Driving safety measured from completed highway journeys",
            detailText = health?.safetyStatusText ?: "Not enough driving data",
            actionLabel = "Start Trip Mode",
            onAction = onPlanTrip
        )
    }
}

@Composable
private fun SubScoreDetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    score: Int?,
    grade: String?,
    summary: String,
    detailText: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).background(iconTint.copy(alpha = 0.14f), RaahiShapeMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(detailText, color = if (score != null) RaahiCyan else RaahiAmber, fontSize = 11.5.sp)
                }

                if (score != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$score", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = RaahiDisplayFont)
                        if (grade != null) {
                            Text("Grade $grade", color = iconTint, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Box(
                        Modifier.background(RaahiBorderSoft, RaahiShapeSmall).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Unavailable", color = RaahiTextDim, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(summary, color = RaahiTextDim, fontSize = 12.sp, lineHeight = 16.sp)

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = RaahiBorderSoft)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onAction),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(actionLabel, color = RaahiOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun WeeklyReportCard(report: WeeklyReportDto) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your car this week", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
                Box(Modifier.background(RaahiGreen.copy(alpha = 0.15f), RaahiShapeSmall).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("7 Days", color = RaahiGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WeeklyStatItem("Distance", "${report.distanceDrivenKm ?: 0} km", Icons.Outlined.Navigation, RaahiOrange)
                WeeklyStatItem("Fuel Spent", report.fuelSpent?.let { "₹${it.toInt()}" } ?: "--", Icons.Outlined.LocalGasStation, RaahiCyan)
                WeeklyStatItem("Trips", "${report.tripsCompleted ?: 0}", Icons.Outlined.Route, RaahiGreen)
            }

            if (report.highlights.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = RaahiBorderSoft)
                Spacer(Modifier.height(10.dp))
                report.highlights.forEach { bullet ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Text("• ", color = RaahiCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(bullet, color = RaahiTextDim, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyStatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(34.dp).background(color.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, fontFamily = RaahiDisplayFont)
        Text(label, color = RaahiTextDim, fontSize = 10.5.sp)
    }
}

@Composable
private fun YearEndReportCard(report: YearEndReportDto) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${report.year} Annual Overview", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
                Text("${report.totalTrips ?: 0} journeys", color = RaahiTextDim, fontSize = 11.5.sp)
            }
            Spacer(Modifier.height(12.dp))

            report.highlights.forEach { h ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = RaahiGreen, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(h, color = RaahiTextDim, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun MaintenanceStatusRow(item: MaintenanceItemDto) {
    val statusColor = when (item.status) {
        "OVERDUE" -> RaahiRed
        "DUE_SOON" -> RaahiAmber
        else -> RaahiGreen
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.type, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(2.dp))
                Text(item.detail, color = RaahiTextDim, fontSize = 11.5.sp)
            }
            Box(
                Modifier.background(statusColor.copy(alpha = 0.14f), RaahiShapeSmall)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(item.status.replace("_", " "), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NoVehicleContent(onSetupVehicle: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

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
                    .background(RaahiOrange.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("No Vehicle Linked", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = RaahiDisplayFont)
            Spacer(Modifier.height(6.dp))
            Text(
                "Add your car to unlock Gaadi ka Report Card, maintenance diagnostics, and real-time health telemetry.",
                color = RaahiTextDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onSetupVehicle,
                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                shape = RaahiShapeMedium
            ) {
                Text("Set up your vehicle", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OdometerUpdateDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var kmText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Current Odometer", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                Text("Enter the current odometer reading shown on your vehicle's instrument cluster:", color = RaahiTextDim, fontSize = 12.5.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = kmText,
                    onValueChange = { kmText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Odometer Reading (km)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = RaahiText,
                        unfocusedTextColor = RaahiText,
                        focusedBorderColor = RaahiOrange,
                        unfocusedBorderColor = RaahiBorderSoft
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMessage, color = RaahiRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val km = kmText.toIntOrNull()
                    if (km != null && km >= 0) onSave(km)
                },
                enabled = kmText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)
            ) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = RaahiTextDim)
            }
        },
        containerColor = RaahiCardBg
    )
}
