# ─── App classes needed by Hilt/DI ────────────────────────────────────
-keep class com.example.optoapp.di.MediaPipeModule { *; }
-keep class com.example.optoapp.domain.FaceLandmarkerUseCase { *; }
-keep class com.example.optoapp.domain.IrisMeasurementExtractor { *; }
-keep class com.example.optoapp.domain.FaceMeasurementExtractor { *; }
-keep class com.example.optoapp.viewmodel.dip.** { *; }

# ─── Data layer (Room entities, DAOs) ─────────────────────────────────
-keep class com.example.optoapp.data.** { *; }

# ─── MediaPipe (all packages - R8 obfuscates enums used by reflection) ─
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.proto.**
-dontwarn com.google.mediapipe.framework.GraphProfiler
-dontwarn com.google.mediapipe.framework.Graph

# ─── Keep dependencies used by MediaPipe via reflection ────────────────
-keep class com.google.common.flogger.** { *; }
-keep class com.google.protobuf.** { *; }
