package `in`.raahi.app.ui.screens.intercity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import `in`.raahi.app.data.LocationResolver
import `in`.raahi.app.network.CreateIntercityTripRequest
import `in`.raahi.app.ui.components.RaahiRideTypeBadge
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    var pickupCity by remember { mutableStateOf("Chandigarh") }
    var destCity by remember { mutableStateOf("Delhi") }
    var pickupAddress by remember { mutableStateOf("Sector 17, Chandigarh") }
    var destAddress by remember { mutableStateOf("Connaught Place, New Delhi") }
    var tripType by remember { mutableStateOf("ONE_WAY") }
    var vehicleCategory by remember { mutableStateOf("SEDAN") }
    var fareText by remember { mutableStateOf("3600") }
    var specialInstructions by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("DIRECT_CASH") }

    var pickupLat by remember { mutableDoubleStateOf(30.7333) }
    var pickupLng by remember { mutableDoubleStateOf(76.7794) }
    var destLat by remember { mutableDoubleStateOf(28.6139) }
    var destLng by remember { mutableDoubleStateOf(77.2090) }
    var routePolylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Road distance and pricing
    var roadDistanceKm by remember { mutableStateOf<Double?>(null) }
    var estimatedDurationMin by remember { mutableStateOf<Int?>(null) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }
    var suggestedFare by remember { mutableIntStateOf(0) }
    var minimumFare by remember { mutableIntStateOf(0) }

    // Agent financial terms
    var customerFareText by remember { mutableStateOf("5000") }
    var agentCommissionText by remember { mutableStateOf("1000") }

    // 24-hour departure date & time
    var departureDateTime by remember {
        mutableStateOf(LocalDateTime.now().plusHours(2).withMinute(0).withSecond(0))
    }
    var departureError by remember { mutableStateOf<String?>(null) }

    // 24-hour return date & time and tour details for Round Trip
    var returnDateTime by remember {
        mutableStateOf(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0))
    }
    var returnError by remember { mutableStateOf<String?>(null) }
    var tourDetails by remember { mutableStateOf("") }

    var createdTripId by remember { mutableStateOf<String?>(null) }

    val actionLoading by viewModel.actionLoading.collectAsState()
    val actionError by viewModel.actionError.collectAsState()

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

    // Vehicle-based pricing helpers
    fun getRecommendedRate(category: String): Double {
        return when (category.uppercase()) {
            "SUV" -> 20.0
            "LUXURY" -> 30.0
            "TRAVELLER" -> 25.0
            else -> 15.0 // HATCHBACK, SEDAN
        }
    }

    fun getMinimumRate(category: String, distanceKm: Double): Double {
        val shortTrip = distanceKm <= 50.0
        return when (category.uppercase()) {
            "SUV" -> if (shortTrip) 20.0 else 18.0
            "LUXURY" -> if (shortTrip) 30.0 else 25.0
            "TRAVELLER" -> if (shortTrip) 25.0 else 22.0
            else -> if (shortTrip) 15.0 else 12.0 // HATCHBACK, SEDAN
        }
    }

    fun recalculateFaresForDistance(dist: Double, category: String) {
        val recRate = getRecommendedRate(category)
        val minRate = getMinimumRate(category, dist)
        val sugg = ceil(dist * recRate).toInt()
        val min = ceil(dist * minRate).toInt()
        suggestedFare = sugg
        minimumFare = min
        if (rideType == "OWNER") {
            fareText = sugg.toString()
        }
    }

    // Recalculate route and fares via Google Routes API
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
                        routePolylinePoints = res.route.points
                        recalculateFaresForDistance(dist, vehicleCategory)
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

    // Recalculate fares dynamically when vehicle category changes
    LaunchedEffect(vehicleCategory) {
        val dist = roadDistanceKm
        if (dist != null && dist > 0.0) {
            recalculateFaresForDistance(dist, vehicleCategory)
        }
    }

    // Resolve pickup coordinates independently whenever pickupCity changes
    LaunchedEffect(pickupCity) {
        val resolved = LocationResolver.resolveCoordinates(context, pickupCity)
        if (resolved != null) {
            pickupLat = resolved.first
            pickupLng = resolved.second
        }
    }

    // Resolve destination coordinates independently whenever destCity changes
    LaunchedEffect(destCity) {
        val resolved = LocationResolver.resolveCoordinates(context, destCity)
        if (resolved != null) {
            destLat = resolved.first
            destLng = resolved.second
        }
    }

    // Recalculate route whenever coordinates update
    LaunchedEffect(pickupLat, pickupLng, destLat, destLng) {
        calculateRouteAndFares()
    }

    // Update Google Map pins, multi-point road polyline, and camera bounds
    LaunchedEffect(pickupLat, pickupLng, destLat, destLng, routePolylinePoints) {
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

            val polylineOpts = PolylineOptions()
                .width(9f)
                .color(android.graphics.Color.parseColor("#EF6C00"))
                .geodesic(true)

            if (routePolylinePoints.isNotEmpty()) {
                polylineOpts.addAll(routePolylinePoints)
            } else {
                polylineOpts.add(pLatLng, dLatLng)
            }
            googleMap.addPolyline(polylineOpts)

            googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {}
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragEnd(marker: Marker) {
                    val pos = marker.position
                    coroutineScope.launch {
                        if (marker.title?.startsWith("Pickup") == true) {
                            pickupLat = pos.latitude
                            pickupLng = pos.longitude
                            LocationResolver.reverseGeocode(context, pos.latitude, pos.longitude)?.let {
                                pickupCity = it
                            }
                        } else {
                            destLat = pos.latitude
                            destLng = pos.longitude
                            LocationResolver.reverseGeocode(context, pos.latitude, pos.longitude)?.let {
                                destCity = it
                            }
                        }
                        calculateRouteAndFares()
                    }
                }
            })

            try {
                val builder = LatLngBounds.builder().include(pLatLng).include(dLatLng)
                routePolylinePoints.forEach { pt -> builder.include(pt) }
                val bounds = builder.build()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 110))
            } catch (_: Exception) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pLatLng, 10f))
            }
        }
    }

    // Validations
    val isSameCity = pickupCity.isNotBlank() && destCity.isNotBlank() && pickupCity.trim().equals(destCity.trim(), ignoreCase = true)
    val fareVal = fareText.toDoubleOrNull() ?: 0.0
    val isRouteValid = pickupCity.isNotBlank() && destCity.isNotBlank() && !isSameCity && pickupAddress.isNotBlank() && destAddress.isNotBlank()

    // Agent calculations & validation: driverPayout = customerFare - agentCommission
    val cFare = customerFareText.toDoubleOrNull() ?: 0.0
    val comm = agentCommissionText.toDoubleOrNull() ?: 0.0
    val calculatedDriverPayout = (cFare - comm).coerceAtLeast(0.0)
    val isDepartureInFuture = departureDateTime.isAfter(LocalDateTime.now().minusMinutes(2))

    // Round Trip validation: return must be strictly after departure, and tour details must be provided
    val isReturnAfterDeparture = returnDateTime.isAfter(departureDateTime)
    val isRoundTripValid = tripType != "ROUND_TRIP" || (isReturnAfterDeparture && tourDetails.isNotBlank())

    // Owner fare validation: cannot be below minimum allowed fare
    val isOwnerFareValid = roadDistanceKm != null && roadDistanceKm!! > 0.0 && fareVal >= minimumFare && minimumFare > 0
    val isAgentFareValid = cFare > 0.0 && comm >= 0.0 && comm < cFare && calculatedDriverPayout > 0.0

    val isFormValid = if (rideType == "OWNER") {
        isRouteValid && isOwnerFareValid && !isCalculatingRoute && isDepartureInFuture && isRoundTripValid
    } else {
        isRouteValid && isAgentFareValid && isDepartureInFuture && isRoundTripValid
    }

    if (createdTripId != null) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF238765),
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
                        "Your owner ride for ${pickupCity.trim()} → ${destCity.trim()} has been published. Drivers can now view the ride and apply."
                    } else {
                        "Your commercial assignment for ${pickupCity.trim()} → ${destCity.trim()} is live with customer fare ₹$customerFareText and driver payout ₹${calculatedDriverPayout.toInt()}."
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
                    colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryButton),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("View Trip Details", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = RaahiCardWhite,
            shape = RoundedCornerShape(20.dp),
        )
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (rideType == "OWNER") "Post Ride as Owner" else "Post Ride as Travel Agent",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = RaahiDisplayFont,
                        )
                        Text(
                            if (rideType == "OWNER") "Zero platform fee • Verified community drivers" else "Custom commercial terms & agency payouts",
                            color = Color(0xFFA0AAB0),
                            fontSize = 11.5.sp,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RaahiCoralPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, RaahiCoralPrimary.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            "INTERCITY",
                            color = RaahiCoralPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ==========================================
            // 1. RIDE TYPE SELECTOR (OWNER vs AGENT)
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiCardWhite,
                border = BorderStroke(1.dp, RaahiCardBorder),
                shadowElevation = RaahiCardElevation,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "POSTING PROFILE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = RaahiTextDim,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // OWNER Option
                        val isOwner = rideType == "OWNER"
                        val ownerBorderColor by animateColorAsState(
                            if (isOwner) RaahiCoralPrimary else RaahiCardBorder,
                            label = "ownerBorder"
                        )
                        val ownerBgColor by animateColorAsState(
                            if (isOwner) RaahiCoralLight else RaahiLightBg,
                            label = "ownerBg"
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    rideType = "OWNER"
                                    if (suggestedFare > 0) fareText = suggestedFare.toString()
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = ownerBgColor,
                            border = BorderStroke(if (isOwner) 2.dp else 1.dp, ownerBorderColor),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (isOwner) RaahiCoralPrimary.copy(alpha = 0.15f) else Color(0xFFE2E8F0),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.DirectionsCar,
                                            contentDescription = null,
                                            tint = if (isOwner) RaahiCoralDark else RaahiTextDim,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (isOwner) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = RaahiCoralPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Vehicle Owner",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isOwner) RaahiPriceText else RaahiText,
                                )
                                Text(
                                    "Direct Personal Trip",
                                    fontSize = 11.sp,
                                    color = if (isOwner) RaahiCoralDark else RaahiTextDim,
                                    fontWeight = if (isOwner) FontWeight.Medium else FontWeight.Normal,
                                )
                            }
                        }

                        // AGENT Option
                        val isAgent = rideType == "AGENT"
                        val agentBorderColor by animateColorAsState(
                            if (isAgent) Color(0xFF5E35B1) else RaahiCardBorder,
                            label = "agentBorder"
                        )
                        val agentBgColor by animateColorAsState(
                            if (isAgent) Color(0xFFEDE7F6) else RaahiLightBg,
                            label = "agentBg"
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    rideType = "AGENT"
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = agentBgColor,
                            border = BorderStroke(if (isAgent) 2.dp else 1.dp, agentBorderColor),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (isAgent) Color(0xFF5E35B1).copy(alpha = 0.15f) else Color(0xFFE2E8F0),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Outlined.SupportAgent,
                                            contentDescription = null,
                                            tint = if (isAgent) Color(0xFF5E35B1) else RaahiTextDim,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    if (isAgent) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF5E35B1),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Travel Agent",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isAgent) Color(0xFF4527A0) else RaahiText,
                                )
                                Text(
                                    "Commission & Terms",
                                    fontSize = 11.sp,
                                    color = if (isAgent) Color(0xFF5E35B1) else RaahiTextDim,
                                    fontWeight = if (isAgent) FontWeight.Medium else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }

            // Error Banner if present
            if (actionError != null) {
                Surface(
                    color = Color(0xFFFFECEC),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RaahiSosRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = RaahiSosRed, modifier = Modifier.size(20.dp))
                        Text(
                            text = actionError ?: "",
                            color = RaahiSosRed,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ==========================================
            // 2. ROUTE TIMELINE & LOCATION SELECTION
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiCardWhite,
                border = BorderStroke(1.dp, RaahiCardBorder),
                shadowElevation = RaahiCardElevation,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(RaahiCoralPrimary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Route Timeline", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                        }

                        // Swap Button
                        OutlinedButton(
                            onClick = {
                                val tempCity = pickupCity
                                val tempAddr = pickupAddress
                                val tempLat = pickupLat
                                val tempLng = pickupLng
                                pickupCity = destCity
                                pickupAddress = destAddress
                                pickupLat = destLat
                                pickupLng = destLng
                                destCity = tempCity
                                destAddress = tempAddr
                                destLat = tempLat
                                destLng = tempLng
                            },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, RaahiCoralPrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = RaahiCoralLight.copy(alpha = 0.6f),
                                contentColor = RaahiCoralPrimary
                            ),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Icon(Icons.Outlined.SwapVert, contentDescription = "Swap Route", modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Swap", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Route timeline container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RaahiLightBg, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ORIGIN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(0xFF238765), CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(72.dp)
                                        .background(Color(0xFFCBD5E1))
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "PICKUP ORIGIN",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF238765),
                                    letterSpacing = 0.5.sp
                                )
                                OutlinedTextField(
                                    value = pickupCity,
                                    onValueChange = { pickupCity = it },
                                    label = { Text("Pickup City *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = RaahiCoralPrimary, modifier = Modifier.size(18.dp))
                                    },
                                    isError = isSameCity || pickupCity.isBlank(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = RaahiCardWhite,
                                        unfocusedContainerColor = RaahiCardWhite,
                                        focusedBorderColor = RaahiCoralPrimary,
                                        unfocusedBorderColor = RaahiCardBorder,
                                    )
                                )
                                OutlinedTextField(
                                    value = pickupAddress,
                                    onValueChange = { pickupAddress = it },
                                    label = { Text("Pickup Address / Landmark *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Place, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(18.dp))
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = RaahiCardWhite,
                                        unfocusedContainerColor = RaahiCardWhite,
                                        focusedBorderColor = RaahiCoralPrimary,
                                        unfocusedBorderColor = RaahiCardBorder,
                                    )
                                )
                            }
                        }

                        // DESTINATION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = 14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(RaahiCoralPrimary, CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "DROP DESTINATION",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RaahiCoralDark,
                                    letterSpacing = 0.5.sp
                                )
                                OutlinedTextField(
                                    value = destCity,
                                    onValueChange = { destCity = it },
                                    label = { Text("Destination City *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = RaahiCoralPrimary, modifier = Modifier.size(18.dp))
                                    },
                                    isError = isSameCity || destCity.isBlank(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = RaahiCardWhite,
                                        unfocusedContainerColor = RaahiCardWhite,
                                        focusedBorderColor = RaahiCoralPrimary,
                                        unfocusedBorderColor = RaahiCardBorder,
                                    )
                                )
                                OutlinedTextField(
                                    value = destAddress,
                                    onValueChange = { destAddress = it },
                                    label = { Text("Destination Address / Landmark *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Flag, contentDescription = null, tint = RaahiTextDim, modifier = Modifier.size(18.dp))
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = RaahiCardWhite,
                                        unfocusedContainerColor = RaahiCardWhite,
                                        focusedBorderColor = RaahiCoralPrimary,
                                        unfocusedBorderColor = RaahiCardBorder,
                                    )
                                )
                            }
                        }
                    }

                    if (isSameCity) {
                        Surface(
                            color = Color(0xFFFFECEC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RaahiSosRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Warning, contentDescription = null, tint = RaahiSosRed, modifier = Modifier.size(16.dp))
                                Text(
                                    "Origin and destination city cannot be identical.",
                                    color = RaahiSosRed,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 3. PROMINENT GOOGLE MAP
                    // ==========================================
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, RaahiCardBorder, RoundedCornerShape(16.dp))
                    ) {
                        AndroidView(
                            factory = { mapView },
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Top-left chip
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp),
                            color = RaahiCharcoalHeader.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(RaahiCoralPrimary, CircleShape))
                                Text("Google Routes Live", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Bottom pill
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            color = RaahiCharcoalHeader.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text("Drag pins on map to adjust exact locations", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // ==========================================
                    // 4. ROUTE STATS & TELEMETRY
                    // ==========================================
                    if (isCalculatingRoute) {
                        Surface(
                            color = RaahiLightBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = RaahiCoralPrimary, strokeWidth = 2.dp)
                                Text("Calculating road distance & verified travel time via Google Routes...", fontSize = 12.sp, color = RaahiTextDim)
                            }
                        }
                    } else if (routeError != null) {
                        Surface(
                            color = Color(0xFFFFECEC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RaahiSosRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(routeError ?: "", color = RaahiSosRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = { calculateRouteAndFares() }) {
                                    Text("Retry", color = RaahiCoralDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (roadDistanceKm != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Distance
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = RaahiLightBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, RaahiCardBorder),
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.Straighten, contentDescription = null, tint = RaahiCoralDark, modifier = Modifier.size(14.dp))
                                        Text("DISTANCE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${roadDistanceKm?.toInt()} km",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = RaahiPriceText
                                    )
                                }
                            }

                            // Travel Duration
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = RaahiLightBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, RaahiCardBorder),
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = RaahiCoralDark, modifier = Modifier.size(14.dp))
                                        Text("DRIVE TIME", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    val duration = estimatedDurationMin
                                    Text(
                                        if (duration != null) "${duration / 60}h ${duration % 60}m" else "--",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = RaahiPriceText
                                    )
                                }
                            }

                            // Suggested Fare (Owner) vs Driver Net Payout (Agent)
                            if (rideType == "OWNER") {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Outlined.Payments, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                                            Text("SUGGESTED", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "₹$suggestedFare",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF5F3FF),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Outlined.Payments, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(14.dp))
                                            Text("NET PAYOUT", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            if (isAgentFareValid) "₹${calculatedDriverPayout.toInt()}" else "--",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF6D28D9)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. SPECIFICATIONS & PRICING
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiCardWhite,
                border = BorderStroke(1.dp, RaahiCardBorder),
                shadowElevation = RaahiCardElevation,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(RaahiCoralPrimary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            if (rideType == "OWNER") "Fare & Trip Specifications" else "Commercial Terms & Vehicle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = RaahiText
                        )
                    }

                    // A. TRIP TYPE (ONE_WAY vs ROUND_TRIP)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("TRIP TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim, letterSpacing = 0.5.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("ONE_WAY" to "One Way", "ROUND_TRIP" to "Round Trip").forEach { (type, label) ->
                                val isSelected = tripType == type
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { tripType = type },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) RaahiCoralLight else RaahiLightBg,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) RaahiCoralPrimary else RaahiCardBorder
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            if (type == "ONE_WAY") Icons.AutoMirrored.Filled.ArrowForward else Icons.Outlined.Sync,
                                            contentDescription = null,
                                            tint = if (isSelected) RaahiCoralDark else RaahiTextDim,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) RaahiCoralDark else RaahiText,
                                            fontSize = 13.5.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // B. DEPARTURE DATE & 24-HOUR TIME PICKER
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DEPARTURE DATE & TIME (24-HOUR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim, letterSpacing = 0.5.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Date Picker Card
                            Surface(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clickable {
                                        val datePicker = DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val updated = departureDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth)
                                                if (updated.isAfter(LocalDateTime.now().minusMinutes(2))) {
                                                    departureDateTime = updated
                                                    departureError = null
                                                } else {
                                                    departureDateTime = updated
                                                    departureError = "Departure time cannot be in the past"
                                                }
                                            },
                                            departureDateTime.year,
                                            departureDateTime.monthValue - 1,
                                            departureDateTime.dayOfMonth
                                        )
                                        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
                                        datePicker.show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = RaahiLightBg,
                                border = BorderStroke(1.dp, RaahiCardBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = RaahiCoralPrimary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("DATE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim)
                                        Text(
                                            departureDateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = RaahiText
                                        )
                                    }
                                }
                            }

                            // 24-Hour Time Picker Card
                            Surface(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .clickable {
                                        val timePicker = TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val updated = departureDateTime.withHour(hourOfDay).withMinute(minute)
                                                if (updated.isAfter(LocalDateTime.now().minusMinutes(2))) {
                                                    departureDateTime = updated
                                                    departureError = null
                                                } else {
                                                    departureDateTime = updated
                                                    departureError = "Departure time cannot be in the past"
                                                }
                                            },
                                            departureDateTime.hour,
                                            departureDateTime.minute,
                                            true // 24-hour format
                                        )
                                        timePicker.show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = RaahiLightBg,
                                border = BorderStroke(1.dp, RaahiCardBorder),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = RaahiCoralPrimary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("TIME (24-HR)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim)
                                        Text(
                                            departureDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = RaahiText
                                        )
                                    }
                                }
                            }
                        }

                        if (!isDepartureInFuture || departureError != null) {
                            Text(
                                departureError ?: "Departure time cannot be in the past. Please select a future time.",
                                color = RaahiSosRed,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    // ROUND TRIP RETURN SCHEDULE & TOUR DETAILS (Shown when Round Trip is selected)
                    if (tripType == "ROUND_TRIP") {
                        val departureDate = departureDateTime.toLocalDate()
                        val returnDate = returnDateTime.toLocalDate()
                        val daysBetween = ChronoUnit.DAYS.between(departureDate, returnDate).toInt().coerceAtLeast(0)
                        val durationBadgeText = if (daysBetween == 0) "Same-Day Return" else "${daysBetween + 1} Days Tour"

                        Surface(
                            color = RaahiLightBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RaahiCoralPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Sync,
                                            contentDescription = null,
                                            tint = RaahiCoralPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "RETURN DATE & TIME (24-HOUR)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RaahiCoralDark,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Surface(
                                        color = RaahiCoralLight,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, RaahiCoralPrimary.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            durationBadgeText,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RaahiCoralDark,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Return Date Picker Card
                                    Surface(
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .clickable {
                                                val datePicker = DatePickerDialog(
                                                    context,
                                                    { _, year, month, dayOfMonth ->
                                                        val updated = returnDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth)
                                                        if (updated.isAfter(departureDateTime)) {
                                                            returnDateTime = updated
                                                            returnError = null
                                                        } else {
                                                            returnDateTime = updated
                                                            returnError = "Return time must be after departure time"
                                                        }
                                                    },
                                                    returnDateTime.year,
                                                    returnDateTime.monthValue - 1,
                                                    returnDateTime.dayOfMonth
                                                )
                                                // Min return date is departure date
                                                datePicker.datePicker.minDate = departureDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                                datePicker.show()
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = RaahiCardWhite,
                                        border = BorderStroke(1.dp, if (returnError != null) RaahiSosRed else RaahiCardBorder),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = RaahiCoralPrimary, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text("RETURN DATE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim)
                                                Text(
                                                    returnDateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = RaahiText
                                                )
                                            }
                                        }
                                    }

                                    // Return Time Picker Card (24-Hour)
                                    Surface(
                                        modifier = Modifier
                                            .weight(0.9f)
                                            .clickable {
                                                val timePicker = TimePickerDialog(
                                                    context,
                                                    { _, hourOfDay, minute ->
                                                        val updated = returnDateTime.withHour(hourOfDay).withMinute(minute)
                                                        if (updated.isAfter(departureDateTime)) {
                                                            returnDateTime = updated
                                                            returnError = null
                                                        } else {
                                                            returnDateTime = updated
                                                            returnError = "Return time must be after departure time"
                                                        }
                                                    },
                                                    returnDateTime.hour,
                                                    returnDateTime.minute,
                                                    true // 24-hour format
                                                )
                                                timePicker.show()
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = RaahiCardWhite,
                                        border = BorderStroke(1.dp, if (returnError != null) RaahiSosRed else RaahiCardBorder),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = RaahiCoralPrimary, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text("TIME (24-HR)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim)
                                                Text(
                                                    returnDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = RaahiText
                                                )
                                            }
                                        }
                                    }
                                }

                                if (returnError != null || !isReturnAfterDeparture) {
                                    Text(
                                        returnError ?: "Return time must be after departure time.",
                                        color = RaahiSosRed,
                                        fontSize = 11.5.sp
                                    )
                                }

                                // Multiline Tour Details & Planned Stops
                                OutlinedTextField(
                                    value = tourDetails,
                                    onValueChange = { tourDetails = it },
                                    label = { Text("Tour Itinerary & Planned Stops *") },
                                    placeholder = {
                                        Text("e.g. Day 1: Chandigarh → Shimla check-in.\nDay 2: Kufri & Mall Road sightseeing.\nDay 3: Return journey back to Chandigarh.")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                    shape = RoundedCornerShape(12.dp),
                                    isError = tourDetails.isBlank(),
                                    supportingText = {
                                        Text(
                                            if (tourDetails.isBlank()) "Please specify duration, planned stops or itinerary for drivers" else "Included in driver assignment agreement",
                                            fontSize = 11.sp,
                                            color = if (tourDetails.isBlank()) RaahiSosRed else RaahiTextDim
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RaahiCoralPrimary,
                                        unfocusedBorderColor = RaahiCardBorder,
                                        focusedLabelColor = RaahiCoralPrimary,
                                    )
                                )
                            }
                        }
                    }

                    // C. REQUIRED VEHICLE CATEGORY (Full titles & automotive icons)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("REQUIRED VEHICLE CATEGORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RaahiTextDim, letterSpacing = 0.5.sp)
                        val vehicleList = remember {
                            listOf(
                                "SEDAN" to ("Sedan" to Icons.Outlined.DirectionsCar),
                                "SUV" to ("SUV" to Icons.Outlined.DirectionsCar),
                                "HATCHBACK" to ("Hatchback" to Icons.Outlined.DirectionsCar),
                                "LUXURY" to ("Luxury" to Icons.Outlined.Star),
                                "TRAVELLER" to ("Traveller" to Icons.Outlined.AirportShuttle),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vehicleList.forEach { (catKey, pair) ->
                                val (displayLabel, icon) = pair
                                val isSelected = vehicleCategory == catKey
                                Surface(
                                    modifier = Modifier
                                        .clickable { vehicleCategory = catKey },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) RaahiCoralLight else RaahiLightBg,
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) RaahiCoralPrimary else RaahiCardBorder),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) RaahiCoralDark else RaahiTextDim,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = displayLabel,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) RaahiCoralDark else RaahiText,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // D. PRICING SECTION
                    if (rideType == "OWNER") {
                        // OWNER PRICING
                        if (roadDistanceKm != null && roadDistanceKm!! > 0) {
                            val distInt = roadDistanceKm!!.toInt()
                            Surface(
                                color = RaahiCoralLight.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, RaahiCoralPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Route Distance", fontSize = 12.sp, color = RaahiTextDim)
                                        Text("$distInt km", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiText)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Suggested Fare (${getRecommendedRate(vehicleCategory).toInt()} ₹/km)", fontSize = 12.sp, color = RaahiTextDim)
                                        Text("₹$suggestedFare", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiCoralDark)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Minimum Allowed Fare (${getMinimumRate(vehicleCategory, roadDistanceKm ?: 0.0).toInt()} ₹/km)", fontSize = 12.sp, color = RaahiTextDim)
                                        Text("₹$minimumFare", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiText)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = RaahiCoralPrimary.copy(alpha = 0.2f))
                                    Text(
                                        "Fare is paid directly to driver upon destination arrival.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = RaahiTextDim
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = fareText,
                                onValueChange = { fareText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Enter Your Ride Fare (₹) *") },
                                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RaahiPriceText, fontSize = 16.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = !isOwnerFareValid && fareText.isNotBlank(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaahiCoralPrimary,
                                    unfocusedBorderColor = RaahiCardBorder,
                                    focusedLabelColor = RaahiCoralPrimary,
                                ),
                                supportingText = {
                                    if (fareVal < minimumFare && fareText.isNotBlank()) {
                                        Text(
                                            "Fare cannot be below minimum ₹$minimumFare for $distInt km ($vehicleCategory).",
                                            color = RaahiSosRed,
                                            fontSize = 11.5.sp,
                                        )
                                    } else {
                                        Text("Prefilled with suggested fare. Minimum acceptable: ₹$minimumFare", fontSize = 11.sp)
                                    }
                                }
                            )
                        } else {
                            Surface(
                                color = RaahiLightBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "Enter pickup and destination to calculate road distance and suggested fare.",
                                    fontSize = 12.sp,
                                    color = RaahiTextDim,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    } else {
                        // AGENT COMMERCIAL TERMS (Streamlined Flow)
                        Surface(
                            color = Color(0xFFF3E8FF),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "${pickupCity.uppercase()} → ${destCity.uppercase()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B21A8)
                                )
                                Text("Customer pays direct cash to driver. Driver remits commission to agent.", fontSize = 11.5.sp, color = RaahiTextDim)
                            }
                        }

                        // Customer Fare
                        OutlinedTextField(
                            value = customerFareText,
                            onValueChange = { customerFareText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Agreed Customer Fare (₹) *") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = RaahiPriceText, fontSize = 16.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RaahiCoralPrimary,
                                unfocusedBorderColor = RaahiCardBorder,
                            )
                        )

                        // Agent Commission
                        OutlinedTextField(
                            value = agentCommissionText,
                            onValueChange = { agentCommissionText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Agent Commission (₹) *") },
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED), fontSize = 16.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            isError = comm >= cFare && customerFareText.isNotBlank(),
                            supportingText = {
                                if (comm >= cFare && customerFareText.isNotBlank()) {
                                    Text("Commission must be strictly less than customer fare (₹${cFare.toInt()})", color = RaahiSosRed, fontSize = 11.5.sp)
                                } else {
                                    Text("Retained by you as travel agent booking commission", fontSize = 11.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF7C3AED),
                                unfocusedBorderColor = RaahiCardBorder,
                            )
                        )

                        // Real-Time Driver Payout Calculation Card
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Customer Agreed Fare", fontSize = 12.sp, color = RaahiTextDim)
                                    Text("₹${cFare.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RaahiText)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Agency Commission", fontSize = 12.sp, color = RaahiTextDim)
                                    Text("- ₹${comm.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF7C3AED))
                                }
                                HorizontalDivider(color = Color(0xFFBBF7D0), modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("DRIVER NET PAYOUT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF15803D))
                                        Text("Direct cash payout to driver", fontSize = 10.5.sp, color = RaahiTextDim)
                                    }
                                    Text("₹${calculatedDriverPayout.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF15803D))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Driver collects total ₹${cFare.toInt()} directly from customer. After arrival and commission payment, confirm settlement to issue completion OTP.",
                                    fontSize = 11.sp,
                                    color = RaahiTextDim,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Special Instructions
                    OutlinedTextField(
                        value = specialInstructions,
                        onValueChange = { specialInstructions = it },
                        label = { Text("Special Instructions (Optional)") },
                        placeholder = { Text("e.g. AC on throughout, toll included in fare, clean car") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2,
                    )
                }
            }

            // ==========================================
            // 6. DISTINCT SUMMARY CARD
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RaahiCardWhite,
                border = BorderStroke(1.5.dp, RaahiCardBorder),
                shadowElevation = RaahiCardElevation,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(RaahiCoralPrimary, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Trip Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RaahiText)
                        }

                        RaahiRideTypeBadge(rideType)
                    }

                    HorizontalDivider(color = RaahiCardBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Route", fontSize = 11.sp, color = RaahiTextDim)
                            Text(
                                "${pickupCity.trim().ifEmpty { "..." }} → ${destCity.trim().ifEmpty { "..." }}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = RaahiPriceText,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RaahiLightBg,
                            border = BorderStroke(1.dp, RaahiCardBorder)
                        ) {
                            Text(
                                "${roadDistanceKm?.toInt() ?: "--"} km",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RaahiCoralDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Departure", fontSize = 11.sp, color = RaahiTextDim)
                            Text(
                                departureDateTime.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm")),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = RaahiText,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Trip Type", fontSize = 11.sp, color = RaahiTextDim)
                            Text(
                                if (tripType == "ROUND_TRIP") "Round Trip" else "One Way",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (tripType == "ROUND_TRIP") RaahiCoralDark else RaahiText,
                            )
                        }
                    }

                    if (tripType == "ROUND_TRIP") {
                        val departureDate = departureDateTime.toLocalDate()
                        val returnDate = returnDateTime.toLocalDate()
                        val daysBetween = ChronoUnit.DAYS.between(departureDate, returnDate).toInt().coerceAtLeast(0)
                        val durationBadgeText = if (daysBetween == 0) "Same-Day Return" else "${daysBetween + 1} Days Tour"

                        Surface(
                            color = RaahiLightBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RaahiCoralPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Return Departure", fontSize = 11.sp, color = RaahiTextDim)
                                    Text(
                                        durationBadgeText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = RaahiCoralDark
                                    )
                                }
                                Text(
                                    returnDateTime.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm")),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = RaahiText
                                )
                                if (tourDetails.isNotBlank()) {
                                    HorizontalDivider(color = RaahiCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))
                                    Text("Tour Itinerary", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = RaahiTextDim)
                                    Text(
                                        tourDetails.trim(),
                                        fontSize = 11.5.sp,
                                        color = RaahiText,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Vehicle Required", fontSize = 11.sp, color = RaahiTextDim)
                            Text(
                                vehicleCategory,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RaahiText,
                            )
                        }
                    }

                    HorizontalDivider(color = RaahiCardBorder)

                    // Pricing breakdown in summary
                    if (rideType == "OWNER") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Agreed Trip Fare", fontSize = 11.5.sp, color = RaahiTextDim)
                                Text("Direct cash on arrival", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = RaahiTextDim)
                            }
                            Text(
                                "₹${fareText.ifEmpty { "0" }}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = RaahiCoralPrimary,
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Agreed Customer Fare", fontSize = 12.sp, color = RaahiTextDim)
                                Text("₹${cFare.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RaahiPriceText)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Agent Commission", fontSize = 12.sp, color = RaahiTextDim)
                                Text("₹${comm.toInt()}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF7C3AED))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Driver Payout", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF15803D))
                                Text("₹${calculatedDriverPayout.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF15803D))
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 7. PUBLISH ACTION BUTTON
            // ==========================================
            Button(
                onClick = {
                    if (actionLoading || !isFormValid) return@Button

                    val departureIso = departureDateTime.atZone(ZoneId.systemDefault()).toInstant().toString()
                    val returnIso = if (tripType == "ROUND_TRIP") {
                        returnDateTime.atZone(ZoneId.systemDefault()).toInstant().toString()
                    } else null

                    val combinedSpecialInstructions = if (tripType == "ROUND_TRIP") {
                        val details = tourDetails.trim()
                        val extra = specialInstructions.trim()
                        if (extra.isNotEmpty()) "Tour Itinerary:\n$details\n\nNotes:\n$extra" else details
                    } else {
                        specialInstructions.trim().ifEmpty { null }
                    }

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
                            returnTime = returnIso,
                            vehicleCategory = vehicleCategory,
                            estimatedDistanceKm = roadDistanceKm,
                            estimatedDurationMin = estimatedDurationMin,
                            fare = fareVal,
                            paymentMode = paymentMode,
                            specialInstructions = combinedSpecialInstructions,
                            rideType = "OWNER",
                            customerFare = fareVal,
                            driverPayout = fareVal,
                            agentCommission = 0.0,
                            paymentRecipient = "DRIVER",
                            settlementResponsibility = "NONE",
                            saveAsDraft = false,
                        )
                    } else {
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
                            returnTime = returnIso,
                            vehicleCategory = vehicleCategory,
                            estimatedDistanceKm = roadDistanceKm,
                            estimatedDurationMin = estimatedDurationMin,
                            fare = cFare,
                            paymentMode = paymentMode,
                            specialInstructions = combinedSpecialInstructions,
                            rideType = "AGENT",
                            customerFare = cFare,
                            driverPayout = calculatedDriverPayout,
                            agentCommission = comm,
                            paymentRecipient = "DRIVER",
                            settlementResponsibility = "DRIVER_TO_AGENT",
                            saveAsDraft = false,
                        )
                    }

                    viewModel.createTrip(req) { created ->
                        createdTripId = created.id
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RaahiPrimaryButton,
                    disabledContainerColor = Color(0xFFCBD5E1),
                ),
                enabled = !actionLoading && isFormValid,
            ) {
                if (actionLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (rideType == "OWNER") "Publish Owner Ride" else "Publish Agent Trip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            if (!isFormValid) {
                Text(
                    text = if (isSameCity) {
                        "Origin and destination city cannot be identical."
                    } else if (!isDepartureInFuture) {
                        "Departure time cannot be in the past."
                    } else if (tripType == "ROUND_TRIP" && !isReturnAfterDeparture) {
                        "Return date and time must be after departure time."
                    } else if (tripType == "ROUND_TRIP" && tourDetails.isBlank()) {
                        "Please fill in the tour itinerary and planned stops."
                    } else if (roadDistanceKm == null || isCalculatingRoute) {
                        "Calculating route distance and suggested fare..."
                    } else if (rideType == "OWNER" && !isOwnerFareValid) {
                        "Please enter a fare of at least ₹$minimumFare for this ${roadDistanceKm?.toInt()} km route ($vehicleCategory)."
                    } else if (rideType == "AGENT" && comm >= cFare) {
                        "Agent commission must be strictly less than customer fare."
                    } else {
                        "Please complete all required fields (*)."
                    },
                    fontSize = 11.5.sp,
                    color = RaahiTextDim,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
