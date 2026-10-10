package `in`.raahi.app.ui.screens.jobs

import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import `in`.raahi.app.data.LatLng
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.geometry.LatLng as MlLatLng

private const val OSM_SOURCE_ID = "osm-raster-source"
private const val OSM_LAYER_ID = "osm-raster-layer"

private const val ROUTE_SOURCE_ID = "live-route-source"
private const val ROUTE_CASING_LAYER_ID = "live-route-casing-layer"
private const val ROUTE_LAYER_ID = "live-route-layer"

private const val CUSTOMER_SOURCE_ID = "customer-loc-source"
private const val CUSTOMER_PULSE_LAYER_ID = "customer-loc-pulse"
private const val CUSTOMER_LAYER_ID = "customer-loc-layer"

private const val HELPER_SOURCE_ID = "helper-loc-source"
private const val HELPER_PULSE_LAYER_ID = "helper-loc-pulse"
private const val HELPER_GLOW_LAYER_ID = "helper-loc-glow"
private const val HELPER_LAYER_ID = "helper-loc-layer"

private fun emptyStyleBuilder(): Style.Builder {
    val styleJson = """{ "version": 8, "sources": {}, "layers": [] }"""
    return Style.Builder().fromJson(styleJson)
}

/**
 * High-performance, production-ready MapLibre live job tracking navigation map.
 * Features:
 * - Real OpenStreetMap raster tiles
 * - Turn-by-turn navigation-quality route polyline with contrast casing
 * - Directional GPS beacon for helper and destination marker for customer
 * - Intelligent user gesture detection: frees camera when exploring and supports re-centering
 */
@Composable
fun LiveJobTrackingMap(
    modifier: Modifier = Modifier,
    customerLocation: LatLng,
    helperLocation: LatLng? = null,
    routePoints: List<LatLng> = emptyList(),
    isUserExploring: Boolean = false,
    onUserExploringChange: (Boolean) -> Unit = {},
    recenterTrigger: Int = 0,
    onMapReady: (MapLibreMap) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewRef = remember { arrayOfNulls<MapView>(1) }
    var activeMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val latestExploring by rememberUpdatedState(isUserExploring)

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            activeMap?.let { map ->
                adjustCamera(map, customerLocation, helperLocation)
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView<android.view.View>(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                try {
                    MapLibre.getInstance(context.applicationContext)
                    MapView(context).also { mv ->
                        mapViewRef[0] = mv
                        mv.onCreate(null)
                        mv.getMapAsync { map ->
                            activeMap = map
                            map.setStyle(emptyStyleBuilder()) { style ->
                                addOsmRasterLayer(style)
                                addRouteLayers(style, routePoints)
                                addCustomerMarkerLayer(style, customerLocation)
                                addHelperMarkerLayer(style, helperLocation)
                            }

                            // Initial camera positioning
                            adjustCamera(map, customerLocation, helperLocation)

                            // Detect user panning/zooming gestures so camera follow pauses gracefully
                            map.addOnCameraMoveStartedListener { reason ->
                                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                                    onUserExploringChange(true)
                                }
                            }

                            onMapReady(map)
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("LiveJobTrackingMap", "Map init error: ${t.message}", t)
                    android.widget.TextView(context).apply {
                        text = "Map loading: ${t.message ?: "Initializing"}"
                        setTextColor(AndroidColor.DKGRAY)
                        setPadding(16, 16, 16, 16)
                    }
                }
            },
            update = { view ->
                val mv = view as? MapView ?: return@AndroidView
                mv.getMapAsync { map ->
                    val style = map.style ?: return@getMapAsync

                    // 1. Update Customer location
                    (style.getSource(CUSTOMER_SOURCE_ID) as? GeoJsonSource)?.setGeoJson(
                        Feature.fromGeometry(Point.fromLngLat(customerLocation.lng, customerLocation.lat))
                    )

                    // 2. Update Helper location
                    (style.getSource(HELPER_SOURCE_ID) as? GeoJsonSource)?.let { source ->
                        if (helperLocation != null) {
                            source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(helperLocation.lng, helperLocation.lat)))
                        } else {
                            source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
                        }
                    }

                    // 3. Update Route line
                    (style.getSource(ROUTE_SOURCE_ID) as? GeoJsonSource)?.let { source ->
                        if (routePoints.size >= 2) {
                            val mlPoints = routePoints.map { Point.fromLngLat(it.lng, it.lat) }
                            source.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(mlPoints)))
                        } else {
                            source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
                        }
                    }

                    // Only auto-frame camera if user is not actively exploring the map
                    if (!latestExploring) {
                        adjustCamera(map, customerLocation, helperLocation)
                    }
                }
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        val mv = mapViewRef[0] ?: return@DisposableEffect onDispose {}
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            if (destroyed) return@LifecycleEventObserver
            runCatching {
                when (event) {
                    Lifecycle.Event.ON_START -> mv.onStart()
                    Lifecycle.Event.ON_RESUME -> mv.onResume()
                    Lifecycle.Event.ON_PAUSE -> mv.onPause()
                    Lifecycle.Event.ON_STOP -> mv.onStop()
                    Lifecycle.Event.ON_DESTROY -> {
                        destroyed = true
                        mv.onDestroy()
                    }
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) {
                destroyed = true
                runCatching { mv.onPause() }
                runCatching { mv.onStop() }
                runCatching { mv.onDestroy() }
            }
        }
    }
}

