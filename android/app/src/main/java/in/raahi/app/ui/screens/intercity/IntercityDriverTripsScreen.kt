package `in`.raahi.app.ui.screens.intercity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.IntercityTripDto
import `in`.raahi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityDriverTripsScreen(
    onBack: () -> Unit,
    onOpenTripDetails: (String) -> Unit,
    onOpenActiveTrip: (String) -> Unit,
    onFindTrips: () -> Unit,
    viewModel: IntercityViewModel = hiltViewModel(),
) {
    val state by viewModel.driverTripsState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadDriverTrips()
    }

    Scaffold(
        topBar = {
            Surface(
                color = RaahiCharcoalHeader,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RaahiDarkSurface)
                            .border(1.dp, Color(0xFF2E3438), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "My Driver Trips",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = RaahiDisplayFont,
                        )
                        Text(
                            text = "View assigned trips & applications",
                            color = Color(0xFFA0AAB0),
                            fontSize = 11.5.sp,
                        )
                    }
                }
            }
        },
        containerColor = RaahiLightBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs: Assigned vs Applied vs Completed
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = RaahiWhiteSurface,
                contentColor = RaahiPrimaryTeal,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Active & Assigned",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) RaahiPrimaryTeal else RaahiTextDim,
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Applications",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) RaahiPrimaryTeal else RaahiTextDim,
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Completed",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 2) RaahiPrimaryTeal else RaahiTextDim,
                        )
                    }
                )
            }

            when (val s = state) {
                is DriverTripsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RaahiPrimaryTeal)
                    }
                }
                is DriverTripsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Could not load driver trips",
                            fontWeight = FontWeight.Bold,
                            color = RaahiText,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = s.message,
                            fontSize = 13.sp,
                            color = RaahiTextDim,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadDriverTrips() },
                            colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                is DriverTripsUiState.Success -> {
                    val filteredTrips = when (selectedTab) {
                        0 -> s.trips.filter { it.viewerRole == "ASSIGNED_DRIVER" && it.status in listOf("ASSIGNED", "ARRIVED", "IN_PROGRESS") }
                        1 -> s.trips.filter { it.viewerRole == "APPLICANT" || (it.viewerRole != "ASSIGNED_DRIVER" && it.status == "PUBLISHED") }
                        else -> s.trips.filter { it.status in listOf("COMPLETED", "CANCELLED") }
                    }

                    if (filteredTrips.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = RaahiTextDim,
                                modifier = Modifier.size(54.dp),
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> "No trips assigned right now"
                                    1 -> "No pending applications"
                                    else -> "No past trips recorded"
                                },
                                fontWeight = FontWeight.Bold,
                                color = RaahiText,
                                fontSize = 16.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Browse available city-to-city trips and apply to get assigned."
                                    1 -> "Find trips on your route and submit driver applications."
                                    else -> "Your completed highway assignments will appear here."
                                },
                                color = RaahiTextDim,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = onFindTrips,
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Find Trips", color = Color.White)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(filteredTrips, key = { it.id }) { trip ->
                                DriverTripItemCard(
                                    trip = trip,
                                    onClick = { onOpenTripDetails(trip.id) },
                                    onOpenActive = { onOpenActiveTrip(trip.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverTripItemCard(
    trip: IntercityTripDto,
    onClick: () -> Unit,
    onOpenActive: () -> Unit,
) {
    val isAssignedActive = trip.viewerRole == "ASSIGNED_DRIVER" && trip.status in listOf("ASSIGNED", "ARRIVED", "IN_PROGRESS")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        `in`.raahi.app.ui.components.RaahiTripCard(
            trip = trip,
            onClick = onClick,
        )
        if (isAssignedActive) {
            Button(
                onClick = onOpenActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text("Open Live Route & OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}
