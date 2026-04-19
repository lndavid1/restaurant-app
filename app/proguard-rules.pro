# ==========================================================
# ProGuard / R8 Rules cho Restaurant App
# Quan trọng: thiếu rule nào → crash release như "bom nổ chậm"
# ==========================================================

# Giữ thông tin stack trace cho debug crash release
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==========================================================
# Firebase & Firestore
# ==========================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.storage.** { *; }
-keep class com.google.firebase.ai.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore data model classes (deserialization dùng reflection)
-keep class com.example.restaurant.data.model.** { *; }
-keepclassmembers class com.example.restaurant.data.model.** {
    <init>();
    <fields>;
}

# ==========================================================
# Gson (JSON serialization/deserialization)
# ==========================================================
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ==========================================================
# Kotlin Coroutines
# ==========================================================
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ==========================================================
# Retrofit & OkHttp
# ==========================================================
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes RuntimeVisibleAnnotations

# Retrofit interfaces
-keep class com.example.restaurant.data.remote.** { *; }

# ==========================================================
# Coil (Image loading)
# ==========================================================
-keep class coil.** { *; }
-dontwarn coil.**

# ==========================================================
# ZXing (QR Code)
# ==========================================================
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ==========================================================
# Android WebView
# ==========================================================
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}

# ==========================================================
# Jetpack Compose (R8 thường handle tốt, nhưng giữ annotation)
# ==========================================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**