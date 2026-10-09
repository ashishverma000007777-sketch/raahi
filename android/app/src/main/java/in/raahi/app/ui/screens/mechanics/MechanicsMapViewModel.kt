package `in`.raahi.app.ui.screens.mechanics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.LatLng
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.data.MechanicsRepository
import `in`.raahi.app.network.MechanicDto
import `in`.raahi.app.network.OsmMechanicShopDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MechanicsMapUiState {
    data object Loading : MechanicsMapUiState()
    data object LocationUnavailable : MechanicsMapUiState()
    data class Loaded(
        val userLocation: LatLng,
        val mechanics: List<MechanicDto>,
        val errorMessage: String? = null,
        val isRefreshing: Boolean = false,
        val shops: List<OsmMechanicShopDto> = emptyList(),
    ) : MechanicsMapUiState()
    data class Error(val message: String) : MechanicsMapUiState()
}

@HiltViewModel
class MechanicsMapViewModel @Inject constructor(
    private val locationProvider: LocationProvider,
    private val mechanicsRepository: MechanicsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MechanicsMapUiState>(MechanicsMapUiState.Loading)
    val state: StateFlow<MechanicsMapUiState> = _state.asStateFlow()

    private var lastLocation: LatLng? = null

    /** Called once location permission is confirmed granted, by the screen. */
    fun start() {
        if (!authRepository.hasAuthToken()) return
        val current = _state.value as? MechanicsMapUiState.Loaded
        if (current == null) {
            _state.value = MechanicsMapUiState.Loading
        }
        viewModelScope.launch {
            val location = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
            if (location == null) {
                if (lastLocation == null) {
                    _state.value = MechanicsMapUiState.LocationUnavailable
                    return@launch
                }
            } else {
                lastLocation = location
            }
            loadNearby(lastLocation ?: location ?: LatLng(28.6139, 77.2090))
        }
    }

    fun retry() {
        val loc = lastLocation
        if (loc != null) loadNearby(loc) else start()
    }

    private fun loadNearby(location: LatLng) {
        val current = _state.value as? MechanicsMapUiState.Loaded
        if (current != null) {
            _state.value = current.copy(isRefreshing = true)
        } else {
            _state.value = MechanicsMapUiState.Loading
        }
        viewModelScope.launch {
            val mechanicsResult = runCatching { mechanicsRepository.nearby(location.lat, location.lng) }
            val shopsResult = runCatching { mechanicsRepository.nearbyShops(location.lat, location.lng) }
            val mechanics = mechanicsResult.getOrDefault(emptyList())
            val shops = shopsResult.getOrDefault(emptyList())
            val error = when {
                mechanicsResult.isFailure && shopsResult.isFailure ->
                    mechanicsResult.exceptionOrNull()?.toUserFriendlyMessage(
                        "Nearby mechanics and repair shops unavailable. Please check your connection."
                    )
                shopsResult.isFailure ->
                    shopsResult.exceptionOrNull()?.toUserFriendlyMessage(
                        "Imported repair shops could not load. Please retry."
                    )
                mechanicsResult.isFailure ->
                    mechanicsResult.exceptionOrNull()?.toUserFriendlyMessage(
                        "Nearby mechanics unavailable. Imported repair shops may still be shown."
                    )
                else -> null
            }
            _state.value = MechanicsMapUiState.Loaded(
                userLocation = location, mechanics = mechanics, errorMessage = error,
                isRefreshing = false, shops = shops
            )
        }
    }
}
