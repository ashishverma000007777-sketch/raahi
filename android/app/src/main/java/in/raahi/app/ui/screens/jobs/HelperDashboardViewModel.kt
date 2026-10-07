package `in`.raahi.app.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.HelperRepository
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.JobsRepository
import `in`.raahi.app.data.LocationProvider
import `in`.raahi.app.network.HelperCommissionDto
import `in`.raahi.app.network.HelperEarningsDto
import `in`.raahi.app.network.HelperJobHistoryDto
import `in`.raahi.app.network.HelperRatingDto
import `in`.raahi.app.network.HelperStatusDto
import `in`.raahi.app.network.JobDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DashTab(val label: String) {
    REQUESTS("Requests"), ACTIVE("Active"), HISTORY("History"), EARNINGS("Earnings"), PROFILE("Profile"),
}

private val ACTIVE_STATES = setOf("MATCHED", "ARRIVED", "IN_PROGRESS", "WORK_DONE")

data class HelperDashUiState(
    val loading: Boolean = true,
    val status: HelperStatusDto? = null,
    val tab: DashTab = DashTab.REQUESTS,
    val requests: List<JobDto> = emptyList(),
    val active: List<JobDto> = emptyList(),
    val history: List<HelperJobHistoryDto> = emptyList(),
    val earnings: HelperEarningsDto? = null,
    val commission: HelperCommissionDto? = null,
    val ratings: List<HelperRatingDto> = emptyList(),
    val tabLoading: Boolean = false,
    val togglingOnline: Boolean = false,
    val acceptingId: String? = null,
    val acceptedJobId: String? = null,
    val dismissed: Set<String> = emptySet(),
    val error: String? = null,
)

@HiltViewModel
class HelperDashboardViewModel @Inject constructor(
    private val jobsRepository: JobsRepository,
    private val authRepository: AuthRepository,
    private val helperRepository: HelperRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(HelperDashUiState())
    val state: StateFlow<HelperDashUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        if (!authRepository.hasAuthToken()) {
            _state.update { it.copy(loading = false) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val st = helperRepository.status()
                _state.update { it.copy(loading = false, status = st) }
                if (st.applicationStatus == "APPROVED") refreshTab()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserFriendlyMessage()) }
            }
        }
    }

    fun selectTab(tab: DashTab) {
        _state.update { it.copy(tab = tab, error = null) }
        refreshTab()
    }

    fun refreshTab() {
        if (!authRepository.hasAuthToken()) return
        val s = _state.value
        if (s.status?.applicationStatus != "APPROVED") return
        _state.update { it.copy(tabLoading = true) }
        viewModelScope.launch {
            try {
                when (_state.value.tab) {
                    DashTab.REQUESTS -> {
                        val fix = runCatching { locationProvider.getCurrentLocation() }.getOrNull()
                        val jobs = jobsRepository.availableJobs(fix?.lat, fix?.lng)
                        _state.update { it.copy(requests = jobs) }
                    }
                    DashTab.ACTIVE -> {
                        val mine = jobsRepository.myJobs().filter { it.viewerRole == "HELPER" && it.status in ACTIVE_STATES }
                        _state.update { it.copy(active = mine) }
                    }
                    DashTab.HISTORY -> _state.update { it.copy(history = helperRepository.history()) }
                    DashTab.EARNINGS -> {
                        val e = helperRepository.earnings()
                        val c = helperRepository.commission()
                        _state.update { it.copy(earnings = e, commission = c) }
                    }
                    DashTab.PROFILE -> _state.update { it.copy(ratings = helperRepository.ratings()) }
                }
                // keep the header (online / commission) fresh
                val st = helperRepository.status()
                _state.update { it.copy(status = st, tabLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(tabLoading = false, error = e.toUserFriendlyMessage()) }
            }
        }
    }

    /** Server decides: only APPROVED + ACTIVE helpers can go online. */
    fun setOnline(online: Boolean) {
        if (!authRepository.hasAuthToken()) return
        _state.update { it.copy(togglingOnline = true, error = null) }
        viewModelScope.launch {
            try {
                var lat: Double? = null
                var lng: Double? = null
                if (online) {
                    val fix = locationProvider.getCurrentLocation()
                        ?: throw IllegalStateException("Turn on location to go online")
                    lat = fix.lat; lng = fix.lng
                }
                helperRepository.setOnline(online, lat, lng)
                val st = helperRepository.status()
                _state.update { it.copy(togglingOnline = false, status = st) }
                if (online) refreshTab()
            } catch (e: IllegalStateException) {
                _state.update { it.copy(togglingOnline = false, error = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(togglingOnline = false, error = e.toUserFriendlyMessage()) }
            }
        }
    }

    fun accept(jobId: String) {
        if (!authRepository.hasAuthToken()) return
        _state.update { it.copy(acceptingId = jobId, error = null) }
        viewModelScope.launch {
            try {
                jobsRepository.acceptJob(jobId)
                _state.update { it.copy(acceptingId = null, acceptedJobId = jobId) }
            } catch (e: Exception) {
                _state.update { it.copy(acceptingId = null, error = e.toUserFriendlyMessage()) }
                refreshTab()
            }
        }
    }

    /** Hides a request on this device only (nothing is sent to the server). */
    fun decline(jobId: String) = _state.update { it.copy(dismissed = it.dismissed + jobId) }

    fun consumeAccepted() = _state.update { it.copy(acceptedJobId = null) }
}
