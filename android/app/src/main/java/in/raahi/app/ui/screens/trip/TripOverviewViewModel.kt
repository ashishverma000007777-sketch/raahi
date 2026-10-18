package `in`.raahi.app.ui.screens.trip

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.raahi.app.data.AuthRepository
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

                // Compute Google Routes polyline
                val polyline = try {
                    when (val res = routesProvider.computeDrivingRoute(trip.startLat, trip.startLng, trip.destLat, trip.destLng)) {
                        is GoogleRoutesStatus.Success -> res.route.points
                        else -> listOf(LatLng(trip.startLat, trip.startLng), LatLng(trip.destLat, trip.destLng))
                    }
                } catch (_: Exception) {
                    listOf(LatLng(trip.startLat, trip.startLng), LatLng(trip.destLat, trip.destLng))
                }

                _state.value = TripOverviewUiState.Success(
                    trip = trip,
                    routePolyline = polyline,
                    effectiveFuelLitres = estLitres,
                    effectiveFuelCost = estCost,
                )
            } catch (e: Exception) {
                _state.value = TripOverviewUiState.Error(e.toUserFriendlyMessage("Failed to load trip overview"))
            }
        }
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
