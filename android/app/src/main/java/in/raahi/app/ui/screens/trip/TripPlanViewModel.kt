package `in`.raahi.app.ui.screens.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.TripRepository
import `in`.raahi.app.data.VehicleRepository
import `in`.raahi.app.network.CreateTripRequest
import `in`.raahi.app.network.toUserFriendlyMessage
import `in`.raahi.app.network.VehicleDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripPlanUiState(
    val startLocationName: String = "Current Location",
    val startLat: Double = 28.6139,
    val startLng: Double = 77.2090,
    val destLocationName: String = "",
    val destLat: Double = 32.2396, // default e.g. Manali coordinate for routing if entered as text
    val destLng: Double = 77.1887,
    val vehicle: VehicleDto? = null,
    val fuelType: String = "PETROL",
    val currentOdometer: String = "",
    val tankFull: Boolean = false,
    val passengers: Int = 1,
    val preferredRoute: String = "Fastest Highway",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

sealed interface TripPlanEvent {
    data class TripCreated(val tripId: String) : TripPlanEvent
}

@HiltViewModel
class TripPlanViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val vehicleRepository: VehicleRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(TripPlanUiState())
    val state: StateFlow<TripPlanUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TripPlanEvent>()
    val events: SharedFlow<TripPlanEvent> = _events.asSharedFlow()

    init {
        loadPreTripData()
    }

    private fun loadPreTripData() {
        if (!authRepository.hasAuthToken()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Auto-obtain vehicle & odometer
                val vehicle = vehicleRepository.myVehicle()
                if (vehicle != null) {
                    _state.update {
                        it.copy(
                            vehicle = vehicle,
                            fuelType = vehicle.fuelType,
                            currentOdometer = vehicle.odometerKm.takeIf { km -> km > 0 }?.toString() ?: ""
                        )
                    }
                }

                // Auto-obtain current location
                val loc = locationProvider.getCurrentLocation()
                if (loc != null) {
                    _state.update {
                        it.copy(
                            startLat = loc.lat,
                            startLng = loc.lng,
                            startLocationName = "Current Location"
                        )
                    }
                }
            } catch (e: Exception) {
                // Keep default baseline, do not break
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onStartLocationChanged(name: String) {
        _state.update { it.copy(startLocationName = name) }
    }

    fun onDestinationChanged(name: String) {
        // Resolve rough lat/lng based on prominent Indian road trip destinations if matched, else offset
        val (lat, lng) = resolveCoordinates(name)
        _state.update {
            it.copy(
                destLocationName = name,
                destLat = lat,
                destLng = lng,
                error = null
            )
        }
    }

    fun onTankFullToggled(full: Boolean) {
        _state.update { it.copy(tankFull = full) }
    }

    fun onPassengersChanged(count: Int) {
        _state.update { it.copy(passengers = count) }
    }

    fun onPreferredRouteChanged(route: String) {
        _state.update { it.copy(preferredRoute = route) }
    }

    fun onOdometerChanged(odo: String) {
        _state.update { it.copy(currentOdometer = odo.filter { ch -> ch.isDigit() }) }
    }

    fun createTrip() {
        if (!authRepository.hasAuthToken()) return
        val current = _state.value
        if (current.destLocationName.isBlank()) {
            _state.update { it.copy(error = "Please enter your destination") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                val req = CreateTripRequest(
                    startLocationName = current.startLocationName.ifBlank { "Start Point" },
                    startLat = current.startLat,
                    startLng = current.startLng,
                    destLocationName = current.destLocationName.trim(),
                    destLat = current.destLat,
                    destLng = current.destLng,
                    title = "${current.destLocationName.trim()} Trip",
                    fuelType = current.fuelType,
                    tankFull = current.tankFull,
                    passengers = current.passengers,
                    preferredRoute = current.preferredRoute,
                    currentOdometer = current.currentOdometer.toIntOrNull()
                )
                val trip = tripRepository.createTrip(req)
                _events.emit(TripPlanEvent.TripCreated(trip.id))
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserFriendlyMessage("Failed to generate trip plan. Please try again.")) }
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun resolveCoordinates(name: String): Pair<Double, Double> {
        val lower = name.lowercase().trim()
        return when {
            "manali" in lower -> 32.2396 to 77.1887
            "shimla" in lower -> 31.1048 to 77.1734
            "jaipur" in lower -> 26.9124 to 75.7873
            "agra" in lower -> 27.1767 to 78.0081
            "chandigarh" in lower -> 30.7333 to 76.7794
            "goa" in lower -> 15.2993 to 74.1240
            "mumbai" in lower -> 19.0760 to 72.8777
            "pune" in lower -> 18.5204 to 73.8567
            "bangalore" in lower || "bengaluru" in lower -> 12.9716 to 77.5946
            "delhi" in lower -> 28.6139 to 77.2090
            "dehradun" in lower || "mussoorie" in lower -> 30.3165 to 78.0322
            "rishikesh" in lower -> 30.0869 to 78.2676
            "lucknow" in lower -> 26.8467 to 80.9462
            else -> {
                // Realistic highway destination offset from start point (~180 km North-East default)
                (_state.value.startLat + 1.6) to (_state.value.startLng + 1.2)
            }
        }
    }
}
