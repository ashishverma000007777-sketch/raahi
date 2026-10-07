package `in`.raahi.app.ui.screens.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.TripRepository
import `in`.raahi.app.network.TripDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TripHistoryUiState {
    data object Loading : TripHistoryUiState
    data class Success(val trips: List<TripDto>) : TripHistoryUiState
    data class Error(val message: String) : TripHistoryUiState
}

@HiltViewModel
class TripHistoryViewModel @Inject constructor(
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TripHistoryUiState>(TripHistoryUiState.Loading)
    val state: StateFlow<TripHistoryUiState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.value = TripHistoryUiState.Loading
            try {
                val trips = tripRepository.listTrips()
                _state.value = TripHistoryUiState.Success(trips)
            } catch (e: Exception) {
                _state.value = TripHistoryUiState.Error(e.toUserFriendlyMessage("Failed to load trip history"))
            }
        }
    }
}
