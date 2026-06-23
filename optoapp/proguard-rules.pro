# ProGuard/R8 rules for OptoApp — full mode (minify + obfuscate + optimize)

# ---- Keep attributes needed by reflection-based frameworks ----
-keepattributes *Annotation*, Signature, Exceptions, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations

# ---- Data layer: Room entities, DTOs, serialization ----
-keep class com.example.optoapp.data.** { *; }
-keep class com.example.optoapp.domain.** { *; }

# Kotlin Serialization: keep @Serializable classes and companion
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.optoapp.**$$serializer { *; }
-keepclassmembers class com.example.optoapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.optoapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ---- Supabase / Ktor ----
-keep class io.ktor.** { *; }
-keep class io.github.jan.supabase.** { *; }

# ---- Keep coroutines internals that R8 may strip ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---- Keep Compose / Material3 internals ----
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }

# ---- Reduce log noise in release ----
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# ---- Android-incompatible classes (used by Ktor but not available on Android) ----
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ---- Remove debug metadata ----
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.Unsafe
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn kotlin.internal.**
