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
import org.maplibre.android.geometry.LatLng as MlLatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.android.style.sources.TileSet
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val OSM_SOURCE_ID = "osm-raster-source"
private const val OSM_LAYER_ID = "osm-raster-layer"

private const val USER_SOURCE_ID = "user-source"
private const val USER_PULSE_LAYER_ID = "user-pulse-layer"
private const val USER_LAYER_ID = "user-layer"

private const val MECHANICS_SOURCE_ID = "mechanics-source"
private const val MECHANIC_HIGHLIGHT_LAYER_ID = "mechanic-highlight-layer"
private const val MECHANICS_LAYER_ID = "mechanics-layer"

private const val SHOPS_SOURCE_ID = "osm-shops-source"
private const val SHOP_HIGHLIGHT_LAYER_ID = "shop-highlight-layer"
private const val SHOPS_LAYER_ID = "osm-shops-layer"

private const val MECHANIC_ID_PROPERTY = "mechanicUserId"
private const val AVAILABLE_PROPERTY = "available"

private fun emptyStyleBuilder(): Style.Builder {
    val styleJson = """{ "version": 8, "sources": {}, "layers": [] }"""
    return Style.Builder().fromJson(styleJson)
}

/**
 * Premium MapLibre map rendering OpenStreetMap raster tiles, high-visibility user location beacon,
 * verified Raahi mechanic pins with real availability styling and animated selection highlight rings.
 */
