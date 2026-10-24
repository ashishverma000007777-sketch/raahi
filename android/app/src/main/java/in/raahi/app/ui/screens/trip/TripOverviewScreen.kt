package `in`.raahi.app.ui.screens.trip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
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
import `in`.raahi.app.network.TripDto
import `in`.raahi.app.network.TripStopDto
import `in`.raahi.app.ui.components.SectionLabel
import `in`.raahi.app.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripOverviewScreen(
    onBack: () -> Unit,
    onStartTrip: (String) -> Unit,
    viewModel: TripOverviewViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    val isStarting by viewModel.isStarting.collectAsState()

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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TripOverviewEvent.TripStarted -> onStartTrip(event.tripId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trip Overview",
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
        when (val s = state) {
            is TripOverviewUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RaahiOrange)
                }
            }
            is TripOverviewUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = RaahiRed, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.loadTrip() },
                            colors = ButtonDefaults.buttonColors(containerColor = RaahiPrimaryButton),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
            is TripOverviewUiState.Success -> {
                val trip = s.trip

                // Update Google Map with route, markers, and bounds
                LaunchedEffect(trip.id, s.routePolyline) {
                    mapView.getMapAsync { googleMap ->
                        googleMap.clear()
                        val pLatLng = LatLng(trip.startLat, trip.startLng)
                        val dLatLng = LatLng(trip.destLat, trip.destLng)

                        // Pickup Marker
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(pLatLng)
                                .title("Start: ${trip.startLocationName}")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )

                        // Destination Marker
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(dLatLng)
                                .title("Destination: ${trip.destLocationName}")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )

                        // Optional stop markers
                        trip.prominentFuelStops?.forEach { stop ->
                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(stop.lat, stop.lng))
                                    .title(stop.name)
                                    .snippet("Fuel Stop")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                            )
                        }

                        // Polyline
                        val polylineOpts = PolylineOptions()
                            .width(8f)
                            .color(android.graphics.Color.parseColor("#EF6C00"))
                            .geodesic(true)

                        if (s.routePolyline.isNotEmpty()) {
                            polylineOpts.addAll(s.routePolyline)
                        } else {
                            polylineOpts.add(pLatLng, dLatLng)
                        }
                        googleMap.addPolyline(polylineOpts)

                        try {
                            val builder = LatLngBounds.builder().include(pLatLng).include(dLatLng)
                            s.routePolyline.forEach { builder.include(it) }
                            val bounds = builder.build()
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 110))
                        } catch (_: Exception) {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pLatLng, 9f))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // 1. PROMINENT GOOGLE MAP ROUTE CARD
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
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                            ) {
                                AndroidView(
                                    factory = { mapView },
                                    modifier = Modifier.fillMaxSize()
                                )

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
                                                .background(Color(0xFF22C55E))
                                        )
                                        Text(
                                            text = "Verified Google Map Route",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Waypoints header row below map
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = trip.title,
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    fontFamily = RaahiDisplayFont,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Filled.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = trip.startLocationName,
                                        color = Color(0xFF475569),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Text(
                                        text = " → ",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    Icon(
                                        Icons.Filled.Navigation,
                                        contentDescription = null,
                                        tint = RaahiOrange,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = trip.destLocationName,
                                        color = Color(0xFF0F172A),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        }
                    }

                    // 2. WEATHER ALERT BANNER (If present)
                    if (trip.weatherWarning != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                Text(
                                    trip.weatherWarning,
                                    color = Color(0xFF92400E),
                                    fontSize = 12.5.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 3. COMPACT ESTIMATES 2x2 GRID (Automotive card standard)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Trip Estimates")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OverviewMetricCard(
                                label = "TOTAL DISTANCE",
                                value = "${trip.distanceKm} km",
                                subtext = "Highway Corridor",
                                icon = Icons.Outlined.Route,
                                accentColor = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f)
                            )
                            OverviewMetricCard(
                                label = "ESTIMATED TIME",
                                value = trip.durationFormatted,
                                subtext = "At ~55 km/h avg",
                                icon = Icons.Outlined.Schedule,
                                accentColor = RaahiOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val fuelVal = if (s.effectiveFuelLitres != null) "${s.effectiveFuelLitres} L" else "Unavailable"
                            val fuelCost = if (s.effectiveFuelCost != null) "₹${s.effectiveFuelCost.toInt()} · ${trip.fuelType ?: "Petrol"}" else "Profile unlinked"
                            OverviewMetricCard(
                                label = "ESTIMATED FUEL",
                                value = fuelVal,
                                subtext = fuelCost,
                                icon = Icons.Outlined.LocalGasStation,
                                accentColor = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )

                            // Tolls: Real data rule (No fake numbers)
                            val tollVal = if (s.tollAvailable && s.tollCost != null) "₹${s.tollCost.toInt()}" else "Unavailable"
                            val tollSub = if (s.tollAvailable) "Google Routes / FASTag" else (s.tollUnavailableReason ?: "Toll data not provided")
                            OverviewMetricCard(
                                label = "ESTIMATED TOLLS",
                                value = tollVal,
                                subtext = tollSub,
                                icon = Icons.Outlined.Toll,
                                accentColor = if (s.tollAvailable) Color(0xFF059669) else Color(0xFF94A3B8),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Total Estimated Trip Budget Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "ESTIMATED TOTAL TRIP BUDGET",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46),
                                        letterSpacing = 0.5.sp
                                    )
                                    val budgetSub = if (s.costPerKm != null) {
                                        "₹${String.format(Locale.ROOT, "%.2f", s.costPerKm)}/km • ${if (s.tollAvailable) "Fuel & Tolls included" else "Estimated fuel only"}"
                                    } else {
                                        if (s.tollAvailable) "Fuel & Tolls included" else "Estimated fuel only (tolls unavailable)"
                                    }
                                    Text(budgetSub, fontSize = 11.sp, color = Color(0xFF047857))
                                }
                                val totalStr = s.totalEstimatedBudget?.let { "₹${it.toInt()}" } ?: "—"
                                Text(
                                    text = totalStr,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46),
                                    fontFamily = RaahiDisplayFont
                                )
                            }
                        }
                    }

                    // 4. ROUTE WEATHER CARD
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (trip.weatherCondition != null) Color(0xFFE0F2FE) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (trip.weatherCondition != null) Icons.Outlined.WbSunny else Icons.Outlined.CloudOff,
                                        contentDescription = null,
                                        tint = if (trip.weatherCondition != null) Color(0xFF0284C7) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = trip.weatherCondition ?: "Weather Unavailable",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (trip.weatherCondition != null) "Open-Meteo live feed • Safe driving" else "Weather provider feed unreachable or offline",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // 5. RECOMMENDED FUEL STOPS
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Recommended Fuel Stops")

                        if (s.fuelStops.isNotEmpty()) {
                            s.fuelStops.forEachIndexed { index, stop ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFFF7ED)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Outlined.LocalGasStation, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(13.dp))
                                                }
                                                Text(stop.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "~${stop.distanceAlongRouteKm} km into route • Detour +${stop.detourKm} km",
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
                                            border = BorderStroke(1.dp, RaahiOrange)
                                        ) {
                                            Text("Open in Maps", fontSize = 10.5.sp, color = RaahiOrange, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else if (!trip.prominentFuelStops.isNullOrEmpty()) {
                            trip.prominentFuelStops.take(2).forEachIndexed { index, stop ->
                                FuelStopCard(stop = stop, index = index + 1)
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocalGasStation,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            "No fuel stations indexed on route",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            "No verified stations along this specific highway corridor in database. Keep tank filled before departure.",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. SMART BREAKS (DHABAS & REST STOPS)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Smart Breaks & Rest Stops Along Route")

                        if (s.smartBreaks.isNotEmpty()) {
                            s.smartBreaks.forEach { stop ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
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
                                                Text(
                                                    stop.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E293B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.height(2.dp))
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
                        } else if (!trip.pitStops.isNullOrEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(trip.pitStops) { stop ->
                                    PitStopCard(stop)
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    "No highway rest stops indexed along corridor. Schedule rest breaks every 2-3 hours.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    val mechanics = trip.mechanicsAlongRoute.orEmpty()
                    if (mechanics.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel("Verified Mechanics Along Route")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(mechanics) { mech ->
                                    MechanicRouteCard(mech)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // 7. GOOGLE MAPS NAVIGATION & START TRIP BUTTONS
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${trip.startLat},${trip.startLng}&destination=${trip.destLat},${trip.destLng}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF0F9FF)
                        )
                    ) {
                        Icon(
                            Icons.Default.Directions,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Open Route in Google Maps",
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.startTrip() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isStarting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(RaahiBrandGradient, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isStarting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            } else {
                                Text(
                                    "Start Trip Mode →",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = RaahiDisplayFont
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

@Composable
private fun OverviewMetricCard(
    label: String,
    value: String,
    subtext: String? = null,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = label,
                    color = Color(0xFF64748B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                color = Color(0xFF0F172A),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = RaahiDisplayFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtext != null) {
                Text(
                    text = subtext,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FuelStopCard(stop: TripStopDto, index: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#$index",
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stop.name,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val distStr = if (stop.distanceKm != null) "At ~${stop.distanceKm.toInt()} km" else "On route"
                Text(
                    "$distStr · ${stop.amenities ?: "Fuel station"}",
                    color = Color(0xFF64748B),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (stop.pricePerLitre != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${String.format(Locale.ROOT, "%.2f", stop.pricePerLitre)}/L",
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PitStopCard(stop: TripStopDto) {
    Surface(
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = null, tint = RaahiOrange, modifier = Modifier.size(15.dp))
                Text("Pit Stop", color = RaahiOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(stop.name, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(stop.amenities ?: "Food & Rest", color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MechanicRouteCard(mech: TripStopDto) {
    Surface(
        modifier = Modifier.width(190.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Build, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(15.dp))
                Text("Verified Mechanic", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(mech.name, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(mech.amenities ?: "Roadside repair", color = Color(0xFF64748B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
