package `in`.raahi.app.ui.screens.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.components.SectionLabel
import `in`.raahi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlanScreen(
    onBack: () -> Unit,
    onTripCreated: (String) -> Unit,
    viewModel: TripPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TripPlanEvent.TripCreated -> onTripCreated(event.tripId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan a Trip", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, fontFamily = RaahiDisplayFont) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // Vehicle & Current Telemetry info badge (Auto-obtained)
            val vehicle = state.vehicle
            if (vehicle != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = RaahiCyan.copy(alpha = 0.25f),
                    background = RaahiCyan.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(RaahiCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = RaahiCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "${vehicle.brand} ${vehicle.model} (${vehicle.fuelType})",
                                color = RaahiText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            val odoText = if (vehicle.odometerKm > 0) "${vehicle.odometerKm} km logged" else "Odometer not set"
                            Text(
                                odoText,
                                color = RaahiTextDim,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Route Points
            SectionLabel("Trip Route")
            Spacer(Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Start Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = RaahiGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Location", color = RaahiTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.startLocationName,
                        onValueChange = { viewModel.onStartLocationChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RaahiText,
                            unfocusedTextColor = RaahiText,
                            focusedBorderColor = RaahiOrange,
                            unfocusedBorderColor = RaahiBorder,
                            focusedContainerColor = RaahiBg2,
                            unfocusedContainerColor = RaahiBg2,
                        ),
                        placeholder = { Text("e.g. Current Location", color = RaahiTextFaint) },
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // Destination
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Navigation, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Destination *", color = RaahiTextDim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.destLocationName,
                        onValueChange = { viewModel.onDestinationChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RaahiText,
                            unfocusedTextColor = RaahiText,
                            focusedBorderColor = RaahiOrange,
                            unfocusedBorderColor = RaahiBorder,
                            focusedContainerColor = RaahiBg2,
                            unfocusedContainerColor = RaahiBg2,
                        ),
                        placeholder = { Text("e.g. Manali, Jaipur, Agra...", color = RaahiTextFaint) },
                        singleLine = true
                    )

                    // Popular suggestions
                    Spacer(Modifier.height(10.dp))
                    Text("Popular Road Trips:", color = RaahiTextFaint, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Manali", "Jaipur", "Agra", "Rishikesh").forEach { spot ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RaahiGlassStrong)
                                    .border(1.dp, RaahiBorderSoft, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onDestinationChanged(spot) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(spot, color = RaahiTextDim, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Trip Preferences (Simple & Compact — NO 15 questions!)
            SectionLabel("Trip Setup (Optional)")
            Spacer(Modifier.height(8.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tank Full Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tank full before start?", color = RaahiText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Helps optimize fuel stop timings", color = RaahiTextFaint, fontSize = 12.sp)
                        }
                        Switch(
                            checked = state.tankFull,
                            onCheckedChange = { viewModel.onTankFullToggled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RaahiOrange,
                                uncheckedTrackColor = RaahiBg2,
                                uncheckedBorderColor = RaahiBorder
                            )
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RaahiBorderSoft)

                    // Passengers Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Passengers", color = RaahiText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 4).forEach { count ->
                                val selected = state.passengers == count
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) RaahiOrange else RaahiBg2)
                                        .border(1.dp, if (selected) RaahiOrange else RaahiBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.onPassengersChanged(count) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$count",
                                        color = if (selected) Color.White else RaahiText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = RaahiBorderSoft)

                    // Preferred Route
                    Text("Preferred Route", color = RaahiText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Fastest Highway", "Scenic / Less Tolls").forEach { route ->
                            val selected = state.preferredRoute == route
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) RaahiOrange.copy(alpha = 0.15f) else RaahiBg2)
                                    .border(1.dp, if (selected) RaahiOrange else RaahiBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.onPreferredRouteChanged(route) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    route,
                                    color = if (selected) RaahiOrange else RaahiTextDim,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = RaahiRed, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = { viewModel.createTrip() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSubmitting,
                shape = RaahiShapeMedium,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RaahiBrandGradient, RaahiShapeMedium),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text(
                            "Calculate Route & Stops →",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = RaahiDisplayFont
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