@Composable
fun MechanicsMapView(
    modifier: Modifier = Modifier,
    userLocation: LatLng,
    mechanics: List<MechanicDto>,
    shops: List<OsmMechanicShopDto> = emptyList(),
    selectedMechanicId: String? = null,
    selectedShopId: String? = null,
    onMechanicClick: (MechanicDto) -> Unit,
    onShopClick: (OsmMechanicShopDto) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewRef = remember { arrayOfNulls<MapView>(1) }
    val latestMechanics by rememberUpdatedState(mechanics)
    val latestShops by rememberUpdatedState(shops)

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
                            addUserLocationLayer(style, userLocation)
                            addMechanicsLayers(style, mechanics, selectedMechanicId)
                            addOsmShopsLayers(style, shops, selectedShopId)
                        }
                        map.cameraPosition = CameraPosition.Builder()
                            .target(MlLatLng(userLocation.lat, userLocation.lng))
                            .zoom(13.8)
                            .build()

                        map.addOnMapClickListener { point ->
                            val screenPoint = map.projection.toScreenLocation(point)
                            val touchBox = android.graphics.RectF(
                                screenPoint.x - 36f,
                                screenPoint.y - 36f,
                                screenPoint.x + 36f,
                                screenPoint.y + 36f
                            )
                            val features = map.queryRenderedFeatures(touchBox, MECHANICS_LAYER_ID)
                            val mechanicId = features.firstOrNull()?.getStringProperty(MECHANIC_ID_PROPERTY)
                            val shopFeatures = map.queryRenderedFeatures(touchBox, SHOPS_LAYER_ID)
                            if (shopFeatures.isNotEmpty()) {
                                val shopId = shopFeatures.firstOrNull()?.getStringProperty("osmShopId")
                                val shop = latestShops.firstOrNull { it.id == shopId }
                                if (shop != null) {
                                    onShopClick(shop)
                                    return@addOnMapClickListener true
                                }
                            }
                            val mechanic = latestMechanics.firstOrNull { it.userId == mechanicId }
                            if (mechanic != null) {
                                onMechanicClick(mechanic)
                                return@addOnMapClickListener true
                            }
                            false
                        }
                        onMapReady(map)
                    }
                }
            } catch (t: Throwable) {
                Log.e("MechanicsMapView", "Map init error: ${t.message}", t)
                android.widget.TextView(context).apply {
                    text = "Map loading: ${t.message ?: "Initializing"}"
                    setTextColor(android.graphics.Color.GRAY)
                    textSize = 13f
                    setPadding(16, 16, 16, 16)
                }
            }
        },
        update = { view ->
            val mv = view as? MapView ?: return@AndroidView
            mv.getMapAsync { map ->
                val style = map.style ?: return@getMapAsync
                (style.getSource(MECHANICS_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(mechanicsToFeatureCollection(mechanics, selectedMechanicId))
                (style.getSource(USER_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(userLocation.lng, userLocation.lat)))
                (style.getSource(SHOPS_SOURCE_ID) as? GeoJsonSource)
                    ?.setGeoJson(osmShopsToFeatureCollection(shops, selectedShopId))
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
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    )
    tileSet.attribution = "© OpenStreetMap contributors"
    style.addSource(RasterSource(OSM_SOURCE_ID, tileSet, 256))
    style.addLayer(RasterLayer(OSM_LAYER_ID, OSM_SOURCE_ID))
}

private fun addUserLocationLayer(style: Style, userLocation: LatLng) {
    style.addSource(
        GeoJsonSource(USER_SOURCE_ID, Feature.fromGeometry(Point.fromLngLat(userLocation.lng, userLocation.lat)))
    )

    // Outer subtle location pulse
    val pulseLayer = CircleLayer(USER_PULSE_LAYER_ID, USER_SOURCE_ID)
    pulseLayer.setProperties(
        circleRadius(20f),
        circleColor("#00CFFF"),
        circleOpacity(0.22f)
    )
    style.addLayer(pulseLayer)

    // Inner distinct location dot
    val userLayer = CircleLayer(USER_LAYER_ID, USER_SOURCE_ID)
    userLayer.setProperties(
        circleRadius(8.5f),
        circleColor("#0284C7"),
        circleStrokeWidth(2.5f),
        circleStrokeColor("#FFFFFF"),
    )
    style.addLayer(userLayer)
}

private fun addMechanicsLayers(style: Style, mechanics: List<MechanicDto>, selectedId: String?) {
    style.addSource(GeoJsonSource(MECHANICS_SOURCE_ID, mechanicsToFeatureCollection(mechanics, selectedId)))

    // 1. Selection Highlight Ring (appears around selected mechanic)
    val highlightLayer = CircleLayer(MECHANIC_HIGHLIGHT_LAYER_ID, MECHANICS_SOURCE_ID)
    highlightLayer.setProperties(
        circleRadius(24f),
        circleColor(
            Expression.match(
                Expression.get(AVAILABLE_PROPERTY),
                Expression.color(android.graphics.Color.parseColor("#EF4444")),
                Expression.stop(true, Expression.color(android.graphics.Color.parseColor("#10B981"))),
            )
        ),
        circleOpacity(
            Expression.match(
                Expression.get("isSelected"),
                Expression.literal(0.0f),
                Expression.stop(true, Expression.literal(0.35f))
            )
        )
    )
    style.addLayer(highlightLayer)

    // 2. Main Mechanic Marker
    val mechanicsLayer = CircleLayer(MECHANICS_LAYER_ID, MECHANICS_SOURCE_ID)
    mechanicsLayer.setProperties(
        circleRadius(
            Expression.match(
                Expression.get("isSelected"),
                Expression.literal(9.5f),
                Expression.stop(true, Expression.literal(13f))
            )
        ),
        // Green when available, red when unavailable
        circleColor(
            Expression.match(
                Expression.get(AVAILABLE_PROPERTY),
                Expression.color(android.graphics.Color.parseColor("#EF4444")),
                Expression.stop(true, Expression.color(android.graphics.Color.parseColor("#10B981"))),
            )
        ),
        circleStrokeWidth(
            Expression.match(
                Expression.get("isSelected"),
                Expression.literal(2f),
                Expression.stop(true, Expression.literal(3.5f))
            )
        ),
        circleStrokeColor(
            Expression.match(
                Expression.get("isSelected"),
                Expression.color(android.graphics.Color.parseColor("#FFFFFF")),
                Expression.stop(true, Expression.color(android.graphics.Color.parseColor("#FFB703")))
            )
        ),
    )
    style.addLayer(mechanicsLayer)
}

private fun addOsmShopsLayers(style: Style, shops: List<OsmMechanicShopDto>, selectedId: String?) {
    style.addSource(GeoJsonSource(SHOPS_SOURCE_ID, osmShopsToFeatureCollection(shops, selectedId)))

    // 1. Selection Highlight Ring for OSM shop
    val highlightLayer = CircleLayer(SHOP_HIGHLIGHT_LAYER_ID, SHOPS_SOURCE_ID)
    highlightLayer.setProperties(
        circleRadius(20f),
        circleColor("#F59E0B"),
        circleOpacity(
            Expression.match(
                Expression.get("isSelected"),
                Expression.literal(0.0f),
                Expression.stop(true, Expression.literal(0.35f))
            )
        )
    )
    style.addLayer(highlightLayer)

    // 2. Main OSM Shop Marker
    val layer = CircleLayer(SHOPS_LAYER_ID, SHOPS_SOURCE_ID)
    layer.setProperties(
        circleRadius(
            Expression.match(
                Expression.get("isSelected"),
                Expression.literal(7.5f),
                Expression.stop(true, Expression.literal(11f))
            )
        ),
        circleColor("#F59E0B"),
        circleStrokeWidth(
            Expression.match(
                Expression.get("isSelected"),
                Expression.literal(2f),
                Expression.stop(true, Expression.literal(3f))
            )
        ),
        circleStrokeColor(
            Expression.match(
                Expression.get("isSelected"),
                Expression.color(android.graphics.Color.parseColor("#FFFFFF")),
                Expression.stop(true, Expression.color(android.graphics.Color.parseColor("#FFB703")))
            )
        ),
    )
    style.addLayer(layer)
}

private fun osmShopsToFeatureCollection(shops: List<OsmMechanicShopDto>, selectedId: String?): FeatureCollection {
    val features = shops.mapNotNull { shop ->
        if (!shop.lat.isFinite() || !shop.lng.isFinite() ||
            shop.lat !in -90.0..90.0 || shop.lng !in -180.0..180.0
        ) return@mapNotNull null
        Feature.fromGeometry(Point.fromLngLat(shop.lng, shop.lat)).apply {
            addStringProperty("osmShopId", shop.id)
            addStringProperty("shopName", shop.name)
            addBooleanProperty("isSelected", shop.id == selectedId)
        }
    }
    return FeatureCollection.fromFeatures(features)
}

private fun mechanicsToFeatureCollection(mechanics: List<MechanicDto>, selectedId: String?): FeatureCollection {
    val features = mechanics.mapNotNull { m ->
        val lat = m.lat ?: return@mapNotNull null
        val lng = m.lng ?: return@mapNotNull null
        Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
            addStringProperty(MECHANIC_ID_PROPERTY, m.userId)
            addBooleanProperty(AVAILABLE_PROPERTY, m.isAvailable)
            addBooleanProperty("isSelected", m.userId == selectedId)
        }
    }
    return FeatureCollection.fromFeatures(features)
}
