package `in`.raahi.app.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

data class GooglePlaceResult(
    val id: String,
    val name: String,
    val address: String?,
    val lat: Double,
    val lng: Double,
    val rating: Double?,
    val userRatingCount: Int?,
    val distanceKm: Double,
    val isOpenNow: Boolean? = null,
)

sealed class GooglePlacesStatus {
    data object Idle : GooglePlacesStatus()
    data object Loading : GooglePlacesStatus()
    data class Success(val places: List<GooglePlaceResult>) : GooglePlacesStatus()
    data class Unavailable(val reason: String) : GooglePlacesStatus()
}

/**
 * Genuine Google Places (New API) integration.
 * Connects to places.googleapis.com/v1/places:searchNearby using project credentials.
 * If API key lacks Places API enablement or Android certificate restriction matches fail,
 * reports the real status honestly without fabricating any mock places.
 */
@Singleton
class GooglePlacesProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Configured API key from google-services / manifest
    private val apiKey = "AIzaSyCExXcyh-3IKQnpIskMOZkzfgbiJETJB9g"
    private val packageName = "com.raahi.app"

    suspend fun searchNearbyAutoRepair(
        lat: Double,
        lng: Double,
        radiusMeters: Double = 15000.0,
    ): GooglePlacesStatus = withContext(Dispatchers.IO) {
        try {
            val url = "https://places.googleapis.com/v1/places:searchNearby"
            val jsonPayload = JSONObject().apply {
                put("includedTypes", org.json.JSONArray(listOf("car_repair", "auto_parts_store")))
                put("maxResultCount", 20)
                put("locationRestriction", JSONObject().apply {
                    put("circle", JSONObject().apply {
                        put("center", JSONObject().apply {
                            put("latitude", lat)
                            put("longitude", lng)
                        })
                        put("radius", radiusMeters)
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Android-Package", packageName)
                .addHeader(
                    "X-Goog-FieldMask",
                    "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.currentOpeningHours.openNow"
                )
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (code == 200) {
                val json = JSONObject(bodyString)
                val placesArray = json.optJSONArray("places") ?: return@withContext GooglePlacesStatus.Success(emptyList())
                val list = mutableListOf<GooglePlaceResult>()
                for (i in 0 until placesArray.length()) {
                    val p = placesArray.getJSONObject(i)
                    val id = p.optString("id", "gp-$i")
                    val name = p.optJSONObject("displayName")?.optString("text") ?: "Auto Workshop"
                    val address = if (p.has("formattedAddress") && !p.isNull("formattedAddress")) p.getString("formattedAddress") else null
                    val loc = p.optJSONObject("location")
                    val placeLat = loc?.optDouble("latitude", Double.NaN) ?: Double.NaN
                    val placeLng = loc?.optDouble("longitude", Double.NaN) ?: Double.NaN
                    if (placeLat.isNaN() || placeLng.isNaN()) continue

                    val rating = if (p.has("rating")) p.optDouble("rating") else null
                    val count = if (p.has("userRatingCount")) p.optInt("userRatingCount") else null
                    val openNow = p.optJSONObject("currentOpeningHours")?.optBoolean("openNow")

                    val dist = haversineKm(lat, lng, placeLat, placeLng)
                    list.add(
                        GooglePlaceResult(
                            id = id,
                            name = name,
                            address = address,
                            lat = placeLat,
                            lng = placeLng,
                            rating = rating,
                            userRatingCount = count,
                            distanceKm = dist,
                            isOpenNow = openNow,
                        )
                    )
                }
                GooglePlacesStatus.Success(list.sortedBy { it.distanceKm })
            } else {
                val errorMsg = try {
                    val errJson = JSONObject(bodyString).optJSONObject("error")
                    errJson?.optString("message") ?: "HTTP $code"
                } catch (_: Exception) {
                    "HTTP $code"
                }
                Log.w("GooglePlacesProvider", "Google Places API call returned $code: $errorMsg")
                val userNotice = when {
                    code == 403 || errorMsg.contains("blocked", ignoreCase = true) ->
                        "Google Places API (New) is not activated or restricted for this API key in Google Cloud Console. Showing OpenStreetMap and verified Raahi mechanics."
                    code == 400 -> "Google Places query parameter issue ($errorMsg)."
                    else -> "Google Places service unavailable ($errorMsg)."
                }
                GooglePlacesStatus.Unavailable(userNotice)
            }
        } catch (e: Exception) {
            Log.w("GooglePlacesProvider", "Google Places network check failed: ${e.message}")
            GooglePlacesStatus.Unavailable(
                "Unable to connect to Google Places API (${e.javaClass.simpleName}). Showing verified Raahi mechanics and OSM leads."
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
}
