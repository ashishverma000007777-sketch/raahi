package `in`.raahi.app.ui.screens.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.VehicleRepository
import `in`.raahi.app.network.VehicleDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MyGarageState {
    data object Loading : MyGarageState()
    data class Success(val vehicles: List<VehicleDto>, val activeVehicleId: String?) : MyGarageState()
    data class Error(val message: String) : MyGarageState()
}

@HiltViewModel
class MyGarageViewModel @Inject constructor(
    private val repository: VehicleRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MyGarageState>(MyGarageState.Loading)
    val state: StateFlow<MyGarageState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = MyGarageState.Loading
            runCatching {
                val list = repository.allVehicles()
                val activeId = repository.getActiveVehicleId() ?: list.firstOrNull()?.id
                _state.value = MyGarageState.Success(list, activeId)
            }.onFailure { e ->
                _state.value = MyGarageState.Error(e.message ?: "Failed to load garage")
            }
        }
    }

    fun setDefault(vehicleId: String) {
        viewModelScope.launch {
            runCatching {
                repository.setActiveVehicle(vehicleId)
                load()
            }
        }
    }

    fun removeVehicle(vehicleId: String) {
        viewModelScope.launch {
            runCatching {
                repository.removeVehicle(vehicleId)
                load()
            }
        }
    }
}
