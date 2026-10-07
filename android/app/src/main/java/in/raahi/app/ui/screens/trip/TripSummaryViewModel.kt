package `in`.raahi.app.ui.screens.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.TripRepository
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.network.TripSummaryDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TripSummaryUiState {
    data object Loading : TripSummaryUiState
    data class Success(val summary: TripSummaryDto) : TripSummaryUiState
    data class Error(val message: String) : TripSummaryUiState
}

@HiltViewModel
class TripSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val tripId: String = checkNotNull(savedStateHandle["tripId"])

    private val _state = MutableStateFlow<TripSummaryUiState>(TripSummaryUiState.Loading)
    val state: StateFlow<TripSummaryUiState> = _state.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        if (!authRepository.hasAuthToken()) return
        viewModelScope.launch {
            _state.value = TripSummaryUiState.Loading
            try {
                val summary = tripRepository.getTripSummary(tripId)
                _state.value = TripSummaryUiState.Success(summary)
            } catch (e: Exception) {
                _state.value = TripSummaryUiState.Error(e.toUserFriendlyMessage("Failed to generate trip summary"))
            }
        }
    }
}
