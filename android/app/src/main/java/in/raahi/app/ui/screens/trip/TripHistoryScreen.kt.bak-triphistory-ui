package `in`.raahi.app.ui.screens.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
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
import `in`.raahi.app.network.TripDto
import `in`.raahi.app.ui.components.GlassCard
import `in`.raahi.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    onBack: () -> Unit,
    onOpenTrip: (TripDto) -> Unit,
    onPlanTrip: () -> Unit,
    viewModel: TripHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip History", color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, fontFamily = RaahiDisplayFont) },
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
            is TripHistoryUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RaahiOrange)
                }
            }
            is TripHistoryUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(s.message, color = RaahiRed, fontSize = 14.sp)
                }
            }
            is TripHistoryUiState.Success -> {
                val trips = s.trips
                if (trips.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape).background(RaahiOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Navigation, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("No Trips Recorded Yet", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = RaahiDisplayFont)
                            Spacer(Modifier.height(6.dp))
                            Text("Plan your highway journey with live fuel stops, pit stops, and weather alerts.", color = RaahiTextDim, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 20.dp))
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = onPlanTrip,
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                                shape = RaahiShapeMedium
                            ) {
                                Text("Plan a Trip", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trips) { trip ->
                            TripHistoryCard(trip = trip, onClick = { onOpenTrip(trip) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripHistoryCard(trip: TripDto, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(trip.title, color = RaahiText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, fontFamily = RaahiDisplayFont)
                val statusColor = when (trip.status) {
                    "COMPLETED" -> RaahiGreen
                    "IN_PROGRESS" -> RaahiCyan
                    else -> RaahiAmber
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(trip.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("${trip.startLocationName} → ${trip.destLocationName}", color = RaahiTextDim, fontSize = 12.sp)

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Distance", color = RaahiTextFaint, fontSize = 11.sp)
                    Text("${trip.distanceKm.toInt()} km", color = RaahiText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                Column {
                    Text("Duration", color = RaahiTextFaint, fontSize = 11.sp)
                    Text(trip.durationFormatted, color = RaahiText, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                val cost = trip.actualFuelCost ?: trip.estimatedFuelCost
                if (cost != null) {
                    Column {
                        Text("Fuel Cost", color = RaahiTextFaint, fontSize = 11.sp)
                        Text("₹${String.format(Locale.ROOT, "%.0f", cost)}", color = RaahiAmber, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
