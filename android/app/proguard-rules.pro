# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Gson rules
-keepattributes EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class in.raahi.app.network.** { *; }
-keep class in.raahi.app.data.** { *; }

# MapLibre Native
-keep class org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**

# Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.** { *; }
