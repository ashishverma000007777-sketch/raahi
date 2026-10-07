package `in`.raahi.app.ui.screens.trip

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.TripDto
import `in`.raahi.app.network.TripStopDto
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.components.SectionLabel
import `in`.raahi.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripOverviewScreen(
    onBack: () -> Unit,
    onStartTrip: (String) -> Unit,
    viewModel: TripOverviewViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isStarting by viewModel.isStarting.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TripOverviewEvent.TripStarted -> onStartTrip(event.tripId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Overview", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, fontFamily = RaahiDisplayFont) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg)
            )
        },
        containerColor = RaahiBg
    ) { padding ->
        when (val s = state) {
            is TripOverviewUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RaahiOrange)
                }
            }
            is TripOverviewUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RaahiRed, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadTrip() }, colors = ButtonDefaults.buttonColors(containerColor = RaahiBg2)) {
                            Text("Retry", color = RaahiText)
                        }
                    }
                }
            }
            is TripOverviewUiState.Success -> {
                val trip = s.trip
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Route Header Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = RaahiOrange.copy(alpha = 0.25f),
                        background = RaahiOrange.copy(alpha = 0.05f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(trip.title, color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = RaahiDisplayFont)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Place, contentDescription = null, tint = RaahiGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(trip.startLocationName, color = RaahiTextDim, fontSize = 13.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("→", color = RaahiTextFaint, fontSize = 13.sp)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Filled.Navigation, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(trip.destLocationName, color = RaahiText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Weather Warning (Honest data rule: legitimate warning if present)
                    if (trip.weatherWarning != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RaahiShapeMedium)
                                .background(RaahiAmber.copy(alpha = 0.12f))
                                .border(1.dp, RaahiAmber.copy(alpha = 0.35f), RaahiShapeMedium)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(trip.weatherWarning, color = RaahiAmber, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    // Useful Information Grid
                    SectionLabel("Trip Estimates")
                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(
                            label = "Total Distance",
                            value = "${trip.distanceKm} km",
                            icon = Icons.Filled.Route,
                            tint = RaahiCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Estimated Time",
                            value = trip.durationFormatted,
                            icon = Icons.Filled.Schedule,
                            tint = RaahiOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val fuelCostText = if (trip.estimatedFuelCost != null) "₹${String.format(Locale.ROOT, "%.0f", trip.estimatedFuelCost)}" else "Unavailable"
                        MetricCard(
                            label = "Estimated Fuel",
                            value = if (trip.estimatedFuelLitres != null) "${trip.estimatedFuelLitres} L" else "Unavailable",
                            subtext = fuelCostText,
                            icon = Icons.Filled.LocalGasStation,
                            tint = RaahiAmber,
                            modifier = Modifier.weight(1f)
                        )
                        // Tolls: Real data rule — "Do NOT invent toll values. If unavailable show Unavailable"
                        val tollText = if (trip.estimatedTolls != null) "₹${trip.estimatedTolls}" else "Unavailable"
                        MetricCard(
                            label = "Estimated Tolls",
                            value = tollText,
                            subtext = if (trip.estimatedTolls == null) "Toll data unlinked" else "FASTag supported",
                            icon = Icons.Filled.Toll,
                            tint = if (trip.estimatedTolls != null) RaahiGreen else RaahiTextFaint,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Weather card
                    Row(Modifier.fillMaxWidth()) {
                        MetricCard(
                            label = "Route Weather",
                            value = trip.weatherCondition ?: "Unavailable",
                            subtext = if (trip.weatherCondition != null) "Open-Meteo live feed" else "Provider unavailable",
                            icon = Icons.Filled.WbSunny,
                            tint = if (trip.weatherCondition != null) RaahiCyan else RaahiTextFaint,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // C. FUEL STOPS: First 2 useful fuel stops prominently!
                    val fuelStops = trip.prominentFuelStops.orEmpty()
                    SectionLabel("Recommended Fuel Stops")
                    Spacer(Modifier.height(8.dp))

                    if (fuelStops.isEmpty()) {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Text(
                                "No fuel stations indexed on this specific route. Keep tank filled before departure.",
                                color = RaahiTextDim,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        fuelStops.take(2).forEachIndexed { index, stop ->
                            FuelStopCard(stop = stop, index = index + 1)
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // D. PIT STOPS & MECHANICS
                    val pitStops = trip.pitStops.orEmpty()
                    if (pitStops.isNotEmpty()) {
                        SectionLabel("Useful Pit Stops (Food & Rest)")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(pitStops) { stop ->
                                PitStopCard(stop)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    val mechanics = trip.mechanicsAlongRoute.orEmpty()
                    if (mechanics.isNotEmpty()) {
                        SectionLabel("Mechanics Along Route")
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(mechanics) { mech ->
                                MechanicRouteCard(mech)
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }

                    // Navigation App Handoff CTA (Honest approach per spec)
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${trip.startLat},${trip.startLng}&destination=${trip.destLat},${trip.destLng}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RaahiShapeMedium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCyan.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = RaahiCyan.copy(alpha = 0.08f))
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, tint = RaahiCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Route in Google Maps", color = RaahiCyan, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    // Start Trip CTA
                    Button(
                        onClick = { viewModel.startTrip() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RaahiShapeMedium,
                        enabled = !isStarting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(RaahiBrandGradient, RaahiShapeMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isStarting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            } else {
                                Text("Start Trip Mode →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = RaahiDisplayFont)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, subtext: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = RaahiTextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, color = RaahiText, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = RaahiDisplayFont)
            if (subtext != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtext, color = RaahiTextFaint, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FuelStopCard(stop: TripStopDto, index: Int) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = RaahiAmber.copy(alpha = 0.25f),
        background = RaahiAmber.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(RaahiAmber.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("#$index", color = RaahiAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                val distStr = if (stop.distanceKm != null) "At ~${stop.distanceKm.toInt()} km" else "On route"
                Text("$distStr · ${stop.amenities ?: "Fuel station"}", color = RaahiTextDim, fontSize = 12.sp)
            }
            if (stop.pricePerLitre != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${String.format(Locale.ROOT, "%.2f", stop.pricePerLitre)}/L", color = RaahiGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (stop.priceDiffPerLitre != null && stop.priceDiffPerLitre > 0) {
                        Text("₹${String.format(Locale.ROOT, "%.2f", stop.priceDiffPerLitre)} cheaper", color = RaahiCyan, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PitStopCard(stop: TripStopDto) {
    GlassCard(modifier = Modifier.width(190.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Restaurant, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pit Stop", color = RaahiOrange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(stop.name, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(stop.amenities ?: "Food & Rest", color = RaahiTextDim, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun MechanicRouteCard(mech: TripStopDto) {
    GlassCard(modifier = Modifier.width(200.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Build, contentDescription = null, tint = RaahiGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Verified Mechanic", color = RaahiGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(mech.name, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(mech.amenities ?: "Roadside repair", color = RaahiTextDim, fontSize = 11.sp, maxLines = 1)
        }
    }
}
