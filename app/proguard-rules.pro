# Keep JNI bridge entry points (called from native code / reflection-free direct calls)
-keepclasseswithmembernames class com.davimikeben.extremen64.LibretroBridge {
    native <methods>;
}
-keep class com.davimikeben.extremen64.LibretroBridge { *; }
-keep class com.davimikeben.extremen64.EmulatorView { *; }

# Compose
-dontwarn androidx.compose.**

# Kotlin
-dontwarn kotlin.**
