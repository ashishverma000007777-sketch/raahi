package `in`.raahi.app.data

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class GoogleRouteData(
    val points: List<LatLng>,
    val distanceMeters: Double,
    val distanceKm: Double,
    val durationSeconds: Double,
    val durationMinutes: Int,
    val formattedEta: String,
    val description: String? = null,
    val tollCost: Double? = null,
    val tollAvailable: Boolean = false,
    val tollUnavailableReason: String? = null,
    val routeLabel: String? = null,
)

sealed class GoogleRoutesStatus {
    data object Idle : GoogleRoutesStatus()
    data object Loading : GoogleRoutesStatus()
    data class Success(
        val route: GoogleRouteData,
        val alternatives: List<GoogleRouteData> = emptyList()
    ) : GoogleRoutesStatus()
    data class Unavailable(val reason: String) : GoogleRoutesStatus()
}

class GoogleRoutesProvider(
    private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Dynamically resolved from strings.xml / Android context
    private val apiKey: String
        get() = try {
            val resId = context.resources.getIdentifier("google_maps_key", "string", context.packageName)
            if (resId != 0) context.getString(resId) else ""
        } catch (_: Exception) {
            ""
        }
    private val packageName: String
        get() = context.packageName

    suspend fun computeDrivingRoute(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double,
    ): GoogleRoutesStatus = withContext(Dispatchers.IO) {
        try {
            require(startLat.isFinite() && startLat in -90.0..90.0) { "Invalid start latitude" }
            require(destLat.isFinite() && destLat in -90.0..90.0) { "Invalid destination latitude" }
            require(startLng.isFinite() && startLng in -180.0..180.0) { "Invalid start longitude" }
            require(destLng.isFinite() && destLng in -180.0..180.0) { "Invalid destination longitude" }

            val url = "https://routes.googleapis.com/directions/v2:computeRoutes"
            val payload = JSONObject().apply {
                put("origin", JSONObject().apply {
                    put("location", JSONObject().apply {
                        put("latLng", JSONObject().apply {
                            put("latitude", startLat)
                            put("longitude", startLng)
                        })
                    })
                })
                put("destination", JSONObject().apply {
                    put("location", JSONObject().apply {
                        put("latLng", JSONObject().apply {
                            put("latitude", destLat)
                            put("longitude", destLng)
                        })
                    })
                })
                put("travelMode", "DRIVE")
                put("routingPreference", "TRAFFIC_AWARE")
                put("computeAlternativeRoutes", true)
                put("extraComputations", org.json.JSONArray().apply {
                    put("TOLLS")
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Android-Package", packageName)
                .addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.description,routes.travelAdvisory.tollInfo")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (code == 200) {
                val json = JSONObject(bodyString)
                val routesJson = json.optJSONArray("routes")
                if (routesJson == null || routesJson.length() == 0) {
                    return@withContext GoogleRoutesStatus.Unavailable("No route found between coordinates")
                }

                val parsedRoutes = mutableListOf<GoogleRouteData>()
                val maxRoutes = minOf(routesJson.length(), 3)
                for (i in 0 until maxRoutes) {
                    val routeObj = routesJson.getJSONObject(i)
                    val distanceMeters = routeObj.optDouble("distanceMeters", 0.0)
                    val durationStr = routeObj.optString("duration", "0s")
                    val durationSec = durationStr.removeSuffix("s").toDoubleOrNull() ?: 0.0
                    val description = routeObj.optString("description").takeIf { it.isNotBlank() }
                    val encodedPolyline = routeObj.optJSONObject("polyline")?.optString("encodedPolyline").orEmpty()

                    val points = if (encodedPolyline.isNotEmpty()) {
                        decodePolyline(encodedPolyline)
                    } else {
                        emptyList()
                    }

                    if (points.isEmpty()) continue

                    val distanceKm = (distanceMeters / 1000.0 * 10.0).roundToInt() / 10.0
                    val durationMinutes = (durationSec / 60.0).roundToInt().coerceAtLeast(1)
                    val formattedEta = when {
                        durationMinutes < 60 -> "$durationMinutes min"
                        else -> "${durationMinutes / 60}h ${durationMinutes % 60}m"
                    }

                    var tollCost: Double? = null
                    var tollAvailable = false
                    var tollUnavailableReason: String? = null

                    val travelAdvisory = routeObj.optJSONObject("travelAdvisory")
                    val tollInfo = travelAdvisory?.optJSONObject("tollInfo")
                    val estimatedPrice = tollInfo?.optJSONArray("estimatedPrice")
                    if (estimatedPrice != null && estimatedPrice.length() > 0) {
                        val priceObj = estimatedPrice.getJSONObject(0)
                        val units = priceObj.optString("units", "0").toDoubleOrNull() ?: 0.0
                        val nanos = priceObj.optDouble("nanos", 0.0)
                        val totalToll = units + (nanos / 1_000_000_000.0)
                        if (totalToll >= 0.0) {
                            tollCost = (totalToll * 100.0).roundToInt() / 100.0
                            tollAvailable = true
                        } else {
                            tollUnavailableReason = "Toll data not provided for this route"
                        }
                    } else {
                        tollUnavailableReason = "Toll data not provided for this route"
                    }

                    parsedRoutes.add(
                        GoogleRouteData(
                            points = points,
                            distanceMeters = distanceMeters,
                            distanceKm = distanceKm,
                            durationSeconds = durationSec,
                            durationMinutes = durationMinutes,
                            formattedEta = formattedEta,
                            description = description,
                            tollCost = tollCost,
                            tollAvailable = tollAvailable,
                            tollUnavailableReason = tollUnavailableReason,
                            routeLabel = null
                        )
                    )
                }

                if (parsedRoutes.isEmpty()) {
                    return@withContext GoogleRoutesStatus.Unavailable("Route polyline was empty")
                }

                val labeledRoutes = assignRouteLabels(parsedRoutes)

                GoogleRoutesStatus.Success(
                    route = labeledRoutes.first(),
                    alternatives = labeledRoutes
                )
            } else {
                val errorMsg = try {
                    val errorObj = JSONObject(bodyString).optJSONObject("error")
                    val msg = errorObj?.optString("message").orEmpty()
                    val reason = errorObj?.optJSONArray("details")?.optJSONObject(0)?.optString("reason").orEmpty()
                    if (reason.isNotEmpty()) "$reason: $msg" else msg.ifEmpty { "HTTP $code" }
                } catch (_: Exception) {
                    "HTTP $code: $bodyString"
                }
                Log.w("GoogleRoutesProvider", "Routes API returned $code: $errorMsg")
                GoogleRoutesStatus.Unavailable("Google Routes API: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e("GoogleRoutesProvider", "Failed to compute driving route", e)
            GoogleRoutesStatus.Unavailable("Google Routes error: ${e.message ?: "Network error"}")
        }
    }

    companion object {
        fun assignRouteLabels(routes: List<GoogleRouteData>): List<GoogleRouteData> {
            if (routes.isEmpty()) return emptyList()
            if (routes.size == 1) {
                return listOf(routes[0].copy(routeLabel = "Fastest Route"))
            }

            val minDuration = routes.minOf { it.durationSeconds }
            val minDistance = routes.minOf { it.distanceKm }

            return routes.mapIndexed { index, route ->
                val isFastest = route.durationSeconds == minDuration
                val isShortest = route.distanceKm == minDistance

                val label = when {
                    isFastest && isShortest -> "Fastest & Shortest"
                    isFastest -> "Fastest"
                    isShortest -> "Shortest Distance"
                    route.tollAvailable && route.tollCost == 0.0 -> "Toll-Free Route"
                    !route.description.isNullOrBlank() -> "Via ${route.description}"
                    else -> "Alternative Route ${index + 1}"
                }
                route.copy(routeLabel = label)
            }
        }

        fun decodePolyline(encoded: String): List<LatLng> {
            val poly = ArrayList<LatLng>()
            var index = 0
            val len = encoded.length
            var lat = 0
            var lng = 0

            while (index < len) {
                var b: Int
                var shift = 0
                var result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or ((b and 0x1f) shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
                lat += dlat

                shift = 0
                result = 0
                do {
                    b = encoded[index++].code - 63
                    result = result or ((b and 0x1f) shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
                lng += dlng

                val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
                poly.add(p)
            }
            return poly
        }
    }
}
