# Projeto MARIO-APK — regras de ofuscação
# Manter classes de entrada e componentes Android
-keep class com.davimikeben.marioapk.** { *; }
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }

# Manter métodos nativos (JNI) do núcleo de emulação
-keepclasseswithmembers class com.davimikeben.marioapk.N64EmulatorCore {
    native <methods>;
}

# Preservar nomes de classes custom Views referenciadas em XML
-keep class com.davimikeben.marioapk.GameSurfaceView { *; }
-keep class com.davimikeben.marioapk.GameRenderer { *; }
-keep class com.davimikeben.marioapk.VirtualKeyboardView { *; }
-keep class com.davimikeben.marioapk.KeyButton { *; }
-keep class com.davimikeben.marioapk.FloatingMenuView { *; }
-keep class com.davimikeben.marioapk.FpsCounterOverlay { *; }

# Não ofuscar enum e listeners públicos
-keepclassmembers enum * { *; }
-keep interface com.davimikeben.marioapk.EmulatorCore { *; }
