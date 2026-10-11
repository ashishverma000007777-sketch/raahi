package `in`.raahi.app.ui.screens.jobs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

// OSRM_LIVE_ROUTE_PATCH
internal data class OsrmRouteResult(
    val geometry: FeatureCollection,
    val distanceMeters: Double,
    val durationSeconds: Double,
)

internal suspend fun fetchOsrmRoute(
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double,
): Result<OsrmRouteResult> = withContext(Dispatchers.IO) {
    runCatching {
        require(startLat.isFinite() && startLat in -90.0..90.0) { "Invalid start latitude" }
        require(endLat.isFinite() && endLat in -90.0..90.0) { "Invalid destination latitude" }
        require(startLng.isFinite() && startLng in -180.0..180.0) { "Invalid start longitude" }
        require(endLng.isFinite() && endLng in -180.0..180.0) { "Invalid destination longitude" }

        val endpoint =
            "https://router.project-osrm.org/route/v1/driving/" +
            "$startLng,$startLat;$endLng,$endLat?overview=full&geometries=geojson"

        var lastFailure: Exception? = null
        var completed: OsrmRouteResult? = null

        // Maximum three attempts; retry only transient network/HTTP failures.
        for (attempt in 0 until 3) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", "Raahi-Android/1.0")
                    setRequestProperty("Accept", "application/json")
                    useCaches = false
                }

                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (status == 429 || status in 500..599) {
                    throw java.io.IOException("Routing service temporarily unavailable (HTTP $status)")
                }
                check(status in 200..299) { "Routing service HTTP $status" }

                val root = JSONObject(body)
                check(root.optString("code") == "Ok") {
                    "No road route available: ${root.optString("code", "unknown")}"
                }

                val routes = root.optJSONArray("routes")
                check(routes != null && routes.length() > 0) { "No road route returned" }
                val route = routes!!.getJSONObject(0)
                val coords = route.getJSONObject("geometry").getJSONArray("coordinates")

                val points = (0 until coords.length()).mapNotNull { i ->
                    val pair = coords.optJSONArray(i) ?: return@mapNotNull null
                    if (pair.length() < 2) return@mapNotNull null
                    val lng = pair.optDouble(0, Double.NaN)
                    val lat = pair.optDouble(1, Double.NaN)
                    if (!lat.isFinite() || !lng.isFinite() ||
                        lat !in -90.0..90.0 || lng !in -180.0..180.0
                    ) null else Point.fromLngLat(lng, lat)
                }
                check(points.size >= 2) { "Route geometry was empty" }

                val distance = route.optDouble("distance", Double.NaN)
                val duration = route.optDouble("duration", Double.NaN)
                check(distance.isFinite() && distance >= 0.0) { "Invalid route distance" }
                check(duration.isFinite() && duration >= 0.0) { "Invalid route duration" }

                completed = OsrmRouteResult(
                    geometry = FeatureCollection.fromFeature(
                        Feature.fromGeometry(LineString.fromLngLats(points))
                    ),
                    distanceMeters = distance,
                    durationSeconds = duration,
                )
                break
            } catch (e: kotlinx.coroutines.CancellationException) {
              throw e
            } catch (e: Exception) {
                lastFailure = e
                val retryable = e is java.io.IOException
                if (!retryable || attempt == 2) throw e
                kotlinx.coroutines.delay(400L * (attempt + 1))
            } finally {
                connection?.disconnect()
            }
        }

        completed ?: throw (lastFailure ?: java.io.IOException("Routing failed"))
    }
}
// OSRM_RETRY_RECOVERY_PATCH_V1

// OSRM_CANCELLATION_SAFETY_V1
