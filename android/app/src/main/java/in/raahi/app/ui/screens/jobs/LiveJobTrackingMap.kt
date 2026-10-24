package `in`.raahi.app.ui.screens.jobs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.*
import `in`.raahi.app.data.GoogleRouteData
import `in`.raahi.app.data.GoogleRoutesProvider
import `in`.raahi.app.data.GoogleRoutesStatus
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LiveJobTrackingMap(
    customerLat: Double,
    customerLng: Double,
    helperLocation: Pair<Double, Double>?,
    helperName: String?,
    jobStatus: String,
    isHelperViewer: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val routesProvider = remember { GoogleRoutesProvider(context.applicationContext) }

    val customerLatLng = remember(customerLat, customerLng) {
        LatLng(customerLat, customerLng)
    }

    val helperLatLng = remember(helperLocation) {
        val loc = helperLocation ?: return@remember null
        val lat = loc.first
        val lng = loc.second
        if (lat.isFinite() && lng.isFinite()) LatLng(lat, lng) else null
    }

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var customerMarker by remember { mutableStateOf<Marker?>(null) }
    var helperMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }

    var routesStatus by remember { mutableStateOf<GoogleRoutesStatus>(GoogleRoutesStatus.Idle) }
    var activeRouteData by remember { mutableStateOf<GoogleRouteData?>(null) }

    // MapView instance with lifecycle
    val mapView = remember {
        try {
            MapsInitializer.initialize(context.applicationContext)
        } catch (_: Exception) {}
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    val customerIcon = remember(googleMap) {
        if (googleMap != null) createCustomerMarkerBitmap() else null
    }
    val helperIcon = remember(googleMap) {
        if (googleMap != null) createHelperMarkerBitmap() else null
    }

    DisposableEffect(lifecycleOwner, mapView) {
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
            mapView.onDestroy()
        }
    }

    // Google Routes fetch when helper coordinate updates
    LaunchedEffect(helperLatLng, customerLatLng) {
        val hLoc = helperLatLng ?: return@LaunchedEffect
        routesStatus = GoogleRoutesStatus.Loading
        val result = routesProvider.computeDrivingRoute(
            startLat = hLoc.latitude,
            startLng = hLoc.longitude,
            destLat = customerLatLng.latitude,
            destLng = customerLatLng.longitude,
        )
        routesStatus = result
        when (result) {
            is GoogleRoutesStatus.Success -> {
                activeRouteData = result.route
            }
            is GoogleRoutesStatus.Unavailable -> {
                activeRouteData = null
            }
            else -> {}
        }
    }

    // Update map markers and polyline when data changes
    LaunchedEffect(googleMap, customerLatLng, helperLatLng, activeRouteData, customerIcon, helperIcon) {
        val map = googleMap ?: return@LaunchedEffect

        // Customer marker
        if (customerMarker == null) {
            val options = MarkerOptions()
                .position(customerLatLng)
                .title("Breakdown Location")
                .snippet("Customer awaiting assistance")
                .anchor(0.5f, 0.5f)
            customerIcon?.let { options.icon(it) }
            customerMarker = map.addMarker(options)
        } else {
            customerMarker?.position = customerLatLng
            customerIcon?.let { customerMarker?.setIcon(it) }
        }

        // Helper marker
        if (helperLatLng != null) {
            val label = helperName?.ifBlank { "Helper" } ?: "Helper"
            if (helperMarker == null) {
                val options = MarkerOptions()
                    .position(helperLatLng)
                    .title(if (isHelperViewer) "You" else label)
                    .snippet("Live Tracking")
                    .anchor(0.5f, 0.5f)
                helperIcon?.let { options.icon(it) }
                helperMarker = map.addMarker(options)
            } else {
                helperMarker?.position = helperLatLng
                helperMarker?.title = if (isHelperViewer) "You" else label
                helperIcon?.let { helperMarker?.setIcon(it) }
            }
        } else {
            helperMarker?.remove()
            helperMarker = null
        }

        // Route Polyline
        val route = activeRouteData
        if (route != null && route.points.isNotEmpty()) {
            if (routePolyline == null) {
                routePolyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(route.points)
                        .color(android.graphics.Color.parseColor("#FF5722")) // RaahiOrange
                        .width(12f)
                        .geodesic(true)
                )
            } else {
                routePolyline?.points = route.points
            }
        } else if (helperLatLng != null) {
            // Straight line connection while Google Routes loads or as fallback
            val fallbackLine = listOf(helperLatLng, customerLatLng)
            if (routePolyline == null) {
                routePolyline = map.addPolyline(
                    PolylineOptions()
                        .addAll(fallbackLine)
                        .color(android.graphics.Color.parseColor("#99FF6B35"))
                        .width(8f)
                        .geodesic(true)
                )
            } else {
                routePolyline?.points = fallbackLine
            }
        } else {
            routePolyline?.remove()
            routePolyline = null
        }

        // Smooth camera framing
        try {
            if (helperLatLng != null) {
                val bounds = LatLngBounds.builder()
                    .include(customerLatLng)
                    .include(helperLatLng)
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 110))
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLatLng, 15f))
            }
        } catch (_: Exception) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLatLng, 15f))
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E222A))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        try {
                            MapsInitializer.initialize(context.applicationContext)
                        } catch (_: Exception) {}
                        map.uiSettings.isZoomControlsEnabled = false
                        map.uiSettings.isCompassEnabled = true
                        map.uiSettings.isMyLocationButtonEnabled = false
                        map.mapType = GoogleMap.MAP_TYPE_NORMAL
                        googleMap = map
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Top Status & ETA Card
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(RaahiBg2.copy(alpha = 0.95f))
                    .border(1.dp, RaahiBorderSoft, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    jobStatus == "PENDING" -> RaahiAmber
                                    helperLatLng != null -> RaahiGreen
                                    else -> RaahiOrange
                                }
                            )
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = when {
                            jobStatus == "PENDING" -> "Searching for nearby helpers · Your location"
                            isHelperViewer -> {
                                if (helperLatLng != null) {
                                    if (activeRouteData != null) {
                                        "Your GPS is active · ETA to customer ${activeRouteData!!.formattedEta} · ${activeRouteData!!.distanceKm} km"
                                    } else {
                                        "Your GPS is active · En route to customer"
                                    }
                                } else {
                                    "Acquiring your GPS location..."
                                }
                            }
                            else -> {
                                if (helperLatLng != null) {
                                    if (activeRouteData != null) {
                                        "Helper en route · ETA ${activeRouteData!!.formattedEta} · ${activeRouteData!!.distanceKm} km"
                                    } else {
                                        "Helper location is live · Heading to your breakdown"
                                    }
                                } else if (jobStatus == "MATCHED" || jobStatus == "EN_ROUTE") {
                                    "Helper accepted · Connecting to live GPS..."
                                } else {
                                    "Waiting for helper location..."
                                }
                            }
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RaahiText,
                    )
                }
            }

            // Route status notice if Google Routes is unavailable
            if (routesStatus is GoogleRoutesStatus.Unavailable && helperLatLng != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Google Routes pending API activation · Showing live pins",
                        fontSize = 10.5.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // Camera control FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Recenter to both
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(RaahiBg2)
                    .border(1.dp, RaahiBorderSoft, CircleShape)
                    .clickable {
                        val map = googleMap ?: return@clickable
                        try {
                            if (helperLatLng != null) {
                                val bounds = LatLngBounds.builder()
                                    .include(customerLatLng)
                                    .include(helperLatLng)
                                    .build()
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 110))
                            } else {
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLatLng, 15f))
                            }
                        } catch (_: Exception) {
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(customerLatLng, 15f))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "Center",
                    tint = RaahiOrange,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

private fun createCustomerMarkerBitmap(): BitmapDescriptor? {
    return runCatching {
        val size = 52
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer glow
        paint.color = android.graphics.Color.parseColor("#33FF3B30")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Outer ring
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 18f, paint)

        // Inner breakdown red circle
        paint.color = android.graphics.Color.parseColor("#FF3B30")
        canvas.drawCircle(size / 2f, size / 2f, 14f, paint)

        // Center dot
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 5f, paint)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }.getOrElse {
        runCatching {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        }.getOrNull()
    }
}

private fun createHelperMarkerBitmap(): BitmapDescriptor? {
    return runCatching {
        val size = 52
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer glow
        paint.color = android.graphics.Color.parseColor("#3300C853")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Outer ring
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 18f, paint)

        // Inner vehicle green circle
        paint.color = android.graphics.Color.parseColor("#00C853")
        canvas.drawCircle(size / 2f, size / 2f, 14f, paint)

        // Center dot
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 5f, paint)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    }.getOrElse {
        runCatching {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        }.getOrNull()
    }
}
