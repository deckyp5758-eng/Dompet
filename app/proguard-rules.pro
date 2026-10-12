# R8 / ProGuard Configuration for Dompet Pintar

# Keep line numbers and source files for clear crash reports in AppLogger & diagnostics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep Data Models & Enums
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.data.calculator.** { *; }
-keep class com.example.core.logger.** { *; }
-keep class com.example.core.result.** { *; }

# Moshi & Retrofit rules
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-dontwarn retrofit2.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose rules
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
