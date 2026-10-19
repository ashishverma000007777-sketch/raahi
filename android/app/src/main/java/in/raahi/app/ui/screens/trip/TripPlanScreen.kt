package `in`.raahi.app.ui.screens.trip

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import `in`.raahi.app.ui.components.*
import `in`.raahi.app.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlanScreen(
    onBack: () -> Unit,
    onTripCreated: (String) -> Unit,
    viewModel: TripPlanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()

    // MapView lifecycle management
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

    // Update Google Map pins, multi-point polyline, and camera bounds
    LaunchedEffect(state.startLat, state.startLng, state.destLat, state.destLng, state.routePolyline) {
        mapView.getMapAsync { googleMap ->
            googleMap.clear()
            val startLatLng = LatLng(state.startLat, state.startLng)
            val destLatLng = LatLng(state.destLat, state.destLng)

            googleMap.addMarker(
                MarkerOptions()
                    .position(startLatLng)
                    .title("Start: ${state.startLocationName}")
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            )

            if (state.destLocationName.isNotBlank()) {
                googleMap.addMarker(
                    MarkerOptions()
                        .position(destLatLng)
                        .title("Destination: ${state.destLocationName}")
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
            }

            val polylineOpts = PolylineOptions()
                .width(8f)
                .color(android.graphics.Color.parseColor("#EF6C00"))
                .geodesic(true)

            if (state.routePolyline.isNotEmpty()) {
                polylineOpts.addAll(state.routePolyline)
                googleMap.addPolyline(polylineOpts)
            }

            googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {}
                override fun onMarkerDrag(marker: Marker) {}
                override fun onMarkerDragEnd(marker: Marker) {
                    val pos = marker.position
                    if (marker.title?.startsWith("Start") == true) {
                        viewModel.onStartCoordsUpdated(pos.latitude, pos.longitude)
                    } else {
                        viewModel.onDestCoordsUpdated(pos.latitude, pos.longitude)
                    }
                }
            })

            try {
                val builder = LatLngBounds.builder().include(startLatLng)
                if (state.destLocationName.isNotBlank()) {
                    builder.include(destLatLng)
                }
                state.routePolyline.forEach { pt -> builder.include(pt) }
                val bounds = builder.build()
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (_: Exception) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 10f))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TripPlanEvent.TripCreated -> onTripCreated(event.tripId)
            }
        }
    }

    // Date & Time Picker trigger
    fun showDateTimePicker() {
        val currentDt = state.departureDateTime
        val calendar = Calendar.getInstance().apply {
            set(currentDt.year, currentDt.monthValue - 1, currentDt.dayOfMonth, currentDt.hour, currentDt.minute)
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val selected = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                        viewModel.onDepartureDateTimeChanged(selected)
                    },
                    currentDt.hour,
                    currentDt.minute,
                    true // 24-hour clock
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Plan a Trip",
                        color = RaahiText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        fontFamily = RaahiDisplayFont
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = RaahiText)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. PREMIUM AUTOMOTIVE VEHICLE CARD (Peugeot OEM reference styling)
            val vehicle = state.vehicle
            if (vehicle != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title row + Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${vehicle.brand} ${vehicle.model}".trim(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E293B),
                                    fontFamily = RaahiDisplayFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!vehicle.variant.isNullOrBlank()) {
                                    Text(
                                        text = vehicle.variant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Active vehicle status badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF059669))
                                    )
                                    Text(
                                        text = "TRIP VEHICLE",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        // Vehicle SUV Illustration
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC)),
                            contentAlignment = Alignment.Center
                        ) {
                            VehicleIllustration(
                                size = VehicleIllustrationSize.Standard,
                                vehicleBrand = vehicle.brand,
                                vehicleModel = vehicle.model,
                                contentDescription = "${vehicle.brand} ${vehicle.model} Front View"
                            )
                        }

                        // Plate & Telemetry Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            VehicleNumberPlate(
                                registrationNumber = vehicle.registrationNumber,
                                style = LicensePlateStyle.Compact
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Odometer pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Speed,
                                            contentDescription = null,
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        val odoStr = if (vehicle.odometerKm > 0) "%,d km".format(vehicle.odometerKm) else "Odo N/A"
                                        Text(
                                            text = odoStr,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }

                                // Fuel pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF7ED),
                                    border = BorderStroke(1.dp, Color(0xFFFFEDD5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalGasStation,
                                            contentDescription = null,
                                            tint = Color(0xFFEA580C),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = vehicle.fuelType.uppercase(),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC2410C)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. TRIP ROUTE CARD (Exact location input with Google Play Geocoder + swap)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Route Waypoints",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        if (state.isResolvingLocation) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = RaahiOrange
                                )
                                Text(
                                    "Resolving exact pin...",
                                    fontSize = 11.sp,
                                    color = RaahiOrange,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Start Location
                    OutlinedTextField(
                        value = state.startLocationName,
                        onValueChange = { viewModel.onStartLocationChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Starting Point / Exact Address", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Sector 37, Chandigarh", color = RaahiTextFaint) },
                        leadingIcon = {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RaahiText,
                            unfocusedTextColor = RaahiText,
                            focusedBorderColor = RaahiOrange,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Swap Button
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFE2E8F0)
                        )
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { viewModel.swapLocations() },
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap Points",
                                    tint = RaahiOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Destination
                    OutlinedTextField(
                        value = state.destLocationName,
                        onValueChange = { viewModel.onDestinationChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Destination / Road Trip Hub *", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Manali, Mall Road Jaipur, Agra...", color = RaahiTextFaint) },
                        leadingIcon = {
                            Icon(Icons.Filled.Navigation, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(20.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = RaahiText,
                            unfocusedTextColor = RaahiText,
                            focusedBorderColor = RaahiOrange,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Popular suggestions chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Popular Road Trips:",
                            color = Color(0xFF64748B),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Sector 37, Chandigarh", "Manali", "Jaipur", "Agra").forEach { spot ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable { viewModel.onDestinationChanged(spot) }
                                ) {
                                    Text(
                                        text = spot,
                                        color = Color(0xFF334155),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. GOOGLE MAP ROUTE PREVIEW & STATS CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    ) {
                        AndroidView(
                            factory = { mapView },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Floating map indicator badge
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xE60F172A),
                            border = BorderStroke(0.5.dp, Color(0x33FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (state.isCalculatingRoute) RaahiOrange else Color(0xFF22C55E))
                                )
                                Text(
                                    text = if (state.isCalculatingRoute) "Calculating Route..." else "Google Routes Polyline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Route Telemetry Summary Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Distance
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            val distText = state.routeDistanceKm?.let { "${String.format(Locale.ROOT, "%.1f", it)} km" } ?: "—"
                            Text(distText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        }

                        VerticalDivider(Modifier.height(24.dp), color = Color(0xFFCBD5E1))

                        // Duration / ETA
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DRIVING TIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            val etaText = state.routeDurationFormatted ?: "—"
                            Text(etaText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = RaahiOrange)
                        }

                        VerticalDivider(Modifier.height(24.dp), color = Color(0xFFCBD5E1))

                        // Fuel Cost Estimate
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("EST. FUEL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            val fuelText = if (state.estimatedFuelCost != null && state.estimatedFuelLitres != null) {
                                "₹${state.estimatedFuelCost!!.toInt()} (${state.estimatedFuelLitres}L)"
                            } else "—"
                            Text(fuelText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                        }
                    }
                }
            }

            // 4. DEPARTURE DATE & TIME PICKER (Calendar + 24-hour Clock)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Departure Date & Time",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDateTimePicker() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (!state.isDepartureValid) Color(0xFFEF4444) else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(RaahiOrange.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = RaahiOrange,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = state.departureDateTime.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy • HH:mm", Locale.ENGLISH)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "24-Hour Time Format",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Text(
                                text = "Change",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RaahiOrange
                            )
                        }
                    }

                    if (!state.isDepartureValid) {
                        Text(
                            text = "Departure date and time cannot be in the past",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // 5. TRIP BUDGET CALCULATOR
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Trip Budget Calculator",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }

                        // Round-trip toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Round Trip",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.isRoundTrip) RaahiOrange else Color(0xFF64748B)
                            )
                            Switch(
                                checked = state.isRoundTrip,
                                onCheckedChange = { viewModel.onRoundTripToggled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = RaahiOrange,
                                    uncheckedTrackColor = Color(0xFFE2E8F0),
                                    uncheckedBorderColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.scale(0.85f)
                            )
                        }
                    }

                    // Editable Mileage & Fuel Rate Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mileage editor
                        OutlinedTextField(
                            value = if (state.editableMileage > 0) state.editableMileage.toString() else "",
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { viewModel.onMileageChanged(it) }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Mileage (km/L)", fontSize = 11.5.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = RaahiText,
                                unfocusedTextColor = RaahiText,
                                focusedBorderColor = RaahiOrange,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                            )
                        )

                        // Fuel price editor
                        OutlinedTextField(
                            value = if (state.editableFuelPrice > 0) state.editableFuelPrice.toString() else "",
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { viewModel.onFuelPriceChanged(it) }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Fuel Price (₹/L)", fontSize = 11.5.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = RaahiText,
                                unfocusedTextColor = RaahiText,
                                focusedBorderColor = RaahiOrange,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                            )
                        )
                    }

                    // Budget Metrics Grid (2x2)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Distance & Fuel Litres
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    val distLabel = if (state.isRoundTrip) "TOTAL DISTANCE (ROUND-TRIP)" else "TOTAL DISTANCE"
                                    val distVal = state.routeDistanceKm?.let {
                                        val effective = if (state.isRoundTrip) it * 2.0 else it
                                        "${String.format(Locale.ROOT, "%.1f", effective)} km"
                                    } ?: "—"
                                    Text(distLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                                    Text(distVal, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("ESTIMATED FUEL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                                    val fuelVal = if (state.estimatedFuelLitres != null && state.estimatedFuelCost != null) {
                                        "${state.estimatedFuelLitres} L (₹${state.estimatedFuelCost?.toInt()})"
                                    } else "—"
                                    Text(fuelVal, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            // Row 2: Toll Cost & Cost per km
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("ESTIMATED TOLLS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                                    val tollText = if (state.tollAvailable && state.tollCost != null) {
                                        "₹${state.tollCost?.toInt()}"
                                    } else {
                                        "Unavailable"
                                    }
                                    Text(
                                        tollText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (state.tollAvailable) Color(0xFF0284C7) else Color(0xFF94A3B8)
                                    )
                                    if (!state.tollAvailable) {
                                        Text(
                                            state.tollUnavailableReason ?: "Toll data not provided for this route",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("COST PER KM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                                    val costKmText = state.costPerKm?.let { "₹${String.format(Locale.ROOT, "%.2f", it)}/km" } ?: "—"
                                    Text(costKmText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                }
                            }
                        }
                    }

                    // Total Cost Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "TOTAL ESTIMATED TRIP COST",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    if (state.tollAvailable) "Includes fuel & tolls" else "Estimated fuel only (tolls unavailable)",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                            val totalStr = state.estimatedTotalCost?.let { "₹${it.toInt()}" } ?: "—"
                            Text(
                                text = totalStr,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                fontFamily = RaahiDisplayFont
                            )
                        }
                    }
                }
            }

            // 6. FUEL RANGE & STOP PLANNER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF7ED)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocalGasStation,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Fuel Range & Stop Planner",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Live telemetry based on remaining fuel & vehicle mileage",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Fuel Input & Range Estimation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = if (state.remainingFuelLitres > 0) state.remainingFuelLitres.toString() else "",
                            onValueChange = { str ->
                                str.toDoubleOrNull()?.let { viewModel.onRemainingFuelChanged(it) }
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Remaining Fuel (L)", fontSize = 11.5.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = RaahiText,
                                unfocusedTextColor = RaahiText,
                                focusedBorderColor = RaahiOrange,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                            )
                        )

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("ESTIMATED RANGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                                Text(
                                    "${state.remainingFuelRangeKm.toInt()} km",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (state.needsRefuel) Color(0xFFDC2626) else Color(0xFF16A34A)
                                )
                            }
                        }
                    }

                    // Range alert banner
                    if (state.refuelAdviceMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (state.needsRefuel) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, if (state.needsRefuel) Color(0xFFFDE68A) else Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.needsRefuel) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (state.needsRefuel) Color(0xFFD97706) else Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.refuelAdviceMessage!!,
                                    color = if (state.needsRefuel) Color(0xFF92400E) else Color(0xFF166534),
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Recommended Fuel Stations along Route
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Verified Fuel Stations Along Corridor:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )

                        if (state.isLoadingFuelStations) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = RaahiOrange)
                                Text("Indexing fuel stations along corridor...", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        } else if (state.recommendedFuelStations.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    "No verified fuel stations indexed along this specific highway segment. Fill tank prior to departure.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            state.recommendedFuelStations.forEach { station ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                station.name,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "~${station.distanceAlongRouteKm} km into trip • Detour +${station.detourKm} km",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${station.lat},${station.lng}")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp),
                                            border = BorderStroke(1.dp, RaahiOrange)
                                        ) {
                                            Text("Open in Maps", fontSize = 10.5.sp, color = RaahiOrange, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. ALTERNATIVE ROUTE COMPARISON
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AltRoute,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Alternative Route Comparison",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Compare genuine routes computed via Google Routes API",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    if (state.alternativeRoutes.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                "Enter a destination above to compute alternative routes.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        state.alternativeRoutes.forEachIndexed { index, route ->
                            val isSelected = state.selectedRouteIndex == index
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectRoute(index) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFFFF7ED) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) RaahiOrange else Color(0xFFE2E8F0)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Badge label
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) RaahiOrange else Color(0xFFE2E8F0)
                                        ) {
                                            Text(
                                                text = route.routeLabel ?: "Route ${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (isSelected) {
                                            Text(
                                                "SELECTED",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RaahiOrange
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "${route.distanceKm} km • ${route.formattedEta}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            val tollNote = if (route.tollAvailable && route.tollCost != null) {
                                                "Toll: ₹${route.tollCost.toInt()}"
                                            } else {
                                                "Toll: Unavailable"
                                            }
                                            Text(
                                                tollNote,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }

                                        val routeFuelCost = ((route.distanceKm / state.editableMileage) * state.editableFuelPrice).roundToInt()
                                        Text(
                                            "₹$routeFuelCost (Fuel)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }
                            }
                        }

                        if (state.alternativeRoutes.size == 1) {
                            Text(
                                "Only 1 primary highway corridor available for this route from Google Routes API.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // 8. SMART BREAK PLANNER
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FreeBreakfast,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Smart Break Suggestions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Dhabas, highway rest stops & washrooms along selected route",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    if (state.isLoadingBreaks) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = RaahiOrange)
                            Text("Finding rest stops along corridor...", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    } else if (state.smartBreaks.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                "No verified highway rest stops found along this immediate route segment. Plan scheduled pauses every 2-3 hours.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        state.smartBreaks.forEach { stop ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (stop.type) {
                                                    "dhaba" -> Color(0xFFFEF3C7)
                                                    "toilet" -> Color(0xFFE0F2FE)
                                                    else -> Color(0xFFF1F5F9)
                                                }
                                            ) {
                                                Text(
                                                    text = stop.typeLabel,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (stop.type) {
                                                        "dhaba" -> Color(0xFF92400E)
                                                        "toilet" -> Color(0xFF0369A1)
                                                        else -> Color(0xFF334155)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            stop.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "~${stop.distanceAlongRouteKm} km into trip • Detour +${stop.detourKm} km",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${stop.lat},${stop.lng}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp),
                                        border = BorderStroke(1.dp, Color(0xFF0284C7))
                                    ) {
                                        Text("Open in Maps", fontSize = 10.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Text(
                            "Stops are suggestions. Tap 'Open in Maps' to inspect locations; stops are never added without confirmation.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            if (state.error != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        state.error!!,
                        color = Color(0xFFDC2626),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 6. ACTION CTA BUTTON
            Button(
                onClick = { viewModel.createTrip() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSubmitting && state.isDepartureValid && state.destLocationName.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (!state.isSubmitting && state.isDepartureValid && state.destLocationName.isNotBlank()) {
                                RaahiBrandGradient
                            } else {
                                androidx.compose.ui.graphics.SolidColor(Color(0xFFCBD5E1))
                            },
                            RoundedCornerShape(14.dp)
                        ),
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

            Spacer(Modifier.height(16.dp))
        }
    }
}
