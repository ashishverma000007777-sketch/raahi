package `in`.raahi.app.ui.screens.intercity

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
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
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import `in`.raahi.app.data.GoogleRoutesProvider
import `in`.raahi.app.data.GoogleRoutesStatus
import `in`.raahi.app.network.CreateIntercityTripRequest
import `in`.raahi.app.ui.components.RaahiRideTypeBadge
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntercityCreateTripScreen(
    onBack: () -> Unit,
    onTripCreated: (String) -> Unit,
    viewModel: IntercityViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val routesProvider = remember { GoogleRoutesProvider(context.applicationContext) }

    // Ride Type: OWNER vs AGENT
    var rideType by remember { mutableStateOf("OWNER") }

    var pickupCity by remember { mutableStateOf("Delhi") }
    var destCity by remember { mutableStateOf("Jaipur") }
    var pickupAddress by remember { mutableStateOf("Connaught Place, New Delhi") }
    var destAddress by remember { mutableStateOf("MI Road, Jaipur") }
    var tripType by remember { mutableStateOf("ONE_WAY") }
    var vehicleCategory by remember { mutableStateOf("SEDAN") }
    var fareText by remember { mutableStateOf("4500") }
    var specialInstructions by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("DIRECT_CASH") }

    // Coordinates lookup for common cities
    fun getCityCoordinates(name: String): Pair<Double, Double> = when (name.trim().lowercase()) {
        "delhi", "new delhi" -> 28.6139 to 77.2090
        "jaipur" -> 26.9124 to 75.7873
        "chandigarh" -> 30.7333 to 76.7794
        "agra" -> 27.1767 to 78.0081
        "mumbai" -> 19.0760 to 72.8777
        "pune" -> 18.5204 to 73.8567
        "bangalore", "bengaluru" -> 12.9716 to 77.5946
        "lucknow" -> 26.8467 to 80.9462
        else -> 28.6139 to 77.2090
    }

    var pickupLat by remember { mutableDoubleStateOf(28.6139) }
    var pickupLng by remember { mutableDoubleStateOf(77.2090) }
    var destLat by remember { mutableDoubleStateOf(26.9124) }
    var destLng by remember { mutableDoubleStateOf(75.7873) }

    // Road distance and pricing
    var roadDistanceKm by remember { mutableStateOf<Double?>(null) }
    var estimatedDurationMin by remember { mutableStateOf<Int?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }
    var suggestedFare by remember { mutableIntStateOf(0) }
    var minimumFare by remember { mutableIntStateOf(0) }

    // Agent financial terms
    var customerFareText by remember { mutableStateOf("5000") }
    var driverPayoutText by remember { mutableStateOf("4000") }
    var agentCommissionText by remember { mutableStateOf("1000") }
    var paymentRecipient by remember { mutableStateOf("DRIVER") } // DRIVER or AGENT
    var settlementResponsibility by remember { mutableStateOf("DRIVER_TO_AGENT") } // DRIVER_TO_AGENT or AGENT_TO_DRIVER

    var selectedDepartureIndex by remember { mutableIntStateOf(0) }
    var createdTripId by remember { mutableStateOf<String?>(null) }

    val actionLoading by viewModel.actionLoading.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

    val departureOptions = remember {
        listOf(
            "In 2 Hours" to { Instant.now().plus(2, ChronoUnit.HOURS) },
            "In 4 Hours" to { Instant.now().plus(4, ChronoUnit.HOURS) },
            "Tomorrow Morning" to { Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS) },
            "Tomorrow Evening" to { Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(18, ChronoUnit.HOURS) },
        )
    }

    // Google MapView lifecycle
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

    // Function to calculate route distance & fares
    fun calculateRouteAndFares() {
        coroutineScope.launch {
            isCalculatingRoute = true
            routeError = null
            try {
                when (val res = routesProvider.computeDrivingRoute(pickupLat, pickupLng, destLat, destLng)) {
                    is GoogleRoutesStatus.Success -> {
                        val dist = res.route.distanceKm
                        roadDistanceKm = dist
                        estimatedDurationMin = res.route.durationMinutes

                        // Formula:
                        // suggestedFare = ceil(distanceKm * 15)
                        // if distanceKm <= 50: minimumFare = ceil(distanceKm * 15)
                        // else: minimumFare = ceil(distanceKm * 12)
                        val sugg = ceil(dist * 15.0).toInt()
                        val min = if (dist <= 50.0) ceil(dist * 15.0).toInt() else ceil(dist * 12.0).toInt()
                        suggestedFare = sugg
                        minimumFare = min

                        if (rideType == "OWNER") {
                            fareText = sugg.toString()
                        }
                        isCalculatingRoute = false
                    }
                    is GoogleRoutesStatus.Unavailable -> {
                        routeError = "Failed to calculate road distance: ${res.reason}. Please retry."
                        isCalculatingRoute = false
                    }
                    else -> {
                        isCalculatingRoute = false
                    }
                }
            } catch (e: Exception) {
                routeError = "Routing error: ${e.message ?: "Unable to reach Google Routes service"}"
                isCalculatingRoute = false
            }
        }
    }

    // Recalculate route whenever cities change
    LaunchedEffect(pickupCity, destCity) {
        val (pLat, pLng) = getCityCoordinates(pickupCity)
        val (dLat, dLng) = getCityCoordinates(destCity)
        pickupLat = pLat
        pickupLng = pLng
        destLat = dLat
        destLng = dLng
        calculateRouteAndFares()
    }

    // Update Google Map pins and polyline
    LaunchedEffect(pickupLat, pickupLng, destLat, destLng) {
        mapView.getMapAsync { googleMap ->
            googleMap.clear()
            val pLatLng = LatLng(pickupLat, pickupLng)
            val dLatLng = LatLng(destLat, destLng)

            googleMap.addMarker(
                MarkerOptions()
                    .position(pLatLng)
                    .title("Pickup: $pickupCity")
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )

            googleMap.addMarker(
                MarkerOptions()
                    .position(dLatLng)
                    .title("Destination: $destCity")
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )

            googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {}
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragEnd(marker: Marker) {
                    val pos = marker.position
                    if (marker.title?.startsWith("Pickup") == true) {
                        pickupLat = pos.latitude
                        pickupLng = pos.longitude
                    } else {
                        destLat = pos.latitude
                        destLng = pos.longitude
                    }
                    calculateRouteAndFares()
                }
            })

            googleMap.addPolyline(
                PolylineOptions()
                    .add(pLatLng, dLatLng)
                    .width(8f)
                    .color(android.graphics.Color.parseColor("#F46B3F"))
                    .geodesic(true)
            )

            try {
                val bounds = LatLngBounds.builder().include(pLatLng).include(dLatLng).build()
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (_: Exception) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pLatLng, 10f))
            }
        }
    }

    // Validations
    val isSameCity = pickupCity.isNotBlank() && destCity.isNotBlank() && pickupCity.trim().equals(destCity.trim(), ignoreCase = true)
    val fareVal = fareText.toDoubleOrNull() ?: 0.0
    val isRouteValid = pickupCity.isNotBlank() && destCity.isNotBlank() && !isSameCity && pickupAddress.isNotBlank() && destAddress.isNotBlank()

    // Owner fare validation: cannot be below minimum allowed fare
    val isOwnerFareValid = roadDistanceKm != null && roadDistanceKm!! > 0.0 && fareVal >= minimumFare && minimumFare > 0
    val isAgentFareValid = (customerFareText.toDoubleOrNull() ?: 0.0) > 0.0 && (driverPayoutText.toDoubleOrNull() ?: 0.0) >= 0.0

    val isFormValid = if (rideType == "OWNER") {
        isRouteValid && isOwnerFareValid && !isCalculatingRoute
    } else {
        isRouteValid && isAgentFareValid
    }

    if (createdTripId != null) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF22B573),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    if (rideType == "OWNER") "Owner Trip Published!" else "Agent Trip Published!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = RaahiText,
                )
            },
            text = {
                Text(
                    if (rideType == "OWNER") {
                        "Your owner ride for ${pickupCity.trim()} → ${destCity.trim()} is published for free. Drivers will see the ₹$fareText fare and can apply."
                    } else {
                        "Your commercial assignment for ${pickupCity.trim()} → ${destCity.trim()} is live with customer fare ₹$customerFareText and driver payout ₹$driverPayoutText."
                    },
                    fontSize = 14.sp,
                    color = RaahiTextDim,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = createdTripId
                        createdTripId = null
                        if (id != null) onTripCreated(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("View Trip Details", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = RaahiBg2,
            shape = RoundedCornerShape(18.dp),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (rideType == "OWNER") "Post Ride as Owner" else "Post Ride as Travel Agent",
                            color = RaahiText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            if (rideType == "OWNER") "Zero commission • Verified drivers" else "Commercial route & commission management",
                            color = RaahiTextDim,
                            fontSize = 11.5.sp
                        )
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ==========================================
            // RIDE TYPE SELECTOR TOGGLE (OWNER vs AGENT)
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = RaahiBg2,
                border = BorderStroke(1.dp, RaahiBorderSoft),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Select Posting Type",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RaahiTextDim,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // OWNER Option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    rideType = "OWNER"
                                    if (suggestedFare > 0) fareText = suggestedFare.toString()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (rideType == "OWNER") Color(0xFFE8F5E9) else RaahiBg,
                            border = BorderStroke(
                                1.5.dp,
                                if (rideType == "OWNER") Color(0xFF22B573) else RaahiBorderSoft
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.DirectionsCar,
                                        contentDescription = null,
                                        tint = if (rideType == "OWNER") Color(0xFF1B5E20) else RaahiTextDim,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Vehicle Owner",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (rideType == "OWNER") Color(0xFF1B5E20) else RaahiText,
                                    )
                                }
                                Text("Free • Direct Cash", fontSize = 10.5.sp, color = RaahiTextDim)
                            }
                        }

                        // AGENT Option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    rideType = "AGENT"
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (rideType == "AGENT") Color(0xFFEDE7F6) else RaahiBg,
                            border = BorderStroke(
                                1.5.dp,
                                if (rideType == "AGENT") Color(0xFF5E35B1) else RaahiBorderSoft
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.SupportAgent,
                                        contentDescription = null,
                                        tint = if (rideType == "AGENT") Color(0xFF4527A0) else RaahiTextDim,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Travel Agent",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (rideType == "AGENT") Color(0xFF4527A0) else RaahiText,
                                    )
                                }
                                Text("Commission & Terms", fontSize = 10.5.sp, color = RaahiTextDim)
                            }
                        }
                    }
                }
            }

            // Error Banner if present
            if (actionError != null) {
                Surface(
                    color = Color(0xFFFFECEC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5484D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFE5484D), modifier = Modifier.size(20.dp))
                        Text(
                            text = actionError ?: "",
                            color = Color(0xFFE5484D),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ==========================================
            // SECTION 1: ROUTE & GOOGLE MAPS
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiBg2,
                border = BorderStroke(1.dp, RaahiBorderSoft),
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(22.dp).background(RaahiOrange, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text("Route & Location Selection", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                    }

                    // City search fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pickupCity,
                            onValueChange = { pickupCity = it },
                            label = { Text("Pickup City *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = isSameCity || pickupCity.isBlank(),
                        )
                        OutlinedTextField(
                            value = destCity,
                            onValueChange = { destCity = it },
                            label = { Text("Destination City *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            isError = isSameCity || destCity.isBlank(),
                        )
                    }

                    if (isSameCity) {
                        Text(
                            "Origin and destination city cannot be identical.",
                            color = Color(0xFFE5484D),
                            fontSize = 12.sp,
                        )
                    }

                    OutlinedTextField(
                        value = pickupAddress,
                        onValueChange = { pickupAddress = it },
                        label = { Text("Pickup Address / Landmark *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = destAddress,
                        onValueChange = { destAddress = it },
                        label = { Text("Destination Address / Landmark *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    // Embedded Google Map for Location & Pin Verification
                    Text("Interactive Google Map Preview (Drag pins to adjust)", fontSize = 12.sp, color = RaahiTextDim)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(RaahiBg, RoundedCornerShape(12.dp))
                    ) {
                        AndroidView(
                            factory = { mapView },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Distance & ETA status card
                    if (isCalculatingRoute) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RaahiOrange, strokeWidth = 2.dp)
                            Text("Calculating actual road distance via Google Routes...", fontSize = 12.sp, color = RaahiTextDim)
                        }
                    } else if (routeError != null) {
                        Surface(
                            color = Color(0xFFFFECEC),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(routeError ?: "", color = Color(0xFFE5484D), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { calculateRouteAndFares() }) {
                                    Text("Retry", color = RaahiOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (roadDistanceKm != null) {
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text("Calculated Road Distance", fontSize = 11.sp, color = RaahiTextDim)
                                    Text("${roadDistanceKm?.toInt()} km", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF15803D))
                                }
                                if (estimatedDurationMin != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Estimated Road Time", fontSize = 11.sp, color = RaahiTextDim)
                                        Text("${estimatedDurationMin!! / 60}h ${estimatedDurationMin!! % 60}m", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RaahiText)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 2: PRICING & COMMERCIAL TERMS
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiBg2,
                border = BorderStroke(1.dp, RaahiBorderSoft),
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(22.dp).background(RaahiOrange, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            if (rideType == "OWNER") "Owner Fare & Pricing" else "Agent Commercial Terms",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = RaahiText
                        )
                    }

                    if (rideType == "OWNER") {
                        // OWNER PRICING LOGIC
                        if (roadDistanceKm != null && roadDistanceKm!! > 0) {
                            val distInt = roadDistanceKm!!.toInt()
                            Surface(
                                color = RaahiSelectionBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Route Distance: $distInt km", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiText)
                                    Text("• Suggested fare at ₹15/km: ₹$suggestedFare", fontSize = 12.5.sp, color = RaahiText)
                                    Text(
                                        "• Minimum allowed fare: ₹$minimumFare (${if (roadDistanceKm!! <= 50.0) "₹15/km for ≤50km" else "₹12/km for >50km"})",
                                        fontSize = 12.5.sp,
                                        color = RaahiTextDim
                                    )
                                    Text(
                                        "Owner ride posting is 100% FREE. No platform commission.",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = fareText,
                                onValueChange = { fareText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Enter Your Ride Fare (₹) *") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RaahiOrange) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = !isOwnerFareValid && fareText.isNotBlank(),
                                supportingText = {
                                    if (fareVal < minimumFare && fareText.isNotBlank()) {
                                        Text(
                                            "Your route is $distInt km. Suggested fare at ₹15/km: ₹$suggestedFare. Minimum allowed fare: ₹$minimumFare. Please enter at least ₹$minimumFare.",
                                            color = Color(0xFFE5484D),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                        )
                                    } else {
                                        Text("Prefilled with suggested fare. You may enter any fare ≥ ₹$minimumFare.", fontSize = 11.5.sp)
                                    }
                                }
                            )
                        } else {
                            Text(
                                "Enter pickup and destination to calculate road distance and suggested fare.",
                                fontSize = 12.5.sp,
                                color = RaahiTextDim
                            )
                        }
                    } else {
                        // AGENT COMMERCIAL TERMS
                        Surface(
                            color = Color(0xFFEDE7F6),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "${pickupCity.uppercase()} → ${destCity.uppercase()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF4527A0)
                                )
                                Text("Agreed direct fare with driver payout and agency commission", fontSize = 12.sp, color = RaahiTextDim)
                            }
                        }

                        // Customer Fare
                        OutlinedTextField(
                            value = customerFareText,
                            onValueChange = {
                                customerFareText = it.filter { ch -> ch.isDigit() }
                                val c = customerFareText.toDoubleOrNull() ?: 0.0
                                val d = driverPayoutText.toDoubleOrNull() ?: 0.0
                                agentCommissionText = (c - d).coerceAtLeast(0.0).toInt().toString()
                            },
                            label = { Text("Customer Agreed Fare (₹) *") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RaahiOrange) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        // Driver Share
                        OutlinedTextField(
                            value = driverPayoutText,
                            onValueChange = {
                                driverPayoutText = it.filter { ch -> ch.isDigit() }
                                val c = customerFareText.toDoubleOrNull() ?: 0.0
                                val d = driverPayoutText.toDoubleOrNull() ?: 0.0
                                agentCommissionText = (c - d).coerceAtLeast(0.0).toInt().toString()
                            },
                            label = { Text("Driver Payout / Share (₹) *") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = Color(0xFF22B573)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        // Agent Commission
                        OutlinedTextField(
                            value = agentCommissionText,
                            onValueChange = { agentCommissionText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Agent Commission (₹) *") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = Color(0xFF5E35B1)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        // Payment Recipient
                        Text("Payment Collected By", fontSize = 12.sp, color = RaahiTextDim)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("DRIVER" to "Directly by Driver", "AGENT" to "Collected by Agent").forEach { (rec, label) ->
                                val isSelected = paymentRecipient == rec
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { paymentRecipient = rec },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFEDE7F6) else RaahiBg,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF5E35B1) else RaahiBorderSoft),
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF4527A0) else RaahiText,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }

                        // Settlement Responsibility
                        Text("Commission Settlement Responsibility", fontSize = 12.sp, color = RaahiTextDim)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("DRIVER_TO_AGENT" to "Driver pays Agent", "AGENT_TO_DRIVER" to "Agent pays Driver").forEach { (resp, label) ->
                                val isSelected = settlementResponsibility == resp
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { settlementResponsibility = resp },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFEDE7F6) else RaahiBg,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF5E35B1) else RaahiBorderSoft),
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF4527A0) else RaahiText,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Trip Type Segmented Buttons
                    Text("Trip Type", fontSize = 13.sp, color = RaahiTextDim)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ONE_WAY" to "One Way", "ROUND_TRIP" to "Round Trip").forEach { (type, label) ->
                            val isSelected = tripType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tripType = type },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) RaahiSelectionBg else RaahiBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) RaahiOrange else RaahiBorderSoft
                                ),
                            ) {
                                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) RaahiOrange else RaahiText,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }

                    // Departure Schedule
                    Text("Departure Time", fontSize = 13.sp, color = RaahiTextDim)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        departureOptions.forEachIndexed { index, (label, _) ->
                            val isSelected = selectedDepartureIndex == index
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDepartureIndex = index },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) RaahiOrange else RaahiBg,
                                border = BorderStroke(1.dp, if (isSelected) RaahiOrange else RaahiBorderSoft),
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else RaahiText,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    // Vehicle Categories
                    Text("Required Vehicle Category", fontSize = 13.sp, color = RaahiTextDim)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SEDAN", "SUV", "HATCHBACK", "LUXURY", "TRAVELLER").forEach { cat ->
                            val isSelected = vehicleCategory == cat
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { vehicleCategory = cat },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) RaahiOrange else RaahiBg,
                                border = BorderStroke(1.dp, if (isSelected) RaahiOrange else RaahiBorderSoft),
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = cat.take(4),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else RaahiText,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        }
                    }

                    // Special Instructions
                    OutlinedTextField(
                        value = specialInstructions,
                        onValueChange = { specialInstructions = it },
                        label = { Text("Special Instructions (Optional)") },
                        placeholder = { Text("e.g. AC on throughout, toll included in fare") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                    )
                }
            }

            // ==========================================
            // SECTION 3: LIVE REVIEW CARD
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiBg2,
                border = BorderStroke(1.dp, RaahiOrange.copy(alpha = 0.4f)),
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(22.dp).background(RaahiOrange, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                        }

                        RaahiRideTypeBadge(rideType)
                    }

                    HorizontalDivider(color = RaahiBorderSoft)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Route", fontSize = 11.5.sp, color = RaahiTextDim)
                            Text(
                                "${pickupCity.trim().ifEmpty { "..." }} → ${destCity.trim().ifEmpty { "..." }}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = RaahiText,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Vehicle", fontSize = 11.5.sp, color = RaahiTextDim)
                            Text(
                                vehicleCategory,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = RaahiText,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Departure", fontSize = 11.5.sp, color = RaahiTextDim)
                            Text(
                                departureOptions[selectedDepartureIndex].first,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = RaahiText,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (rideType == "OWNER") "Total Fare" else "Customer Fare",
                                fontSize = 11.5.sp,
                                color = RaahiTextDim
                            )
                            Text(
                                "₹${if (rideType == "OWNER") fareText.ifEmpty { "0" } else customerFareText.ifEmpty { "0" }}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF22B573),
                            )
                        }
                    }
                }
            }

            // ==========================================
            // SECTION 4: PUBLISH ACTION
            // ==========================================
            Button(
                onClick = {
                    if (actionLoading || !isFormValid) return@Button

                    val departureIso = departureOptions[selectedDepartureIndex].second().toString()

                    val req = if (rideType == "OWNER") {
                        CreateIntercityTripRequest(
                            pickupCity = pickupCity.trim(),
                            destCity = destCity.trim(),
                            pickupAddress = pickupAddress.trim(),
                            destAddress = destAddress.trim(),
                            pickupLat = pickupLat,
                            pickupLng = pickupLng,
                            destLat = destLat,
                            destLng = destLng,
                            departureTime = departureIso,
                            tripType = tripType,
                            vehicleCategory = vehicleCategory,
                            estimatedDistanceKm = roadDistanceKm,
                            estimatedDurationMin = estimatedDurationMin,
                            fare = fareVal,
                            paymentMode = paymentMode,
                            specialInstructions = specialInstructions.trim().ifEmpty { null },
                            rideType = "OWNER",
                            customerFare = fareVal,
                            driverPayout = fareVal,
                            agentCommission = 0.0,
                            paymentRecipient = "DRIVER",
                            settlementResponsibility = "NONE",
                            saveAsDraft = false,
                        )
                    } else {
                        val cFare = customerFareText.toDoubleOrNull() ?: fareVal
                        val dPayout = driverPayoutText.toDoubleOrNull() ?: fareVal
                        val comm = agentCommissionText.toDoubleOrNull() ?: (cFare - dPayout)
                        CreateIntercityTripRequest(
                            pickupCity = pickupCity.trim(),
                            destCity = destCity.trim(),
                            pickupAddress = pickupAddress.trim(),
                            destAddress = destAddress.trim(),
                            pickupLat = pickupLat,
                            pickupLng = pickupLng,
                            destLat = destLat,
                            destLng = destLng,
                            departureTime = departureIso,
                            tripType = tripType,
                            vehicleCategory = vehicleCategory,
                            estimatedDistanceKm = roadDistanceKm,
                            estimatedDurationMin = estimatedDurationMin,
                            fare = cFare,
                            paymentMode = paymentMode,
                            specialInstructions = specialInstructions.trim().ifEmpty { null },
                            rideType = "AGENT",
                            customerFare = cFare,
                            driverPayout = dPayout,
                            agentCommission = comm,
                            paymentRecipient = paymentRecipient,
                            settlementResponsibility = settlementResponsibility,
                            saveAsDraft = false,
                        )
                    }

                    viewModel.createTrip(req) { created ->
                        createdTripId = created.id
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RaahiOrange),
                enabled = !actionLoading && isFormValid,
            ) {
                if (actionLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (rideType == "OWNER") "Publish Owner Ride (Free)" else "Publish Agent Trip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
