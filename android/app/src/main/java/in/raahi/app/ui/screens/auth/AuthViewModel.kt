package `in`.raahi.app.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.raahi.app.data.AuthRepository
import `in`.raahi.app.data.PhoneAuthEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    data object EnteringPhone : AuthUiState()
    data object SendingOtp : AuthUiState()
    data class OtpSent(val verificationId: String, val phone: String) : AuthUiState()
    data object VerifyingOtp : AuthUiState()
    data class SignedIn(val isNewUser: Boolean) : AuthUiState()
    data class Error(val message: String, val fallback: AuthUiState) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.EnteringPhone)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var lastVerificationId: String = ""

    fun sendOtp(rawPhone: String, activity: Activity) {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.length != 10 || digits.first() !in '6'..'9') {
            _state.value = AuthUiState.Error(
                "Please enter a valid 10-digit mobile number.",
                AuthUiState.EnteringPhone
            )
            return
        }
        val e164 = "+91$digits"
        _state.value = AuthUiState.SendingOtp

        authRepository.sendOtp(e164, activity, resendToken)
            .onEach { event ->
                when (event) {
                    is PhoneAuthEvent.CodeSent -> {
                        lastVerificationId = event.verificationId
                        resendToken = event.token
                        _state.value = AuthUiState.OtpSent(event.verificationId, e164)
                    }

                    is PhoneAuthEvent.AutoVerified -> {
                        viewModelScope.launch {
                            runCatching { authRepository.signInWithCredential(event.credential) }
                                .onSuccess { _state.value = AuthUiState.SignedIn(it.isNewUser) }
                                .onFailure {
                                    _state.value = AuthUiState.Error(
                                        it.message ?: "Sign-in failed. Please try again.",
                                        AuthUiState.EnteringPhone
                                    )
                                }
                        }
                    }

                    is PhoneAuthEvent.Failed -> {
                        _state.value = AuthUiState.Error(event.message, AuthUiState.EnteringPhone)
                    }
                }
            }
            .catch {
                _state.value = AuthUiState.Error(
                    AuthRepository.DEFAULT_UNAVAILABLE_MSG,
                    AuthUiState.EnteringPhone
                )
            }
            .launchIn(viewModelScope)
    }

    fun verifyOtp(verificationId: String, phone: String, code: String) {
        val cleanCode = code.filter { it.isDigit() }
        if (cleanCode.length != 6) {
            _state.value = AuthUiState.Error(
                "Please enter the complete 6-digit OTP.",
                AuthUiState.OtpSent(verificationId, phone)
            )
            return
        }
        _state.value = AuthUiState.VerifyingOtp
        viewModelScope.launch {
            runCatching { authRepository.confirmOtpAndSignIn(verificationId, cleanCode) }
                .onSuccess { _state.value = AuthUiState.SignedIn(it.isNewUser) }
                .onFailure { e ->
                    val message = if (e is FirebaseAuthInvalidCredentialsException)
                        "Invalid OTP. Please check the code and try again."
                    else
                        e.message ?: "Verification failed. Please try again."
                    _state.value = AuthUiState.Error(message, AuthUiState.OtpSent(verificationId, phone))
                }
        }
    }

    fun resendOtp(phone: String, activity: Activity) {
        val raw = phone.removePrefix("+91").trim()
        val e164 = if (phone.startsWith("+91")) phone else "+91$raw"
        _state.value = AuthUiState.SendingOtp
        authRepository.sendOtp(e164, activity, resendToken)
            .onEach { event ->
                when (event) {
                    is PhoneAuthEvent.CodeSent -> {
                        lastVerificationId = event.verificationId
                        resendToken = event.token
                        _state.value = AuthUiState.OtpSent(event.verificationId, e164)
                    }
                    is PhoneAuthEvent.AutoVerified -> {
                        viewModelScope.launch {
                            runCatching { authRepository.signInWithCredential(event.credential) }
                                .onSuccess { _state.value = AuthUiState.SignedIn(it.isNewUser) }
                                .onFailure {
                                    _state.value = AuthUiState.Error(
                                        it.message ?: "Sign-in failed. Please try again.",
                                        AuthUiState.OtpSent(lastVerificationId, e164)
                                    )
                                }
                        }
                    }
                    is PhoneAuthEvent.Failed -> {
                        _state.value = AuthUiState.Error(
                            event.message,
                            AuthUiState.OtpSent(lastVerificationId, e164)
                        )
                    }
                }
            }
            .catch {
                _state.value = AuthUiState.Error(
                    AuthRepository.DEFAULT_UNAVAILABLE_MSG,
                    AuthUiState.OtpSent(lastVerificationId, e164)
                )
            }
            .launchIn(viewModelScope)
    }

    fun resetToEnteringPhone() {
        _state.value = AuthUiState.EnteringPhone
    }
}
