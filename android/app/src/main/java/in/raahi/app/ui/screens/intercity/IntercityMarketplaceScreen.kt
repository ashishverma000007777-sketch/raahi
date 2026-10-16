package `in`.raahi.app.ui.screens.intercity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.raahi.app.data.CityCatalogue
import `in`.raahi.app.network.IntercityTripDto
import `in`.raahi.app.ui.theme.*

private val POPULAR_CITIES: List<String> by lazy {
    listOf("All") + CityCatalogue.popularCities().map { it.name }.distinct()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityMarketplaceScreen(
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToAgencyTrips: () -> Unit,
    onNavigateToDriverTrips: () -> Unit,
    onNavigateToActiveTrip: (String) -> Unit,
    onOpenTripDetails: (String) -> Unit = {},
    viewModel: IntercityViewModel = hiltViewModel(),
) {
    val landingState by viewModel.landingState.collectAsState()
    val isOnline by viewModel.marketplaceOnline.collectAsState()
    val preferredCity by viewModel.preferredPickupCity.collectAsState()
    val isUpdatingStatus by viewModel.isUpdatingStatus.collectAsState()
    val actionSuccess by viewModel.actionSuccess.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCityChip by remember { mutableStateOf("All") }
    var showCityDialog by remember { mutableStateOf(false) }
    var citySelectorError by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadLandingData()
    }

    LaunchedEffect(actionSuccess) {
        actionSuccess?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionStatus()
        }
    }

    LaunchedEffect(actionError) {
        actionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Intercity Marketplace",
                            color = RaahiText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = RaahiDisplayFont,
                        )
                        Text(
                            text = "Travel Agents ↔ Verified Drivers",
                            color = RaahiTextDim,
                            fontSize = 12.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = RaahiText,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToCreate) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Post Trip", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = RaahiBg,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = landingState) {
                is MarketplaceLandingUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RaahiOrange)
                    }
                }
                is MarketplaceLandingUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RaahiSelectionBg,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Warning, contentDescription = null, tint = RaahiOrange)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Unable to connect",
                            fontWeight = FontWeight.Bold,
                            color = RaahiText,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = s.message,
                            fontSize = 13.5.sp,
                            color = RaahiTextDim,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.loadLandingData() },
                            colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
                is MarketplaceLandingUiState.Success -> {
                    val user = s.user
                    val activeTrip = s.activeTrip
                    val isAgent = s.role == "TRAVEL_AGENT" || s.role == "ADMIN"
                    val publishedTrips = s.publishedTrips

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Welcome Banner
                        MarketplaceUserBanner(
                            userName = user?.name ?: "Partner",
                            agencyName = user?.agencyName,
                            role = s.role,
                        )

                        // 1. Online / Offline Trip Alerts Banner
                        MarketplaceOnlineStatusBanner(
                            isOnline = isOnline,
                            preferredCity = preferredCity,
                            isUpdating = isUpdatingStatus,
                            onToggleOnline = { newOnline ->
                                viewModel.setMarketplaceStatus(newOnline, preferredCity)
                            },
                            onChangeCity = { showCityDialog = true },
                        )

                        // 2. Single Pickup-City Search Bar ("Where do you want to start?")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = RaahiBg2,
                            border = BorderStroke(1.dp, RaahiBorderSoft),
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
                                Spacer(Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { query ->
                                        searchQuery = query
                                        selectedCityChip = ""
                                        viewModel.searchMarketplaceTrips(query)
                                    },
                                    placeholder = { Text("Search pickup city (e.g. Delhi)", fontSize = 13.5.sp, color = RaahiTextDim) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RaahiOrange,
                                        unfocusedBorderColor = RaahiBorderSoft,
                                    ),
                                    leadingIcon = {
                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(20.dp))
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = {
                                                searchQuery = ""
                                                selectedCityChip = "All"
                                                viewModel.searchMarketplaceTrips(null)
                                            }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = RaahiTextDim, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                )

                                Spacer(Modifier.height(10.dp))

                                // Quick City Filter Chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    items(POPULAR_CITIES) { city ->
                                        val isSelected = (selectedCityChip == city) ||
                                                (city == "All" && searchQuery.isBlank() && selectedCityChip == "All")
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedCityChip = city
                                                searchQuery = if (city == "All") "" else city
                                                viewModel.searchMarketplaceTrips(if (city == "All") null else city)
                                            },
                                            label = {
                                                Text(
                                                    text = city,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = RaahiSelectionBg,
                                                selectedLabelColor = RaahiOrange,
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = if (isSelected) RaahiOrange else RaahiBorderSoft,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Active Trip Card (Only when an active trip exists)
                        if (activeTrip != null) {
                            ActiveTripCard(
                                trip = activeTrip,
                                onClick = { onNavigateToActiveTrip(activeTrip.id) },
                            )
                        }

                        // 4. Available Published Rides Feed (Auto-loaded)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "Available Trips",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = RaahiText,
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = RaahiOrange.copy(alpha = 0.12f),
                                    ) {
                                        Text(
                                            text = "${publishedTrips.size}",
                                            color = RaahiOrange,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                        )
                                    }
                                }

                                TextButton(onClick = onNavigateToSearch) {
                                    Text("Advanced Filters", color = RaahiOrange, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(14.dp))
                                }
                            }

                            if (publishedTrips.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = RaahiBg2,
                                    border = BorderStroke(1.dp, RaahiBorderSoft),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Icon(
                                            Icons.Outlined.DirectionsCar,
                                            contentDescription = null,
                                            tint = RaahiTextDim,
                                            modifier = Modifier.size(44.dp),
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = if (searchQuery.isNotBlank()) "No trips departing from \"$searchQuery\"" else "No published trips right now",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = RaahiText,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Try clearing the search filter or post a new trip as a travel agent.",
                                            fontSize = 12.5.sp,
                                            color = RaahiTextDim,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        )
                                        Spacer(Modifier.height(14.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (searchQuery.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        searchQuery = ""
                                                        selectedCityChip = "All"
                                                        viewModel.searchMarketplaceTrips(null)
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                ) {
                                                    Text("Show All Cities", fontSize = 12.5.sp)
                                                }
                                            }
                                            Button(
                                                onClick = onNavigateToCreate,
                                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                                                shape = RoundedCornerShape(10.dp),
                                            ) {
                                                Text("Post a Trip", fontSize = 12.5.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                publishedTrips.forEach { trip ->
                                    MarketplaceTripCard(
                                        trip = trip,
                                        onViewDetails = { onOpenTripDetails(trip.id) },
                                    )
                                }
                            }
                        }

                        // 5. Quick Actions Section
                        Text(
                            text = "Marketplace Actions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = RaahiText,
                        )

                        // Action 1: Post a Trip
                        MarketplaceActionCard(
                            icon = Icons.Default.Add,
                            title = "Post a Trip",
                            subtitle = "Publish route, timing & driver fare for verified drivers.",
                            badge = if (isAgent) "TRAVEL AGENT" else "POST TRIP",
                            accentColor = Color(0xFF22B573),
                            onClick = onNavigateToCreate,
                        )

                        // Action 2: My Agency Trips
                        MarketplaceActionCard(
                            icon = Icons.Outlined.BusinessCenter,
                            title = "My Agency Trips",
                            subtitle = "Manage trips posted by your agency, review applicants.",
                            badge = "MANAGE TRIPS",
                            accentColor = Color(0xFF4D9EFF),
                            onClick = onNavigateToAgencyTrips,
                        )

                        // Action 3: My Driver Trips
                        MarketplaceActionCard(
                            icon = Icons.Outlined.DirectionsCar,
                            title = "My Driver Trips",
                            subtitle = "View applications, assigned trips and OTP verification.",
                            badge = "MY ASSIGNMENTS",
                            accentColor = Color(0xFF9C51E0),
                            onClick = onNavigateToDriverTrips,
                        )

                        // 6. Marketplace Standards Banner
                        MarketplaceFeaturesBanner()

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // City Selector Modal
    if (showCityDialog) {
        PreferredCitySelectorModal(
            currentCity = preferredCity,
            isSaving = isUpdatingStatus,
            errorMessage = citySelectorError,
            onDismiss = {
                citySelectorError = null
                showCityDialog = false
            },
            onSaveCity = { selectedCity ->
                citySelectorError = null
                val cleanCity = if (selectedCity.equals("All Cities", ignoreCase = true) || selectedCity.equals("All", ignoreCase = true)) null else selectedCity
                viewModel.updatePreferredCity(
                    city = cleanCity,
                    onSuccess = { savedCanonical ->
                        showCityDialog = false
                        citySelectorError = null
                        // Synchronize marketplace search and quick filter chips with saved hub
                        val displayCity = savedCanonical ?: "All"
                        searchQuery = savedCanonical.orEmpty()
                        selectedCityChip = displayCity
                    },
                    onError = { err ->
                        citySelectorError = err
                    }
                )
            }
        )
    }
}

@Composable
private fun MarketplaceOnlineStatusBanner(
    isOnline: Boolean,
    preferredCity: String?,
    isUpdating: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    onChangeCity: () -> Unit,
) {
    val activeColor = Color(0xFF22B573)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isOnline) Color(0xFFF0FDF4) else RaahiBg2,
        border = BorderStroke(1.5.dp, if (isOnline) activeColor.copy(alpha = 0.5f) else RaahiBorderSoft),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isOnline) activeColor else RaahiTextDim, CircleShape)
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Trip Alerts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = RaahiText,
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOnline) activeColor.copy(alpha = 0.15f) else RaahiSelectionBg,
                            ) {
                                Text(
                                    text = if (isOnline) "ONLINE" else "OFFLINE",
                                    color = if (isOnline) activeColor else RaahiTextDim,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = if (isOnline) "Instant alerts enabled for matching rides" else "Turn on to get notified of new trips",
                            fontSize = 12.sp,
                            color = RaahiTextDim,
                        )
                    }
                }

                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = RaahiOrange,
                    )
                } else {
                    Switch(
                        checked = isOnline,
                        onCheckedChange = onToggleOnline,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = activeColor,
                            uncheckedThumbColor = RaahiTextDim,
                            uncheckedTrackColor = RaahiBorderSoft,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Preferred Hub City Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangeCity),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = if (isOnline) activeColor else RaahiOrange,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Preferred Hub: ",
                        fontSize = 12.5.sp,
                        color = RaahiTextDim,
                    )
                    Text(
                        text = if (!preferredCity.isNullOrBlank()) preferredCity else "All Cities (Tap to set)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = RaahiText,
                    )
                }

                Text(
                    text = "Change",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RaahiOrange,
                )
            }
        }
    }
}

