# Keep libretro core loader
-keep class com.davimikeben.sm64emulator.core.** { *; }
-keep class com.davimikeben.sm64emulator.input.** { *; }

# Keep JNI bridge
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep ROM verification data
-keepassets assets/rom/**
