package `in`.raahi.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun createEncrypted(): SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "raahi_keystore_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Encrypted storage only. If the Keystore-backed file is corrupted (common after restore /
     * keystore reset) it is wiped once and recreated, which just signs the user out. We never
     * downgrade to plaintext; if encryption is truly unavailable the token lives in memory only
     * for this process (null prefs) and the user re-authenticates next launch.
     */
    private val prefs: SharedPreferences? by lazy {
        try {
            createEncrypted()
        } catch (e: Exception) {
            try {
                context.deleteSharedPreferences("raahi_keystore_secure_prefs")
                createEncrypted()
            } catch (e2: Exception) {
                null
            }
        }
    }

    private val cachedToken by lazy { AtomicReference<String?>(prefs?.getString("auth_token", null)) }
    private val _tokenFlow by lazy { MutableStateFlow<String?>(cachedToken.get()) }
    val tokenFlow: Flow<String?> get() = _tokenFlow.asStateFlow()

    /** Non-blocking instant token lookup from memory — zero disk I/O, zero runBlocking */
    fun getCachedToken(): String? = cachedToken.get()

    suspend fun getToken(): String? = cachedToken.get()

    suspend fun saveToken(token: String) {
        cachedToken.set(token)
        _tokenFlow.value = token
        prefs?.edit()?.putString("auth_token", token)?.apply()
    }

    suspend fun clear() {
        cachedToken.set(null)
        _tokenFlow.value = null
        prefs?.edit()?.remove("auth_token")?.apply()
    }
}
