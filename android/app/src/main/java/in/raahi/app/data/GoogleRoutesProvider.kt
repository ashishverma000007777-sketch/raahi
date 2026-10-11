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
)

sealed class GoogleRoutesStatus {
    data object Idle : GoogleRoutesStatus()
    data object Loading : GoogleRoutesStatus()
    data class Success(val route: GoogleRouteData) : GoogleRoutesStatus()
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
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Android-Package", packageName)
                .addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (code == 200) {
                val json = JSONObject(bodyString)
                val routes = json.optJSONArray("routes")
                if (routes == null || routes.length() == 0) {
                    return@withContext GoogleRoutesStatus.Unavailable("No route found between coordinates")
                }
                val routeObj = routes.getJSONObject(0)
                val distanceMeters = routeObj.optDouble("distanceMeters", 0.0)
                val durationStr = routeObj.optString("duration", "0s")
                val durationSec = durationStr.removeSuffix("s").toDoubleOrNull() ?: 0.0
                val encodedPolyline = routeObj.optJSONObject("polyline")?.optString("encodedPolyline").orEmpty()

                val points = if (encodedPolyline.isNotEmpty()) {
                    decodePolyline(encodedPolyline)
                } else {
                    emptyList()
                }

                if (points.isEmpty()) {
                    return@withContext GoogleRoutesStatus.Unavailable("Route polyline was empty")
                }

                val distanceKm = (distanceMeters / 1000.0 * 10.0).roundToInt() / 10.0
                val durationMinutes = (durationSec / 60.0).roundToInt().coerceAtLeast(1)
                val formattedEta = when {
                    durationMinutes < 60 -> "$durationMinutes min"
                    else -> "${durationMinutes / 60}h ${durationMinutes % 60}m"
                }

                GoogleRoutesStatus.Success(
                    GoogleRouteData(
                        points = points,
                        distanceMeters = distanceMeters,
                        distanceKm = distanceKm,
                        durationSeconds = durationSec,
                        durationMinutes = durationMinutes,
                        formattedEta = formattedEta,
                    )
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
