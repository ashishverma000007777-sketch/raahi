package `in`.raahi.app.ui.screens.helper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.HelperApplicationForm
import `in`.raahi.app.data.HelperRepository
import `in`.raahi.app.network.HelperStatusDto
import `in`.raahi.app.network.toUserFriendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class WizardStep(val title: String) {
    BASICS("Experience & area"),
    SERVICES("Services"),
    EQUIPMENT("Equipment"),
    KYC("Identity verification"),
    VEHICLE("Service vehicle"),
    PAYOUT("Payout details"),
    REVIEW("Review & submit"),
}

data class HelperApplicationUiState(
    val loading: Boolean = true,
    val status: HelperStatusDto? = null,
    val applying: Boolean = false,          // true = wizard visible (new or re-application)
    val step: WizardStep = WizardStep.BASICS,
    val form: HelperApplicationForm = HelperApplicationForm(),
    val submitting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HelperApplicationViewModel @Inject constructor(
    private val helperRepository: HelperRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HelperApplicationUiState())
    val state: StateFlow<HelperApplicationUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val st = helperRepository.status()
                // NONE = never applied. REJECTED shows the reason first and offers a re-application.
                _state.update { it.copy(loading = false, status = st, applying = st.applicationStatus == "NONE") }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserFriendlyMessage()) }
            }
        }
    }

    fun startApplication() = _state.update { it.copy(applying = true, step = WizardStep.BASICS, error = null) }

    fun update(block: (HelperApplicationForm) -> HelperApplicationForm) =
        _state.update { it.copy(form = block(it.form), error = null) }

    fun next() {
        val s = _state.value
        val problem = validate(s.step, s.form)
        if (problem != null) { _state.update { it.copy(error = problem) }; return }
        val idx = WizardStep.entries.indexOf(s.step)
        if (idx < WizardStep.entries.lastIndex) _state.update { it.copy(step = WizardStep.entries[idx + 1], error = null) }
    }

    /** Returns true when the wizard consumed the back press (moved a step back). */
    fun back(): Boolean {
        val s = _state.value
        if (!s.applying) return false
        val idx = WizardStep.entries.indexOf(s.step)
        return if (idx > 0) { _state.update { it.copy(step = WizardStep.entries[idx - 1], error = null) }; true } else false
    }

    fun submit() {
        val s = _state.value
        WizardStep.entries.dropLast(1).forEach { step ->
            validate(step, s.form)?.let { msg -> _state.update { it.copy(step = step, error = msg) }; return }
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                helperRepository.apply(s.form)
                _state.update { it.copy(submitting = false, applying = false) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(submitting = false, error = e.toUserFriendlyMessage()) }
            }
        }
    }

    // Client-side checks mirror the server rules (the server stays authoritative).
    private fun validate(step: WizardStep, f: HelperApplicationForm): String? = when (step) {
        WizardStep.BASICS -> when {
            (f.experienceYears.toIntOrNull() ?: -1) !in 0..60 -> "Enter your years of experience (0–60)"
            f.serviceArea.isBlank() -> "Enter your service area"
            else -> null
        }
        WizardStep.SERVICES -> if (f.services.isEmpty()) "Select at least one service" else null
        WizardStep.EQUIPMENT -> null
        WizardStep.KYC -> when {
            f.aadhaarFront == null -> "Upload the front of your Aadhaar"
            f.aadhaarBack == null -> "Upload the back of your Aadhaar"
            f.selfie == null -> "Take or upload a selfie"
            else -> null
        }
        WizardStep.VEHICLE -> when {
            f.vehicleType.isBlank() -> "Select your service vehicle type"
            f.vehicleBrand.isBlank() || f.vehicleModel.isBlank() -> "Select the vehicle brand and model"
            !Regex("^[A-Za-z]{2}[0-9]{1,2}[A-Za-z]{0,3}[0-9]{4}$").matches(f.vehicleReg.trim().replace(" ", "")) -> "Enter a valid registration number"
            (f.radiusKm.toIntOrNull() ?: 0) !in 1..100 -> "Service radius must be 1–100 km"
            else -> null
        }
        WizardStep.PAYOUT -> {
            val hasUpi = f.payoutUpi.isNotBlank()
            val hasBank = f.payoutBankAcct.isNotBlank()
            when {
                !hasUpi && !hasBank -> "Add a UPI ID or bank account"
                hasUpi && !Regex("^[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}$").matches(f.payoutUpi.trim()) -> "Enter a valid UPI ID"
                hasBank && !Regex("^[0-9]{9,18}$").matches(f.payoutBankAcct.trim()) -> "Enter a valid bank account number"
                hasBank && !Regex("^[A-Za-z]{4}0[A-Za-z0-9]{6}$").matches(f.payoutIfsc.trim()) -> "Enter a valid IFSC code"
                hasBank && f.payoutHolder.isBlank() -> "Enter the account holder name"
                else -> null
            }
        }
        WizardStep.REVIEW -> null
    }
}
