package `in`.raahi.app.ui.screens.mechanics

import android.util.Log
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
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.android.geometry.LatLng as MlLatLng

private const val OSM_SOURCE_ID = "osm-raster-source"
private const val OSM_LAYER_ID = "osm-raster-layer"
private const val USER_SOURCE_ID = "user-source"
private const val USER_PULSE_LAYER_ID = "user-pulse-layer"
private const val USER_LAYER_ID = "user-layer"
private const val ITEMS_SOURCE_ID = "items-source"
private const val ITEMS_LAYER_ID = "items-layer"
private const val SELECTED_SOURCE_ID = "selected-item-source"
private const val SELECTED_LAYER_ID = "selected-item-layer"

private const val ITEM_ID_PROP = "itemId"
private const val SOURCE_PROP = "source"
private const val AVAILABLE_PROP = "available"

private fun emptyStyleBuilder(): Style.Builder {
    val styleJson = """{ "version": 8, "sources": {}, "layers": [] }"""
    return Style.Builder().fromJson(styleJson)
}

@Composable
fun MechanicsMapView(
    modifier: Modifier = Modifier,
    userLocation: LatLng,
    items: List<UnifiedMechanicItem>,
    selectedItem: UnifiedMechanicItem? = null,
    cameraTarget: LatLng? = null,
    cameraZoom: Double? = null,
    onCameraMoved: () -> Unit = {},
    onItemClick: (UnifiedMechanicItem) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewRef = remember { arrayOfNulls<MapView>(1) }
    val latestItems by rememberUpdatedState(items)
    var isMapFullyReady by remember { mutableStateOf(false) }

    AndroidView<android.view.View>(
        modifier = modifier,
        factory = { context ->
            try {
                MapLibre.getInstance(context.applicationContext)
                MapView(context).also { mv ->
                    mapViewRef[0] = mv
                    mv.onCreate(null)
                    mv.getMapAsync { map ->
                        map.setStyle(emptyStyleBuilder()) { style ->
                            addOsmRasterLayer(style)
                            addUserLocationLayers(style, userLocation)
                            addUnifiedItemsLayers(style, items)
                            addSelectedItemLayer(style, selectedItem)
                            isMapFullyReady = true
                        }

                        // Initialize camera strictly to user location with comfortable zoom
                        map.cameraPosition = CameraPosition.Builder()
                            .target(MlLatLng(userLocation.lat, userLocation.lng))
                            .zoom(cameraZoom ?: 14.0)
                            .build()

                        map.addOnMapClickListener { point ->
                            val screenPoint = map.projection.toScreenLocation(point)
                            val features = map.queryRenderedFeatures(screenPoint, ITEMS_LAYER_ID)
                            val clickedId = features.firstOrNull()?.getStringProperty(ITEM_ID_PROP)
                            if (clickedId != null) {
                                val match = latestItems.firstOrNull { it.id == clickedId }
                                if (match != null) {
                                    onItemClick(match)
                                    return@addOnMapClickListener true
                                }
                            }
                            false
                        }
                        onMapReady(map)
                    }
                }
            } catch (t: Throwable) {
                Log.e("MechanicsMapView", "MapLibre initialization failed: ${t.message}", t)
                android.widget.TextView(context).apply {
                    text = "Map preview unavailable: ${t.localizedMessage ?: "Renderer issue"}"
                    setTextColor(android.graphics.Color.DKGRAY)
                    textSize = 13f
                    setPadding(24, 24, 24, 24)
                }
            }
        },
        update = { view ->
            val mv = view as? MapView ?: return@AndroidView
            mv.getMapAsync { map ->
                val style = map.style ?: return@getMapAsync

                // Update dynamic GeoJSON data without resetting base style
                (style.getSource(USER_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(userLocation.lng, userLocation.lat)))

                (style.getSource(ITEMS_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(itemsToFeatureCollection(items))

                (style.getSource(SELECTED_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(selectedItemToFeatureCollection(selectedItem))

                // Smooth camera animation when cameraTarget is provided
                if (cameraTarget != null) {
                    val targetPos = MlLatLng(cameraTarget.lat, cameraTarget.lng)
                    val zoom = cameraZoom ?: map.cameraPosition.zoom.coerceAtLeast(14.0)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(targetPos, zoom),
                        700,
                        object : MapLibreMap.CancelableCallback {
                            override fun onFinish() { onCameraMoved() }
                            override fun onCancel() { onCameraMoved() }
                        }
                    )
                }
            }
        },
    )

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

private fun addUserLocationLayers(style: Style, userLocation: LatLng) {
    style.addSource(
        GeoJsonSource(USER_SOURCE_ID, Feature.fromGeometry(Point.fromLngLat(userLocation.lng, userLocation.lat)))
    )

    // Outer subtle radar pulse ring
    val pulseLayer = CircleLayer(USER_PULSE_LAYER_ID, USER_SOURCE_ID)
    pulseLayer.setProperties(
        circleRadius(18f),
        circleColor("#00CFFF"),
        circleOpacity(0.20f),
        circleStrokeWidth(1.5f),
        circleStrokeColor("#00CFFF"),
        circleStrokeOpacity(0.40f)
    )
    style.addLayer(pulseLayer)

    // Inner bright user dot
    val userLayer = CircleLayer(USER_LAYER_ID, USER_SOURCE_ID)
    userLayer.setProperties(
        circleRadius(8.5f),
        circleColor("#00CFFF"),
        circleStrokeWidth(3f),
        circleStrokeColor("#FFFFFF"),
    )
    style.addLayer(userLayer)
}

private fun addUnifiedItemsLayers(style: Style, items: List<UnifiedMechanicItem>) {
    style.addSource(GeoJsonSource(ITEMS_SOURCE_ID, itemsToFeatureCollection(items)))
    val layer = CircleLayer(ITEMS_LAYER_ID, ITEMS_SOURCE_ID)

    // Distinct colors for sources:
    // Verified Raahi -> Green (#22B573) / Red (#FF3D5A)
    // Community OSM -> Warm Amber (#FFA45B)
    // Google Places -> Google Blue (#4285F4)
    layer.setProperties(
        circleRadius(9.5f),
        circleColor(
            Expression.match(
                Expression.get(SOURCE_PROP),
                Expression.color(android.graphics.Color.parseColor("#FFA45B")), // fallback
                Expression.stop(
                    "VERIFIED",
                    Expression.match(
                        Expression.get(AVAILABLE_PROP),
                        Expression.color(android.graphics.Color.parseColor("#FF3D5A")),
                        Expression.stop(true, Expression.color(android.graphics.Color.parseColor("#22B573")))
                    )
                ),
                Expression.stop("OSM_COMMUNITY", Expression.color(android.graphics.Color.parseColor("#FFA45B"))),
                Expression.stop("GOOGLE_PLACES", Expression.color(android.graphics.Color.parseColor("#4285F4")))
            )
        ),
        circleStrokeWidth(2.5f),
        circleStrokeColor("#FFFFFF")
    )
    style.addLayer(layer)
}

private fun addSelectedItemLayer(style: Style, selected: UnifiedMechanicItem?) {
    style.addSource(GeoJsonSource(SELECTED_SOURCE_ID, selectedItemToFeatureCollection(selected)))
    val selectedLayer = CircleLayer(SELECTED_LAYER_ID, SELECTED_SOURCE_ID)
    selectedLayer.setProperties(
        circleRadius(16f),
        circleColor("#FF6B35"),
        circleOpacity(0.25f),
        circleStrokeWidth(3f),
        circleStrokeColor("#FF6B35")
    )
    style.addLayer(selectedLayer)
}

private fun itemsToFeatureCollection(items: List<UnifiedMechanicItem>): FeatureCollection {
    val features = items.mapNotNull { item ->
        if (!item.lat.isFinite() || !item.lng.isFinite() ||
            item.lat !in -90.0..90.0 || item.lng !in -180.0..180.0
        ) return@mapNotNull null

        Feature.fromGeometry(Point.fromLngLat(item.lng, item.lat)).apply {
            addStringProperty(ITEM_ID_PROP, item.id)
            addStringProperty(SOURCE_PROP, item.source.name)
            addBooleanProperty(AVAILABLE_PROP, item.isAvailable)
        }
    }
    return FeatureCollection.fromFeatures(features)
}

private fun selectedItemToFeatureCollection(selected: UnifiedMechanicItem?): FeatureCollection {
    if (selected == null || !selected.lat.isFinite() || !selected.lng.isFinite()) {
        return FeatureCollection.fromFeatures(emptyList())
    }
    val feature = Feature.fromGeometry(Point.fromLngLat(selected.lng, selected.lat)).apply {
        addStringProperty(ITEM_ID_PROP, selected.id)
    }
    return FeatureCollection.fromFeatures(listOf(feature))
}
