package `in`.raahi.app.ui.screens.trip

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
                            val fuelText = if (state.routeEstimatedFuelCost != null && state.routeEstimatedFuelLitres != null) {
                                "₹${state.routeEstimatedFuelCost!!.toInt()} (${state.routeEstimatedFuelLitres}L)"
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

            // 5. TRIP SETUP & PREFERENCES
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
                    Text(
                        text = "Trip Preferences",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // Tank Full Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tank full before start?", color = Color(0xFF1E293B), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("Optimizes fuel stop corridor alerts", color = Color(0xFF64748B), fontSize = 11.5.sp)
                        }
                        Switch(
                            checked = state.tankFull,
                            onCheckedChange = { viewModel.onTankFullToggled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RaahiOrange,
                                uncheckedTrackColor = Color(0xFFE2E8F0),
                                uncheckedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Passengers Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Passengers", color = Color(0xFF1E293B), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 4, 6).forEach { count ->
                                val selected = state.passengers == count
                                Surface(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { viewModel.onPassengersChanged(count) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) RaahiOrange else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (selected) RaahiOrange else Color(0xFFCBD5E1))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "$count",
                                            color = if (selected) Color.White else Color(0xFF334155),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Preferred Route
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Preferred Route Strategy", color = Color(0xFF1E293B), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Fastest Highway", "Scenic / Less Tolls").forEach { route ->
                                val selected = state.preferredRoute == route
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) RaahiOrange.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (selected) RaahiOrange else Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { viewModel.onPreferredRouteChanged(route) }
                                ) {
                                    Text(
                                        text = route,
                                        color = if (selected) RaahiOrange else Color(0xFF475569),
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
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
