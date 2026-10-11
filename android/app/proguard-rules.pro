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

# Google Maps SDK for Android
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**

# Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.** { *; }
