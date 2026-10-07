package `in`.raahi.app.ui.screens.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.TripRepository
import `in`.raahi.app.network.TripDto
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

sealed interface TripOverviewUiState {
    data object Loading : TripOverviewUiState
    data class Success(val trip: TripDto) : TripOverviewUiState
    data class Error(val message: String) : TripOverviewUiState
}

sealed interface TripOverviewEvent {
    data class TripStarted(val tripId: String) : TripOverviewEvent
}

@HiltViewModel
class TripOverviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
) : ViewModel() {

    val tripId: String = checkNotNull(savedStateHandle["tripId"])

    private val _state = MutableStateFlow<TripOverviewUiState>(TripOverviewUiState.Loading)
    val state: StateFlow<TripOverviewUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TripOverviewEvent>()
    val events: SharedFlow<TripOverviewEvent> = _events.asSharedFlow()

    private val _isStarting = MutableStateFlow(false)
    val isStarting: StateFlow<Boolean> = _isStarting.asStateFlow()

    init {
        loadTrip()
    }

    fun loadTrip() {
        viewModelScope.launch {
            _state.value = TripOverviewUiState.Loading
            try {
                val trip = tripRepository.getTripDetails(tripId)
                _state.value = TripOverviewUiState.Success(trip)
            } catch (e: Exception) {
                _state.value = TripOverviewUiState.Error(e.toUserFriendlyMessage("Failed to load trip overview"))
            }
        }
    }

    fun startTrip() {
        viewModelScope.launch {
            _isStarting.value = true
            try {
                val updated = tripRepository.startTrip(tripId)
                _events.emit(TripOverviewEvent.TripStarted(updated.id))
            } catch (e: Exception) {
                // If already in progress, still proceed to active
                _events.emit(TripOverviewEvent.TripStarted(tripId))
            } finally {
                _isStarting.value = false
            }
        }
    }
}
