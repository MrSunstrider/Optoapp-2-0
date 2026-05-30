# Proguard rules for OptoApp
-keep class com.example.optoapp.data.** { *; }

# ─── MediaPipe ────────────────────────────────────────────────────────────
-dontobfuscate
-keep class com.google.mediapipe.tasks.** { *; }
-keep class com.google.mediapipe.framework.** { *; }
-dontwarn com.google.mediapipe.proto.**
-dontwarn com.google.mediapipe.framework.GraphProfiler
-dontwarn com.google.mediapipe.framework.Graph

# Cuando actives isMinifyEnabled en release, reduce ruido en logcat (no aplica con minify desactivado).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
