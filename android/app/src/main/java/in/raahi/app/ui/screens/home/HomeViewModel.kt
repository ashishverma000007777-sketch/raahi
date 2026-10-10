package `in`.raahi.app.ui.screens.home

import android.content.Context
import android.location.Geocoder
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.raahi.app.BuildConfig
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.HomeRepository
import `in`.raahi.app.data.JobsRepository
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.data.MechanicsRepository
import `in`.raahi.app.data.HelperRepository
import `in`.raahi.app.data.VehicleRepository
import `in`.raahi.app.network.AlertDto
import `in`.raahi.app.network.AiStatusDto
import `in`.raahi.app.network.CarHealthDto
import `in`.raahi.app.network.FuelRateDto
import `in`.raahi.app.network.FuelSummaryDto
import `in`.raahi.app.network.HomeSummaryDto
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.network.HelperStatusDto
import `in`.raahi.app.network.RaahiWebSocketClient
import `in`.raahi.app.network.UserDto
import `in`.raahi.app.network.VehicleDto
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import `in`.raahi.app.network.toUserFriendlyMessage
import javax.inject.Inject

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Loaded(
        val user: UserDto?,
        val activeJob: JobDto?,
        val activeTrip: `in`.raahi.app.network.TripDto? = null,
        // Real vehicle + computed Car Health, or null if "Set up your car" hasn't been done
        // yet (or the calls simply failed) — either way, never fabricated, and never allowed
        // to block the rest of Home from loading (see the two independent runCatching below).
        val vehicle: VehicleDto?,
        val carHealth: CarHealthDto?,
        val offlineError: String? = null,
        val isRetrying: Boolean = false,
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

/** One independently-loaded data source. Failed is NOT the same as "zero" — the UI shows a dash. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String?) : Load<Nothing>
}

sealed interface NearbyMechanics {
    data object Loading : NearbyMechanics
    data object NoPermission : NearbyMechanics
    data object NoLocation : NearbyMechanics
    data object Failed : NearbyMechanics
    /** [total] is the real /mechanics/nearby result size (the backend caps it at 20). */
    data class Found(val total: Int, val available: Int) : NearbyMechanics
}

/** The backend's fuel rate row for the user's actual state (from GPS reverse-geocoding). */
data class FuelPrice(val rate: FuelRateDto)

data class HomeMetrics(
    val summary: Load<HomeSummaryDto> = Load.Loading,
    val fuel: Load<FuelSummaryDto> = Load.Loading,
    val ai: Load<AiStatusDto> = Load.Loading,
    val mechanics: NearbyMechanics = NearbyMechanics.Loading,
    val fuelPrice: FuelPrice? = null,
    val roadAlerts: Load<List<AlertDto>> = Load.Loading,
)

private val ACTIVE_JOB_STATUSES = setOf("PENDING", "MATCHED", "ARRIVED", "IN_PROGRESS", "WORK_DONE")

// Geocoder admin-area names that differ from the backend's fuel_rates.state keys.
// This is a name-matching table, not data: prices always come from /fuel-rates.
private val STATE_ALIASES = mapOf("uttar pradesh" to "up", "nct of delhi" to "delhi", "national capital territory of delhi" to "delhi")

