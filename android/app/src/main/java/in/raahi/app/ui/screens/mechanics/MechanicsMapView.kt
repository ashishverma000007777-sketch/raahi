package `in`.raahi.app.ui.screens.mechanics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng as GoogleLatLng
import `in`.raahi.app.data.LatLng

@Composable
fun MechanicsMapView(
    userLocation: LatLng?,
    mechanics: List<UnifiedMechanicItem>,
    selectedId: String?,
    cameraTarget: LatLng?,
    cameraZoom: Double?,
    onSelect: (String) -> Unit,
    onMapReady: (GoogleMap) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val markerMap = remember { mutableMapOf<String, Marker>() }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }

    val verifiedIcon = remember { createMechanicMarkerBitmap(isVerified = true, isSelected = false) }
    val verifiedSelectedIcon = remember { createMechanicMarkerBitmap(isVerified = true, isSelected = true) }
    val googleIcon = remember { createMechanicMarkerBitmap(isVerified = false, isSelected = false) }
    val googleSelectedIcon = remember { createMechanicMarkerBitmap(isVerified = false, isSelected = true) }
    val userDotIcon = remember { createUserDotBitmap() }

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
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

    // Camera animation when cameraTarget updates
    LaunchedEffect(googleMap, cameraTarget, cameraZoom) {
        val map = googleMap ?: return@LaunchedEffect
        val target = cameraTarget ?: return@LaunchedEffect
        val zoom = cameraZoom?.toFloat() ?: 14.5f
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(GoogleLatLng(target.lat, target.lng), zoom))
    }

    // Update user location marker
    LaunchedEffect(googleMap, userLocation) {
        val map = googleMap ?: return@LaunchedEffect
        if (userLocation != null) {
            val uLatLng = GoogleLatLng(userLocation.lat, userLocation.lng)
            if (userLocationMarker == null) {
                userLocationMarker = map.addMarker(
                    MarkerOptions()
                        .position(uLatLng)
                        .title("Your Location")
                        .icon(userDotIcon)
                        .anchor(0.5f, 0.5f)
                        .zIndex(100f)
                )
            } else {
                userLocationMarker?.position = uLatLng
            }
        } else {
            userLocationMarker?.remove()
            userLocationMarker = null
        }
    }

    // Synchronize mechanic markers on the map
    LaunchedEffect(googleMap, mechanics, selectedId) {
        val map = googleMap ?: return@LaunchedEffect

        val currentIds = mechanics.map { it.id }.toSet()

        // Remove markers not in current list
        val iterator = markerMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!currentIds.contains(entry.key)) {
                entry.value.remove()
                iterator.remove()
            }
        }

        // Add or update markers
        for (item in mechanics) {
            val isSelected = item.id == selectedId
            val icon = when {
                item.source == MechanicSource.VERIFIED && isSelected -> verifiedSelectedIcon
                item.source == MechanicSource.VERIFIED -> verifiedIcon
                isSelected -> googleSelectedIcon
                else -> googleIcon
            }

            val existingMarker = markerMap[item.id]
            if (existingMarker != null) {
                existingMarker.setIcon(icon)
                existingMarker.zIndex = if (isSelected) 50f else 10f
            } else {
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(GoogleLatLng(item.lat, item.lng))
                        .title(item.name)
                        .snippet(item.address ?: item.specializations ?: "")
                        .icon(icon)
                        .anchor(0.5f, 0.5f)
                        .zIndex(if (isSelected) 50f else 10f)
                )
                if (marker != null) {
                    marker.tag = item.id
                    markerMap[item.id] = marker
                }
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    map.uiSettings.isZoomControlsEnabled = false
                    map.uiSettings.isCompassEnabled = true
                    map.uiSettings.isMyLocationButtonEnabled = false
                    map.mapType = GoogleMap.MAP_TYPE_NORMAL

                    map.setOnMarkerClickListener { marker ->
                        val id = marker.tag as? String
                        if (id != null) {
                            onSelect(id)
                            true
                        } else {
                            false
                        }
                    }

                    map.setOnMapClickListener {
                        onSelect("")
                    }

                    googleMap = map
                    onMapReady(map)
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}

private fun createUserDotBitmap(): BitmapDescriptor {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Outer glow
    paint.color = android.graphics.Color.parseColor("#3300CFFF")
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // White border ring
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, 15f, paint)

    // Inner bright cyan dot
    paint.color = android.graphics.Color.parseColor("#00CFFF")
    canvas.drawCircle(size / 2f, size / 2f, 11f, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun createMechanicMarkerBitmap(isVerified: Boolean, isSelected: Boolean): BitmapDescriptor {
    val size = if (isSelected) 60 else 46
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val primaryColor = if (isVerified) {
        android.graphics.Color.parseColor("#1B6BFF") // Raahi Verified Blue
    } else {
        android.graphics.Color.parseColor("#10B981") // Google Places Emerald
    }

    // Selection outer ring
    if (isSelected) {
        paint.color = android.graphics.Color.parseColor("#44FF6B35")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    }

    // Outer white halo
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (if (isSelected) 6f else 2f), paint)

    // Main marker circle
    paint.color = primaryColor
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - (if (isSelected) 10f else 6f), paint)

    // Center indicator dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, if (isSelected) 6f else 4f, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
