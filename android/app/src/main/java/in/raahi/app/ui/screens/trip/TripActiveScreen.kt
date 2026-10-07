package `in`.raahi.app.ui.screens.trip

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.TripStopDto
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.components.SectionLabel
import `in`.raahi.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripActiveScreen(
    onBack: () -> Unit,
    onTripCompleted: (String) -> Unit,
    onSos: () -> Unit,
    onNearbyMechanics: () -> Unit,
    viewModel: TripActiveViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val isCompleting by viewModel.isCompleting.collectAsState()

    var showCompleteDialog by remember { mutableStateOf(false) }
    var endOdometer by remember { mutableStateOf("") }
    var fuelCostInput by remember { mutableStateOf("") }
    var fuelLitresInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TripActiveEvent.TripCompleted -> onTripCompleted(event.tripId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RaahiGreen)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Trip Mode Active", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, fontFamily = RaahiDisplayFont)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                },
                actions = {
                    TextButton(onClick = { showCompleteDialog = true }) {
                        Text("End Trip", color = RaahiRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg)
            )
        },
        containerColor = RaahiBg
    ) { padding ->
        when (val s = state) {
            is TripActiveUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RaahiOrange)
                }
            }
            is TripActiveUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = RaahiRed, fontSize = 14.sp)
                }
            }
            is TripActiveUiState.Active -> {
                val trip = s.trip
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Progress Indicator
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Route Progress", color = RaahiTextDim, fontSize = 12.sp)
                            Text("${(s.progressPercent * 100).toInt()}%", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { s.progressPercent },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = RaahiOrange,
                            trackColor = RaahiGlassStrong,
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // Hero Remaining Distance & ETA Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = RaahiOrange.copy(alpha = 0.35f),
                        background = RaahiBg2
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(trip.destLocationName, color = RaahiTextDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${s.remainingKm} km",
                                color = RaahiText,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RaahiDisplayFont
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, tint = RaahiCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ETA ~ ${s.etaFormatted}", color = RaahiCyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Next Useful Stop Card
                    val next = s.nextStop
                    if (next != null) {
                        SectionLabel("Next Useful Stop")
                        Spacer(Modifier.height(8.dp))
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderColor = RaahiAmber.copy(alpha = 0.3f),
                            background = RaahiAmber.copy(alpha = 0.05f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(RaahiAmber.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when (next.stopType) {
                                        "FUEL" -> Icons.Filled.LocalGasStation
                                        "MECHANIC" -> Icons.Filled.Build
                                        else -> Icons.Filled.Restaurant
                                    }
                                    Icon(icon, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(next.name, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    val sub = if (next.distanceKm != null) "In ~${next.distanceKm.toInt()} km" else "Coming up next"
                                    Text("$sub · ${next.amenities ?: "Stop"}", color = RaahiTextDim, fontSize = 12.sp)
                                }
                                if (next.pricePerLitre != null) {
                                    Text("₹${String.format(Locale.ROOT, "%.2f", next.pricePerLitre)}/L", color = RaahiGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Fuel & Weather Status
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Fuel Status", color = RaahiTextDim, fontSize = 11.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(trip.fuelType ?: "Petrol", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val fuelEst = if (trip.estimatedFuelLitres != null) "~${trip.estimatedFuelLitres}L total" else "Normal"
                                Text(fuelEst, color = RaahiTextFaint, fontSize = 11.sp)
                            }
                        }

                        GlassCard(modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.WbSunny, contentDescription = null, tint = RaahiCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Live Weather", color = RaahiTextDim, fontSize = 11.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(trip.weatherCondition ?: "Unavailable", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text(if (trip.weatherCondition != null) "Open-Meteo" else "Offline", color = RaahiTextFaint, fontSize = 11.sp)
                            }
                        }
                    }

                    if (trip.weatherWarning != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RaahiShapeMedium)
                                .background(RaahiAmber.copy(alpha = 0.12f))
                                .border(1.dp, RaahiAmber.copy(alpha = 0.35f), RaahiShapeMedium)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = RaahiAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(trip.weatherWarning, color = RaahiAmber, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Emergency & Safety Shortcuts
                    SectionLabel("Highway Assistance")
                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Emergency SOS
                        Button(
                            onClick = onSos,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RaahiShapeMedium,
                            colors = ButtonDefaults.buttonColors(containerColor = RaahiRed.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = RaahiRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SOS Help", color = RaahiRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Nearby Mechanic
                        Button(
                            onClick = onNearbyMechanics,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RaahiShapeMedium,
                            colors = ButtonDefaults.buttonColors(containerColor = RaahiGreen.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Filled.Build, contentDescription = null, tint = RaahiGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mechanics", color = RaahiGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Launch Navigation App Handoff
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${trip.startLat},${trip.startLng}&destination=${trip.destLat},${trip.destLng}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RaahiShapeMedium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCyan.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = RaahiCyan.copy(alpha = 0.06f))
                    ) {
                        Icon(Icons.Filled.Navigation, contentDescription = null, tint = RaahiCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Switch to Google Maps Navigation", color = RaahiCyan, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    // Finish Trip Button
                    Button(
                        onClick = { showCompleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RaahiShapeMedium,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(RaahiBrandGradient, RaahiShapeMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Complete Trip & View Story →", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = RaahiDisplayFont)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    // Complete Trip Confirmation & End Log Dialog
    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            containerColor = RaahiBg2,
            title = { Text("Complete Your Trip", color = RaahiText, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Have you reached your destination? You can optionally record ending odometer and fuel spent to keep your vehicle history accurate.", color = RaahiTextDim, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = endOdometer,
                        onValueChange = { endOdometer = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Current Odometer (km)", color = RaahiTextDim) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RaahiText,
                            unfocusedTextColor = RaahiText,
                            focusedBorderColor = RaahiOrange,
                            unfocusedBorderColor = RaahiBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = fuelCostInput,
                        onValueChange = { fuelCostInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Fuel Spent on Trip (₹ optional)", color = RaahiTextDim) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RaahiText,
                            unfocusedTextColor = RaahiText,
                            focusedBorderColor = RaahiOrange,
                            unfocusedBorderColor = RaahiBorder
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteDialog = false
                        viewModel.completeTrip(
                            endOdo = endOdometer.toIntOrNull(),
                            fuelCost = fuelCostInput.toDoubleOrNull(),
                            fuelLitres = fuelLitresInput.toDoubleOrNull()
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)
                ) {
                    Text("Finish", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("Cancel", color = RaahiTextDim)
                }
            }
        )
    }
}