@Composable
private fun MarketplaceTripCard(
    trip: IntercityTripDto,
    onViewDetails: () -> Unit,
) {
    `in`.raahi.app.ui.components.RaahiTripCard(
        trip = trip,
        onClick = onViewDetails,
    )
}


@Composable
private fun ActiveTripCard(
    trip: IntercityTripDto,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF7ED),
        border = BorderStroke(1.5.dp, RaahiOrange),
        shadowElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(RaahiOrange, CircleShape)
                    )
                    Text(
                        text = "ACTIVE TRIP IN PROGRESS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = RaahiOrange,
                    )
                }

                Surface(
                    color = RaahiOrange,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = trip.status,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${trip.pickupCity} → ${trip.destCity}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = RaahiText,
                        )
                        `in`.raahi.app.ui.components.RaahiRideTypeBadge(trip.rideType)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Open Tracking", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RaahiOrange)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(14.dp))
                    }
                }

                Text(
                    text = "Fare: ₹${trip.fare.toInt()} • ${trip.vehicleCategory}",
                    fontSize = 12.sp,
                    color = RaahiTextDim,
                )

                val nextActionText = when (trip.status) {
                    "ASSIGNED" -> "Next: Driver is assigned • Awaiting pickup arrival"
                    "ARRIVED" -> "Next: Driver has arrived • Enter OTP to start trip"
                    "IN_PROGRESS" -> "Next: Trip in progress • Enter drop OTP upon arrival"
                    "PUBLISHED" -> "Next: Trip published • Waiting for drivers to apply"
                    else -> "Next: Tap to view details and live status"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = RaahiOrange.copy(alpha = 0.08f),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = nextActionText,
                        fontSize = 11.5.sp,
                        color = RaahiOrange,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketplaceActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = RaahiBg2,
        border = BorderStroke(1.dp, RaahiBorderSoft),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = RaahiText,
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = badge,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp,
                            softWrap = false,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = RaahiTextDim,
                    lineHeight = 16.sp,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = RaahiTextDim,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun MarketplaceFeaturesBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = RaahiBg2,
        border = BorderStroke(1.dp, RaahiBorderSoft),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Marketplace Standards",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = RaahiText,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FeaturePill(icon = Icons.Default.CheckCircle, text = "0% Platform Fee")
                FeaturePill(icon = Icons.Outlined.Lock, text = "Dual OTP Safety")
                FeaturePill(icon = Icons.Outlined.Navigation, text = "Google Maps Live")
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF22B573), modifier = Modifier.size(14.dp))
        Text(text, fontSize = 11.sp, color = RaahiTextDim, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MarketplaceUserBanner(
    userName: String,
    agencyName: String?,
    role: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = RaahiBg2,
        border = BorderStroke(1.dp, RaahiBorderSoft),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome, $userName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = RaahiText,
                )
                Text(
                    text = if (!agencyName.isNullOrBlank()) agencyName else if (role == "TRAVEL_AGENT") "Registered Travel Agency" else "Verified Highway Driver",
                    fontSize = 12.sp,
                    color = RaahiTextDim,
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = RaahiSelectionBg,
            ) {
                Text(
                    text = if (role == "TRAVEL_AGENT") "TRAVEL AGENT" else "VERIFIED DRIVER",
                    color = RaahiOrange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
