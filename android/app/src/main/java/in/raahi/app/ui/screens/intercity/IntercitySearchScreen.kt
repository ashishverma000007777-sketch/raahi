package `in`.raahi.app.ui.screens.intercity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.network.IntercityTripDto
import `in`.raahi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercitySearchScreen(
    onBack: () -> Unit,
    onOpenTripDetails: (String) -> Unit,
    onCreateTrip: () -> Unit,
    initialPickupCity: String = "",
    initialDestCity: String = "",
    viewModel: IntercityViewModel = hiltViewModel(),
) {
    var pickupCity by remember { mutableStateOf(initialPickupCity) }
    var destCity by remember { mutableStateOf(initialDestCity) }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val searchState by viewModel.searchState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.searchTrips(pickupCity, destCity, selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Intercity Marketplace", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                },
                actions = {
                    TextButton(onClick = onCreateTrip) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Post Trip", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg)
            )
        },
        containerColor = RaahiBg,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(18.dp),
                color = RaahiBg2,
                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiBorderSoft),
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Where do you want to start?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = RaahiText,
                        fontFamily = RaahiDisplayFont,
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = pickupCity,
                            onValueChange = { pickupCity = it },
                            placeholder = { Text("Pickup City (e.g. Delhi)", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RaahiOrange,
                                unfocusedBorderColor = RaahiBorderSoft,
                            ),
                            leadingIcon = {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(18.dp))
                            }
                        )

                        OutlinedTextField(
                            value = destCity,
                            onValueChange = { destCity = it },
                            placeholder = { Text("To (Optional)", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RaahiOrange,
                                unfocusedBorderColor = RaahiBorderSoft,
                            ),
                            leadingIcon = {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RaahiBlue, modifier = Modifier.size(18.dp))
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Vehicle Categories Filter Chips
                    val categories = listOf("ALL", "SEDAN", "SUV", "HATCHBACK", "LUXURY", "TRAVELLER")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = cat
                                    viewModel.searchTrips(pickupCity, destCity, cat)
                                },
                                label = {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RaahiSelectionBg,
                                    selectedLabelColor = RaahiOrange,
                                    selectedLeadingIconColor = RaahiOrange,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) RaahiOrange else RaahiBorderSoft,
                                    enabled = true,
                                    selected = isSelected,
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.searchTrips(pickupCity, destCity, selectedCategory) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Search Available Trips", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Results List
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = searchState) {
                    is SearchTripsUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = RaahiOrange)
                        }
                    }
                    is SearchTripsUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Unable to load trips", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(state.message, color = RaahiTextDim, fontSize = 13.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.searchTrips(pickupCity, destCity, selectedCategory) },
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    is SearchTripsUiState.Success -> {
                        if (state.trips.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.DirectionsCar,
                                    contentDescription = null,
                                    tint = RaahiTextDim,
                                    modifier = Modifier.size(54.dp),
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No trips available right now",
                                    color = RaahiText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Try another route or check again later. Travel agents post new assignments regularly.",
                                    color = RaahiTextDim,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.trips) { trip ->
                                    IntercityTripCard(
                                        trip = trip,
                                        onClick = { onOpenTripDetails(trip.id) },
                                    )
                                }
                            }
                        }
                    }
                    is SearchTripsUiState.Idle -> {}
                }
            }
        }
    }
}

@Composable
fun IntercityTripCard(
    trip: IntercityTripDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    `in`.raahi.app.ui.components.RaahiTripCard(
        trip = trip,
        onClick = onClick,
        modifier = modifier,
    )
}
