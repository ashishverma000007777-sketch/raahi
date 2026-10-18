package `in`.raahi.app.ui.screens.trip

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.GoogleRoutesProvider
import `in`.raahi.app.data.GoogleRoutesStatus
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.data.LocationResolver
import `in`.raahi.app.data.TripRepository
import `in`.raahi.app.data.VehicleRepository
import `in`.raahi.app.network.CreateTripRequest
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class TripPlanUiState(
    val startLocationName: String = "Current Location",
    val startLat: Double = 28.6139,
    val startLng: Double = 77.2090,
    val destLocationName: String = "",
    val destLat: Double = 32.2396,
    val destLng: Double = 77.1887,
    val departureDateTime: LocalDateTime = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0),
    val isDepartureValid: Boolean = true,
    val vehicle: VehicleDto? = null,
    val fuelType: String = "PETROL",
    val currentOdometer: String = "",
    val tankFull: Boolean = false,
    val passengers: Int = 1,
    val preferredRoute: String = "Fastest Highway",
    val routePolyline: List<LatLng> = emptyList(),
    val routeDistanceKm: Double? = null,
    val routeDurationMinutes: Int? = null,
    val routeDurationFormatted: String? = null,
    val routeEstimatedFuelLitres: Double? = null,
    val routeEstimatedFuelCost: Double? = null,
    val isCalculatingRoute: Boolean = false,
    val isResolvingLocation: Boolean = false,
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
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(TripPlanUiState())
    val state: StateFlow<TripPlanUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TripPlanEvent>()
    val events: SharedFlow<TripPlanEvent> = _events.asSharedFlow()

    private val routesProvider = GoogleRoutesProvider(context)
    private var resolveStartJob: Job? = null
    private var resolveDestJob: Job? = null
    private var routeJob: Job? = null

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
                    val resolvedName = LocationResolver.reverseGeocode(context, loc.lat, loc.lng)
                    _state.update {
                        it.copy(
                            startLat = loc.lat,
                            startLng = loc.lng,
                            startLocationName = resolvedName?.let { name -> "$name (Current Location)" } ?: "Current Location"
                        )
                    }
                }
            } catch (_: Exception) {
                // Keep baseline default coordinates
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onStartLocationChanged(name: String) {
        _state.update { it.copy(startLocationName = name, error = null) }
        resolveStartJob?.cancel()
        resolveStartJob = viewModelScope.launch {
            delay(400) // debounce
            _state.update { it.copy(isResolvingLocation = true) }
            val coords = LocationResolver.resolveCoordinates(context, name)
            _state.update { it.copy(isResolvingLocation = false) }
            if (coords != null) {
                _state.update { it.copy(startLat = coords.first, startLng = coords.second) }
                calculateDrivingRoute()
            }
        }
    }

    fun onDestinationChanged(name: String) {
        _state.update { it.copy(destLocationName = name, error = null) }
        resolveDestJob?.cancel()
        resolveDestJob = viewModelScope.launch {
            delay(400) // debounce
            _state.update { it.copy(isResolvingLocation = true) }
            val coords = LocationResolver.resolveCoordinates(context, name)
            _state.update { it.copy(isResolvingLocation = false) }
            if (coords != null) {
                _state.update { it.copy(destLat = coords.first, destLng = coords.second) }
                calculateDrivingRoute()
            }
        }
    }

    fun onStartCoordsUpdated(lat: Double, lng: Double) {
        viewModelScope.launch {
            val name = LocationResolver.reverseGeocode(context, lat, lng)
            _state.update {
                it.copy(
                    startLat = lat,
                    startLng = lng,
                    startLocationName = name ?: "${String.format(Locale.ROOT, "%.4f", lat)}, ${String.format(Locale.ROOT, "%.4f", lng)}"
                )
            }
            calculateDrivingRoute()
        }
    }

    fun onDestCoordsUpdated(lat: Double, lng: Double) {
        viewModelScope.launch {
            val name = LocationResolver.reverseGeocode(context, lat, lng)
            _state.update {
                it.copy(
                    destLat = lat,
                    destLng = lng,
                    destLocationName = name ?: "${String.format(Locale.ROOT, "%.4f", lat)}, ${String.format(Locale.ROOT, "%.4f", lng)}"
                )
            }
            calculateDrivingRoute()
        }
    }

    fun swapLocations() {
        val current = _state.value
        if (current.destLocationName.isBlank()) return
        _state.update {
            it.copy(
                startLocationName = it.destLocationName,
                startLat = it.destLat,
                startLng = it.destLng,
                destLocationName = it.startLocationName,
                destLat = it.startLat,
                destLng = it.startLng
            )
        }
        calculateDrivingRoute()
    }

    fun onDepartureDateTimeChanged(newDateTime: LocalDateTime) {
        val isValid = newDateTime.isAfter(LocalDateTime.now().minusMinutes(2))
        _state.update {
            it.copy(
                departureDateTime = newDateTime,
                isDepartureValid = isValid,
                error = if (!isValid) "Departure date and time cannot be in the past" else null
            )
        }
    }

    fun calculateDrivingRoute() {
        val current = _state.value
        if (current.destLocationName.isBlank()) return

        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            _state.update { it.copy(isCalculatingRoute = true) }
            try {
                when (val res = routesProvider.computeDrivingRoute(current.startLat, current.startLng, current.destLat, current.destLng)) {
                    is GoogleRoutesStatus.Success -> {
                        val route = res.route
                        val fuelEst = calculateFuel(current.fuelType, route.distanceKm)
                        _state.update {
                            it.copy(
                                routePolyline = route.points,
                                routeDistanceKm = route.distanceKm,
                                routeDurationMinutes = route.durationMinutes,
                                routeDurationFormatted = route.formattedEta,
                                routeEstimatedFuelLitres = fuelEst.first,
                                routeEstimatedFuelCost = fuelEst.second,
                                isCalculatingRoute = false,
                                error = null
                            )
                        }
                    }
                    is GoogleRoutesStatus.Unavailable -> {
                        // Fallback straight-line baseline if API is rate-limited or offline
                        val straightKm = haversineKm(current.startLat, current.startLng, current.destLat, current.destLng) * 1.25
                        val dist = (straightKm * 10.0).roundToInt() / 10.0
                        val durMin = (dist / 55.0 * 60.0).roundToInt().coerceAtLeast(5)
                        val formattedEta = if (durMin < 60) "$durMin min" else "${durMin / 60}h ${durMin % 60}m"
                        val fuelEst = calculateFuel(current.fuelType, dist)
                        _state.update {
                            it.copy(
                                routePolyline = listOf(LatLng(current.startLat, current.startLng), LatLng(current.destLat, current.destLng)),
                                routeDistanceKm = dist,
                                routeDurationMinutes = durMin,
                                routeDurationFormatted = formattedEta,
                                routeEstimatedFuelLitres = fuelEst.first,
                                routeEstimatedFuelCost = fuelEst.second,
                                isCalculatingRoute = false
                            )
                        }
                    }
                    else -> {
                        _state.update { it.copy(isCalculatingRoute = false) }
                    }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isCalculatingRoute = false) }
            }
        }
    }

    private fun calculateFuel(fuelType: String, distanceKm: Double): Pair<Double, Double> {
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
        return litres to cost
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

        if (!current.departureDateTime.isAfter(LocalDateTime.now().minusMinutes(2))) {
            _state.update { it.copy(error = "Departure date and time cannot be in the past") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                val formattedDep = current.departureDateTime.format(DateTimeFormatter.ofPattern("EEE, dd MMM • HH:mm", Locale.ENGLISH))
                val title = "${current.destLocationName.trim()} Trip ($formattedDep)"
                val req = CreateTripRequest(
                    startLocationName = current.startLocationName.ifBlank { "Start Point" },
                    startLat = current.startLat,
                    startLng = current.startLng,
                    destLocationName = current.destLocationName.trim(),
                    destLat = current.destLat,
                    destLng = current.destLng,
                    title = title,
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
}
