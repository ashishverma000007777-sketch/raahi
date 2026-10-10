package `in`.raahi.app.ui.screens.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.VehicleRepository
import `in`.raahi.app.network.VehicleDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MyVehiclesUiState {
    data object Loading : MyVehiclesUiState()
    data class Success(val vehicle: VehicleDto?) : MyVehiclesUiState()
    data class Error(val message: String) : MyVehiclesUiState()
}

@HiltViewModel
class MyVehiclesViewModel @Inject constructor(
    private val repository: VehicleRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MyVehiclesUiState>(MyVehiclesUiState.Loading)
    val state: StateFlow<MyVehiclesUiState> = _state.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    init {
        loadVehicle()
    }

    fun loadVehicle() {
        if (!authRepository.hasAuthToken()) {
            _state.value = MyVehiclesUiState.Error("Please log in to manage your vehicles.")
            return
        }
        _state.value = MyVehiclesUiState.Loading
        viewModelScope.launch {
            runCatching { repository.myVehicle() }
                .onSuccess { _state.value = MyVehiclesUiState.Success(it) }
                .onFailure { e -> _state.value = MyVehiclesUiState.Error(e.toUserFriendlyMessage("Failed to load vehicle details.")) }
        }
    }

    fun deleteVehicle(onDeleted: () -> Unit) {
        if (!authRepository.hasAuthToken()) return
        _isDeleting.value = true
        viewModelScope.launch {
            runCatching { repository.deleteVehicle() }
                .onSuccess {
                    _isDeleting.value = false
                    _state.value = MyVehiclesUiState.Success(null)
                    onDeleted()
                }
                .onFailure { e ->
                    _isDeleting.value = false
                    _state.value = MyVehiclesUiState.Error(e.toUserFriendlyMessage("Could not remove vehicle. Please try again."))
                }
        }
    }
}
