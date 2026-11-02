package com.davimikeben.extremen64

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Bridge to the bundled libretro core (Mupen64Plus-Next).
 * Nothing here depends on an external emulator app.
 */
object LibretroBridge {
    private const val TAG = "LibretroBridge"
    private const val CORE_LIB_NAME = "mupen64plus_libretro"
    private const val BRIDGE_LIB_NAME = "extremen64"

    /** Game that is physically shipped inside the APK. */
    const val ROM_ASSET = "rom/Super Mario 64 (USA).z64"
    const val ROM_FILE_NAME = "Super Mario 64 (USA).z64"

    @Volatile
    private var coreLoaded = false

    init {
        System.loadLibrary(BRIDGE_LIB_NAME)
    }

    // ---- core control -----------------------------------------------------
    external fun loadCore(corePath: String): Boolean
    external fun loadGame(gamePath: String): Boolean
    external fun runFrame()
    external fun resetGame()
    external fun unloadGame()

    // ---- input ------------------------------------------------------------
    external fun setButtonState(buttonId: Int, pressed: Boolean)
    external fun setAnalog(axisId: Int, value: Float)

    // ---- video ------------------------------------------------------------
    external fun nativeInitGL()
    external fun nativeSetViewport(width: Int, height: Int)
    external fun nativeRenderFrame()
    external fun nativeSetPaths(systemDir: String, saveDir: String)
    external fun isHardwareRendering(): Boolean

    /** True when the emulation core reported that the game booted. */
    @Volatile
    var gameRunning = false
        private set

    /**
     * Maps the libretro core into the process.
     * @return the path/soname to hand over to the native loader, or null.
     */
    @Synchronized
    fun ensureCoreMapped(context: Context): String? {
        if (!coreLoaded) {
            try {
                System.loadLibrary(CORE_LIB_NAME)
                coreLoaded = true
                Log.i(TAG, "Core mapped via System.loadLibrary($CORE_LIB_NAME)")
            } catch (t: Throwable) {
                Log.w(TAG, "System.loadLibrary failed: ${t.message}")
            }
        }
        if (coreLoaded) return CORE_LIB_NAME

        val extracted = File(context.applicationInfo.nativeLibraryDir, "lib$CORE_LIB_NAME.so")
        if (extracted.exists()) {
            Log.i(TAG, "Core found extracted at ${extracted.absolutePath}")
            return extracted.absolutePath
        }
        Log.e(TAG, "Core library unavailable for ABI ${android.os.Build.SUPPORTED_ABIS.first()}")
        return null
    }

    /** Copies the bundled ROM out of the APK into app-private storage. */
    fun installBundledRom(context: Context): File? {
        val target = File(context.filesDir, "rom/$ROM_FILE_NAME")
        if (!extractAsset(context, ROM_ASSET, target)) return null
        Log.i(TAG, "ROM ready: ${target.absolutePath} (${target.length()} bytes)")
        return target
    }

    /** Copies an asset to app-private storage (automatic, no user interaction). */
    fun extractAsset(context: Context, assetPath: String, targetFile: File): Boolean {
        if (targetFile.exists() && targetFile.length() > 0) return true
        return try {
            targetFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to extract asset $assetPath", t)
            false
        }
    }

    /** Verifies the ROM is present inside the APK and readable. */
    fun verifyBundledRom(context: Context): Boolean {
        return try {
            context.assets.open(ROM_ASSET).use { it.read() }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Bundled ROM missing from APK", t)
            false
        }
    }

    fun markGameRunning(running: Boolean) {
        gameRunning = running
    }
}
