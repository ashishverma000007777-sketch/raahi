package `in`.raahi.app.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import `in`.raahi.app.BuildConfig
import `in`.raahi.app.network.FcmTokenRequest
import `in`.raahi.app.network.RaahiApi
import `in`.raahi.app.network.UpdateProfileRequest
import `in`.raahi.app.network.UserDto
import `in`.raahi.app.network.VerifyRequest
import `in`.raahi.app.network.apiCall
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class PhoneAuthEvent {
    data class CodeSent(
        val verificationId: String,
        val token: PhoneAuthProvider.ForceResendingToken? = null
    ) : PhoneAuthEvent()
    data class AutoVerified(val credential: PhoneAuthCredential) : PhoneAuthEvent()
    data class Failed(val message: String) : PhoneAuthEvent()
}

data class SignInResult(val token: String, val isNewUser: Boolean, val user: UserDto)

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth?,
    private val api: RaahiApi,
    private val tokenManager: TokenManager,
) {
    companion object {
        private const val TAG = "AuthRepository"
        const val DEFAULT_UNAVAILABLE_MSG = "OTP service is currently unavailable. Please try again."
    }

    /**
     * Checks whether Firebase is properly configured with an API key and App ID.
     */
    private fun isFirebaseReady(context: Context): Boolean {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) return false
            val app = FirebaseApp.getInstance()
            val apiKey = app.options.apiKey
            val appId = app.options.applicationId
            !apiKey.isNullOrBlank() && !appId.isNullOrBlank() && !apiKey.contains("dummy", ignoreCase = true)
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Sends a real Firebase OTP.
     * Emits CodeSent ONLY after Firebase confirms the SMS dispatch.
     * Never generates a fake OTP or simulates success.
     */
    fun sendOtp(
        phone: String,
        activity: Activity,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null
    ): Flow<PhoneAuthEvent> = callbackFlow {
        val auth = firebaseAuth
        if (auth == null || !isFirebaseReady(activity) || activity.isFinishing || activity.isDestroyed) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "sendOtp aborted: Firebase is not configured or activity is invalid.")
            }
            trySend(PhoneAuthEvent.Failed(DEFAULT_UNAVAILABLE_MSG))
            awaitClose { }
            return@callbackFlow
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Phone auth auto-verified via instant SMS/SIM.")
                }
                trySend(PhoneAuthEvent.AutoVerified(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Firebase phone auth failed: ${e.javaClass.simpleName} - ${e.message}", e)
                }
                val userMsg = when {
                    e is FirebaseNetworkException ->
                        "Network connection error. Please check your internet and try again."
                    e is FirebaseAuthInvalidCredentialsException ->
                        "Invalid phone number. Please enter a valid 10-digit mobile number."
                    e.message?.contains("quota", ignoreCase = true) == true ||
                    e.message?.contains("too many", ignoreCase = true) == true ->
                        "Too many OTP attempts. Please try again later."
                    else ->
                        DEFAULT_UNAVAILABLE_MSG
                }
                trySend(PhoneAuthEvent.Failed(userMsg))
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Real Firebase OTP code sent successfully: verificationId=$verificationId")
                }
                trySend(PhoneAuthEvent.CodeSent(verificationId, token))
            }
        }

        try {
            val builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (resendToken != null) {
                builder.setForceResendingToken(resendToken)
            }

            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "verifyPhoneNumber call failed", e)
            }
            trySend(PhoneAuthEvent.Failed(DEFAULT_UNAVAILABLE_MSG))
        }

        awaitClose { }
    }

    /** Confirms the 6-digit code against Firebase, then exchanges the Firebase ID token for our JWT. */
    suspend fun confirmOtpAndSignIn(verificationId: String, code: String): SignInResult {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        return signInWithCredential(credential)
    }

    suspend fun signInWithCredential(credential: PhoneAuthCredential): SignInResult {
        val auth = firebaseAuth ?: throw IllegalStateException(DEFAULT_UNAVAILABLE_MSG)
        val authResult = auth.signInWithCredential(credential).await()
        val idToken = authResult.user?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("Firebase sign-in succeeded but no ID token was returned")

        val data = apiCall { api.verify(VerifyRequest(idToken = idToken)) }
        tokenManager.saveToken(data.token)
        return SignInResult(data.token, data.isNewUser, data.user)
    }

    /** Returns true only when a real backend JWT is stored. */
    fun hasAuthToken(): Boolean = !tokenManager.getCachedToken().isNullOrBlank()

    /** GET /auth/me — used by Home to load the signed-in user's real profile (name, vehicle, role, rating). */
    suspend fun currentUser(): UserDto = apiCall { api.me() }

    /** PUT /auth/profile — used by the new-user profile-setup step and any later profile edits. */
    suspend fun updateProfile(name: String?, vehicleType: String?, vehicleReg: String?): UserDto =
        apiCall { api.updateProfile(UpdateProfileRequest(name = name, vehicleType = vehicleType, vehicleReg = vehicleReg)) }

    suspend fun signOut() {
        runCatching { api.logout() }
        firebaseAuth?.signOut()
        tokenManager.clear()
    }

    /** No-ops when signed out (no auth token to call the API with yet). */
    suspend fun registerFcmToken(token: String) {
        if (tokenManager.getToken() == null) return
        apiCall { api.updateFcmToken(FcmTokenRequest(token)) }
    }
}
