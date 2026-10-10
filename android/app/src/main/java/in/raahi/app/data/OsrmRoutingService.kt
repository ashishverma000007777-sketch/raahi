package `in`.raahi.app.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteResult(
    val points: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Double,
)

/**
 * Service to fetch driving route geometry and ETA from OSRM with intelligent throttling
 * to prevent excessive API requests on minor GPS fluctuations.
 */
@Singleton
class OsrmRoutingService @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val gson = Gson()
    private val client by lazy {
        okHttpClient.newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private var cachedStart: LatLng? = null
    private var cachedEnd: LatLng? = null
    private var cachedResult: RouteResult? = null
    private var lastFetchTimestamp: Long = 0L

    companion object {
        private const val MIN_FETCH_INTERVAL_MS = 6_000L
        private const val MIN_DISTANCE_DELTA_METERS = 40.0
    }

    suspend fun getRoute(start: LatLng, end: LatLng): RouteResult? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val prevStart = cachedStart
        val prevEnd = cachedEnd
        val prevResult = cachedResult

        // Throttling: If recent and endpoints haven't moved significantly, return cached route
        if (prevResult != null && prevStart != null && prevEnd != null) {
            val startDelta = haversineMeters(start.lat, start.lng, prevStart.lat, prevStart.lng)
            val endDelta = haversineMeters(end.lat, end.lng, prevEnd.lat, prevEnd.lng)
            val elapsed = now - lastFetchTimestamp
            if (elapsed < MIN_FETCH_INTERVAL_MS || (startDelta < MIN_DISTANCE_DELTA_METERS && endDelta < MIN_DISTANCE_DELTA_METERS)) {
                return@withContext prevResult
            }
        }

        try {
            val url = "https://router.project-osrm.org/route/v1/driving/${start.lng},${start.lat};${end.lng},${end.lat}?overview=full&geometries=geojson"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Raahi-Android-Client/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext prevResult

                val bodyStr = response.body?.string() ?: return@withContext prevResult
                val root = gson.fromJson(bodyStr, JsonObject::class.java)
                val code = root.get("code")?.asString
                if (code != "Ok") return@withContext prevResult

                val routes = root.getAsJsonArray("routes")
                if (routes == null || routes.size() == 0) return@withContext prevResult

                val firstRoute = routes.get(0).asJsonObject
                val distance = firstRoute.get("distance")?.asDouble ?: 0.0
                val duration = firstRoute.get("duration")?.asDouble ?: 0.0

                val geometry = firstRoute.getAsJsonObject("geometry")
                val coordinates = geometry?.getAsJsonArray("coordinates") ?: return@withContext prevResult

                val points = mutableListOf<LatLng>()
                for (i in 0 until coordinates.size()) {
                    val coord = coordinates.get(i).asJsonArray
                    val lng = coord.get(0).asDouble
                    val lat = coord.get(1).asDouble
                    points.add(LatLng(lat, lng))
                }

                val result = RouteResult(points = points, distanceMeters = distance, durationSeconds = duration)
                cachedStart = start
                cachedEnd = end
                cachedResult = result
                lastFetchTimestamp = now
                return@withContext result
            }
        } catch (e: Exception) {
            Log.w("OsrmRoutingService", "Route request failed: ${e.message}")
            return@withContext prevResult
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
