package `in`.raahi.app.ui.screens.mechanics
import android.util.Log

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import `in`.raahi.app.data.LatLng
import `in`.raahi.app.network.MechanicDto
import `in`.raahi.app.network.OsmMechanicShopDto
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
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
private const val USER_LAYER_ID = "user-layer"
private const val MECHANICS_SOURCE_ID = "mechanics-source"
private const val MECHANICS_LAYER_ID = "mechanics-layer"
private const val SHOPS_SOURCE_ID = "osm-shops-source"
private const val SHOPS_LAYER_ID = "osm-shops-layer"
private const val MECHANIC_ID_PROPERTY = "mechanicUserId"
private const val AVAILABLE_PROPERTY = "available"

/**
 * Raw XYZ raster tiles from tile.openstreetmap.org — same tile source the Flutter reference
 * used (flutter_map's TileLayer). No vector style / API key needed, which keeps this fully
 * within "MapLibre + OpenStreetMap" as specified rather than pulling in a hosted vector-style
 * provider that would need its own API key.
 */
private fun emptyStyleBuilder(): Style.Builder {
    val styleJson = """{ "version": 8, "sources": {}, "layers": [] }"""
    return Style.Builder().fromJson(styleJson)
}

@Composable
fun MechanicsMapView(
    modifier: Modifier = Modifier,
    userLocation: LatLng,
    mechanics: List<MechanicDto>,
    shops: List<OsmMechanicShopDto> = emptyList(),
    onMechanicClick: (MechanicDto) -> Unit,
    onMapReady: (MapLibreMap) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewRef = remember { arrayOfNulls<MapView>(1) }
    // Keep the latest list for the click listener (the listener is registered once).
    val latestMechanics by rememberUpdatedState(mechanics)
    val latestShops by rememberUpdatedState(shops)

    AndroidView<android.view.View>(
        modifier = modifier,
        factory = { context ->
          // Map creation can throw on devices without usable GL / native lib problems. A failed
          // map must never take the whole app down: fall back to an empty view and keep the
          // list UI below it working.
          try {
            MapLibre.getInstance(context.applicationContext)
            MapView(context).also { mv ->
                mapViewRef[0] = mv
                mv.onCreate(null)
                Log.d("RAAHI_MAP_DEBUG", "MapView onCreate")
                var frameCount = 0
                mv.addOnWillStartLoadingMapListener { Log.d("RAAHI_MAP_DEBUG", "WILL_START_LOADING_MAP") }
                mv.addOnDidFinishLoadingMapListener { Log.d("RAAHI_MAP_DEBUG", "DID_FINISH_LOADING_MAP") }
                mv.addOnDidFailLoadingMapListener { error -> Log.e("RAAHI_MAP_DEBUG", "DID_FAIL_LOADING_MAP: $error") }
                mv.addOnDidFinishLoadingStyleListener { Log.d("RAAHI_MAP_DEBUG", "DID_FINISH_LOADING_STYLE") }
                mv.addOnSourceChangedListener { source -> Log.d("RAAHI_MAP_DEBUG", "SOURCE_CHANGED: $source") }
                mv.addOnTileActionListener { a, b, c, d, e, f, g -> Log.d("RAAHI_MAP_DEBUG", "TILE_ACTION: $a $b $c $d $e $f $g") }
                mv.addOnShaderCompileFailedListener { a, b, c -> Log.e("RAAHI_MAP_DEBUG", "SHADER_COMPILE_FAILED: $a $b $c") }
                mv.addOnDidFinishRenderingFrameListener { a, b, c ->
                    frameCount++
                    if (frameCount <= 3 || frameCount % 60 == 0) Log.d("RAAHI_MAP_DEBUG", "RENDER_FRAME #$frameCount fully=$a p1=$b p2=$c")
                }
                Log.d("RAAHI_MAP_DEBUG", "MapView listeners registered")
                mv.getMapAsync { map ->
                    map.setStyle(emptyStyleBuilder()) { style ->
                        addOsmRasterLayer(style)
                        addUserLocationLayer(style, userLocation)
                        addMechanicsLayer(style, mechanics)
                        addOsmShopsLayer(style, shops)
                    }
                    map.cameraPosition = CameraPosition.Builder()
                        .target(MlLatLng(userLocation.lat, userLocation.lng))
                        .zoom(13.5)
                        .build()
                    map.addOnMapClickListener { point ->
                        val screenPoint = map.projection.toScreenLocation(point)
                        val features = map.queryRenderedFeatures(screenPoint, MECHANICS_LAYER_ID)
                        val mechanicId = features.firstOrNull()?.getStringProperty(MECHANIC_ID_PROPERTY)
                        val shopFeatures = map.queryRenderedFeatures(screenPoint, SHOPS_LAYER_ID)
                        if (shopFeatures.isNotEmpty()) return@addOnMapClickListener true
                        val mechanic = latestMechanics.firstOrNull { it.userId == mechanicId }
                        if (mechanic != null) onMechanicClick(mechanic)
                        mechanic != null
                    }
                    onMapReady(map)
                }
            }
          } catch (t: Throwable) {
            Log.e("RAAHI_MAP_DEBUG", "MAP_INIT_FAILED: ${t.javaClass.name}: ${t.message}", t)
            android.widget.TextView(context).apply {
                text = "Map failed to initialize: ${t.javaClass.simpleName}\n${t.message ?: "No details"}"
                setTextColor(android.graphics.Color.RED)
                textSize = 13f
                setPadding(16, 16, 16, 16)
            }
          }
        },
        update = { view ->
            val mv = view as? MapView ?: return@AndroidView
            // Re-applied whenever the mechanics list or the user's location fix changes —
            // MapLibre requires re-setting the GeoJSON data on the already-loaded style
            // rather than rebuilding the whole style from scratch on every recomposition.
            mv.getMapAsync { map ->
                val style = map.style ?: return@getMapAsync
                (style.getSource(MECHANICS_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(mechanicsToFeatureCollection(mechanics))
                (style.getSource(USER_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(userLocation.lng, userLocation.lat)))
                (style.getSource(SHOPS_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(osmShopsToFeatureCollection(shops))
            }
        },
    )

    DisposableEffect(lifecycleOwner) {
        val mv = mapViewRef[0] ?: return@DisposableEffect onDispose {}
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            if (destroyed) return@LifecycleEventObserver
        Log.d("RAAHI_MAP_DEBUG", "LIFECYCLE_EVENT: $event")
            runCatching {
                when (event) {
                    Lifecycle.Event.ON_START -> mv.onStart()
                    Lifecycle.Event.ON_RESUME -> mv.onResume()
                    Lifecycle.Event.ON_PAUSE -> mv.onPause()
                    Lifecycle.Event.ON_STOP -> mv.onStop()
                    Lifecycle.Event.ON_DESTROY -> { destroyed = true; mv.onDestroy() }
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Screen left composition (back / navigate away) while the Activity is still alive:
            // the GL surface must still be torn down, otherwise reopening the screen leaks and
            // can crash the native renderer.
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
    tileSet.attribution = "© OpenStreetMap contributors, © CARTO"
    style.addSource(RasterSource(OSM_SOURCE_ID, tileSet, 256))
    style.addLayer(RasterLayer(OSM_LAYER_ID, OSM_SOURCE_ID))
}

private fun addUserLocationLayer(style: Style, userLocation: LatLng) {
    style.addSource(
        GeoJsonSource(USER_SOURCE_ID, Feature.fromGeometry(Point.fromLngLat(userLocation.lng, userLocation.lat)))
    )
    val userLayer = CircleLayer(USER_LAYER_ID, USER_SOURCE_ID)
    // setProperties(...), not a fluent withProperties(...) — the latter isn't part of the
    // base Layer API on org.maplibre.android.style.layers.Layer (verified by re-checking
    // against the only Layer method this codebase can confirm elsewhere: constructor +
    // setProperties is the documented pattern; withProperties was a genuine, unconfirmed
    // guess caught during the final audit).
    userLayer.setProperties(
        circleRadius(8f),
        circleColor("#00CFFF"),
        circleStrokeWidth(2.5f),
        circleStrokeColor("#FFFFFF"),
    )
    style.addLayer(userLayer)
}

private fun addMechanicsLayer(style: Style, mechanics: List<MechanicDto>) {
    style.addSource(GeoJsonSource(MECHANICS_SOURCE_ID, mechanicsToFeatureCollection(mechanics)))
    val mechanicsLayer = CircleLayer(MECHANICS_LAYER_ID, MECHANICS_SOURCE_ID)
    mechanicsLayer.setProperties(
        circleRadius(9f),
        // Green when available, red when not — same visual language as the Flutter
        // reference's green/red pins, via a match expression on the per-feature
        // "available" property instead of two separate layers.
        circleColor(
            Expression.match(
                Expression.get(AVAILABLE_PROPERTY),
                Expression.color(android.graphics.Color.parseColor("#FF3D5A")), // default: not available
                Expression.stop(true, Expression.color(android.graphics.Color.parseColor("#00E676"))), // available
            )
        ),
        circleStrokeWidth(2f),
        circleStrokeColor("#FFFFFF"),
    )
    style.addLayer(mechanicsLayer)
}

private fun addOsmShopsLayer(style: Style, shops: List<OsmMechanicShopDto>) {
    style.addSource(GeoJsonSource(SHOPS_SOURCE_ID, osmShopsToFeatureCollection(shops)))
    val layer = CircleLayer(SHOPS_LAYER_ID, SHOPS_SOURCE_ID)
    layer.setProperties(
        circleRadius(7f),
        circleColor("#F59E0B"),
        circleStrokeWidth(2f),
        circleStrokeColor("#FFFFFF"),
    )
    style.addLayer(layer)
}

private fun osmShopsToFeatureCollection(shops: List<OsmMechanicShopDto>): FeatureCollection {
    val features = shops.mapNotNull { shop ->
        if (!shop.lat.isFinite() || !shop.lng.isFinite() ||
            shop.lat !in -90.0..90.0 || shop.lng !in -180.0..180.0
        ) return@mapNotNull null
        Feature.fromGeometry(Point.fromLngLat(shop.lng, shop.lat)).apply {
            addStringProperty("osmShopId", shop.id)
            addStringProperty("shopName", shop.name)
        }
    }
    return FeatureCollection.fromFeatures(features)
}

private fun mechanicsToFeatureCollection(mechanics: List<MechanicDto>): FeatureCollection {
    val features = mechanics.mapNotNull { m ->
        val lat = m.lat ?: return@mapNotNull null
        val lng = m.lng ?: return@mapNotNull null
        Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
            addStringProperty(MECHANIC_ID_PROPERTY, m.userId)
            addBooleanProperty(AVAILABLE_PROPERTY, m.isAvailable)
        }
    }
    return FeatureCollection.fromFeatures(features)
}
