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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng as GoogleLatLng
import `in`.raahi.app.data.LatLng
import kotlinx.coroutines.delay

@Composable
fun MechanicsMapView(
    userLocation: LatLng?,
    mechanics: List<UnifiedMechanicItem>,
    selectedId: String?,
    cameraTarget: LatLng?,
    cameraZoom: Double?,
    onSelect: (String) -> Unit,
    onCameraMoved: () -> Unit = {},
    onMapReady: (GoogleMap) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    val topPaddingPx = with(density) { 60.dp.roundToPx() }
    val bottomPaddingPx = with(density) { 210.dp.roundToPx() }

    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val markerMap = remember { mutableMapOf<String, Marker>() }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var initialCameraPositioned by remember { mutableStateOf(false) }

    // Defer marker icon bitmap generation until Google Maps SDK is initialized and ready
    val verifiedIcon = remember(googleMap) {
        if (googleMap != null) createMechanicMarkerBitmap(isVerified = true, isSelected = false) else null
    }
    val verifiedMidIcon = remember(googleMap) {
        if (googleMap != null) createMechanicMarkerBitmap(isVerified = true, isSelected = true, scaleFactor = 0.88f) else null
    }
    val verifiedSelectedIcon = remember(googleMap) {
        if (googleMap != null) createMechanicMarkerBitmap(isVerified = true, isSelected = true, scaleFactor = 1.0f) else null
    }
    val googleIcon = remember(googleMap) {
        if (googleMap != null) createMechanicMarkerBitmap(isVerified = false, isSelected = false) else null
    }
    val googleMidIcon = remember(googleMap) {
        if (googleMap != null) createMechanicMarkerBitmap(isVerified = false, isSelected = true, scaleFactor = 0.88f) else null
    }
    val googleSelectedIcon = remember(googleMap) {
        if (googleMap != null) createMechanicMarkerBitmap(isVerified = false, isSelected = true, scaleFactor = 1.0f) else null
    }
    val userDotIcon = remember(googleMap) {
        if (googleMap != null) createUserDotBitmap() else null
    }

    val mapView = remember {
        MapView(context).apply {
            try {
                MapsInitializer.initialize(context)
            } catch (_: Exception) {}
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

    // Initial camera positioning as soon as GoogleMap is bound and valid user location is available
    LaunchedEffect(googleMap, userLocation) {
        val map = googleMap ?: return@LaunchedEffect
        val uLoc = userLocation ?: return@LaunchedEffect
        if (!initialCameraPositioned && isValidLocation(uLoc)) {
            val zoom = cameraZoom?.toFloat() ?: 14.5f
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(GoogleLatLng(uLoc.lat, uLoc.lng), zoom))
            initialCameraPositioned = true
        }
    }

    // Camera animation when cameraTarget explicitly updates (e.g. selection or recenter button)
    LaunchedEffect(googleMap, cameraTarget, cameraZoom) {
        val map = googleMap ?: return@LaunchedEffect
        val target = cameraTarget ?: return@LaunchedEffect
        if (isValidLocation(target)) {
            val zoom = cameraZoom?.toFloat() ?: 14.5f
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(GoogleLatLng(target.lat, target.lng), zoom))
            initialCameraPositioned = true
        }
    }

    // Update user location marker
    LaunchedEffect(googleMap, userLocation, userDotIcon) {
        val map = googleMap ?: return@LaunchedEffect
        if (userLocation != null) {
            val uLatLng = GoogleLatLng(userLocation.lat, userLocation.lng)
            val currentMarker = userLocationMarker
            if (currentMarker == null) {
                val opts = MarkerOptions()
                    .position(uLatLng)
                    .title("Your Location")
                    .anchor(0.5f, 0.5f)
                    .zIndex(100f)
                if (userDotIcon != null) {
                    opts.icon(userDotIcon)
                }
                userLocationMarker = map.addMarker(opts)
            } else {
                currentMarker.position = uLatLng
                if (userDotIcon != null) {
                    currentMarker.setIcon(userDotIcon)
                }
            }
        } else {
            userLocationMarker?.remove()
            userLocationMarker = null
        }
    }

    // Synchronize mechanic markers on the map
    LaunchedEffect(googleMap, mechanics, selectedId, verifiedIcon, verifiedSelectedIcon, googleIcon, googleSelectedIcon) {
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
            if (!isValidCoordinate(item.lat, item.lng)) continue

            val isSelected = item.id == selectedId
            val icon = when {
                item.source == MechanicSource.VERIFIED && isSelected -> verifiedSelectedIcon
                item.source == MechanicSource.VERIFIED -> verifiedIcon
                isSelected -> googleSelectedIcon
                else -> googleIcon
            }

            val existingMarker = markerMap[item.id]
            if (existingMarker != null) {
                if (icon != null) {
                    existingMarker.setIcon(icon)
                }
                existingMarker.setAnchor(0.5f, 0.92f)
                existingMarker.zIndex = if (isSelected) 100f else 10f
            } else {
                val opts = MarkerOptions()
                    .position(GoogleLatLng(item.lat, item.lng))
                    .title(item.name)
                    .snippet(item.address ?: item.specializations ?: "")
                    .anchor(0.5f, 0.92f)
                    .zIndex(if (isSelected) 100f else 10f)
                if (icon != null) {
                    opts.icon(icon)
                }
                val marker = map.addMarker(opts)
                if (marker != null) {
                    marker.tag = item.id
                    markerMap[item.id] = marker
                }
            }
        }
    }

    // Ensure map camera viewport padding is applied so target markers are centered in unobstructed area above cards
    LaunchedEffect(googleMap, topPaddingPx, bottomPaddingPx) {
        googleMap?.setPadding(0, topPaddingPx, 0, bottomPaddingPx)
    }

    var prevSelectedId by remember { mutableStateOf<String?>(null) }

    // Smooth scale-up pop transition when a marker is selected and reset when deselected
    LaunchedEffect(selectedId) {
        val currentSelectedId = selectedId
        // Deselect previous marker smoothly back to standard size and lower zIndex
        if (prevSelectedId != null && prevSelectedId != currentSelectedId) {
            val prevMarker = markerMap[prevSelectedId]
            val prevItem = mechanics.firstOrNull { it.id == prevSelectedId }
            if (prevMarker != null && prevItem != null) {
                val icon = if (prevItem.source == MechanicSource.VERIFIED) verifiedIcon else googleIcon
                if (icon != null) {
                    prevMarker.setIcon(icon)
                }
                prevMarker.zIndex = 10f
            }
        }

        // Pop animation for newly selected marker: intermediate frame -> final prominent Raahi marker
        if (currentSelectedId != null) {
            val newMarker = markerMap[currentSelectedId]
            val newItem = mechanics.firstOrNull { it.id == currentSelectedId }
            if (newMarker != null && newItem != null) {
                newMarker.zIndex = 100f
                val midIcon = if (newItem.source == MechanicSource.VERIFIED) verifiedMidIcon else googleMidIcon
                val finalIcon = if (newItem.source == MechanicSource.VERIFIED) verifiedSelectedIcon else googleSelectedIcon
                if (midIcon != null) {
                    newMarker.setIcon(midIcon)
                    delay(50)
                }
                if (finalIcon != null) {
                    newMarker.setIcon(finalIcon)
                }
            }
        }
        prevSelectedId = currentSelectedId
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    map.uiSettings.isZoomControlsEnabled = false
                    map.uiSettings.isCompassEnabled = true
                    map.uiSettings.isMyLocationButtonEnabled = false
                    map.mapType = GoogleMap.MAP_TYPE_NORMAL
                    map.setPadding(0, topPaddingPx, 0, bottomPaddingPx)

                    // Immediately center on valid initial position if available to prevent default (0,0) viewport
                    val initialLoc = cameraTarget ?: userLocation
                    if (initialLoc != null && isValidLocation(initialLoc)) {
                        val zoom = cameraZoom?.toFloat() ?: 14.5f
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(GoogleLatLng(initialLoc.lat, initialLoc.lng), zoom))
                        initialCameraPositioned = true
                    }

                    map.setOnCameraMoveStartedListener { reason ->
                        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                            onCameraMoved()
                        }
                    }

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

