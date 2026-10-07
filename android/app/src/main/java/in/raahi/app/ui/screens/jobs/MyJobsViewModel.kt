package `in`.raahi.app.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.JobsRepository
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MyJobsUiState {
    data object Loading : MyJobsUiState()
    data class Loaded(val jobs: List<JobDto>, val errorMessage: String? = null) : MyJobsUiState()
    data class Error(val message: String) : MyJobsUiState()
}

@HiltViewModel
class MyJobsViewModel @Inject constructor(
    private val jobsRepository: JobsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<MyJobsUiState>(MyJobsUiState.Loading)
    val state: StateFlow<MyJobsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (!authRepository.hasAuthToken()) {
            _state.value = MyJobsUiState.Loaded(emptyList())
            return
        }
        _state.value = MyJobsUiState.Loading
        viewModelScope.launch {
            runCatching { jobsRepository.myJobs() }
                .onSuccess { jobs -> _state.value = MyJobsUiState.Loaded(jobs) }
                .onFailure { e ->
                    val friendly = e.toUserFriendlyMessage("Could not load your requests. Check your connection.")
                    _state.value = MyJobsUiState.Loaded(emptyList(), errorMessage = friendly)
                }
        }
    }
}
