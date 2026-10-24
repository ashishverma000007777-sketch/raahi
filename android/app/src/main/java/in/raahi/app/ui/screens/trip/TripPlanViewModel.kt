package `in`.raahi.app.ui.screens.trip

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.DailyRepository
import `in`.raahi.app.data.GoogleRouteData
import `in`.raahi.app.data.GoogleRoutesProvider
import `in`.raahi.app.data.GoogleRoutesStatus
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.data.LocationResolver
import `in`.raahi.app.data.TripRepository
import `in`.raahi.app.data.VehicleRepository
import `in`.raahi.app.network.CreateTripRequest
import `in`.raahi.app.network.PlaceDto
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

data class FuelStationStop(
    val name: String,
    val lat: Double,
    val lng: Double,
    val distanceAlongRouteKm: Double,
    val detourKm: Double,
)

data class SmartBreakStop(
    val id: String,
    val name: String,
    val type: String, // "dhaba", "toilet", "parking"
    val typeLabel: String, // "Highway Dhaba / Food", "Restroom / Washroom", "Highway Rest Stop"
    val lat: Double,
    val lng: Double,
    val distanceAlongRouteKm: Double,
    val detourKm: Double,
)

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

    // Feature 1: Trip Budget Calculator
    val isRoundTrip: Boolean = false,
    val editableMileage: Double = 14.5,
    val editableFuelPrice: Double = 96.72,
    val routeDistanceKm: Double? = null,
    val routeDurationMinutes: Int? = null,
    val routeDurationFormatted: String? = null,
    val routePolyline: List<LatLng> = emptyList(),
    val estimatedFuelLitres: Double? = null,
    val estimatedFuelCost: Double? = null,
    val tollCost: Double? = null,
    val tollAvailable: Boolean = false,
    val tollUnavailableReason: String? = null,
    val estimatedTotalCost: Double? = null,
    val costPerKm: Double? = null,

    // Feature 2: Fuel Range & Stop Planner
    val remainingFuelLitres: Double = 12.0,
    val remainingFuelRangeKm: Double = 174.0,
    val needsRefuel: Boolean = false,
    val refuelAdviceMessage: String? = null,
    val recommendedFuelStations: List<FuelStationStop> = emptyList(),
    val isLoadingFuelStations: Boolean = false,

    // Feature 3: Alternative Route Comparison
    val alternativeRoutes: List<GoogleRouteData> = emptyList(),
    val selectedRouteIndex: Int = 0,

    // Feature 4: Smart Break Suggestions
    val smartBreaks: List<SmartBreakStop> = emptyList(),
    val isLoadingBreaks: Boolean = false,

    // Status flags
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
    private val dailyRepository: DailyRepository,
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
    private var stopsJob: Job? = null

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
                    val defaultMileage = defaultMileageForFuel(vehicle.fuelType)
                    val defaultPrice = defaultPriceForFuel(vehicle.fuelType)
                    _state.update {
                        it.copy(
                            vehicle = vehicle,
                            fuelType = vehicle.fuelType,
                            currentOdometer = vehicle.odometerKm.takeIf { km -> km > 0 }?.toString() ?: "",
                            editableMileage = defaultMileage,
                            editableFuelPrice = defaultPrice,
                            remainingFuelRangeKm = (it.remainingFuelLitres * defaultMileage * 10.0).roundToInt() / 10.0
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

    fun onRoundTripToggled(enabled: Boolean) {
        val current = _state.value
        val budget = recalculateBudget(
            distanceKm = current.routeDistanceKm,
            isRoundTrip = enabled,
            mileage = current.editableMileage,
            fuelPrice = current.editableFuelPrice,
            tollCost = current.tollCost,
            tollAvailable = current.tollAvailable
        )
        _state.update {
            it.copy(
                isRoundTrip = enabled,
                estimatedFuelLitres = budget.litres,
                estimatedFuelCost = budget.fuelCost,
                tollCost = budget.tollCost,
                estimatedTotalCost = budget.totalCost,
                costPerKm = budget.costPerKm
            )
        }
    }

    fun onMileageChanged(mileage: Double) {
        if (mileage <= 0.0) return
        val current = _state.value
        val budget = recalculateBudget(
            distanceKm = current.routeDistanceKm,
            isRoundTrip = current.isRoundTrip,
            mileage = mileage,
            fuelPrice = current.editableFuelPrice,
            tollCost = current.tollCost,
            tollAvailable = current.tollAvailable
        )
        val fuelRange = recalculateFuelRange(
            remainingLitres = current.remainingFuelLitres,
            mileage = mileage,
            routeDistanceKm = current.routeDistanceKm
        )
        _state.update {
            it.copy(
                editableMileage = mileage,
                estimatedFuelLitres = budget.litres,
                estimatedFuelCost = budget.fuelCost,
                tollCost = budget.tollCost,
                estimatedTotalCost = budget.totalCost,
                costPerKm = budget.costPerKm,
                remainingFuelRangeKm = fuelRange.rangeKm,
                needsRefuel = fuelRange.needsRefuel,
                refuelAdviceMessage = fuelRange.advice
            )
        }
    }

    fun onFuelPriceChanged(price: Double) {
        if (price <= 0.0) return
        val current = _state.value
        val budget = recalculateBudget(
            distanceKm = current.routeDistanceKm,
            isRoundTrip = current.isRoundTrip,
            mileage = current.editableMileage,
            fuelPrice = price,
            tollCost = current.tollCost,
            tollAvailable = current.tollAvailable
        )
        _state.update {
            it.copy(
                editableFuelPrice = price,
                estimatedFuelCost = budget.fuelCost,
                estimatedTotalCost = budget.totalCost,
                costPerKm = budget.costPerKm
            )
        }
    }

    fun onRemainingFuelChanged(litres: Double) {
        if (litres < 0.0) return
        val current = _state.value
        val fuelRange = recalculateFuelRange(
            remainingLitres = litres,
            mileage = current.editableMileage,
            routeDistanceKm = current.routeDistanceKm
        )
        _state.update {
            it.copy(
                remainingFuelLitres = litres,
                remainingFuelRangeKm = fuelRange.rangeKm,
                needsRefuel = fuelRange.needsRefuel,
                refuelAdviceMessage = fuelRange.advice
            )
        }
    }

    fun selectRoute(index: Int) {
        val routes = _state.value.alternativeRoutes
        if (index !in routes.indices) return
        val selected = routes[index]
        val current = _state.value

        val budget = recalculateBudget(
            distanceKm = selected.distanceKm,
            isRoundTrip = current.isRoundTrip,
            mileage = current.editableMileage,
            fuelPrice = current.editableFuelPrice,
            tollCost = selected.tollCost,
            tollAvailable = selected.tollAvailable
        )
        val fuelRange = recalculateFuelRange(
            remainingLitres = current.remainingFuelLitres,
            mileage = current.editableMileage,
            routeDistanceKm = selected.distanceKm
        )

        _state.update {
            it.copy(
                selectedRouteIndex = index,
                routePolyline = selected.points,
                routeDistanceKm = selected.distanceKm,
                routeDurationMinutes = selected.durationMinutes,
                routeDurationFormatted = selected.formattedEta,
                estimatedFuelLitres = budget.litres,
                estimatedFuelCost = budget.fuelCost,
                tollCost = budget.tollCost,
                tollAvailable = selected.tollAvailable,
                tollUnavailableReason = selected.tollUnavailableReason,
                estimatedTotalCost = budget.totalCost,
                costPerKm = budget.costPerKm,
                remainingFuelRangeKm = fuelRange.rangeKm,
                needsRefuel = fuelRange.needsRefuel,
                refuelAdviceMessage = fuelRange.advice
            )
        }

        fetchStopsAlongRoute(selected.points)
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
                        val routes = res.alternatives.ifEmpty { listOf(res.route) }
                        val activeRoute = routes.first()

                        val budget = recalculateBudget(
                            distanceKm = activeRoute.distanceKm,
                            isRoundTrip = current.isRoundTrip,
                            mileage = current.editableMileage,
                            fuelPrice = current.editableFuelPrice,
                            tollCost = activeRoute.tollCost,
                            tollAvailable = activeRoute.tollAvailable
                        )
                        val fuelRange = recalculateFuelRange(
                            remainingLitres = current.remainingFuelLitres,
                            mileage = current.editableMileage,
                            routeDistanceKm = activeRoute.distanceKm
                        )

                        _state.update {
                            it.copy(
                                alternativeRoutes = routes,
                                selectedRouteIndex = 0,
                                routePolyline = activeRoute.points,
                                routeDistanceKm = activeRoute.distanceKm,
                                routeDurationMinutes = activeRoute.durationMinutes,
                                routeDurationFormatted = activeRoute.formattedEta,
                                estimatedFuelLitres = budget.litres,
                                estimatedFuelCost = budget.fuelCost,
                                tollCost = budget.tollCost,
                                tollAvailable = activeRoute.tollAvailable,
                                tollUnavailableReason = activeRoute.tollUnavailableReason,
                                estimatedTotalCost = budget.totalCost,
                                costPerKm = budget.costPerKm,
                                remainingFuelRangeKm = fuelRange.rangeKm,
                                needsRefuel = fuelRange.needsRefuel,
                                refuelAdviceMessage = fuelRange.advice,
                                isCalculatingRoute = false,
                                error = null
                            )
                        }

                        fetchStopsAlongRoute(activeRoute.points)
                    }
                    is GoogleRoutesStatus.Unavailable -> {
                        // Fallback straight-line baseline if API is rate-limited or offline
                        val straightKm = haversineKm(current.startLat, current.startLng, current.destLat, current.destLng) * 1.25
                        val dist = (straightKm * 10.0).roundToInt() / 10.0
                        val durMin = (dist / 55.0 * 60.0).roundToInt().coerceAtLeast(5)
                        val formattedEta = if (durMin < 60) "$durMin min" else "${durMin / 60}h ${durMin % 60}m"

                        val budget = recalculateBudget(
                            distanceKm = dist,
                            isRoundTrip = current.isRoundTrip,
                            mileage = current.editableMileage,
                            fuelPrice = current.editableFuelPrice,
                            tollCost = null,
                            tollAvailable = false
                        )
                        val fuelRange = recalculateFuelRange(
                            remainingLitres = current.remainingFuelLitres,
                            mileage = current.editableMileage,
                            routeDistanceKm = dist
                        )

                        val fallbackRoute = GoogleRouteData(
                            points = listOf(LatLng(current.startLat, current.startLng), LatLng(current.destLat, current.destLng)),
                            distanceMeters = dist * 1000.0,
                            distanceKm = dist,
                            durationSeconds = durMin * 60.0,
                            durationMinutes = durMin,
                            formattedEta = formattedEta,
                            description = "Direct Corridor",
                            tollCost = null,
                            tollAvailable = false,
                            tollUnavailableReason = "Routes API offline; toll data unavailable",
                            routeLabel = "Direct Corridor"
                        )

                        _state.update {
                            it.copy(
                                alternativeRoutes = listOf(fallbackRoute),
                                selectedRouteIndex = 0,
                                routePolyline = fallbackRoute.points,
                                routeDistanceKm = dist,
                                routeDurationMinutes = durMin,
                                routeDurationFormatted = formattedEta,
                                estimatedFuelLitres = budget.litres,
                                estimatedFuelCost = budget.fuelCost,
                                tollCost = null,
                                tollAvailable = false,
                                tollUnavailableReason = "Routes API offline; toll data unavailable",
                                estimatedTotalCost = budget.totalCost,
                                costPerKm = budget.costPerKm,
                                remainingFuelRangeKm = fuelRange.rangeKm,
                                needsRefuel = fuelRange.needsRefuel,
                                refuelAdviceMessage = fuelRange.advice,
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

    private fun fetchStopsAlongRoute(polyline: List<LatLng>) {
        if (polyline.isEmpty()) return
        stopsJob?.cancel()
        stopsJob = viewModelScope.launch {
            _state.update { it.copy(isLoadingFuelStations = true, isLoadingBreaks = true) }
            try {
                // Sample points along route polyline (e.g. 25%, 50%, 75%)
                val samplePoints = if (polyline.size >= 4) {
                    listOf(
                        polyline[polyline.size / 4],
                        polyline[polyline.size / 2],
                        polyline[polyline.size * 3 / 4]
                    )
                } else {
                    polyline
                }

                val foundStations = mutableListOf<FuelStationStop>()
                val foundBreaks = mutableListOf<SmartBreakStop>()

                for (pt in samplePoints) {
                    // Fuel stations
                    try {
                        val fuelPlaces = dailyRepository.nearbyPlaces(pt.latitude, pt.longitude, "fuel")
                        for (p in fuelPlaces) {
                            val detour = calculateDetourKm(p.lat, p.lng, polyline)
                            val distAlong = calculateDistanceAlongRouteKm(p.lat, p.lng, polyline)
                            if (foundStations.none { it.name.equals(p.name, ignoreCase = true) }) {
                                foundStations.add(
                                    FuelStationStop(
                                        name = p.name,
                                        lat = p.lat,
                                        lng = p.lng,
                                        distanceAlongRouteKm = distAlong,
                                        detourKm = detour
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}

                    // Smart breaks: dhabas
                    try {
                        val dhabas = dailyRepository.nearbyPlaces(pt.latitude, pt.longitude, "dhaba")
                        for (p in dhabas) {
                            val detour = calculateDetourKm(p.lat, p.lng, polyline)
                            val distAlong = calculateDistanceAlongRouteKm(p.lat, p.lng, polyline)
                            if (foundBreaks.none { it.name.equals(p.name, ignoreCase = true) }) {
                                foundBreaks.add(
                                    SmartBreakStop(
                                        id = p.id,
                                        name = p.name,
                                        type = "dhaba",
                                        typeLabel = "Highway Dhaba / Food",
                                        lat = p.lat,
                                        lng = p.lng,
                                        distanceAlongRouteKm = distAlong,
                                        detourKm = detour
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}

                    // Smart breaks: toilets / washrooms
                    try {
                        val toilets = dailyRepository.nearbyPlaces(pt.latitude, pt.longitude, "toilet")
                        for (p in toilets) {
                            val detour = calculateDetourKm(p.lat, p.lng, polyline)
                            val distAlong = calculateDistanceAlongRouteKm(p.lat, p.lng, polyline)
                            if (foundBreaks.none { it.name.equals(p.name, ignoreCase = true) }) {
                                foundBreaks.add(
                                    SmartBreakStop(
                                        id = p.id,
                                        name = p.name,
                                        type = "toilet",
                                        typeLabel = "Restroom / Washroom",
                                        lat = p.lat,
                                        lng = p.lng,
                                        distanceAlongRouteKm = distAlong,
                                        detourKm = detour
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                val rankedStations = foundStations.sortedBy { it.detourKm }.take(4)
                val rankedBreaks = foundBreaks.sortedBy { it.detourKm }.take(6)

                _state.update {
                    it.copy(
                        recommendedFuelStations = rankedStations,
                        smartBreaks = rankedBreaks,
                        isLoadingFuelStations = false,
                        isLoadingBreaks = false
                    )
                }
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        recommendedFuelStations = emptyList(),
                        smartBreaks = emptyList(),
                        isLoadingFuelStations = false,
                        isLoadingBreaks = false
                    )
                }
            }
        }
    }

    private data class BudgetResult(
        val litres: Double?,
        val fuelCost: Double?,
        val tollCost: Double?,
        val totalCost: Double?,
        val costPerKm: Double?
    )

    private fun recalculateBudget(
        distanceKm: Double?,
        isRoundTrip: Boolean,
        mileage: Double,
        fuelPrice: Double,
        tollCost: Double?,
        tollAvailable: Boolean,
    ): BudgetResult {
        if (distanceKm == null || distanceKm <= 0.0) {
            return BudgetResult(null, null, null, null, null)
        }
        val effectiveDist = if (isRoundTrip) distanceKm * 2.0 else distanceKm
        val safeMileage = if (mileage > 0.0) mileage else 14.5
        val litres = ((effectiveDist / safeMileage) * 10.0).roundToInt() / 10.0
        val fuelCost = (litres * fuelPrice).roundToInt().toDouble()

        val effectiveToll = if (tollAvailable && tollCost != null) {
            if (isRoundTrip) ((tollCost * 2.0) * 100.0).roundToInt() / 100.0 else tollCost
        } else null

        val totalCost = fuelCost + (effectiveToll ?: 0.0)
        val costPerKm = ((totalCost / effectiveDist) * 100.0).roundToInt() / 100.0

        return BudgetResult(
            litres = litres,
            fuelCost = fuelCost,
            tollCost = effectiveToll,
            totalCost = totalCost,
            costPerKm = costPerKm
        )
    }

    private data class FuelRangeResult(
        val rangeKm: Double,
        val needsRefuel: Boolean,
        val advice: String
    )

    private fun recalculateFuelRange(
        remainingLitres: Double,
        mileage: Double,
        routeDistanceKm: Double?
    ): FuelRangeResult {
        val safeMileage = if (mileage > 0.0) mileage else 14.5
        val rangeKm = ((remainingLitres * safeMileage) * 10.0).roundToInt() / 10.0
        val tripDist = routeDistanceKm ?: 0.0
        val needsRefuel = tripDist > 0.0 && rangeKm < tripDist
        val advice = when {
            tripDist <= 0.0 -> "Enter destination to plan fuel range."
            needsRefuel -> "Refuel required: Estimated range of ${rangeKm.toInt()} km is less than trip distance (${tripDist.toInt()} km). Suggest refueling before ${((rangeKm * 0.85).toInt())} km."
            else -> "Fuel sufficient: Range of ${rangeKm.toInt()} km covers the one-way distance (${tripDist.toInt()} km)."
        }
        return FuelRangeResult(rangeKm, needsRefuel, advice)
    }

    private fun defaultMileageForFuel(fuelType: String): Double = when (fuelType.uppercase()) {
        "DIESEL" -> 18.0
        "CNG" -> 24.0
        "EV" -> 7.0
        else -> 14.5 // PETROL
    }

    private fun defaultPriceForFuel(fuelType: String): Double = when (fuelType.uppercase()) {
        "DIESEL" -> 89.62
        "CNG" -> 79.50
        "EV" -> 12.00
        else -> 96.72 // PETROL
    }

    private fun calculateDetourKm(lat: Double, lng: Double, polyline: List<LatLng>): Double {
        if (polyline.isEmpty()) return 0.0
        var minDist = Double.MAX_VALUE
        for (pt in polyline) {
            val dist = haversineKm(lat, lng, pt.latitude, pt.longitude)
            if (dist < minDist) {
                minDist = dist
            }
        }
        return (minDist * 10.0).roundToInt() / 10.0
    }

    private fun calculateDistanceAlongRouteKm(lat: Double, lng: Double, polyline: List<LatLng>): Double {
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
        var cumulativeKm = 0.0
        for (i in 0 until closestIdx) {
            cumulativeKm += haversineKm(polyline[i].latitude, polyline[i].longitude, polyline[i + 1].latitude, polyline[i + 1].longitude)
        }
        return (cumulativeKm * 10.0).roundToInt() / 10.0
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
                val preferredRouteLabel = current.alternativeRoutes.getOrNull(current.selectedRouteIndex)?.routeLabel ?: "Primary Highway"
                val req = CreateTripRequest(
                    startLocationName = current.startLocationName.ifBlank { "Start Point" },
                    startLat = current.startLat,
                    startLng = current.startLng,
                    destLocationName = current.destLocationName.trim(),
                    destLat = current.destLat,
                    destLng = current.destLng,
                    title = title,
                    fuelType = current.fuelType,
                    tankFull = !current.needsRefuel,
                    passengers = 1,
                    preferredRoute = preferredRouteLabel,
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
