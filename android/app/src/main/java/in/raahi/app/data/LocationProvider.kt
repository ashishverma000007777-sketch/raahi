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
        manualLocation = LatLng(lat, lng)
    }

    @SuppressLint("MissingPermission")
    suspend fun getFreshGpsLocation(): LatLng? {
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(8000)
            .setMaxUpdateAgeMillis(30000)
            .build()

        // 1. Attempt fresh high-accuracy location with an 8-second timeout
        val freshLocation = withTimeoutOrNull(8000L) {
            runCatching { client.getCurrentLocation(request, null).await() }.getOrNull()
        }

        if (freshLocation != null) {
            return LatLng(freshLocation.latitude, freshLocation.longitude)
        }

        // 2. Emergency fallback to last known location (essential for SOS when satellites are obscured)
        val lastLocation = runCatching { client.lastLocation.await() }.getOrNull()
        if (lastLocation != null) {
            return LatLng(lastLocation.latitude, lastLocation.longitude)
        }

        return null
    }

    @SuppressLint("MissingPermission") // caller is required to have checked permission first
    suspend fun getCurrentLocation(): LatLng? {
        val manual = manualLocation
        if (manual != null) {
            return manual
        }
        return getFreshGpsLocation()
    }
}