private fun createUserDotBitmap(): BitmapDescriptor? {
    return try {
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

        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (_: Exception) {
        null
    }
}

private fun createMechanicMarkerBitmap(
    isVerified: Boolean,
    isSelected: Boolean,
    scaleFactor: Float = 1.0f,
): BitmapDescriptor? {
    return try {
        val baseWidth = if (isSelected) 64f else 46f
        val baseHeight = if (isSelected) 78f else 56f
        val width = (baseWidth * scaleFactor).toInt().coerceAtLeast(1)
        val height = (baseHeight * scaleFactor).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val primaryColor = if (isSelected) {
            android.graphics.Color.parseColor("#FF5722") // Raahi Deep Coral
        } else if (isVerified) {
            android.graphics.Color.parseColor("#1B6BFF") // Raahi Verified Blue
        } else {
            android.graphics.Color.parseColor("#FF6B35") // Raahi Orange
        }

        val centerX = width / 2f
        val circleRadius = (if (isSelected) 24f else 17f) * scaleFactor
        val circleCenterY = circleRadius + ((if (isSelected) 5f else 3f) * scaleFactor)

        // 1. Soft ground shadow under bottom tip
        paint.color = android.graphics.Color.parseColor("#33000000")
        canvas.drawOval(
            centerX - ((if (isSelected) 12f else 8f) * scaleFactor),
            height - (7f * scaleFactor),
            centerX + ((if (isSelected) 12f else 8f) * scaleFactor),
            height - (1f * scaleFactor),
            paint
        )

        // 2. Pin shape path (Top circle + tapered tip)
        val tipY = height - (5f * scaleFactor)
        val path = android.graphics.Path()
        path.moveTo(centerX, tipY)
        path.quadTo(
            centerX - circleRadius * 0.95f,
            circleCenterY + circleRadius * 0.7f,
            centerX - circleRadius,
            circleCenterY
        )
        path.arcTo(
            centerX - circleRadius,
            circleCenterY - circleRadius,
            centerX + circleRadius,
            circleCenterY + circleRadius,
            180f,
            180f,
            false
        )
        path.quadTo(
            centerX + circleRadius * 0.95f,
            circleCenterY + circleRadius * 0.7f,
            centerX,
            tipY
        )
        path.close()

        // 3. Selection outer glowing aura
        if (isSelected) {
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#44FF6B35")
                style = Paint.Style.STROKE
                strokeWidth = 6f * scaleFactor
            }
            canvas.drawPath(path, glowPaint)
        }

        // 4. Clean white border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, borderPaint)

        // 5. Fill pin body
        val innerCircleRadius = circleRadius - ((if (isSelected) 3.5f else 2.5f) * scaleFactor)
        val innerTipY = tipY - ((if (isSelected) 4f else 3f) * scaleFactor)
        val innerPath = android.graphics.Path()
        innerPath.moveTo(centerX, innerTipY)
        innerPath.quadTo(
            centerX - innerCircleRadius * 0.95f,
            circleCenterY + innerCircleRadius * 0.7f,
            centerX - innerCircleRadius,
            circleCenterY
        )
        innerPath.arcTo(
            centerX - innerCircleRadius,
            circleCenterY - innerCircleRadius,
            centerX + innerCircleRadius,
            circleCenterY + innerCircleRadius,
            180f,
            180f,
            false
        )
        innerPath.quadTo(
            centerX + innerCircleRadius * 0.95f,
            circleCenterY + innerCircleRadius * 0.7f,
            centerX,
            innerTipY
        )
        innerPath.close()

        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(innerPath, paint)

        // 6. Inner white circle badge
        val badgeRadius = (if (isSelected) 12f else 8.5f) * scaleFactor
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(centerX, circleCenterY, badgeRadius, paint)

        // 7. Inner center pip
        paint.color = primaryColor
        canvas.drawCircle(centerX, circleCenterY, (if (isSelected) 6f else 4.2f) * scaleFactor, paint)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (_: Exception) {
        null
    }
}

private fun isValidLocation(loc: LatLng?): Boolean {
    return loc != null && isValidCoordinate(loc.lat, loc.lng)
}

private fun isValidCoordinate(lat: Double, lng: Double): Boolean {
    return lat.isFinite() && lng.isFinite() &&
        !(lat == 0.0 && lng == 0.0) &&
        lat in -90.0..90.0 && lng in -180.0..180.0
}

