package `in`.raahi.app.ui.screens.intercity

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DirectionsCar
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
fun IntercityAgentDashboardScreen(
    onBack: () -> Unit,
    onOpenTripDetails: (String) -> Unit,
    onCreateTrip: () -> Unit,
    viewModel: IntercityViewModel = hiltViewModel(),
) {
    val state by viewModel.agentDashboardState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadAgentTrips()
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = RaahiCharcoalHeader,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(RaahiDarkSurface, CircleShape)
                            .border(1.dp, Color(0xFF2E3438), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "My Agency Trips",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = RaahiDisplayFont,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onCreateTrip,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RaahiPrimaryTeal,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Post", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTrip,
                containerColor = RaahiPrimaryTeal,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post Trip")
            }
        },
        containerColor = RaahiLightBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs: Open & Active Trips vs History
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = RaahiWhiteSurface,
                contentColor = RaahiPrimaryTeal,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Open & Active", fontWeight = FontWeight.Bold) },
                    selectedContentColor = RaahiPrimaryTeal,
                    unselectedContentColor = RaahiSecondaryText,
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Past & Completed", fontWeight = FontWeight.Bold) },
                    selectedContentColor = RaahiPrimaryTeal,
                    unselectedContentColor = RaahiSecondaryText,
                )
            }

            when (val s = state) {
                is AgentDashboardUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RaahiPrimaryTeal)
                    }
                }
                is AgentDashboardUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Could not load agency trips", fontWeight = FontWeight.Bold, color = RaahiPrimaryText)
                        Spacer(Modifier.height(8.dp))
                        Text(s.message, fontSize = 13.sp, color = RaahiSecondaryText)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAgentTrips() }, colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal)) {
                            Text("Retry")
                        }
                    }
                }
                is AgentDashboardUiState.Success -> {
                    val filteredTrips = if (selectedTab == 0) {
                        s.trips.filter { it.status in listOf("PUBLISHED", "ASSIGNED", "ARRIVED", "IN_PROGRESS", "DRAFT") }
                    } else {
                        s.trips.filter { it.status in listOf("COMPLETED", "CANCELLED") }
                    }

                    if (filteredTrips.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(54.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (selectedTab == 0) "No trips posted yet" else "No past trips yet",
                                fontWeight = FontWeight.Bold,
                                color = RaahiText,
                                fontSize = 17.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (selectedTab == 0) "Publish your first city-to-city assignment to connect with verified drivers." else "Completed trips will appear here.",
                                fontSize = 13.sp,
                                color = RaahiTextDim,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (selectedTab == 0) {
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = onCreateTrip,
                                    colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Post First Trip", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTrips) { trip ->
                                AgentTripCard(
                                    trip = trip,
                                    onClick = { onOpenTripDetails(trip.id) },
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
private fun AgentTripCard(
    trip: IntercityTripDto,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RaahiCardShape,
        color = RaahiWhiteSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderColor),
        shadowElevation = RaahiCardElevation,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(trip.pickupCity, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RaahiPrimaryText)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaahiPrimaryTeal, modifier = Modifier.size(16.dp))
                    Text(trip.destCity, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RaahiPrimaryText)
                }

                Text("₹${trip.fare.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = RaahiPriceText)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = RaahiLightTeal, shape = RoundedCornerShape(6.dp)) {
                        Text(trip.vehicleCategory, color = RaahiPrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Surface(
                        color = if (trip.status == "COMPLETED") Color(0xFFE8F8F0) else RaahiLightTeal,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            trip.status,
                            color = if (trip.status == "COMPLETED") Color(0xFF22B573) else RaahiPrimaryTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (trip.status == "PUBLISHED") {
                    Text(
                        "${trip.applicantCount} applied",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RaahiBlue
                    )
                } else if (trip.driverName != null) {
                    Text(
                        "Driver: ${trip.driverName}",
                        fontSize = 12.sp,
                        color = RaahiTextDim
                    )
                }
            }
        }
    }
}