private fun adjustCamera(map: MapLibreMap, customer: LatLng, helper: LatLng?) {
    try {
        if (helper != null) {
            val bounds = LatLngBounds.Builder()
                .include(MlLatLng(customer.lat, customer.lng))
                .include(MlLatLng(helper.lat, helper.lng))
                .build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 130))
        } else {
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(MlLatLng(customer.lat, customer.lng))
                        .zoom(14.8)
                        .build()
                )
            )
        }
    } catch (_: Exception) {
        // Fallback if bounds cannot be computed with 0 delta
        map.cameraPosition = CameraPosition.Builder()
            .target(MlLatLng(customer.lat, customer.lng))
            .zoom(14.5)
            .build()
    }
}

private fun addOsmRasterLayer(style: Style) {
    val tileSet = TileSet(
        "2.1.0",
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    )
    tileSet.attribution = "© OpenStreetMap contributors"
    style.addSource(RasterSource(OSM_SOURCE_ID, tileSet, 256))
    style.addLayer(RasterLayer(OSM_LAYER_ID, OSM_SOURCE_ID))
}

private fun addRouteLayers(style: Style, points: List<LatLng>) {
    val initialCollection = if (points.size >= 2) {
        FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(LineString.fromLngLats(points.map { Point.fromLngLat(it.lng, it.lat) }))))
    } else {
        FeatureCollection.fromFeatures(emptyList())
    }
    style.addSource(GeoJsonSource(ROUTE_SOURCE_ID, initialCollection))

    // 1. Route Casing (dark border underneath for navigation clarity)
    val casingLayer = LineLayer(ROUTE_CASING_LAYER_ID, ROUTE_SOURCE_ID)
    casingLayer.setProperties(
        lineColor("#B91C1C"),
        lineWidth(8f),
        lineCap(Property.LINE_CAP_ROUND),
        lineJoin(Property.LINE_JOIN_ROUND),
        lineOpacity(0.90f)
    )
    style.addLayer(casingLayer)

    // 2. Main Navigation Polyline (Raahi automotive coral)
    val lineLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)
    lineLayer.setProperties(
        lineColor("#FF4B3A"),
        lineWidth(5f),
        lineCap(Property.LINE_CAP_ROUND),
        lineJoin(Property.LINE_JOIN_ROUND),
        lineOpacity(0.98f)
    )
    style.addLayer(lineLayer)
}

private fun addCustomerMarkerLayer(style: Style, loc: LatLng) {
    style.addSource(
        GeoJsonSource(CUSTOMER_SOURCE_ID, Feature.fromGeometry(Point.fromLngLat(loc.lng, loc.lat)))
    )

    // Outer soft pulse / destination beacon
    val pulseLayer = CircleLayer(CUSTOMER_PULSE_LAYER_ID, CUSTOMER_SOURCE_ID)
    pulseLayer.setProperties(
        circleRadius(18f),
        circleColor("#00CFFF"),
        circleOpacity(0.22f)
    )
    style.addLayer(pulseLayer)

    // Inner distinct destination point
    val circleLayer = CircleLayer(CUSTOMER_LAYER_ID, CUSTOMER_SOURCE_ID)
    circleLayer.setProperties(
        circleRadius(8f),
        circleColor("#0284C7"),
        circleStrokeWidth(2.5f),
        circleStrokeColor("#FFFFFF")
    )
    style.addLayer(circleLayer)
}

private fun addHelperMarkerLayer(style: Style, loc: LatLng?) {
    val initialCollection = if (loc != null) {
        FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(loc.lng, loc.lat))))
    } else {
        FeatureCollection.fromFeatures(emptyList())
    }
    style.addSource(GeoJsonSource(HELPER_SOURCE_ID, initialCollection))

    // Outer radar pulse
    val pulseLayer = CircleLayer(HELPER_PULSE_LAYER_ID, HELPER_SOURCE_ID)
    pulseLayer.setProperties(
        circleRadius(24f),
        circleColor("#FF4B3A"),
        circleOpacity(0.18f)
    )
    style.addLayer(pulseLayer)

    // Mid navigational halo
    val glowLayer = CircleLayer(HELPER_GLOW_LAYER_ID, HELPER_SOURCE_ID)
    glowLayer.setProperties(
        circleRadius(13f),
        circleColor("#FF4B3A"),
        circleOpacity(0.38f)
    )
    style.addLayer(glowLayer)

    // Inner vehicle navigation puck
    val markerLayer = CircleLayer(HELPER_LAYER_ID, HELPER_SOURCE_ID)
    markerLayer.setProperties(
        circleRadius(8.5f),
        circleColor("#FF4B3A"),
        circleStrokeWidth(2.5f),
        circleStrokeColor("#FFFFFF")
    )
    style.addLayer(markerLayer)
}
