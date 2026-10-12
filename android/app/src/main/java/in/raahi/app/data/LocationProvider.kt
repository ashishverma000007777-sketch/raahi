package `in`.raahi.app.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class LatLng(val lat: Double, val lng: Double)

/**
 * High-accuracy location provider with fallback to lastKnownLocation and timeout protection.
 * Critical for roadside assistance and SOS in spotty or indoor coverage.
 */
@Singleton
class LocationProvider @Inject constructor(@ApplicationContext private val context: Context) {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @Volatile
    var manualLocation: LatLng? = null
        private set

    @Volatile
    var selectedCityName: String = "Chandigarh"
        private set

    fun setManualLocation(cityName: String, lat: Double, lng: Double) {
        selectedCityName = cityName
        manualLocation = LatLng(lat, lng)
    }

    fun clearManualLocation(cityName: String = "Chandigarh") {
        selectedCityName = cityName
        manualLocation = null
    }

    fun setGpsLocation(cityName: String, lat: Double, lng: Double) {
        selectedCityName = cityName
        val coords = LatLng(lat, lng)
        manualLocation = coords
        lastConfirmedGpsLocation = coords
    }

    @Volatile
    var lastConfirmedGpsLocation: LatLng? = null
        private set

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownGpsLocation(): LatLng? {
        val loc = runCatching { client.lastLocation.await() }.getOrNull()
        if (loc != null && loc.latitude.isFinite() && loc.longitude.isFinite()) {
            val coords = LatLng(loc.latitude, loc.longitude)
            lastConfirmedGpsLocation = coords
            return coords
        }
        return null
    }

    /**
     * Obtains the user's REAL physical device GPS location.
     * Bypasses any manual city override set on the home screen.
     * Checks fast lastLocation first, attempts fresh high-accuracy GPS fix,
     * and falls back to lastLocation if fresh fix takes longer than timeout.
     */
    @SuppressLint("MissingPermission")
    suspend fun getRealGpsLocation(freshTimeoutMs: Long = 7000L): LatLng? {
        // Fast path: if last known location is available and fresh (< 2 mins), return it or attempt fresh
        val cached = getLastKnownGpsLocation()

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(freshTimeoutMs)
            .setMaxUpdateAgeMillis(15000)
            .build()

        val freshLocation = withTimeoutOrNull(freshTimeoutMs) {
            runCatching { client.getCurrentLocation(request, null).await() }.getOrNull()
        }

        if (freshLocation != null && freshLocation.latitude.isFinite() && freshLocation.longitude.isFinite()) {
            val coords = LatLng(freshLocation.latitude, freshLocation.longitude)
            lastConfirmedGpsLocation = coords
            return coords
        }

        if (cached != null) {
            return cached
        }

        val fallback = getLastKnownGpsLocation()
        if (fallback != null) {
            return fallback
        }

        return lastConfirmedGpsLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getFreshGpsLocation(): LatLng? {
        return getRealGpsLocation(8000L)
    }

    @SuppressLint("MissingPermission") // caller is required to have checked permission first
    suspend fun getCurrentLocation(): LatLng? {
        val manual = manualLocation
        if (manual != null) {
            return manual
        }
        return getRealGpsLocation()
    }
}
