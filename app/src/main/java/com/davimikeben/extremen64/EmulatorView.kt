package com.davimikeben.extremen64

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GLES3 surface that drives the bundled libretro core.
 * Everything (core loading, ROM loading, frame pumping, audio) happens inside
 * the app - there is no external emulator and no user-provided file.
 */
class EmulatorView(context: Context) : GLSurfaceView(context), GLSurfaceView.Renderer {

    private val appContext: Context = context.applicationContext

    @Volatile var fps: Float = 0f
        private set
    @Volatile var running: Boolean = false
        private set
    @Volatile var status: String = "Preparando..."
        private set
    @Volatile var error: String? = null
        private set
    @Volatile var hardwareRendering: Boolean = false
        private set

    private var booted = false
    private var frames = 0
    private var windowStart = 0L

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(this)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        LibretroBridge.nativeInitGL()
        if (!booted) {
            booted = true
            boot()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        LibretroBridge.nativeSetViewport(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!running) return
        LibretroBridge.runFrame()
        LibretroBridge.nativeRenderFrame()

        frames++
        val now = System.currentTimeMillis()
        if (windowStart == 0L) windowStart = now
        val elapsed = now - windowStart
        if (elapsed >= 500) {
            fps = frames * 1000f / elapsed
            frames = 0
            windowStart = now
        }
    }

    private fun boot() {
        try {
            status = "Extraindo ROM integrada..."
            val rom: File = LibretroBridge.installBundledRom(appContext) ?: run {
                error = "ROM integrada não encontrada no APK."
                status = "Erro"
                return
            }

            status = "Carregando núcleo de emulação..."
            val corePath = LibretroBridge.ensureCoreMapped(appContext) ?: run {
                error = "Núcleo de emulação indisponível para este dispositivo."
                status = "Erro"
                return
            }

            val systemDir = File(appContext.filesDir, "system").apply { mkdirs() }
            val saveDir = File(appContext.filesDir, "save").apply { mkdirs() }
            LibretroBridge.nativeSetPaths(systemDir.absolutePath, saveDir.absolutePath)

            status = "Inicializando núcleo..."
            if (!LibretroBridge.loadCore(corePath)) {
                error = "Falha ao inicializar o núcleo de emulação."
                status = "Erro"
                return
            }

            status = "Carregando ${LibretroBridge.ROM_FILE_NAME}..."
            if (!LibretroBridge.loadGame(rom.absolutePath)) {
                error = "O núcleo não conseguiu carregar a ROM."
                status = "Erro"
                return
            }

            hardwareRendering = LibretroBridge.isHardwareRendering()
            running = true
            LibretroBridge.markGameRunning(true)
            status = "Em execução"
            Log.i("ExtremeN64", "Boot OK (hardware rendering = $hardwareRendering)")
        } catch (t: Throwable) {
            Log.e("ExtremeN64", "Boot failed", t)
            error = "Erro inesperado: ${t.message}"
            status = "Erro"
        }
    }

    fun pauseEmulation() { running = false }

    fun resumeEmulation() { if (booted && error == null) running = true }

    fun resetEmulation() { LibretroBridge.resetGame() }

    fun shutdownEmulation() {
        running = false
        LibretroBridge.markGameRunning(false)
        LibretroBridge.unloadGame()
    }
}
