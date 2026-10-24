package `in`.raahi.app.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Robust Centralized Location & Geocoding Resolver for Raahi.
 *
 * Implements a prioritized lookup hierarchy:
 * 1. Curated Offline CityCatalogue (0ms latency, verified Punjab/Tricity/Haryana/Rajasthan/HP coordinates).
 * 2. Prefix & alias extraction (e.g. "Abohar, Punjab" -> Abohar).
 * 3. Native Android System Geocoder (powered by Google Play Services on Android devices) with
 *    modern API 33+ listener and backward-compatible synchronous I/O.
 *
 * Crucially: NEVER defaults unknown coordinates to Delhi. Returns null if unresolved so UI can guide the user.
 */
object LocationResolver {

    private const val TAG = "LocationResolver"

    /**
     * Resolves latitude and longitude coordinates for a city, town, village, or address query.
     */
    suspend fun resolveCoordinates(
        context: Context,
        query: String?
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (query.isNullOrBlank()) return@withContext null
        val clean = query.trim()

        // 1. If query is a bare city name without locality/address qualifiers, check Curated CityCatalogue (0ms latency)
        val hasAddressModifiers = clean.contains(",") || clean.contains("-") || clean.contains("/") ||
                listOf("sector", "phase", "road", "street", "near", "colony", "nagar", "enclave", "block", "floor", "house", "shop", "flat", "plot", "chowk", "chauraha", "village", "bus stand", "railway").any {
                    clean.contains(it, ignoreCase = true)
                }

        if (!hasAddressModifiers) {
            val directCoords = CityCatalogue.getCoordinates(clean)
            if (directCoords != null) {
                return@withContext directCoords
            }
        }

        // 2. Query Android System Geocoder (powered by Google Play Services) for specific addresses, sectors, landmarks, or small towns
        val geocoderResult = runCatching {
            resolveWithSystemGeocoder(context, clean)
        }.getOrNull()

        if (geocoderResult != null) {
            return@withContext geocoderResult
        }

        // 3. Fallback: If geocoder returned null or failed (e.g. offline), check direct CityCatalogue or parts
        val directCoords = CityCatalogue.getCoordinates(clean)
        if (directCoords != null) {
            return@withContext directCoords
        }

        val parts = clean.split(",", "-", "/").map { it.trim() }.filter { it.isNotBlank() }
        for (part in parts) {
            val partCoords = CityCatalogue.getCoordinates(part)
            if (partCoords != null) {
                return@withContext partCoords
            }
        }

        Log.w(TAG, "Location '$clean' could not be resolved to valid coordinates.")
        null
    }

    /**
     * Resolves coordinates using Android's native Geocoder API (backed by Google Play Services).
     */
    @Suppress("DEPRECATION")
    private suspend fun resolveWithSystemGeocoder(context: Context, query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context.applicationContext, Locale("en", "IN"))
        val searchQuery = if (query.contains("india", ignoreCase = true)) query else "$query, India"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                try {
                    geocoder.getFromLocationName(searchQuery, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            cont.resume(addr.latitude to addr.longitude)
                        } else {
                            cont.resume(null)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Android 33+ Geocoder error for '$query': ${e.message}")
                    cont.resume(null)
                }
            }
        } else {
            try {
                val addresses = geocoder.getFromLocationName(searchQuery, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    addr.latitude to addr.longitude
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Legacy Geocoder error for '$query': ${e.message}")
                null
            }
        }
    }

    /**
     * Reverse geocodes coordinates to a human-readable city or locality name using Android native Geocoder.
     */
    @Suppress("DEPRECATION")
    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        if (!lat.isFinite() || !lng.isFinite()) return@withContext null
        try {
            val geocoder = Geocoder(context.applicationContext, Locale("en", "IN"))
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.featureName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reverse geocoding error: ${e.message}")
            null
        }
    }
}
