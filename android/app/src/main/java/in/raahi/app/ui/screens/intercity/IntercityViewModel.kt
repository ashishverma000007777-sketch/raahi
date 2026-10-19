package `in`.raahi.app.ui.screens.intercity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.CityCatalogue
import `in`.raahi.app.data.IntercityTripRepository
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchTripsUiState {
    data object Idle : SearchTripsUiState
    data object Loading : SearchTripsUiState
    data class Success(val trips: List<IntercityTripDto>) : SearchTripsUiState
    data class Error(val message: String) : SearchTripsUiState
}

sealed interface TripDetailUiState {
    data object Loading : TripDetailUiState
    data class Success(
        val trip: IntercityTripDto,
        val applications: List<IntercityTripApplicationDto> = emptyList(),
        val currentUserId: String? = null,
        val currentUserRole: String? = null,
    ) : TripDetailUiState
    data class Error(val message: String) : TripDetailUiState
}

sealed interface AgentDashboardUiState {
    data object Loading : AgentDashboardUiState
    data class Success(val trips: List<IntercityTripDto>) : AgentDashboardUiState
    data class Error(val message: String) : AgentDashboardUiState
}

sealed interface DriverTripsUiState {
    data object Loading : DriverTripsUiState
    data class Success(val trips: List<IntercityTripDto>) : DriverTripsUiState
    data class Error(val message: String) : DriverTripsUiState
}

sealed interface MarketplaceLandingUiState {
    data object Loading : MarketplaceLandingUiState
    data class Success(
        val user: UserDto?,
        val activeTrip: IntercityTripDto?,
        val role: String,
        val onlineStatus: MarketplaceStatusDto? = null,
        val publishedTrips: List<IntercityTripDto> = emptyList(),
    ) : MarketplaceLandingUiState
    data class Error(val message: String) : MarketplaceLandingUiState
}

