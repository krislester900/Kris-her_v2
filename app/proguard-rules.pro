# ProGuard / R8 rules for kris&her v2

# ------------------------------------------------------------------
# Firebase
# ------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.auth.internal.** { *; }

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firestore.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }

# Firebase Functions
-keep class com.google.firebase.functions.** { *; }

# Firebase App Check
-keep class com.google.firebase.appcheck.** { *; }

# ------------------------------------------------------------------
# Google Play Services / Credential Manager
# ------------------------------------------------------------------
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# AndroidX Credentials
-keep class androidx.credentials.** { *; }
-keep class androidx.credentials.playservices.** { *; }

# Google Identity (Google Sign-In)
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ------------------------------------------------------------------
# Moshi (JSON parsing)
# ------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass interface *
-keep @com.squareup.moshi.JsonClass class *
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.Json <methods>;
}

# ------------------------------------------------------------------
# Room Database
# ------------------------------------------------------------------
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}

# Entity classes
-keep class com.example.model.** { *; }

# DAO classes
-keep class com.example.data.**Dao { *; }

# ------------------------------------------------------------------
# Kotlinx Coroutines
# ------------------------------------------------------------------
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.internal.** { *; }

# ------------------------------------------------------------------
# Kotlinx Serialization (if used)
# ------------------------------------------------------------------
# -keep class kotlinx.serialization.** { *; }

# ------------------------------------------------------------------
# OkHttp / Logging Interceptor
# ------------------------------------------------------------------
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ------------------------------------------------------------------
# Coil (Image loading)
# ------------------------------------------------------------------
-keep class coil3.** { *; }

# ------------------------------------------------------------------
# Retrofit / Converter
# ------------------------------------------------------------------
-keep class retrofit2.** { *; }
-keep class com.squareup.retrofit2.** { *; }

# ------------------------------------------------------------------
# Jetpack Compose
# ------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# ------------------------------------------------------------------
# Kotlin Standard Library
# ------------------------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.jvm.** { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.coroutines.** { *; }

# ------------------------------------------------------------------
# Project specific
# ------------------------------------------------------------------
# Keep VaultSecurityManager for PIN handling
-keep class com.example.security.VaultSecurityManager { *; }

# Keep PresenceManager
-keep class com.example.data.PresenceManager { *; }

# Keep AudioManagerViewModel and related
-keep class com.example.viewmodel.AudioManagerViewModel { *; }
-keep class com.example.viewmodel.StickerViewModel { *; }
-keep class com.example.viewmodel.NotificationViewModel { *; }
-keep class com.example.viewmodel.ChatViewModel { *; }

# Keep Firestore repositories
-keep class com.example.data.*Repository { *; }
-keep class com.example.data.FirestoreChatRepository { *; }
-keep class com.example.data.FirebaseRepositorySupport { *; }

# Keep ChatRepository (Room)
-keep class com.example.data.ChatRepository { *; }
-keep class com.example.data.AppDatabase { *; }

# Keep Model classes
-keep class com.example.model.** { *; }

# Keep UI Components
-keep class com.example.ui.components.** { *; }
-keep class com.example.ui.screens.** { *; }

# ------------------------------------------------------------------
# Preserve line numbers for debugging
# ------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable

# ------------------------------------------------------------------
# Prevent obfuscation of resource names (R class)
# ------------------------------------------------------------------
-keepclassmembers class **.R$* {
    public static <fields>;
}

# ------------------------------------------------------------------
# Don't warn about missing classes (common with optional deps)
# ------------------------------------------------------------------
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.coroutines.internal.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn coil3.**
-dontwarn retrofit2.**
-dontwarn androidx.compose.**
-dontwarn kotlinx.coroutines.**
