package `in`.raahi.app.ui.screens.intercity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import `in`.raahi.app.data.CityCatalogue
import `in`.raahi.app.network.IntercityTripApplicationDto
import `in`.raahi.app.network.IntercityTripDto
import `in`.raahi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityTripDetailScreen(
    tripId: String,
    onBack: () -> Unit,
    onOpenActiveTrip: (String) -> Unit,
    viewModel: IntercityViewModel = hiltViewModel(),
) {
    val detailState by viewModel.detailState.collectAsState()
    val actionLoading by viewModel.actionLoading.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val actionSuccess by viewModel.actionSuccess.collectAsState()

    var showApplyDialog by remember { mutableStateOf(false) }
    var applyNotes by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var cancelError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tripId) {
        viewModel.loadTripDetails(tripId)
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
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Trip Details",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = RaahiDisplayFont,
                    )
                }
            }
        },
        containerColor = RaahiLightBg,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = detailState) {
                is TripDetailUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RaahiPrimaryTeal)
                    }
                }
                is TripDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("Error", color = RaahiSosRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = RaahiTextDim, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadTripDetails(tripId) },
                            colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                is TripDetailUiState.Success -> {
                    val trip = state.trip
                    val isAgent = trip.agentId == state.currentUserId || state.currentUserRole == "TRAVEL_AGENT"
                    val isDriver = trip.driverId == state.currentUserId
                    val hasApplied = trip.viewerRole == "APPLICANT"

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Alerts / Notification banners
                        if (actionError != null) {
                            Surface(
                                color = Color(0xFFFFECEC),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiSosRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.Warning, contentDescription = null, tint = RaahiSosRed)
                                    Text(actionError ?: "", color = RaahiSosRed, fontSize = 13.sp)
                                }
                            }
                        }

                        if (actionSuccess != null) {
                            Surface(
                                color = RaahiLightTeal,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiPrimaryTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaahiPrimaryTeal)
                                    Text(actionSuccess ?: "", color = RaahiPrimaryTeal, fontSize = 13.sp)
                                }
                            }
                        }

                        // Hero Card: Route & Fare
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RaahiCardShape,
                            color = RaahiCardWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCardBorder),
                            shadowElevation = RaahiCardElevation,
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(trip.pickupCity, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = RaahiText)
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaahiPrimaryTeal)
                                        Text(trip.destCity, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = RaahiText)
                                    }

                                    Text("₹${trip.fare.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = RaahiPriceText)
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    `in`.raahi.app.ui.components.RaahiRideTypeBadge(trip.rideType)
                                    BadgePill(trip.vehicleCategory, RaahiPrimaryTeal, RaahiLightTeal)
                                    BadgePill(if (trip.tripType == "ROUND_TRIP") "Round Trip" else "One Way", RaahiBlue, Color(0xFFEAF3FF))
                                    BadgePill(trip.status, if (trip.status == "COMPLETED") RaahiPrimaryTealDark else RaahiPrimaryTeal, RaahiLightTeal)
                                }
                            }
                        }

                        // OTP Display Box for Travel Agent
                        if (isAgent && (trip.pickupOtp != null || trip.completionOtp != null)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RaahiCardShape,
                                color = RaahiLightTeal,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiPrimaryTeal),
                                shadowElevation = RaahiCardElevation,
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = RaahiPrimaryTeal)
                                        Text("Trip Verification OTPs (Confidential)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiPrimaryTeal)
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    if (trip.pickupOtp != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Pickup OTP (Share with driver at start):", fontSize = 13.sp, color = RaahiText)
                                            Text(trip.pickupOtp, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = RaahiPrimaryTeal)
                                        }
                                    }

                                    if (trip.completionOtp != null) {
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Completion OTP (Verify upon drop):", fontSize = 13.sp, color = RaahiText)
                                            Text(trip.completionOtp, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = RaahiPrimaryTealDark)
                                        }
                                    }
                                }
                            }
                        }
                        // Addresses and Timing Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RaahiCardShape,
                            color = RaahiCardWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCardBorder),
                            shadowElevation = RaahiCardElevation,
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Trip Schedule & Locations", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)

                                LocationRow("Pickup Address", trip.pickupAddress, Icons.Outlined.LocationOn, RaahiPrimaryTeal)
                                LocationRow("Destination Address", trip.destAddress, Icons.Outlined.LocationOn, RaahiBlue)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Departure Time (24h)", fontSize = 12.sp, color = RaahiTextDim)
                                        val formattedDeparture = try {
                                            val instant = java.time.Instant.parse(trip.departureTime)
                                            val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                                            zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                                        } catch (_: Exception) {
                                            trip.departureTime.take(16).replace("T", " ")
                                        }
                                        Text(formattedDeparture, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = RaahiText)
                                    }
                                    if (trip.returnTime != null) {
                                        Column {
                                            Text("Return Time (24h)", fontSize = 12.sp, color = RaahiTextDim)
                                            val formattedReturn = try {
                                                val instant = java.time.Instant.parse(trip.returnTime)
                                                val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                                                zdt.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                                            } catch (_: Exception) {
                                                trip.returnTime.take(16).replace("T", " ")
                                            }
                                            Text(formattedReturn, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = RaahiText)
                                        }
                                    }
                                }

                                if (!trip.specialInstructions.isNullOrBlank()) {
                                    HorizontalDivider(color = RaahiCardBorder)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            if (trip.tripType == "ROUND_TRIP") "Tour Itinerary & Instructions" else "Special Instructions",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = RaahiTextDim
                                        )
                                        Text(trip.specialInstructions, fontSize = 13.sp, color = RaahiText, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }

                        // Intercity Route Map Card
                        TripRouteMapCard(trip)

                        // Counterparty Information Card (Agency or Driver)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RaahiCardShape,
                            color = RaahiCardWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCardBorder),
                            shadowElevation = RaahiCardElevation,
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Travel Agency & Payment", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        Modifier.size(40.dp).background(RaahiLightTeal, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = RaahiPrimaryTeal)
                                    }
                                    Column {
                                        Text(trip.agencyName ?: trip.agentName ?: "Travel Agency", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                                        if (trip.agentPhone != null) {
                                            Text(trip.agentPhone, fontSize = 12.sp, color = RaahiTextDim)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Payment Mode", fontSize = 13.sp, color = RaahiTextDim)
                                    Text(trip.paymentMode ?: "DIRECT_CASH", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RaahiText)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Payment Status", fontSize = 13.sp, color = RaahiTextDim)
                                    Text(trip.paymentStatus, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (trip.paymentStatus == "PAID") RaahiPrimaryTeal else RaahiSosRed)
                                }

                                if (trip.rideType == "AGENT") {
                                    HorizontalDivider(color = RaahiCardBorder)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Customer Fare", fontSize = 13.sp, color = RaahiTextDim)
                                        Text("₹${(trip.customerFare ?: trip.fare).toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RaahiText)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Driver Payout", fontSize = 13.sp, color = RaahiTextDim)
                                        Text("₹${(trip.driverPayout ?: trip.fare).toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiPriceText)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Agent Commission", fontSize = 13.sp, color = RaahiTextDim)
                                        Text("₹${(trip.agentCommission ?: 0.0).toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RaahiPriceText)
                                    }
                                } else {
                                    HorizontalDivider(color = RaahiCardBorder)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Owner Journey", fontSize = 13.sp, color = RaahiTextDim)
                                        Text("0% Commission • Keep 100% Fare", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RaahiPrimaryTeal)
                                    }
                                    if (trip.estimatedDistanceKm != null) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Calculated Road Distance", fontSize = 13.sp, color = RaahiTextDim)
                                            Text("${trip.estimatedDistanceKm} km", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RaahiText)
                                        }
                                    }
                                }
                            }
                        }

                        // Assigned Driver Card (if assigned)
                        if (trip.driverId != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RaahiCardShape,
                                color = RaahiCardWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCardBorder),
                                shadowElevation = RaahiCardElevation,
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Assigned Driver", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(trip.driverName ?: "Driver", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                                            if (trip.driverVehicleType != null || trip.driverVehicleReg != null) {
                                                Text("${trip.driverVehicleType ?: ""} • ${trip.driverVehicleReg ?: ""}", fontSize = 12.sp, color = RaahiTextDim)
                                            }
                                        }
                                        if (trip.driverRatingAvg != null && trip.driverRatingAvg > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(16.dp))
                                                Text(String.format("%.1f", trip.driverRatingAvg), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiText)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Agent View: Driver Applications List
                        if (isAgent && trip.status == "PUBLISHED") {
                            Text("Driver Applications (${state.applications.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RaahiText)

                            if (state.applications.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RaahiCardShape,
                                    color = RaahiCardWhite,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCardBorder),
                                    shadowElevation = RaahiCardElevation,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Waiting for driver applications", fontWeight = FontWeight.Medium, color = RaahiTextDim, fontSize = 14.sp)
                                        Text("Drivers across the route have been alerted.", fontSize = 12.sp, color = RaahiTextDim)
                                    }
                                }
                            } else {
                                state.applications.forEach { app ->
                                    ApplicationCard(
                                        app = app,
                                        onSelect = { viewModel.selectDriver(trip.id, app.driverId) },
                                        isLoading = actionLoading,
                                    )
                                }
                            }
                        }

                        // Driver Action Buttons
                        if (isDriver || (trip.status in listOf("ASSIGNED", "ARRIVED", "IN_PROGRESS") && isAgent)) {
                            Button(
                                onClick = { onOpenActiveTrip(trip.id) },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                            ) {
                                Icon(Icons.Outlined.Navigation, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Live Trip Tracking & Verification", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }
                        }

                        if (!isAgent && trip.status == "PUBLISHED") {
                            if (hasApplied) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = RaahiLightTeal,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RaahiPrimaryTeal),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RaahiPrimaryTeal)
                                        Text("You have applied for this trip. The agent is reviewing your profile.", color = RaahiPrimaryTeal, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { showApplyDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                                    enabled = !actionLoading,
                                ) {
                                    Text("Apply as Driver (₹${trip.fare.toInt()})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                }
                            }
                        }

                        // Cancel Trip Button (for either party before trip starts)
                        if (trip.status in listOf("PUBLISHED", "ASSIGNED") && (isAgent || isDriver)) {
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RaahiSosRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RaahiSosRed),
                            ) {
                                Text("Cancel Trip", fontWeight = FontWeight.SemiBold, color = RaahiSosRed)
                            }
                        }
                    }
                }
            }
        }
    }

    // Apply Modal Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text("Apply for Intercity Trip", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Add an optional note to the travel agent (e.g. vehicle model, experience):", fontSize = 13.sp, color = RaahiTextDim)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = applyNotes,
                        onValueChange = { applyNotes = it },
                        placeholder = { Text("e.g. Maruti Dzire 2023, 5 yrs highway experience") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApplyDialog = false
                        viewModel.applyForTrip(tripId, applyNotes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                ) {
                    Text("Confirm Application", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Cancel Modal Dialog
    if (showCancelDialog) {
        val currentTrip = (detailState as? TripDetailUiState.Success)?.trip
        if (currentTrip != null) {
            CancelIntercityTripModal(
                trip = currentTrip,
                isCancelling = actionLoading,
                errorMessage = cancelError,
                initialReason = cancelReason,
                onDismiss = {
                    cancelError = null
                    showCancelDialog = false
                },
                onConfirmCancel = { reason ->
                    cancelReason = reason
                    cancelError = null
                    viewModel.cancelTrip(
                        tripId = tripId,
                        reason = reason,
                        onSuccess = {
                            showCancelDialog = false
                            cancelError = null
                        },
                        onError = { err ->
                            cancelError = err
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ApplicationCard(
    app: IntercityTripApplicationDto,
    onSelect: () -> Unit,
    isLoading: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RaahiCardShape,
        color = RaahiCardWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCardBorder),
        shadowElevation = RaahiCardElevation,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(app.driverName ?: "Driver", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                    if (app.driverVehicleType != null) {
                        Text("${app.driverVehicleType} • ${app.driverVehicleReg ?: ""}", fontSize = 12.sp, color = RaahiTextDim)
                    }
                }
                if (app.driverRatingAvg != null && app.driverRatingAvg > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(15.dp))
                        Text(String.format("%.1f", app.driverRatingAvg), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiText)
                    }
                }
            }

            if (!app.driverNotes.isNullOrBlank()) {
                Text("\"${app.driverNotes}\"", fontSize = 12.sp, color = RaahiTextDim, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }

            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryTeal),
                enabled = !isLoading,
            ) {
                Text("Select & Assign Driver", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun LocationRow(label: String, address: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column {
            Text(label, fontSize = 11.sp, color = RaahiTextDim)
            Text(address, fontSize = 13.5.sp, color = RaahiText, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BadgePill(text: String, textColor: Color, bgColor: Color) {
    Surface(color = bgColor, shape = RoundedCornerShape(6.dp)) {
        Text(text = text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun TripRouteMapCard(trip: IntercityTripDto) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val pCoords = remember(trip) {
        if (trip.pickupLat != 0.0 && trip.pickupLng != 0.0) {
            trip.pickupLat to trip.pickupLng
        } else {
            CityCatalogue.getCoordinates(trip.pickupCity) ?: (30.7333 to 76.7794)
        }
    }

    val dCoords = remember(trip) {
        if (trip.destLat != 0.0 && trip.destLng != 0.0) {
            trip.destLat to trip.destLng
        } else {
            CityCatalogue.getCoordinates(trip.destCity) ?: (28.6139 to 77.2090)
        }
    }

    val mapView = remember {
        try {
            MapsInitializer.initialize(context.applicationContext)
        } catch (_: Exception) {}
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mapView.onDestroy()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(pCoords, dCoords) {
        mapView.getMapAsync { googleMap ->
            googleMap.clear()
            val pLatLng = LatLng(pCoords.first, pCoords.second)
            val dLatLng = LatLng(dCoords.first, dCoords.second)

            googleMap.addMarker(
                MarkerOptions()
                    .position(pLatLng)
                    .title("Pickup: ${trip.pickupCity}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            googleMap.addMarker(
                MarkerOptions()
                    .position(dLatLng)
                    .title("Destination: ${trip.destCity}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )

            googleMap.addPolyline(
                PolylineOptions()
                    .add(pLatLng, dLatLng)
                    .width(8f)
                    .color(android.graphics.Color.parseColor("#EF6C00"))
                    .geodesic(true)
            )

            try {
                val bounds = LatLngBounds.builder().include(pLatLng).include(dLatLng).build()
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 90))
            } catch (_: Exception) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pLatLng, 10f))
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RaahiCardShape,
        color = RaahiCardWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, RaahiCardBorder),
        shadowElevation = RaahiCardElevation,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Intercity Route Overview", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                if (trip.estimatedDistanceKm != null) {
                    Text("${trip.estimatedDistanceKm.toInt()} km", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiCoralPrimary)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Button(
                onClick = {
                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${pCoords.first},${pCoords.second}&destination=${dCoords.first},${dCoords.second}&travelmode=driving")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RaahiCoralPrimary),
            ) {
                Icon(Icons.Outlined.Navigation, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Open Navigation in Maps", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