@HiltViewModel
class IntercityViewModel @Inject constructor(
    private val repository: IntercityTripRepository,
    private val authRepository: AuthRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _landingState = MutableStateFlow<MarketplaceLandingUiState>(MarketplaceLandingUiState.Loading)
    val landingState: StateFlow<MarketplaceLandingUiState> = _landingState.asStateFlow()

    private val _searchState = MutableStateFlow<SearchTripsUiState>(SearchTripsUiState.Idle)
    val searchState: StateFlow<SearchTripsUiState> = _searchState.asStateFlow()

    private val _detailState = MutableStateFlow<TripDetailUiState>(TripDetailUiState.Loading)
    val detailState: StateFlow<TripDetailUiState> = _detailState.asStateFlow()

    private val _agentDashboardState = MutableStateFlow<AgentDashboardUiState>(AgentDashboardUiState.Loading)
    val agentDashboardState: StateFlow<AgentDashboardUiState> = _agentDashboardState.asStateFlow()

    private val _driverTripsState = MutableStateFlow<DriverTripsUiState>(DriverTripsUiState.Loading)
    val driverTripsState: StateFlow<DriverTripsUiState> = _driverTripsState.asStateFlow()

    private val _driverTrips = MutableStateFlow<List<IntercityTripDto>>(emptyList())
    val driverTrips: StateFlow<List<IntercityTripDto>> = _driverTrips.asStateFlow()

    private val _marketplaceOnline = MutableStateFlow(false)
    val marketplaceOnline: StateFlow<Boolean> = _marketplaceOnline.asStateFlow()

    private val _preferredPickupCity = MutableStateFlow<String?>(null)
    val preferredPickupCity: StateFlow<String?> = _preferredPickupCity.asStateFlow()

    private val _isUpdatingStatus = MutableStateFlow(false)
    val isUpdatingStatus: StateFlow<Boolean> = _isUpdatingStatus.asStateFlow()

    private val _actionLoading = MutableStateFlow(false)
    val actionLoading: StateFlow<Boolean> = _actionLoading.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _actionSuccess = MutableStateFlow<String?>(null)
    val actionSuccess: StateFlow<String?> = _actionSuccess.asStateFlow()

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    fun loadCurrentUser() {
        viewModelScope.launch {
            val user = runCatching { authRepository.currentUser() }.getOrNull()
            _currentUser.value = user
        }
    }

    fun clearActionStatus() {
        _actionError.value = null
        _actionSuccess.value = null
    }

    fun searchTrips(pickupCity: String?, destCity: String?, vehicleCategory: String?) {
        _searchState.value = SearchTripsUiState.Loading
        viewModelScope.launch {
            try {
                val trips = repository.searchTrips(
                    pickupCity = pickupCity?.takeIf { it.isNotBlank() },
                    destCity = destCity?.takeIf { it.isNotBlank() },
                    vehicleCategory = vehicleCategory?.takeIf { it != "ALL" && it.isNotBlank() },
                )
                _searchState.value = SearchTripsUiState.Success(trips)
            } catch (e: Exception) {
                _searchState.value = SearchTripsUiState.Error(
                    e.toUserFriendlyMessage("Could not load intercity trips")
                )
            }
        }
    }

    fun loadTripDetails(tripId: String) {
        _detailState.value = TripDetailUiState.Loading
        viewModelScope.launch {
            try {
                val user = runCatching { authRepository.currentUser() }.getOrNull()
                val trip = repository.getTripDetails(tripId)
                val apps = if (trip.agentId == user?.id) {
                    runCatching { repository.getTripApplications(tripId) }.getOrDefault(emptyList())
                } else emptyList()

                _detailState.value = TripDetailUiState.Success(
                    trip = trip,
                    applications = apps,
                    currentUserId = user?.id,
                    currentUserRole = user?.role,
                )
            } catch (e: Exception) {
                _detailState.value = TripDetailUiState.Error(
                    e.toUserFriendlyMessage("Could not load trip details")
                )
            }
        }
    }

    fun loadAgentTrips() {
        _agentDashboardState.value = AgentDashboardUiState.Loading
        viewModelScope.launch {
            try {
                val trips = repository.getAgentTrips()
                _agentDashboardState.value = AgentDashboardUiState.Success(trips)
            } catch (e: Exception) {
                _agentDashboardState.value = AgentDashboardUiState.Error(
                    e.toUserFriendlyMessage("Could not load agent trips")
                )
            }
        }
    }

    fun loadLandingData(pickupCity: String? = null) {
        _landingState.value = MarketplaceLandingUiState.Loading
        viewModelScope.launch {
            try {
                val user = runCatching { authRepository.currentUser() }.getOrNull()
                if (user != null) _currentUser.value = user
                val active = runCatching { repository.getActiveTrip() }.getOrNull()
                val role = user?.role ?: "DRIVER"
                val onlineStatus = runCatching { repository.getMarketplaceStatus() }.getOrNull()
                if (onlineStatus != null) {
                    _marketplaceOnline.value = onlineStatus.online
                    _preferredPickupCity.value = onlineStatus.pickupCity
                }
                val cleanCity = pickupCity?.trim()?.takeIf { it.isNotBlank() } ?: _preferredPickupCity.value
                val trips = runCatching {
                    repository.searchTrips(pickupCity = cleanCity)
                }.getOrDefault(emptyList())

                _landingState.value = MarketplaceLandingUiState.Success(
                    user = user,
                    activeTrip = active,
                    role = role,
                    onlineStatus = onlineStatus,
                    publishedTrips = trips,
                )
            } catch (e: Exception) {
                _landingState.value = MarketplaceLandingUiState.Error(
                    e.toUserFriendlyMessage("Could not load marketplace")
                )
            }
        }
    }

    fun searchMarketplaceTrips(pickupCity: String?) {
        val cleanPickup = pickupCity?.trim()?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            try {
                val trips = repository.searchTrips(pickupCity = cleanPickup)
                val current = _landingState.value
                if (current is MarketplaceLandingUiState.Success) {
                    _landingState.value = current.copy(publishedTrips = trips)
                }
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Could not search trips")
            }
        }
    }

    fun setMarketplaceStatus(online: Boolean, pickupCity: String?) {
        if (_isUpdatingStatus.value) return
        _isUpdatingStatus.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                val canonical = if (pickupCity.isNullOrBlank() || pickupCity.equals("All", ignoreCase = true) || pickupCity.equals("All Cities", ignoreCase = true)) {
                    null
                } else {
                    CityCatalogue.canonicalName(pickupCity) ?: pickupCity.trim()
                }
                val updated = repository.updateMarketplaceStatus(online, canonical)
                _marketplaceOnline.value = updated.online
                _preferredPickupCity.value = updated.pickupCity

                val refreshedTrips = runCatching {
                    repository.searchTrips(pickupCity = updated.pickupCity)
                }.getOrDefault(emptyList())

                val current = _landingState.value
                if (current is MarketplaceLandingUiState.Success) {
                    _landingState.value = current.copy(
                        onlineStatus = updated,
                        publishedTrips = refreshedTrips,
                    )
                }
                _actionSuccess.value = if (updated.online) {
                    "You are now ONLINE for trip alerts${if (!canonical.isNullOrBlank()) " in $canonical" else ""}!"
                } else {
                    "Trip alerts set to OFFLINE."
                }
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Could not update trip alerts status")
            } finally {
                _isUpdatingStatus.value = false
            }
        }
    }

    /**
     * Atomically validates, persists, and synchronizes the preferred pickup hub city.
     * Prevents duplicate in-flight requests, preserves previous state on failure,
     * updates online matching preferences, and refreshes marketplace published trips.
     */
    fun updatePreferredCity(
        city: String?,
        onSuccess: (String?) -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        if (_isUpdatingStatus.value) return // Prevent duplicate Save requests

        val isAllCities = city.isNullOrBlank() || city.equals("All", ignoreCase = true) || city.equals("All Cities", ignoreCase = true)
        val canonical = if (isAllCities) null else {
            val resolved = CityCatalogue.canonicalName(city)
            if (resolved == null) {
                val err = "Selected city '$city' is not currently supported in the intercity network."
                _actionError.value = err
                onError(err)
                return
            }
            resolved
        }

        _isUpdatingStatus.value = true
        _actionError.value = null
        val online = _marketplaceOnline.value

        viewModelScope.launch {
            try {
                val updated = repository.updateMarketplaceStatus(online, canonical)
                _marketplaceOnline.value = updated.online
                _preferredPickupCity.value = updated.pickupCity

                // Refresh marketplace published trips for the updated preferred hub city
                val refreshedTrips = runCatching {
                    repository.searchTrips(pickupCity = updated.pickupCity)
                }.getOrDefault(emptyList())

                val current = _landingState.value
                if (current is MarketplaceLandingUiState.Success) {
                    _landingState.value = current.copy(
                        onlineStatus = updated,
                        publishedTrips = refreshedTrips,
                    )
                }

                _actionSuccess.value = "Preferred hub set to ${updated.pickupCity ?: "All Cities"}"
                onSuccess(updated.pickupCity)
            } catch (e: Exception) {
                val msg = e.toUserFriendlyMessage("Could not update preferred hub city")
                _actionError.value = msg
                onError(msg)
                // Retain previously saved city by leaving _preferredPickupCity unchanged
            } finally {
                _isUpdatingStatus.value = false
            }
        }
    }

    fun loadDriverTrips() {
        _driverTripsState.value = DriverTripsUiState.Loading
        viewModelScope.launch {
            try {
                val trips = repository.getDriverTrips()
                _driverTrips.value = trips
                _driverTripsState.value = DriverTripsUiState.Success(trips)
            } catch (e: Exception) {
                _driverTrips.value = emptyList()
                _driverTripsState.value = DriverTripsUiState.Error(
                    e.toUserFriendlyMessage("Could not load driver trips")
                )
            }
        }
    }

    fun createTrip(
        req: CreateIntercityTripRequest,
        onSuccess: (IntercityTripDto) -> Unit,
    ) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                val trip = repository.createTrip(req)
                _actionSuccess.value = "Trip published successfully!"
                onSuccess(trip)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to create trip")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun applyForTrip(tripId: String, notes: String?) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.applyForTrip(tripId, notes)
                _actionSuccess.value = "Application submitted successfully! The travel agent will review your profile."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to apply for trip")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun selectDriver(tripId: String, driverId: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.selectDriver(tripId, driverId)
                _actionSuccess.value = "Driver confirmed! Pickup OTP has been generated."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to assign driver")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun arriveAtPickup(tripId: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.arriveAtPickup(tripId)
                _actionSuccess.value = "Arrival marked! Ask passenger/agent for the 6-digit pickup OTP."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to mark arrival")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun verifyPickupOtp(tripId: String, otp: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.verifyPickupOtp(tripId, otp)
                _actionSuccess.value = "Pickup confirmed! Trip is now IN PROGRESS. Drive safely!"
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Invalid OTP. Please verify with agent.")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun verifyCompletionOtp(tripId: String, otp: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.verifyCompletionOtp(tripId, otp)
                _actionSuccess.value = "Destination verified! Trip successfully completed."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Invalid completion OTP. Please verify with passenger/agent.")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun cancelTrip(
        tripId: String,
        reason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        if (_actionLoading.value) return // Prevent duplicate requests
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.cancelTrip(tripId, reason.trim().takeIf { it.isNotBlank() })
                _actionSuccess.value = "Trip cancelled."
                loadTripDetails(tripId)
                runCatching { loadDriverTrips() }
                runCatching { loadLandingData() }
                onSuccess()
            } catch (e: Exception) {
                val msg = e.toUserFriendlyMessage("Failed to cancel trip")
                _actionError.value = msg
                onError(msg)
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun settlePayment(tripId: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.settlePayment(tripId)
                _actionSuccess.value = "Payment marked as settled."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to settle payment")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun arriveAtDestination(tripId: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                val updated = repository.arriveAtDestination(tripId)
                if (updated.rideType == "OWNER" || updated.status == "COMPLETED") {
                    _actionSuccess.value = "Arrived at destination! Trip completed successfully."
                } else {
                    _actionSuccess.value = "Arrived at destination! Awaiting commission settlement."
                }
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to mark destination arrival")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun completeOwnerTrip(tripId: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.completeOwnerTrip(tripId)
                _actionSuccess.value = "Trip marked completed!"
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to complete trip")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun submitSettlement(tripId: String, settlementRef: String, notes: String? = null) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.submitSettlement(tripId, settlementRef, notes)
                _actionSuccess.value = "Settlement reference submitted! Waiting for agent confirmation."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to submit settlement")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun confirmSettlement(tripId: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.confirmSettlement(tripId)
                _actionSuccess.value = "Settlement confirmed! Completion OTP generated."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to confirm settlement")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun raiseDispute(tripId: String, reason: String, evidence: String? = null) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.raiseDispute(tripId, reason, evidence)
                _actionSuccess.value = "Dispute raised. Support has been notified."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to raise dispute")
            } finally {
                _actionLoading.value = false
            }
        }
    }

    fun resolveDispute(tripId: String, resolution: String, action: String) {
        _actionLoading.value = true
        _actionError.value = null
        viewModelScope.launch {
            try {
                repository.resolveDispute(tripId, resolution, action)
                _actionSuccess.value = "Dispute resolved."
                loadTripDetails(tripId)
            } catch (e: Exception) {
                _actionError.value = e.toUserFriendlyMessage("Failed to resolve dispute")
            } finally {
                _actionLoading.value = false
            }
        }
    }
}
