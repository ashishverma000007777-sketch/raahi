package `in`.raahi.app.network

import `in`.raahi.app.data.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Zero-blocking, instant memory lookup — eliminates runBlocking disk I/O
        val token = tokenManager.getCachedToken()
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        val response = chain.proceed(request)
        if (response.code == 401) {
            // Token expired or invalidated on server — clear cached credentials
            CoroutineScope(Dispatchers.IO).launch {
                tokenManager.clear()
            }
        }
        return response
    }
}
