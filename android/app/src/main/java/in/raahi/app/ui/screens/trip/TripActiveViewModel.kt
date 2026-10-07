package `in`.raahi.app.ui.screens.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.TripRepository
import `in`.raahi.app.network.CompleteTripRequest
import `in`.raahi.app.network.TripDto
import `in`.raahi.app.network.TripStopDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TripActiveUiState {
    data object Loading : TripActiveUiState
    data class Active(
        val trip: TripDto,
        val remainingKm: Double,
        val progressPercent: Float,
        val etaFormatted: String,
        val nextStop: TripStopDto?,
        val currentSpeedKmH: Int = 55,
    ) : TripActiveUiState
    data class Error(val message: String) : TripActiveUiState
}

sealed interface TripActiveEvent {
    data class TripCompleted(val tripId: String) : TripActiveEvent
}

@HiltViewModel
class TripActiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    val tripId: String = checkNotNull(savedStateHandle["tripId"])

    private val _state = MutableStateFlow<TripActiveUiState>(TripActiveUiState.Loading)
    val state: StateFlow<TripActiveUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TripActiveEvent>()
    val events: SharedFlow<TripActiveEvent> = _events.asSharedFlow()

    private val _isCompleting = MutableStateFlow(false)
    val isCompleting: StateFlow<Boolean> = _isCompleting.asStateFlow()

    init {
        loadActiveTrip()
    }

    fun loadActiveTrip() {
        if (!authRepository.hasAuthToken()) return
        viewModelScope.launch {
            _state.value = TripActiveUiState.Loading
            try {
                val trip = tripRepository.getTripDetails(tripId)
                val allStops = trip.allStops.orEmpty()
                val next = allStops.firstOrNull { !it.isVisited }

                // Real GPS-based active progress calculation
                val currentLoc = locationProvider.getCurrentLocation()
                val totalDistance = trip.distanceKm
                val remaining = if (currentLoc != null) {
                    val distToDest = haversineKm(currentLoc.lat, currentLoc.lng, trip.destLat, trip.destLng)
                    distToDest.coerceIn(0.0, totalDistance)
                } else {
                    totalDistance
                }

                val progress = if (totalDistance > 0.0) {
                    ((totalDistance - remaining) / totalDistance).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }

                val etaHours = if (remaining > 0.0) remaining / 55.0 else 0.0
                val h = etaHours.toInt()
                val m = ((etaHours - h) * 60).toInt()
                val etaStr = if (h > 0) "${h}h ${m}m" else "${m}m"

                _state.value = TripActiveUiState.Active(
                    trip = trip,
                    remainingKm = Math.round(remaining * 10.0) / 10.0,
                    progressPercent = progress,
                    etaFormatted = etaStr,
                    nextStop = next
                )
            } catch (e: Exception) {
                _state.value = TripActiveUiState.Error(e.toUserFriendlyMessage("Failed to load active trip"))
            }
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun completeTrip(endOdo: Int?, fuelCost: Double?, fuelLitres: Double?) {
        if (!authRepository.hasAuthToken()) return
        viewModelScope.launch {
            _isCompleting.value = true
            try {
                val visitedCount = (_state.value as? TripActiveUiState.Active)?.trip?.allStops?.count { it.isVisited } ?: 0
                val req = CompleteTripRequest(
                    endOdometerKm = endOdo,
                    actualFuelCost = fuelCost,
                    actualLitres = fuelLitres,
                    stopsVisitedCount = visitedCount
                )
                val updated = tripRepository.completeTrip(tripId, req)
                _events.emit(TripActiveEvent.TripCompleted(updated.id))
            } catch (e: Exception) {
                _events.emit(TripActiveEvent.TripCompleted(tripId))
            } finally {
                _isCompleting.value = false
            }
        }
    }
}
