package com.davimikeben.sm64emulator.core

import android.content.Context
import android.util.Log
import android.view.Surface
import com.davimikeben.sm64emulator.rom.RomLoader
import java.io.File

/**
 * Orquestra o ciclo de vida do emulador:
 *  init → loadRom → runFrame(loop) → pause/resume/reset/saveState → deinit
 *
 * Trata erros e expõe estado observável para a UI (FPS, status).
 */
class EmulatorRuntime private constructor(private val context: Context) {
    enum class State { IDLE, INITIALIZING, READY, RUNNING, PAUSED, ERROR }
    @Volatile var state: State = State.IDLE
        private set
    @Volatile var lastError: String? = null
        private set
    @Volatile var fps: Float = 0f
        private set
    @Volatile var frameCount: Long = 0L
        private set

    private val threadLock = Object()
    private var renderThread: Thread? = null
    private var runLoop = false

    fun start(corePath: String, surface: Surface, onReady: () -> Unit, onError: (String) -> Unit) {
        synchronized(threadLock) {
            if (state == State.RUNNING || state == State.INITIALIZING) return
            state = State.INITIALIZING
        }
        Thread {
            try {
                LibretroBridge.loadNativeLib()
                val systemDir = File(context.filesDir, "system").apply { mkdirs() }.absolutePath
                val saveDir = File(context.filesDir, "saves").apply { mkdirs() }.absolutePath

                val rc = LibretroBridge.nativeInit(corePath, systemDir, saveDir)
                if (rc != 0) throw RuntimeException("nativeInit rc=$rc")
                LibretroBridge.nativeSetSurface(surface)
                LibretroBridge.nativeSetAudioSampleRate(44100)

                val romFile = RomLoader.ensureExtracted(context)
                val rcRom = LibretroBridge.nativeLoadRom(romFile.absolutePath)
                if (rcRom != 0) throw RuntimeException("nativeLoadRom rc=$rcRom (path=${romFile.absolutePath})")

                state = State.READY
                onReady()
            } catch (t: Throwable) {
                Log.e(TAG, "start falhou", t)
                state = State.ERROR
                lastError = t.message ?: t.javaClass.simpleName
                onError(lastError!!)
            }
        }.also { renderThread = it }.start()
    }

    fun resumeLoop() {
        synchronized(threadLock) {
            if (state != State.READY && state != State.PAUSED) return
            runLoop = true
            state = State.RUNNING
            LibretroBridge.nativePause(false)
        }
        Thread {
            while (runLoop) {
                try {
                    LibretroBridge.nativeRunFrame()
                    frameCount++
                    fps = LibretroBridge.nativeGetLastFps()
                } catch (t: Throwable) {
                    Log.e(TAG, "runFrame", t)
                    state = State.ERROR
                    lastError = t.message
                    runLoop = false
                    break
                }
            }
        }.start()
    }

    fun pauseLoop() {
        synchronized(threadLock) {
            if (state == State.RUNNING) {
                runLoop = false
                LibretroBridge.nativePause(true)
                state = State.PAUSED
            }
        }
    }

    fun reset() {
        synchronized(threadLock) {
            LibretroBridge.nativeReset()
        }
    }

    fun saveState(slot: Int): Boolean = try { LibretroBridge.nativeSaveState(slot) == 0 } catch (t: Throwable) { false }
    fun loadState(slot: Int): Boolean = try { LibretroBridge.nativeLoadState(slot) == 0 } catch (t: Throwable) { false }

    fun setPerformanceMode(mode: Int) {
        try { LibretroBridge.nativeSetPerformanceMode(mode) } catch (t: Throwable) { Log.w(TAG, "setPerf $t") }
    }

    fun setInput(button: Int, value: Int) {
        try { LibretroBridge.nativeSetInput(0, LibretroConstants.RETRO_DEVICE_JOYPAD, button, value) } catch (t: Throwable) {}
    }

    fun setInputState(device: Int, index: Int, id: Int, value: Int) {
        try { LibretroBridge.nativeSetInputState(0, device, index, id, value) } catch (t: Throwable) {}
    }

    fun deinit() {
        synchronized(threadLock) {
            runLoop = false
            try { LibretroBridge.nativeDeinit() } catch (t: Throwable) {}
            state = State.IDLE
        }
    }

    companion object {
        private const val TAG = "EmulatorRuntime"
        @Volatile private var INSTANCE: EmulatorRuntime? = null
        fun get(context: Context): EmulatorRuntime =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmulatorRuntime(context.applicationContext).also { INSTANCE = it }
            }
    }
}
