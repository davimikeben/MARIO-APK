package com.davimikeben.sm64emulator.core

import android.util.Log

/**
 * Ponte JNI para o núcleo libretro (mupen64plus_next ou parallel_n64).
 *
 * O core .so é colocado em jniLibs/<abi>/libretro_mupen64plus_next.so
 * pelo script scripts/fetch_libretro_core.sh.
 *
 * O wrapper sm64_emulator (.so nativo em src/main/cpp/) apenas repassa
 * as chamadas ao core carregado dinamicamente via dlopen + dlsym,
 * de modo que mantemos uma única interface Java e independência de ABI.
 */
class LibretroBridge {
    companion object {
        private const val TAG = "LibretroBridge"
        @Volatile private var initialized = false

        /** Carrega a lib nativa sm64_emulator (.so) que faz dlopen do core. */
        @JvmStatic external fun nativeInit(corePath: String, systemDir: String, saveDir: String): Int
        @JvmStatic external fun nativeLoadRom(romPath: String): Int
        @JvmStatic external fun nativeRunFrame(): Int
        @JvmStatic external fun nativeReset()
        @JvmStatic external fun nativePause(pause: Boolean)
        @JvmStatic external fun nativeSetInput(port: Int, device: Int, button: Int, value: Int)
        @JvmStatic external fun nativeSetInputState(port: Int, device: Int, index: Int, id: Int, value: Int)
        @JvmStatic external fun nativeGetVideoWidth(): Int
        @JvmStatic external fun nativeGetVideoHeight(): Int
        @JvmStatic external fun nativeGetPixelFormat(): Int
        @JvmStatic external fun nativeDeinit()
        @JvmStatic external fun nativeGetLastFps(): Float
        @JvmStatic external fun nativeSetPerformanceMode(mode: Int)
        @JvmStatic external fun nativeSetSurface(surface: android.view.Surface)
        @JvmStatic external fun nativeSetAudioSampleRate(rate: Int)
        @JvmStatic external fun nativeGetCoreName(): String
        @JvmStatic external fun nativeGetCoreVersion(): String
        @JvmStatic external fun nativeSaveState(slot: Int): Int
        @JvmStatic external fun nativeLoadState(slot: Int): Int
        @JvmStatic external fun nativeGetRegion(): Int

        @JvmStatic fun loadNativeLib() {
            if (initialized) return
            try {
                System.loadLibrary("sm64_emulator")
                initialized = true
                Log.i(TAG, "Biblioteca nativa sm64_emulator carregada")
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao carregar sm64_emulator: ${t.message}")
                throw t
            }
        }
    }
}
