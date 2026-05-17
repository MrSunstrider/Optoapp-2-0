# Proguard rules for OptoApp
-keep class com.example.optoapp.data.** { *; }

# Cuando actives isMinifyEnabled en release, reduce ruido en logcat (no aplica con minify desactivado).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
