package `in`.raahi.app.di

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.raahi.app.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(@ApplicationContext context: Context): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val app = FirebaseApp.getInstance()
            val apiKey = app.options.apiKey
            val appId = app.options.applicationId
            if (apiKey.isNullOrBlank() || appId.isNullOrBlank()) {
                if (BuildConfig.DEBUG) {
                    Log.w("FirebaseModule", "Firebase options missing apiKey or applicationId")
                }
                return null
            }
            FirebaseAuth.getInstance(app)
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) {
                Log.e("FirebaseModule", "FirebaseAuth initialization failed: ${e.message}", e)
            }
            null
        }
    }
}
