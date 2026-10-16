package `in`.raahi.app.ui.screens.intercity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import `in`.raahi.app.ui.components.RaahiRideTypeBadge
import `in`.raahi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityActiveTripScreen(
    tripId: String,
    onBack: () -> Unit,
    viewModel: IntercityViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val detailState by viewModel.detailState.collectAsState()
    val actionLoading by viewModel.actionLoading.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val actionSuccess by viewModel.actionSuccess.collectAsState()

    var showOtpDialog by remember { mutableStateOf(false) }
    var enteredOtp by remember { mutableStateOf("") }
    var isPickupOtpVerification by remember { mutableStateOf(true) }

    var showSettlementDialog by remember { mutableStateOf(false) }
    var settlementRefInput by remember { mutableStateOf("") }
    var settlementNotesInput by remember { mutableStateOf("") }

    var showDisputeDialog by remember { mutableStateOf(false) }
    var disputeReasonInput by remember { mutableStateOf("") }
    var disputeEvidenceInput by remember { mutableStateOf("") }

    var showResolveDialog by remember { mutableStateOf(false) }
    var resolveNotesInput by remember { mutableStateOf("") }

    var showCancelModal by remember { mutableStateOf(false) }

    LaunchedEffect(tripId) {
        viewModel.loadTripDetails(tripId)
    }

    // Google MapView setup
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Live Trip Tracking", color = RaahiText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RaahiBg)
            )
        },
        containerColor = RaahiBg,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = detailState) {
                is TripDetailUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RaahiOrange)
                    }
                }
                is TripDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Error", color = RaahiOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = RaahiTextDim, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTripDetails(tripId) }, colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange)) {
                            Text("Retry")
                        }
                    }
                }
                is TripDetailUiState.Success -> {
                    val trip = state.trip
                    val isOwnerTrip = trip.rideType.equals("OWNER", ignoreCase = true)
                    val isAgentOrOwner = trip.agentId == state.currentUserId
                    val isDriver = trip.driverId == state.currentUserId

                    // Configure Google Map with pickup, destination, and route
                    LaunchedEffect(trip) {
                        mapView.getMapAsync { googleMap ->
                            googleMap.clear()
                            val pickupLatLng = LatLng(trip.pickupLat, trip.pickupLng)
                            val destLatLng = LatLng(trip.destLat, trip.destLng)

                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(pickupLatLng)
                                    .title("Pickup: ${trip.pickupCity}")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                            )

                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(destLatLng)
                                    .title("Destination: ${trip.destCity}")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            )

                            // Geodesic route polyline connecting pickup to destination
                            googleMap.addPolyline(
                                PolylineOptions()
                                    .add(pickupLatLng, destLatLng)
                                    .width(10f)
                                    .color(android.graphics.Color.parseColor("#FF6B35"))
                                    .geodesic(true)
                            )

                            try {
                                val bounds = LatLngBounds.builder()
                                    .include(pickupLatLng)
                                    .include(destLatLng)
                                    .build()
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                            } catch (_: Exception) {
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 10f))
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Embedded Google Map View
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            AndroidView(
                                factory = { mapView },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Floating status & ride type badge on top of map
                            Surface(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopStart),
                                shape = RoundedCornerShape(12.dp),
                                color = RaahiBg2.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, RaahiBorderSoft),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                when (trip.status) {
                                                    "IN_PROGRESS" -> Color(0xFF22B573)
                                                    "COMPLETED" -> Color(0xFF4D9EFF)
                                                    "DISPUTED" -> Color(0xFFE5484D)
                                                    else -> RaahiOrange
                                                },
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = trip.status.replace("_", " "),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = RaahiText
                                    )
                                    RaahiRideTypeBadge(trip.rideType)
                                }
                            }
                        }

                        // Bottom Action & Information Sheet
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            color = RaahiBg2,
                            shadowElevation = 16.dp,
                            border = BorderStroke(1.dp, RaahiBorderSoft),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                // Notification Banners
                                if (actionError != null) {
                                    Surface(
                                        color = Color(0xFFFFECEC),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = actionError ?: "",
                                            color = Color(0xFFE5484D),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                if (actionSuccess != null) {
                                    Surface(
                                        color = Color(0xFFE8F8F0),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = actionSuccess ?: "",
                                            color = Color(0xFF22B573),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                // Dispute Alert Banner
                                if (trip.status == "DISPUTED") {
                                    Surface(
                                        color = Color(0xFFFFECEC),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFFE5484D)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFE5484D), modifier = Modifier.size(18.dp))
                                                Text("TRIP UNDER DISPUTE", fontWeight = FontWeight.Bold, color = Color(0xFFE5484D), fontSize = 13.sp)
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = trip.disputeReason ?: "A dispute was raised for this trip. Raahi Support is reviewing details.",
                                                fontSize = 12.sp,
                                                color = RaahiText
                                            )
                                            if (!trip.disputeEvidence.isNullOrBlank()) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "Details: ${trip.disputeEvidence}",
                                                    fontSize = 11.sp,
                                                    color = RaahiTextDim
                                                )
                                            }
                                            if (isAgentOrOwner) {
                                                Spacer(Modifier.height(8.dp))
                                                Button(
                                                    onClick = { showResolveDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Resolve Dispute", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Route Info Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(trip.pickupCity, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RaahiText)
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(16.dp))
                                        Text(trip.destCity, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = RaahiText)
                                    }

                                    Text("₹${trip.fare.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = RaahiOrange)
                                }

                                // Google Maps External Turn-by-Turn Navigation Button
                                // Note: Launches Google Maps Turn-by-Turn without altering or completing the active trip session
                                val targetLat = if (trip.status in listOf("ASSIGNED", "ARRIVED")) trip.pickupLat else trip.destLat
                                val targetLng = if (trip.status in listOf("ASSIGNED", "ARRIVED")) trip.pickupLng else trip.destLng
                                val targetLabel = if (trip.status in listOf("ASSIGNED", "ARRIVED")) "Pickup (${trip.pickupCity})" else "Destination (${trip.destCity})"
                                val targetAddress = if (trip.status in listOf("ASSIGNED", "ARRIVED")) trip.pickupAddress else trip.destAddress

                                Button(
                                    onClick = {
                                        val navUri = Uri.parse("google.navigation:q=$targetLat,$targetLng&mode=d")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                                            setPackage("com.google.android.apps.maps")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (_: Exception) {
                                            val geoUri = Uri.parse("geo:$targetLat,$targetLng?q=${Uri.encode(targetAddress.ifBlank { targetLabel })}")
                                            val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            try {
                                                context.startActivity(fallbackIntent)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RaahiBlue),
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Turn-by-Turn Navigation (Google Maps)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // Financial Breakdown Surface
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = RaahiBg,
                                    border = BorderStroke(1.dp, RaahiBorderSoft),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isOwnerTrip) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Ride Type", fontSize = 12.sp, color = RaahiTextDim)
                                                Text("Direct Owner Journey (0% Commission)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF22B573))
                                            }
                                            if (trip.estimatedDistanceKm != null) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Calculated Road Distance", fontSize = 12.sp, color = RaahiTextDim)
                                                    Text("${trip.estimatedDistanceKm} km", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RaahiText)
                                                }
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Driver Payout (100%)", fontSize = 12.sp, color = RaahiTextDim)
                                                Text("₹${trip.fare.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiText)
                                            }
                                        } else {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Customer Fare", fontSize = 12.sp, color = RaahiTextDim)
                                                Text("₹${(trip.customerFare ?: trip.fare).toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RaahiText)
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Driver Payout", fontSize = 12.sp, color = RaahiTextDim)
                                                Text("₹${(trip.driverPayout ?: trip.fare).toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF22B573))
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Agent Commission", fontSize = 12.sp, color = RaahiTextDim)
                                                Text("₹${(trip.agentCommission ?: 0.0).toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RaahiOrange)
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Settlement Responsibility", fontSize = 12.sp, color = RaahiTextDim)
                                                Text(trip.settlementResponsibility?.replace("_", " ") ?: "Driver to Agent", fontWeight = FontWeight.Medium, fontSize = 11.sp, color = RaahiText)
                                            }
                                        }
                                    }
                                }

                                // Calling counterparty button
                                val phoneToCall = if (isAgentOrOwner) trip.driverPhone else trip.agentPhone
                                val nameToCall = if (isAgentOrOwner) {
                                    trip.driverName ?: "Driver"
                                } else {
                                    if (isOwnerTrip) trip.agentName ?: "Owner" else trip.agencyName ?: trip.agentName ?: "Travel Agent"
                                }
                                if (!phoneToCall.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneToCall"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Call $nameToCall ($phoneToCall)", fontSize = 13.sp)
                                    }
                                }

                                // AGENT / OWNER VIEW: OTP DISPLAY & SETTLEMENT CONTROLS
                                if (isAgentOrOwner) {
                                    if (trip.pickupOtp != null && trip.status in listOf("ASSIGNED", "ARRIVED")) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFFFF7ED),
                                            border = BorderStroke(1.5.dp, RaahiOrange),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Pickup OTP:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiOrange)
                                                    Text("Share with driver when they arrive", fontSize = 11.sp, color = RaahiTextDim)
                                                }
                                                Text(trip.pickupOtp, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = RaahiOrange)
                                            }
                                        }
                                    }

                                    // Agent Settlement Confirmation
                                    if (!isOwnerTrip && trip.status == "SETTLEMENT_SUBMITTED") {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFFFF7ED),
                                            border = BorderStroke(1.5.dp, RaahiOrange),
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Settlement Submitted by Driver", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiOrange)
                                                Text("Reference: ${trip.settlementRef ?: "N/A"}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RaahiText)
                                                if (!trip.settlementNotes.isNullOrBlank()) {
                                                    Text("Notes: ${trip.settlementNotes}", fontSize = 12.sp, color = RaahiTextDim)
                                                }
                                                Button(
                                                    onClick = { viewModel.confirmSettlement(trip.id) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22B573)),
                                                    enabled = !actionLoading,
                                                ) {
                                                    Text("Confirm Settlement Received", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // Agent Completion OTP Banner
                                    if (!isOwnerTrip && trip.completionOtp != null && (trip.status == "SETTLEMENT_CONFIRMED" || trip.status == "IN_PROGRESS")) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFE8F8F0),
                                            border = BorderStroke(1.5.dp, Color(0xFF22B573)),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Completion OTP:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF22B573))
                                                    Text("Share with driver to complete drop", fontSize = 11.sp, color = RaahiTextDim)
                                                }
                                                Text(trip.completionOtp, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF22B573))
                                            }
                                        }
                                    }

                                    if (trip.status == "COMPLETED" && trip.paymentStatus != "SETTLED") {
                                        Button(
                                            onClick = { viewModel.settlePayment(trip.id) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22B573)),
                                            enabled = !actionLoading,
                                        ) {
                                            Text("Mark Payment Settled", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // DRIVER VIEW: WORKFLOW CONTROLS
                                if (isDriver) {
                                    when (trip.status) {
                                        "ASSIGNED" -> {
                                            Button(
                                                onClick = { viewModel.arriveAtPickup(trip.id) },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                                                enabled = !actionLoading,
                                            ) {
                                                Text("I Have Arrived at Pickup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            }
                                        }
                                        "ARRIVED" -> {
                                            Button(
                                                onClick = {
                                                    isPickupOtpVerification = true
                                                    enteredOtp = ""
                                                    showOtpDialog = true
                                                },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                                                enabled = !actionLoading,
                                            ) {
                                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Enter Pickup OTP from ${if (isOwnerTrip) "Owner" else "Agent/Customer"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                        "IN_PROGRESS" -> {
                                            if (isOwnerTrip) {
                                                // OWNER RIDE: Direct completion by Driver, no completion OTP required, zero commission!
                                                Button(
                                                    onClick = { viewModel.completeOwnerTrip(trip.id) },
                                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22B573)),
                                                    enabled = !actionLoading,
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Arrived at Destination — Complete Trip", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                            } else {
                                                // AGENT RIDE: Move to settlement stage
                                                Button(
                                                    onClick = { viewModel.arriveAtDestination(trip.id) },
                                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22B573)),
                                                    enabled = !actionLoading,
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Arrived at Destination — Begin Settlement", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                        "AWAITING_AGENT_SETTLEMENT" -> {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFFFF7ED),
                                                border = BorderStroke(1.dp, RaahiOrange),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Awaiting Settlement with Agent", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiOrange)
                                                    Text("Please transfer commission or submit payment reference to the Travel Agent.", fontSize = 12.sp, color = RaahiTextDim)
                                                    Button(
                                                        onClick = {
                                                            settlementRefInput = ""
                                                            settlementNotesInput = ""
                                                            showSettlementDialog = true
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                                                        enabled = !actionLoading
                                                    ) {
                                                        Text("Submit Settlement Reference", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        "SETTLEMENT_SUBMITTED" -> {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFEAF3FF),
                                                border = BorderStroke(1.dp, RaahiBlue),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text("Settlement Reference Submitted", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiBlue)
                                                    Spacer(Modifier.height(4.dp))
                                                    Text("Ref: ${trip.settlementRef ?: ""}", fontSize = 12.sp, color = RaahiText, fontWeight = FontWeight.SemiBold)
                                                    Text("Waiting for Travel Agent to confirm settlement...", fontSize = 12.sp, color = RaahiTextDim)
                                                }
                                            }
                                        }
                                        "SETTLEMENT_CONFIRMED" -> {
                                            Button(
                                                onClick = {
                                                    isPickupOtpVerification = false
                                                    enteredOtp = ""
                                                    showOtpDialog = true
                                                },
                                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22B573)),
                                                enabled = !actionLoading,
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text("Enter Completion OTP from Agent", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                        "COMPLETED" -> {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color(0xFFE8F8F0),
                                                shape = RoundedCornerShape(12.dp),
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🎉 Trip Successfully Completed!", fontWeight = FontWeight.Bold, color = Color(0xFF22B573), fontSize = 15.sp)
                                                    Text("Fare ₹${trip.fare.toInt()} settled per agreement.", color = RaahiTextDim, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Secondary Actions: Dispute & Cancellation
                                if (trip.status in listOf("ASSIGNED", "ARRIVED", "IN_PROGRESS", "AWAITING_AGENT_SETTLEMENT", "SETTLEMENT_SUBMITTED", "SETTLEMENT_CONFIRMED")) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { showDisputeDialog = true }) {
                                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFE5484D), modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Report Dispute", color = Color(0xFFE5484D), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        if (trip.status in listOf("ASSIGNED", "ARRIVED")) {
                                            TextButton(onClick = { showCancelModal = true }) {
                                                Text("Cancel Trip", color = RaahiTextDim, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Cancel Trip Modal
                    if (showCancelModal) {
                        CancelIntercityTripModal(
                            trip = trip,
                            isCancelling = actionLoading,
                            errorMessage = actionError,
                            onDismiss = { showCancelModal = false },
                            onConfirmCancel = { reason ->
                                viewModel.cancelTrip(
                                    tripId = trip.id,
                                    reason = reason,
                                    onSuccess = { showCancelModal = false }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // OTP Verification Dialog (Pickup OTP or Drop Completion OTP)
    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = {
                Text(
                    if (isPickupOtpVerification) "Verify Pickup OTP" else "Verify Destination OTP",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        if (isPickupOtpVerification)
                            "Ask the owner/agent for the 6-digit pickup OTP to start the journey:"
                        else
                            "Ask the agent/passenger for the 6-digit destination arrival OTP to finalize this trip:",
                        fontSize = 13.sp,
                        color = RaahiTextDim
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { if (it.length <= 6) enteredOtp = it.filter { ch -> ch.isDigit() } },
                        label = { Text("6-Digit OTP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOtpDialog = false
                        if (isPickupOtpVerification) {
                            viewModel.verifyPickupOtp(tripId, enteredOtp)
                        } else {
                            viewModel.verifyCompletionOtp(tripId, enteredOtp)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPickupOtpVerification) RaahiOrange else Color(0xFF22B573)
                    ),
                    enabled = enteredOtp.length == 6,
                ) {
                    Text("Verify & Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settlement Reference Dialog for Drivers
    if (showSettlementDialog) {
        AlertDialog(
            onDismissRequest = { showSettlementDialog = false },
            title = {
                Text("Submit Commission Settlement", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter payment transaction reference (UPI UTR / Reference ID / Cash receipt):", fontSize = 13.sp, color = RaahiTextDim)
                    OutlinedTextField(
                        value = settlementRefInput,
                        onValueChange = { settlementRefInput = it },
                        label = { Text("Reference / UTR ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = settlementNotesInput,
                        onValueChange = { settlementNotesInput = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettlementDialog = false
                        viewModel.submitSettlement(tripId, settlementRefInput.trim(), settlementNotesInput.trim().takeIf { it.isNotBlank() })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                    enabled = settlementRefInput.isNotBlank() && !actionLoading
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettlementDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dispute Creation Dialog
    if (showDisputeDialog) {
        AlertDialog(
            onDismissRequest = { showDisputeDialog = false },
            title = {
                Text("Report / Raise Dispute", fontWeight = FontWeight.Bold, color = Color(0xFFE5484D))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Describe the issue encountered during this trip:", fontSize = 13.sp, color = RaahiTextDim)
                    OutlinedTextField(
                        value = disputeReasonInput,
                        onValueChange = { disputeReasonInput = it },
                        label = { Text("Reason for Dispute") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = disputeEvidenceInput,
                        onValueChange = { disputeEvidenceInput = it },
                        label = { Text("Additional Evidence / Details (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisputeDialog = false
                        viewModel.raiseDispute(tripId, disputeReasonInput.trim(), disputeEvidenceInput.trim().takeIf { it.isNotBlank() })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)),
                    enabled = disputeReasonInput.isNotBlank() && !actionLoading
                ) {
                    Text("Submit Dispute")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisputeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dispute Resolution Dialog for Agent/Admin
    if (showResolveDialog) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = {
                Text("Resolve Dispute", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter resolution notes and agreement outcome:", fontSize = 13.sp, color = RaahiTextDim)
                    OutlinedTextField(
                        value = resolveNotesInput,
                        onValueChange = { resolveNotesInput = it },
                        label = { Text("Resolution Notes") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResolveDialog = false
                        viewModel.resolveDispute(tripId, resolveNotesInput.trim(), "RESOLVED")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22B573)),
                    enabled = resolveNotesInput.isNotBlank() && !actionLoading
                ) {
                    Text("Confirm Resolution")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
