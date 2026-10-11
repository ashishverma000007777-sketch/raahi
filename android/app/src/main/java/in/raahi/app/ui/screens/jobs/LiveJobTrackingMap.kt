package `in`.raahi.app.ui.screens.jobs

import android.graphics.Color as AndroidColor
import android.util.Log
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
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.PersonPinCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import `in`.raahi.app.ui.theme.*
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MapLatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.*

private const val OSM_SOURCE_ID = "live-osm-source"
private const val OSM_LAYER_ID = "live-osm-layer"
private const val CUSTOMER_SOURCE = "live-customer-source"
private const val CUSTOMER_PULSE_LAYER = "live-customer-pulse-layer"
private const val CUSTOMER_LAYER = "live-customer-layer"
private const val HELPER_SOURCE = "live-helper-source"
private const val HELPER_PULSE_LAYER = "live-helper-pulse-layer"
private const val HELPER_LAYER = "live-helper-layer"
private const val ROUTE_SOURCE = "live-route-source"
private const val ROUTE_LAYER = "live-route-layer"

private val EMPTY_FEATURES = FeatureCollection.fromFeatures(emptyList<Feature>())

@Composable
fun LiveJobTrackingMap(
    customerLat: Double,
    customerLng: Double,
    helperLocation: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
    helperName: String? = null,
    jobStatus: String = "MATCHED",
    isHelperViewer: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapRef = remember { arrayOfNulls<MapView>(1) }
    val mapInstance = remember { arrayOfNulls<MapLibreMap>(1) }
    val latestHelper by rememberUpdatedState(helperLocation)

    var liveDistanceKm by remember { mutableStateOf<Double?>(null) }
    var liveDurationMin by remember { mutableStateOf<Int?>(null) }
    var routeGeometry by remember { mutableStateOf<FeatureCollection?>(null) }
    var isRoutingLoading by remember { mutableStateOf(false) }

    val validCustomer = customerLat.isFinite() && customerLng.isFinite() &&
        customerLat in -90.0..90.0 && customerLng in -180.0..180.0

    val validHelper = helperLocation?.let { (lat, lng) ->
        lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0
    } == true

    // Fetch road route and update distance / ETA when coordinates change
    LaunchedEffect(customerLat, customerLng, helperLocation) {
        if (validCustomer && validHelper && helperLocation != null) {
            val (hLat, hLng) = helperLocation
            val straightDist = haversineKm(hLat, hLng, customerLat, customerLng)

            isRoutingLoading = true
            val routeResult = runCatching {
                fetchOsrmRoute(startLat = hLat, startLng = hLng, endLat = customerLat, endLng = customerLng)
            }.getOrNull()?.getOrNull()

            isRoutingLoading = false
            if (routeResult != null) {
                routeGeometry = routeResult.geometry
                liveDistanceKm = routeResult.distanceMeters / 1000.0
                liveDurationMin = ceil(routeResult.durationSeconds / 60.0).toInt().coerceAtLeast(1)
            } else {
                // Fallback straight-line
                val startPt = Point.fromLngLat(hLng, hLat)
                val endPt = Point.fromLngLat(customerLng, customerLat)
                routeGeometry = FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(listOf(startPt, endPt))))
                liveDistanceKm = straightDist
                liveDurationMin = ceil((straightDist / 35.0) * 60.0).toInt().coerceAtLeast(1)
            }
        } else {
            routeGeometry = null
            liveDistanceKm = null
            liveDurationMin = null
        }
    }

    if (!validCustomer) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, RaahiBorderSoft, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("Breakdown location coordinates unavailable", color = RaahiTextDim, fontSize = 12.sp)
        }
        return
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, RaahiBorderSoft, RoundedCornerShape(18.dp))
            .shadow(4.dp, RoundedCornerShape(18.dp))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapLibre.getInstance(context.applicationContext)
                MapView(context).also { mapView ->
                    mapRef[0] = mapView
                    mapView.onCreate(null)
                    mapView.getMapAsync { map ->
                        mapInstance[0] = map
                        map.setStyle(Style.Builder().fromJson("""{"version":8,"sources":{},"layers":[]}""")) { style ->
                            // 1. Base OSM raster tiles
                            addOsmRasterLayer(style)

                            // 2. Road route line layer
                            style.addSource(GeoJsonSource(ROUTE_SOURCE, routeGeometry ?: EMPTY_FEATURES))
                            val lineLayer = LineLayer(ROUTE_LAYER, ROUTE_SOURCE)
                            lineLayer.setProperties(
                                lineColor("#FF6B35"),
                                lineWidth(4.5f),
                                lineOpacity(0.90f),
                                lineCap("round"),
                                lineJoin("round")
                            )
                            style.addLayer(lineLayer)

                            // 3. Customer breakdown marker (Red/Coral pulse + center pin)
                            val customerPoint = Point.fromLngLat(customerLng, customerLat)
                            style.addSource(GeoJsonSource(CUSTOMER_SOURCE, Feature.fromGeometry(customerPoint)))

                            val custPulse = CircleLayer(CUSTOMER_PULSE_LAYER, CUSTOMER_SOURCE)
                            custPulse.setProperties(
                                circleRadius(18f),
                                circleColor("#FF3D5A"),
                                circleOpacity(0.20f),
                                circleStrokeWidth(1.5f),
                                circleStrokeColor("#FF3D5A"),
                                circleStrokeOpacity(0.40f)
                            )
                            style.addLayer(custPulse)

                            val custPin = CircleLayer(CUSTOMER_LAYER, CUSTOMER_SOURCE)
                            custPin.setProperties(
                                circleRadius(8.5f),
                                circleColor("#FF3D5A"),
                                circleStrokeWidth(3f),
                                circleStrokeColor("#FFFFFF")
                            )
                            style.addLayer(custPin)

                            // 4. Helper vehicle marker (Green pulse + vehicle pin)
                            style.addSource(GeoJsonSource(HELPER_SOURCE, EMPTY_FEATURES))

                            val helperPulse = CircleLayer(HELPER_PULSE_LAYER, HELPER_SOURCE)
                            helperPulse.setProperties(
                                circleRadius(20f),
                                circleColor("#22B573"),
                                circleOpacity(0.25f),
                                circleStrokeWidth(2f),
                                circleStrokeColor("#22B573"),
                                circleStrokeOpacity(0.50f)
                            )
                            style.addLayer(helperPulse)

                            val helperPin = CircleLayer(HELPER_LAYER, HELPER_SOURCE)
                            helperPin.setProperties(
                                circleRadius(9.5f),
                                circleColor("#22B573"),
                                circleStrokeWidth(3f),
                                circleStrokeColor("#FFFFFF")
                            )
                            style.addLayer(helperPin)

                            // Initial camera framing
                            fitMapBounds(map, customerLat, customerLng, latestHelper)
                        }
                    }
                }
            },
            update = { view ->
                val mapView = view as? MapView ?: return@AndroidView
                mapView.getMapAsync { map ->
                    val style = map.style ?: return@getMapAsync

                    // Update live helper pin
                    val helperSource = style.getSource(HELPER_SOURCE) as? GeoJsonSource
                    if (validHelper && latestHelper != null) {
                        val (hLat, hLng) = latestHelper!!
                        helperSource?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(hLng, hLat)))
                    } else {
                        helperSource?.setGeoJson(EMPTY_FEATURES)
                    }

                    // Update route geometry
                    val lineSource = style.getSource(ROUTE_SOURCE) as? GeoJsonSource
                    lineSource?.setGeoJson(routeGeometry ?: EMPTY_FEATURES)
                }
            }
        )

        // Top Status Header Overlay
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.94f))
                    .border(1.dp, RaahiBorderSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (validHelper) RaahiGreen else RaahiAmber)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            when {
                                !validHelper -> "Waiting for helper GPS…"
                                jobStatus == "ARRIVED" -> "Helper has arrived at scene"
                                jobStatus == "IN_PROGRESS" -> "Repair work in progress"
                                else -> if (isHelperViewer) "Navigating to customer" else "Helper en route (${helperName ?: "Assigned"})"
                            },
                            color = RaahiText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (validHelper && liveDistanceKm != null && liveDurationMin != null) {
                            Text(
                                "${"%.1f".format(liveDistanceKm)} km away · ETA ~ $liveDurationMin mins",
                                color = RaahiOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else if (!validHelper) {
                            Text("Real-time WebSocket tracking active", color = RaahiTextDim, fontSize = 10.sp)
                        }
                    }
                }

                if (isRoutingLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RaahiOrange, strokeWidth = 2.dp)
                }
            }
        }

        // Bottom Map Action Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Re-frame / Fit both button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, RaahiBorderSoft, CircleShape)
                    .clickable {
                        mapInstance[0]?.let { fitMapBounds(it, customerLat, customerLng, latestHelper) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.CenterFocusStrong, contentDescription = "Fit bounds", tint = RaahiText, modifier = Modifier.size(18.dp))
            }

            // Focus on breakdown location
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, RaahiBorderSoft, CircleShape)
                    .clickable {
                        mapInstance[0]?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(MapLatLng(customerLat, customerLng), 15.5),
                            600
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PersonPinCircle, contentDescription = "Breakdown spot", tint = RaahiRed, modifier = Modifier.size(18.dp))
            }

            // Focus on helper if available
            if (validHelper && latestHelper != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(RaahiGreen)
                        .clickable {
                            val (hLat, hLng) = latestHelper!!
                            mapInstance[0]?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(MapLatLng(hLat, hLng), 16.0),
                                600
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Navigation, contentDescription = "Focus Helper", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val mapView = mapRef[0] ?: return@DisposableEffect onDispose {}
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            if (destroyed) return@LifecycleEventObserver
            runCatching {
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> {
                        destroyed = true
                        mapView.onDestroy()
                    }
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) {
                destroyed = true
                runCatching { mapView.onPause() }
                runCatching { mapView.onStop() }
                runCatching { mapView.onDestroy() }
            }
            mapRef[0] = null
            mapInstance[0] = null
        }
    }
}

private fun addOsmRasterLayer(style: Style) {
    val tileSet = TileSet(
        "2.1.0",
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
    )
    tileSet.attribution = "© OpenStreetMap contributors"
    style.addSource(RasterSource(OSM_SOURCE_ID, tileSet, 256))
    style.addLayer(RasterLayer(OSM_LAYER_ID, OSM_SOURCE_ID))
}

private fun fitMapBounds(
    map: MapLibreMap,
    customerLat: Double,
    customerLng: Double,
    helper: Pair<Double, Double>?,
) {
    if (helper != null && helper.first.isFinite() && helper.second.isFinite()) {
        val (hLat, hLng) = helper
        val bounds = LatLngBounds.Builder()
            .include(MapLatLng(customerLat, customerLng))
            .include(MapLatLng(hLat, hLng))
            .build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 700)
    } else {
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(MapLatLng(customerLat, customerLng), 14.5),
            700
        )
    }
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