private fun normState(name: String): String = name.trim().lowercase(Locale.ENGLISH).let { STATE_ALIASES[it] ?: it }

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val jobsRepository: JobsRepository,
    private val vehicleRepository: VehicleRepository,
    private val homeRepository: HomeRepository,
    private val mechanicsRepository: MechanicsRepository,
    private val locationProvider: LocationProvider,
    private val helperRepository: HelperRepository,
    private val webSocketClient: RaahiWebSocketClient,
    private val tripRepository: `in`.raahi.app.data.TripRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _metrics = MutableStateFlow(HomeMetrics())
    val metrics: StateFlow<HomeMetrics> = _metrics.asStateFlow()

    private val _selectedCity = MutableStateFlow(locationProvider.selectedCityName)

    private val _helperStatus = MutableStateFlow<HelperStatusDto?>(null)
    val helperStatus: StateFlow<HelperStatusDto?> = _helperStatus.asStateFlow()
    private val _helperBusy = MutableStateFlow(false)
    val helperBusy: StateFlow<Boolean> = _helperBusy.asStateFlow()
    private val _helperError = MutableStateFlow<String?>(null)
    val helperError: StateFlow<String?> = _helperError.asStateFlow()

    private val _walletBalance = MutableStateFlow<Double?>(null)
    val walletBalance: StateFlow<Double?> = _walletBalance.asStateFlow()

    private val _incomingRequest = MutableStateFlow<JobDto?>(null)
    val incomingRequest: StateFlow<JobDto?> = _incomingRequest.asStateFlow()
    private var incomingPollingJob: Job? = null
    private val seenIncomingRequestIds = mutableSetOf<String>()
    private var incomingRequestsSeeded = false

    fun dismissIncomingRequest() {
        _incomingRequest.value = null
    }

    private fun syncIncomingRequestPolling(status: `in`.raahi.app.network.HelperStatusDto) {
        if (status.applicationStatus != "APPROVED" || !status.online ||
            !status.accountActive || status.blockedUntil != null) {
            incomingPollingJob?.cancel()
            incomingPollingJob = null
            return
        }
        if (incomingPollingJob?.isActive == true) return

        incomingPollingJob = viewModelScope.launch {
            while (true) {
                try {
                    val fix = locationProvider.getCurrentLocation()
                    if (fix != null) {
                        val requests = jobsRepository.availableJobs(fix.lat, fix.lng)
                            .filter { it.status == "PENDING" }
                        val ids = requests.map { it.id }.toSet()

                        if (!incomingRequestsSeeded) {
                            seenIncomingRequestIds.addAll(ids)
                            incomingRequestsSeeded = true
                        } else {
                            val newRequest = requests.firstOrNull { it.id !in seenIncomingRequestIds }
                            seenIncomingRequestIds.addAll(ids)
                            if (newRequest != null) {
                                _incomingRequest.value = newRequest
                                playIncomingRequestSound()
                            }
                            if (seenIncomingRequestIds.size > 500) {
                                seenIncomingRequestIds.clear()
                                seenIncomingRequestIds.addAll(ids)
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Keep polling after a temporary network failure.
                }
                delay(15_000)
            }
        }
    }

    private fun playIncomingRequestSound() {
        viewModelScope.launch {
            val tone = runCatching {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            }.getOrNull() ?: return@launch

            try {
                repeat(3) {
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
                    delay(260)
                }
            } finally {
                tone.release()
            }
        }
    }

    fun refreshHelperStatus() {
        if (!authRepository.hasAuthToken()) return
        viewModelScope.launch {
            runCatching { helperRepository.status() }
                .onSuccess {
                    _helperStatus.value = it
                    _walletBalance.value = it.commissionBalance
                    _helperError.value = null
                    syncIncomingRequestPolling(it)
                }
                .onFailure { _helperError.value = it.toUserFriendlyMessage("Unable to load helper status") }
        }
    }

    fun setHelperOnline(online: Boolean, hasLocationPermission: Boolean) {
        val current = _helperStatus.value
        if (current == null) {
            _helperError.value = "Helper status is still loading. Please retry."
            return
        }
        if (current.applicationStatus != "APPROVED") {
            _helperError.value = "Your helper application must be approved first."
            return
        }
        if (online && !hasLocationPermission) {
            _helperError.value = "Enable location permission to go online."
            return
        }
        if (online && (!current.accountActive || current.blockedUntil != null)) {
            _helperError.value = "Your helper account is not currently allowed online."
            return
        }
        _helperBusy.value = true
        _helperError.value = null
        viewModelScope.launch {
            try {
                val fix = if (online) locationProvider.getCurrentLocation()
                    ?: throw IllegalStateException("Turn on device location and try again")
                else null
                helperRepository.setOnline(online, fix?.lat, fix?.lng)
                _helperStatus.value = helperRepository.status()
            } catch (e: Exception) {
                _helperError.value = e.toUserFriendlyMessage("Could not update online status")
            } finally {
                _helperBusy.value = false
            }
        }
    }
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    fun selectCity(cityName: String, lat: Double, lng: Double) {
        locationProvider.setManualLocation(cityName, lat, lng)
        _selectedCity.value = cityName
        viewModelScope.launch {
            refreshLocationBased(hasLocationPermission = true)
        }
    }

    fun useCurrentLocation(hasPermission: Boolean, onResult: (Boolean, String?) -> Unit) {
        if (!hasPermission) {
            onResult(false, "Location permission not granted")
            return
        }
        viewModelScope.launch {
            val here = locationProvider.getFreshGpsLocation()
            if (here != null) {
                val detectedName = withContext(Dispatchers.IO) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        val addresses = Geocoder(context, Locale.ENGLISH).getFromLocation(here.lat, here.lng, 1)
                        val addr = addresses?.firstOrNull()
                        addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: "My Location"
                    }.getOrDefault("My Location")
                }
                locationProvider.setGpsLocation(detectedName, here.lat, here.lng)
                _selectedCity.value = detectedName
                refreshLocationBased(hasLocationPermission = true)
                onResult(true, detectedName)
            } else {
                onResult(false, "Could not acquire GPS fix")
            }
        }
    }

    init {
        load()
        refreshHelperStatus()
        // Home is the first screen reached only once actually authenticated, so this is
        // where the app-wide WebSocket connection starts; RaahiWebSocketClient is a Hilt
        // singleton, so it stays connected across every other screen for the process
        // lifetime — no per-screen connect/disconnect needed elsewhere.
        viewModelScope.launch {
            runCatching { webSocketClient.connect(BuildConfig.BASE_URL) }
        }
        registerFcmToken()
    }

    private fun registerFcmToken() {
        // Covers the normal case (token already exists by the time the user signs in);
        // RaahiFirebaseMessagingService.onNewToken covers the rotation case afterward.
        viewModelScope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                authRepository.registerFcmToken(token)
            }
        }
    }

    fun load() {
        val currentLoaded = _state.value as? HomeUiState.Loaded
        if (currentLoaded != null) {
            _state.value = currentLoaded.copy(isRetrying = true)
        } else {
            _state.value = HomeUiState.Loading
        }
        viewModelScope.launch {
            if (!authRepository.hasAuthToken()) {
                _state.value = HomeUiState.Loaded(
                    user = null,
                    activeJob = null,
                    activeTrip = null,
                    vehicle = null,
                    carHealth = null,
                    offlineError = null,
                    isRetrying = false,
                )
                return@launch
            }

            // Safe parallel fetching of independent startup resources
            val userDeferred = async { runCatching { authRepository.currentUser() } }
            val jobsDeferred = async { runCatching { jobsRepository.myJobs() } }
            val tripDeferred = async { runCatching { tripRepository.getActiveTrip() } }
            val vehicleDeferred = async { runCatching { vehicleRepository.myVehicle() } }

            val userResult = userDeferred.await()
            val user = userResult.getOrNull()
            val activeJob = jobsDeferred.await()
                .getOrDefault(emptyList())
                .firstOrNull { it.status in ACTIVE_JOB_STATUSES }
            val activeTrip = tripDeferred.await().getOrNull()
            val vehicle = vehicleDeferred.await().getOrNull()
            val carHealth = if (vehicle != null) runCatching { vehicleRepository.carHealth() }.getOrNull() else null

            val offlineError = if (user == null) {
                userResult.exceptionOrNull()?.toUserFriendlyMessage("Unable to reach Raahi servers. You appear to be offline.")
            } else null

            _state.value = HomeUiState.Loaded(
                user = user,
                activeJob = activeJob,
                activeTrip = activeTrip,
                vehicle = vehicle,
                carHealth = carHealth,
                offlineError = offlineError,
                isRetrying = false,
            )
        }
    }

    private var lastMetricsRefreshTime = 0L

    /**
     * Loads every per-user metric from its real source. Each source is fetched independently
     * so one failure only affects its own tile. Called on ON_RESUME with a 15-second throttle
     * to avoid duplicate requests when navigating back and forth.
     */
    fun refreshMetrics(hasLocationPermission: Boolean, force: Boolean = false) {
        if (!authRepository.hasAuthToken()) return
        val now = System.currentTimeMillis()
        if (!force && (now - lastMetricsRefreshTime < 15_000L)) {
            return
        }
        lastMetricsRefreshTime = now
        viewModelScope.launch { runLoad({ homeRepository.summary() }) { r -> _metrics.update { it.copy(summary = r) } } }
        viewModelScope.launch { runLoad({ homeRepository.fuelSummary() }) { r -> _metrics.update { it.copy(fuel = r) } } }
        viewModelScope.launch { runLoad({ homeRepository.aiStatus() }) { r -> _metrics.update { it.copy(ai = r) } } }
        viewModelScope.launch { runLoad({ homeRepository.roadAlerts() }) { r -> _metrics.update { it.copy(roadAlerts = r) } } }
        viewModelScope.launch { refreshLocationBased(hasLocationPermission) }
    }

    private suspend fun <T> runLoad(fetch: suspend () -> T, publish: (Load<T>) -> Unit) {
        publish(runCatching { fetch() }.fold({ Load.Ready(it) }, { Load.Failed(it.toUserFriendlyMessage()) }))
    }

    private suspend fun refreshLocationBased(hasLocationPermission: Boolean) {
        if (!hasLocationPermission) {
            _metrics.update { it.copy(mechanics = NearbyMechanics.NoPermission, fuelPrice = null) }
            return
        }
        val here = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
        if (here == null) {
            _metrics.update { it.copy(mechanics = NearbyMechanics.NoLocation, fuelPrice = null) }
            return
        }
        // Mechanics near the user's real GPS fix, straight from /mechanics/nearby.
        viewModelScope.launch {
            val result = runCatching { mechanicsRepository.nearby(here.lat, here.lng) }
                .fold({ list -> NearbyMechanics.Found(list.size, list.count { it.isAvailable }) }, { NearbyMechanics.Failed })
            _metrics.update { it.copy(mechanics = result) }
        }
        // Fuel price for the state the user is actually in: on-device reverse geocode -> match
        // against the backend's /fuel-rates rows. No match => no price shown (never a guess).
        viewModelScope.launch {
            val adminArea = withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale.ENGLISH).getFromLocation(here.lat, here.lng, 1)?.firstOrNull()?.adminArea
                }.getOrNull()
            }
            // Use only real /fuel-rates rows. Prefer the reverse-geocoded state;
            // if geocoding fails, try the selected city only when it names a state
            // (e.g. Punjab), never infer a price from an unrelated city name.
            val rates = runCatching { homeRepository.fuelRates() }.getOrNull().orEmpty()
            val selected = locationProvider.selectedCityName.trim()
            val stateNames = listOfNotNull(adminArea?.trim()?.takeIf { it.isNotBlank() },
                selected.takeIf { candidate -> rates.any { normState(it.state) == normState(candidate) } })
            val price = stateNames.asSequence()
                .flatMap { name -> rates.asSequence().filter { normState(it.state) == normState(name) } }
                .firstOrNull()
                ?.let { FuelPrice(it) }
            _metrics.update { it.copy(fuelPrice = price) }
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    private data class Five<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
