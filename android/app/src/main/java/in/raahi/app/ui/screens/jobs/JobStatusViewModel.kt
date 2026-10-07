package `in`.raahi.app.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.SavedStateHandle
import `in`.raahi.app.data.JobsRepository
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.network.toUserFriendlyMessage
import `in`.raahi.app.network.RaahiWebSocketClient
import `in`.raahi.app.network.WsEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class JobStatusUiState {
    data object Loading : JobStatusUiState()
    data class Loaded(
        val job: JobDto, val liveLocation: Pair<Double, Double>? = null,
        val actionError: String? = null, val acting: Boolean = false,
    ) : JobStatusUiState()
    data class Error(val message: String) : JobStatusUiState()
}

private val TERMINAL_STATUSES = setOf("COMPLETED", "CANCELLED", "EXPIRED")
private val LOCATION_SHARING_STATUSES = setOf("MATCHED", "ARRIVED", "IN_PROGRESS")
// The WebSocket (see RaahiWebSocketClient) delivers job:status_change/job:helper_location
// in real time now, so this is a slow safety-net poll for when the socket is down or still
// reconnecting, not the primary update mechanism 6B originally used.
private const val FALLBACK_POLL_INTERVAL_MS = 20_000L
private const val LOCATION_SHARE_INTERVAL_MS = 8_000L

@HiltViewModel
class JobStatusViewModel @Inject constructor(
    private val jobsRepository: JobsRepository,
    private val authRepository: AuthRepository,
    private val webSocketClient: RaahiWebSocketClient,
    private val locationProvider: LocationProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val jobId: String = checkNotNull(savedStateHandle["jobId"])

    private val _state = MutableStateFlow<JobStatusUiState>(JobStatusUiState.Loading)
    val state: StateFlow<JobStatusUiState> = _state.asStateFlow()

    val wsConnectionState = webSocketClient.connectionState

    init {
        if (authRepository.hasAuthToken()) {
            startPolling()
            listenForLiveUpdates()
            startLocationSharingIfHelper()
        } else {
            _state.value = JobStatusUiState.Error("Sign in to view this request")
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                val current = _state.value
                if (current is JobStatusUiState.Loaded && current.job.status in TERMINAL_STATUSES) return@launch
                refresh()
                delay(FALLBACK_POLL_INTERVAL_MS)
            }
        }
    }

    private fun listenForLiveUpdates() {
        viewModelScope.launch {
            webSocketClient.events.collect { event ->
                when (event) {
                    is WsEvent.JobStatusChange -> if (event.jobId == jobId) refresh()
                    is WsEvent.JobHelperLocation -> if (event.jobId == jobId) {
                        _state.update {
                            (it as? JobStatusUiState.Loaded)?.copy(liveLocation = event.lat to event.lng) ?: it
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /** Helper side, while IN_PROGRESS: push a location fix over the socket periodically so
     * the requester's screen can show where their helper is. Stops once the job leaves
     * IN_PROGRESS or this ViewModel is cleared. */
    private fun startLocationSharingIfHelper() {
        viewModelScope.launch {
            while (true) {
                val current = _state.value
                if (current is JobStatusUiState.Loaded && current.job.viewerRole == "HELPER" && current.job.status in LOCATION_SHARING_STATUSES) {
                    val fix = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
                    if (fix != null) webSocketClient.sendJobLocationUpdate(jobId, fix.lat, fix.lng)
                }
                delay(LOCATION_SHARE_INTERVAL_MS)
            }
        }
    }

    fun refresh() {
        if (!authRepository.hasAuthToken()) return
        viewModelScope.launch {
            runCatching { jobsRepository.getJob(jobId) }
                .onSuccess { job ->
                    _state.update { prev ->
                        val prevLoc = (prev as? JobStatusUiState.Loaded)?.liveLocation
                        JobStatusUiState.Loaded(job, liveLocation = prevLoc)
                    }
                }
                .onFailure { e ->
                    if (_state.value !is JobStatusUiState.Loaded) {
                        _state.value = JobStatusUiState.Error(e.toUserFriendlyMessage("Could not load this request"))
                    }
                }
        }
    }

    // ---- customer actions
    fun cancel(reason: String?) = act { jobsRepository.cancelJob(jobId, reason); jobsRepository.getJob(jobId) }

    fun confirmCompletion(otp: String) = act { jobsRepository.confirmCompletion(jobId, otp) }

    fun rate(stars: Int, comment: String?) = act { jobsRepository.rate(jobId, stars, comment); jobsRepository.getJob(jobId) }

    fun report(message: String) = act { jobsRepository.report(jobId, message); jobsRepository.getJob(jobId) }

    // ---- helper actions
    /** Arrival proof = fresh GPS fix and/or the arrival OTP the customer shows. */
    fun arrive(otp: String?) = act {
        val fix = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
        jobsRepository.arrive(jobId, fix?.lat, fix?.lng, otp)
    }

    fun startWork() = act { jobsRepository.startWork(jobId) }

    fun workDone(finalAmount: Double?, paymentMode: String) = act { jobsRepository.workDone(jobId, finalAmount, paymentMode) }

    private fun act(block: suspend () -> JobDto) {
        if (!authRepository.hasAuthToken()) return
        val current = _state.value as? JobStatusUiState.Loaded ?: return
        _state.value = current.copy(acting = true, actionError = null)
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { job -> _state.value = JobStatusUiState.Loaded(job, liveLocation = current.liveLocation) }
                .onFailure { e ->
                    _state.update {
                        (it as? JobStatusUiState.Loaded)?.copy(acting = false, actionError = e.toUserFriendlyMessage("Action failed. Check connection."))
                            ?: it
                    }
                }
        }
    }
}
