package `in`.raahi.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
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
    val phone: String? = null,
    val primaryType: String? = null,
    val types: List<String> = emptyList(),
)

sealed class GooglePlacesStatus {
    data object Idle : GooglePlacesStatus()
    data object Loading : GooglePlacesStatus()
    data class Success(val places: List<GooglePlaceResult>) : GooglePlacesStatus()
    data class Unavailable(val reason: String) : GooglePlacesStatus()
}

/**
 * Genuine Google Places (New API) integration with OpenStreetMap / Overpass fallback.
 * Authenticates using project Google Maps API credentials, Android package name, and SHA-1 signing cert.
 * Searches real automotive repair, tyre, puncture, and garage workshops around the user's location.
 */
@Singleton
class GooglePlacesProvider @Inject constructor(
    @ApplicationContext private val context: Context
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

    private val certSha1: String
        get() = resolveSigningSha1(context)

    suspend fun searchNearbyAutoRepair(
        lat: Double,
        lng: Double,
        radiusMeters: Double = 5000.0,
    ): GooglePlacesStatus = withContext(Dispatchers.IO) {
        // Controlled stepped radius expansion: 5 km -> 10 km -> 15 km
        val radii = listOf(radiusMeters.coerceAtLeast(3000.0), 10000.0, 15000.0).distinct()
        val aggregatedPlaces = mutableMapOf<String, GooglePlaceResult>()
        var lastErrorNotice: String? = null

        for (rad in radii) {
            val status = queryGooglePlacesInternal(lat, lng, rad)
            when (status) {
                is GooglePlacesStatus.Success -> {
                    for (p in status.places) {
                        aggregatedPlaces[p.id] = p
                    }
                    if (aggregatedPlaces.size >= 4) {
                        break // Sufficient nearby results fetched
                    }
                }
                is GooglePlacesStatus.Unavailable -> {
                    lastErrorNotice = status.reason
                    break // Stop expanding if API is blocked / rejected
                }
                else -> {}
            }
        }

        if (aggregatedPlaces.isNotEmpty()) {
            return@withContext GooglePlacesStatus.Success(
                aggregatedPlaces.values.sortedBy { it.distanceKm }
            )
        }

        // Resilient fallback: Query OpenStreetMap / Overpass for real repair shops & garages
        Log.i("GooglePlacesProvider", "Querying Overpass API fallback for coordinates ($lat, $lng)")
        val fallbackResults = queryOverpassFallback(lat, lng, radiusMeters = 10000.0)
        if (fallbackResults.isNotEmpty()) {
            return@withContext GooglePlacesStatus.Success(fallbackResults)
        }

        if (lastErrorNotice != null) {
            GooglePlacesStatus.Unavailable(lastErrorNotice)
        } else {
            GooglePlacesStatus.Success(emptyList())
        }
    }

    private fun queryGooglePlacesInternal(
        lat: Double,
        lng: Double,
        radiusMeters: Double
    ): GooglePlacesStatus {
        try {
            val url = "https://places.googleapis.com/v1/places:searchNearby"
            val jsonPayload = JSONObject().apply {
                put("includedTypes", org.json.JSONArray(listOf("car_repair", "tire_shop", "auto_parts_store")))
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
                .addHeader("X-Android-Cert", certSha1)
                .addHeader(
                    "X-Goog-FieldMask",
                    "places.id,places.displayName,places.formattedAddress,places.location,places.nationalPhoneNumber,places.rating,places.userRatingCount,places.currentOpeningHours.openNow,places.primaryType,places.types"
                )
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string().orEmpty()

            if (code == 200) {
                val json = JSONObject(bodyString)
                val placesArray = json.optJSONArray("places") ?: return GooglePlacesStatus.Success(emptyList())
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
                    val phone = if (p.has("nationalPhoneNumber")) p.optString("nationalPhoneNumber").takeIf { it.isNotBlank() } else null
                    val primaryType = p.optString("primaryType").takeIf { it.isNotBlank() }
                    val typesList = mutableListOf<String>()
                    val typesArray = p.optJSONArray("types")
                    if (typesArray != null) {
                        for (j in 0 until typesArray.length()) {
                            typesList.add(typesArray.getString(j))
                        }
                    }

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
                            phone = phone,
                            primaryType = primaryType,
                            types = typesList,
                        )
                    )
                }
                return GooglePlacesStatus.Success(list.sortedBy { it.distanceKm })
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
                        "Google Places API (New) requires activation or billing in Google Cloud Console. Showing verified Raahi mechanics."
                    code == 400 -> "Google Places query parameter issue ($errorMsg)."
                    else -> "Google Places service unavailable ($errorMsg)."
                }
                return GooglePlacesStatus.Unavailable(userNotice)
            }
        } catch (e: Exception) {
            Log.w("GooglePlacesProvider", "Google Places network check failed: ${e.message}")
            return GooglePlacesStatus.Unavailable(
                "Unable to connect to Google Places API (${e.javaClass.simpleName}). Showing verified Raahi mechanics."
            )
        }
    }

    private fun queryOverpassFallback(lat: Double, lng: Double, radiusMeters: Double = 10000.0): List<GooglePlaceResult> {
        return try {
            val rad = radiusMeters.toInt()
            val overpassQuery = "[out:json][timeout:6];(node(around:$rad,$lat,$lng)[shop=car_repair];node(around:$rad,$lat,$lng)[shop=tyres];node(around:$rad,$lat,$lng)[amenity=vehicle_repair];);out 25;"
            val requestBody = "data=" + java.net.URLEncoder.encode(overpassQuery, "UTF-8")
            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .addHeader("User-Agent", "RaahiApp/1.0 (Roadside Assistance)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val elements = json.optJSONArray("elements") ?: return emptyList()

            val results = mutableListOf<GooglePlaceResult>()
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val id = "osm-${el.optLong("id")}"
                val elLat = el.optDouble("lat", Double.NaN)
                val elLon = el.optDouble("lon", Double.NaN)
                if (elLat.isNaN() || elLon.isNaN()) continue

                val tags = el.optJSONObject("tags") ?: JSONObject()
                val name = tags.optString("name").ifBlank {
                    tags.optString("name:en").ifBlank { "Auto Workshop" }
                }
                val phone = tags.optString("phone").ifBlank {
                    tags.optString("contact:phone").ifBlank { null }
                }
                val street = tags.optString("addr:street")
                val city = tags.optString("addr:city")
                val address = when {
                    street.isNotBlank() && city.isNotBlank() -> "$street, $city"
                    street.isNotBlank() -> street
                    city.isNotBlank() -> city
                    else -> null
                }
                val isTyres = tags.optString("shop") == "tyres"
                val dist = haversineKm(lat, lng, elLat, elLon)

                results.add(
                    GooglePlaceResult(
                        id = id,
                        name = name,
                        address = address,
                        lat = elLat,
                        lng = elLon,
                        rating = null,
                        userRatingCount = null,
                        distanceKm = dist,
                        isOpenNow = null,
                        phone = phone,
                        primaryType = if (isTyres) "tire_shop" else "car_repair",
                        types = if (isTyres) listOf("tire_shop") else listOf("car_repair")
                    )
                )
            }
            results.sortedBy { it.distanceKm }
        } catch (e: Exception) {
            Log.w("GooglePlacesProvider", "Overpass fallback failed: ${e.message}")
            emptyList()
        }
    }

    private fun resolveSigningSha1(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return "A63E0B2DEDA15CBB2C61A5CACF5CF909BFAF795F"
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(cert)
            digest.joinToString("") { "%02X".format(it) }
        } catch (_: Exception) {
            "A63E0B2DEDA15CBB2C61A5CACF5CF909BFAF795F"
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c * 10.0).roundToInt() / 10.0
    }
}
