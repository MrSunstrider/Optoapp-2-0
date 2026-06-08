# Proguard rules for OptoApp — R8 full mode (minify + obfuscate + optimize)
-keep class com.example.optoapp.data.** { *; }

# Reduce ruido en logcat (release builds)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
