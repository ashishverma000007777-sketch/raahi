package `in`.raahi.app.ui.screens.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.SavingsRepository
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.network.SavingsSummaryDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SavingsUiState {
    data object Loading : SavingsUiState()
    data class Loaded(val savings: SavingsSummaryDto) : SavingsUiState()
    data class Error(val message: String) : SavingsUiState()
}

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val repository: SavingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SavingsUiState>(SavingsUiState.Loading)
    val state: StateFlow<SavingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (!authRepository.hasAuthToken()) {
            _state.value = SavingsUiState.Error("Sign in to view savings")
            return
        }
        _state.value = SavingsUiState.Loading
        viewModelScope.launch {
            runCatching {
                repository.getSavings()
            }.onSuccess { summary ->
                _state.value = SavingsUiState.Loaded(summary)
            }.onFailure { e ->
                _state.value = SavingsUiState.Error(e.toUserFriendlyMessage("Could not load savings overview."))
            }
        }
    }
}
