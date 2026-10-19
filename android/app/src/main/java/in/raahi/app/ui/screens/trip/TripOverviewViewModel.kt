package `in`.raahi.app.ui.screens.trip

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.DailyRepository
import `in`.raahi.app.data.GoogleRoutesProvider
import `in`.raahi.app.data.GoogleRoutesStatus
import `in`.raahi.app.data.TripRepository
import `in`.raahi.app.network.TripDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

sealed interface TripOverviewUiState {
    data object Loading : TripOverviewUiState
    data class Success(
        val trip: TripDto,
        val routePolyline: List<LatLng> = emptyList(),
        val effectiveFuelLitres: Double? = null,
        val effectiveFuelCost: Double? = null,
        val tollCost: Double? = null,
        val tollAvailable: Boolean = false,
        val tollUnavailableReason: String? = null,
        val totalEstimatedBudget: Double? = null,
        val costPerKm: Double? = null,
        val fuelStops: List<FuelStationStop> = emptyList(),
        val smartBreaks: List<SmartBreakStop> = emptyList(),
    ) : TripOverviewUiState
    data class Error(val message: String) : TripOverviewUiState
}

sealed interface TripOverviewEvent {
    data class TripStarted(val tripId: String) : TripOverviewEvent
}

@HiltViewModel
class TripOverviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val dailyRepository: DailyRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val tripId: String = checkNotNull(savedStateHandle["tripId"])

    private val _state = MutableStateFlow<TripOverviewUiState>(TripOverviewUiState.Loading)
    val state: StateFlow<TripOverviewUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TripOverviewEvent>()
    val events: SharedFlow<TripOverviewEvent> = _events.asSharedFlow()

    private val _isStarting = MutableStateFlow(false)
    val isStarting: StateFlow<Boolean> = _isStarting.asStateFlow()

    private val routesProvider = GoogleRoutesProvider(context)

    init {
        loadTrip()
    }

    fun loadTrip() {
        if (!authRepository.hasAuthToken()) return
        viewModelScope.launch {
            _state.value = TripOverviewUiState.Loading
            try {
                val trip = tripRepository.getTripDetails(tripId)

                // Calculate realistic fuel estimate from vehicle fuel type & distance if missing from server
                val (estLitres, estCost) = computeFuel(trip.fuelType ?: "PETROL", trip.distanceKm, trip.estimatedFuelLitres, trip.estimatedFuelCost)

                // Compute Google Routes polyline and genuine toll information
                var tollCost: Double? = null
                var tollAvailable = false
                var tollUnavailableReason: String? = "Toll data not provided for this route"

                val polyline = try {
                    when (val res = routesProvider.computeDrivingRoute(trip.startLat, trip.startLng, trip.destLat, trip.destLng)) {
                        is GoogleRoutesStatus.Success -> {
                            tollCost = res.route.tollCost ?: trip.estimatedTolls
                            tollAvailable = res.route.tollAvailable || trip.estimatedTolls != null
                            tollUnavailableReason = if (!tollAvailable) (res.route.tollUnavailableReason ?: "Toll data not provided for this route") else null
                            res.route.points
                        }
                        else -> {
                            if (trip.estimatedTolls != null) {
                                tollCost = trip.estimatedTolls
                                tollAvailable = true
                                tollUnavailableReason = null
                            }
                            listOf(LatLng(trip.startLat, trip.startLng), LatLng(trip.destLat, trip.destLng))
                        }
                    }
                } catch (_: Exception) {
                    if (trip.estimatedTolls != null) {
                        tollCost = trip.estimatedTolls
                        tollAvailable = true
                        tollUnavailableReason = null
                    }
                    listOf(LatLng(trip.startLat, trip.startLng), LatLng(trip.destLat, trip.destLng))
                }

                val totalBudget = estCost + (if (tollAvailable) (tollCost ?: 0.0) else 0.0)
                val costPerKm = if (trip.distanceKm > 0.0) ((totalBudget / trip.distanceKm) * 100.0).roundToInt() / 100.0 else null

                // Fetch real fuel stations and smart rest stops along polyline
                val (fuelStops, smartBreaks) = fetchRouteStops(polyline)

                _state.value = TripOverviewUiState.Success(
                    trip = trip,
                    routePolyline = polyline,
                    effectiveFuelLitres = estLitres,
                    effectiveFuelCost = estCost,
                    tollCost = tollCost,
                    tollAvailable = tollAvailable,
                    tollUnavailableReason = tollUnavailableReason,
                    totalEstimatedBudget = totalBudget,
                    costPerKm = costPerKm,
                    fuelStops = fuelStops,
                    smartBreaks = smartBreaks,
                )
            } catch (e: Exception) {
                _state.value = TripOverviewUiState.Error(e.toUserFriendlyMessage("Failed to load trip overview"))
            }
        }
    }

    private suspend fun fetchRouteStops(polyline: List<LatLng>): Pair<List<FuelStationStop>, List<SmartBreakStop>> {
        if (polyline.isEmpty()) return emptyList<FuelStationStop>() to emptyList<SmartBreakStop>()
        val samplePoints = if (polyline.size >= 4) {
            listOf(polyline[polyline.size / 4], polyline[polyline.size / 2], polyline[polyline.size * 3 / 4])
        } else {
            polyline
        }

        val foundStations = mutableListOf<FuelStationStop>()
        val foundBreaks = mutableListOf<SmartBreakStop>()

        for (pt in samplePoints) {
            try {
                val fuelPlaces = dailyRepository.nearbyPlaces(pt.latitude, pt.longitude, "fuel")
                for (p in fuelPlaces) {
                    val detour = calculateDetourKm(p.lat, p.lng, polyline)
                    val distAlong = calculateDistAlongKm(p.lat, p.lng, polyline)
                    if (foundStations.none { it.name.equals(p.name, ignoreCase = true) }) {
                        foundStations.add(FuelStationStop(p.name, p.lat, p.lng, distAlong, detour))
                    }
                }
            } catch (_: Exception) {}

            try {
                val dhabas = dailyRepository.nearbyPlaces(pt.latitude, pt.longitude, "dhaba")
                for (p in dhabas) {
                    val detour = calculateDetourKm(p.lat, p.lng, polyline)
                    val distAlong = calculateDistAlongKm(p.lat, p.lng, polyline)
                    if (foundBreaks.none { it.name.equals(p.name, ignoreCase = true) }) {
                        foundBreaks.add(SmartBreakStop(p.id, p.name, "dhaba", "Highway Dhaba / Food", p.lat, p.lng, distAlong, detour))
                    }
                }
            } catch (_: Exception) {}

            try {
                val toilets = dailyRepository.nearbyPlaces(pt.latitude, pt.longitude, "toilet")
                for (p in toilets) {
                    val detour = calculateDetourKm(p.lat, p.lng, polyline)
                    val distAlong = calculateDistAlongKm(p.lat, p.lng, polyline)
                    if (foundBreaks.none { it.name.equals(p.name, ignoreCase = true) }) {
                        foundBreaks.add(SmartBreakStop(p.id, p.name, "toilet", "Restroom / Washroom", p.lat, p.lng, distAlong, detour))
                    }
                }
            } catch (_: Exception) {}
        }

        return foundStations.sortedBy { it.detourKm }.take(3) to foundBreaks.sortedBy { it.detourKm }.take(4)
    }

    private fun calculateDetourKm(lat: Double, lng: Double, polyline: List<LatLng>): Double {
        if (polyline.isEmpty()) return 0.0
        var minDist = Double.MAX_VALUE
        for (pt in polyline) {
            val dist = haversineKm(lat, lng, pt.latitude, pt.longitude)
            if (dist < minDist) minDist = dist
        }
        return (minDist * 10.0).roundToInt() / 10.0
    }

    private fun calculateDistAlongKm(lat: Double, lng: Double, polyline: List<LatLng>): Double {
        if (polyline.isEmpty()) return 0.0
        var closestIdx = 0
        var minDist = Double.MAX_VALUE
        for (i in polyline.indices) {
            val dist = haversineKm(lat, lng, polyline[i].latitude, polyline[i].longitude)
            if (dist < minDist) {
                minDist = dist
                closestIdx = i
            }
        }
        var cumulative = 0.0
        for (i in 0 until closestIdx) {
            cumulative += haversineKm(polyline[i].latitude, polyline[i].longitude, polyline[i + 1].latitude, polyline[i + 1].longitude)
        }
        return (cumulative * 10.0).roundToInt() / 10.0
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    private fun computeFuel(
        fuelType: String,
        distanceKm: Double,
        serverLitres: Double?,
        serverCost: Double?
    ): Pair<Double, Double> {
        if (serverLitres != null && serverCost != null && serverLitres > 0.0 && serverCost > 0.0) {
            return serverLitres to serverCost
        }

        val mileage = when (fuelType.uppercase()) {
            "DIESEL" -> 18.0
            "CNG" -> 24.0
            "EV" -> 7.0
            else -> 14.5 // PETROL
        }
        val litres = ((distanceKm / mileage) * 10.0).roundToInt() / 10.0
        val pricePerUnit = when (fuelType.uppercase()) {
            "DIESEL" -> 89.62
            "CNG" -> 79.50
            "EV" -> 12.00
            else -> 96.72 // PETROL
        }
        val cost = (litres * pricePerUnit).roundToInt().toDouble()
        return (serverLitres ?: litres) to (serverCost ?: cost)
    }

    fun startTrip() {
        if (!authRepository.hasAuthToken()) return
        viewModelScope.launch {
            _isStarting.value = true
            try {
                val updated = tripRepository.startTrip(tripId)
                _events.emit(TripOverviewEvent.TripStarted(updated.id))
            } catch (_: Exception) {
                // If already in progress, still proceed to active
                _events.emit(TripOverviewEvent.TripStarted(tripId))
            } finally {
                _isStarting.value = false
            }
        }
    }
}
