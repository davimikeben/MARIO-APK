package com.davimikeben.sm64emulator.ui

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davimikeben.sm64emulator.core.EmulatorRuntime
import com.davimikeben.sm64emulator.core.LibretroConstants
import com.davimikeben.sm64emulator.databinding.ActivityGameBinding
import com.davimikeben.sm64emulator.input.VirtualKeyboardView
import com.davimikeben.sm64emulator.perf.PerfManager
import com.davimikeben.sm64emulator.util.SettingsManager
import java.io.File

/**
 * Activity do jogo. Carrega ROM, inicializa core, renderiza, captura input.
 *
 * Fluxo:
 *  1. onCreate → configura SurfaceView + overlay de teclado + FPS + botão flutuante
 *  2. surfaceCreated → EmulatorRuntime.start(core, surface, onReady, onError)
 *  3. onReady → resumeLoop() → jogo inicia
 *  4. FPS overlay mostra contador discreto no canto
 *  5. Botão flutuante abre menu in-game sem reiniciar
 */
class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private val runtime by lazy { EmulatorRuntime.get(applicationContext) }

    companion object {
        private const val TAG = "GameActivity"
        private const val CORE_LIB = "libretro_mupen64plus_next.so"
    }

    /**
     * v1.2.0: resolução robusta do path do core libretro.
     * 1) nativeLibraryDir (com useLegacyPackaging=true os .so são extraídos para cá);
     * 2) fallback: extrai do APK p/ filesDir/core (só funciona em Android ≤ 9);
     * 3) último recurso: path esperado — o dlopen vai falhar com dlerror claro.
     */
    private fun resolveCorePath(): String {
        val dir = applicationInfo.nativeLibraryDir
        File(dir, CORE_LIB).takeIf { it.exists() && it.length() > 0 }?.let {
            Log.i(TAG, "Core encontrado em nativeLibraryDir: ${it.absolutePath}")
            return it.absolutePath
        }
        try {
            val dst = File(File(filesDir, "core"), CORE_LIB)
            if (!dst.exists() || dst.length() == 0L) {
                java.util.zip.ZipFile(applicationInfo.sourceDir).use { zip ->
                    val entry = zip.getEntry("lib/arm64-v8a/$CORE_LIB")
                        ?: throw IllegalStateException("$CORE_LIB ausente no APK")
                    zip.getInputStream(entry).use { ins -> dst.outputStream().use { ins.copyTo(it) } }
                }
            }
            Log.w(TAG, "Core extraído do APK para ${dst.absolutePath} (fallback)")
            return dst.absolutePath
        } catch (t: Throwable) {
            Log.e(TAG, "Fallback de extração falhou", t)
            return "$dir/$CORE_LIB"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SettingsManager.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura teclado virtual
        binding.keyboardView.apply {
            opacity = SettingsManager.keyboardOpacity
            scalePercent = SettingsManager.keyboardScale
            style = SettingsManager.keyboardStyle
            hapticEnabled = SettingsManager.hapticEnabled
            listener = object : VirtualKeyboardView.KeyboardListener {
                override fun onButton(buttonId: Int, pressed: Boolean) {
                    runtime.setInput(buttonId, if (pressed) 1 else 0)
                }
                override fun onAnalog(stick: Int, axis: Int, value: Float) {
                    // stick 0=esquerdo (N64 Control Stick), 1=direito (C-Left/C-Down)
                    runtime.setInputState(LibretroConstants.RETRO_DEVICE_ANALOG, stick, axis, (value * 32767).toInt())
                }
            }
        }

        // FPS counter — discreto, configurável
        binding.fpsOverlay.alpha = 0.85f
        binding.fpsOverlay.setMonitorMode(
            fpsOnly = SettingsManager.showFpsOnly,
            showCpuGpu = SettingsManager.showCpuGpu,
            showRam = SettingsManager.showRam,
            showTemp = SettingsManager.showTemp
        )

        // Botão flutuante abre menu in-game
        binding.floatingMenuBtn.setOnClickListener { showInGameMenu() }

        // Configura Surface
        binding.gameSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val corePath = resolveCorePath()
                runtime.start(corePath, holder.surface,
                    onReady = {
                        runtime.setPerformanceMode(SettingsManager.performanceMode)
                        runtime.resumeLoop()
                        runOnUiThread { Toast.makeText(this@GameActivity, "▶ Jogo iniciado", Toast.LENGTH_SHORT).show() }
                    },
                    onError = { msg ->
                        runOnUiThread {
                            Toast.makeText(this@GameActivity, "ERRO: $msg", Toast.LENGTH_LONG).show()
                            binding.fpsOverlay.setError(msg)
                        }
                    })
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                runtime.pauseLoop()
            }
        })

        // Atualiza FPS overlay periodicamente
        binding.fpsOverlay.start(this, runtime)
    }

    override fun onPause() {
        super.onPause()
        binding.keyboardView.releaseAll()
        runtime.pauseLoop()
    }

    override fun onResume() {
        super.onResume()
        if (runtime.state == EmulatorRuntime.State.PAUSED) runtime.resumeLoop()
    }

    override fun onDestroy() {
        binding.fpsOverlay.stop()
        runtime.deinit()
        super.onDestroy()
    }

    private fun showInGameMenu() {
        // Pausa loop mas NÃO destrói nem reinicializa o core
        runtime.pauseLoop()
        InGameMenuDialog(
            context = this,
            onModifyControls = {
                // Permite mover/redimensionar botões do teclado
                binding.keyboardView.toggleEditMode()
            },
            onTransparency = {
                SettingsSheetDialog(
                    this,
                    "🎨 Transparência",
                    listOf(
                        SettingsSheetDialog.Item("Opacidade do teclado", { "${SettingsManager.keyboardOpacity}%" }, listOf(-10, +10)) { d ->
                            SettingsManager.keyboardOpacity = (SettingsManager.keyboardOpacity + d).coerceIn(10, 100)
                            binding.keyboardView.opacity = SettingsManager.keyboardOpacity
                        },
                        SettingsSheetDialog.Item("Escala", { "${SettingsManager.keyboardScale}%" }, listOf(-10, +10)) { d ->
                            SettingsManager.keyboardScale = (SettingsManager.keyboardScale + d).coerceIn(50, 200)
                            binding.keyboardView.scalePercent = SettingsManager.keyboardScale
                        }
                    )
                ).show()
            },
            onGraphics = {
                SettingsSheetDialog(
                    this,
                    "🎨 Gráficos",
                    listOf(
                        SettingsSheetDialog.Item("Resolução interna", { "${SettingsManager.internalResolution}x" }, listOf(-1, +1)) { d ->
                            SettingsManager.internalResolution = (SettingsManager.internalResolution + d).coerceIn(1, 4)
                        },
                        SettingsSheetDialog.Item("Filtragem", { when (SettingsManager.filtering) { 0 -> "Nearest"; 1 -> "Linear"; 2 -> "Bilinear"; else -> "?" } }, listOf(-1, +1)) { d ->
                            SettingsManager.filtering = (SettingsManager.filtering + d + 3) % 3
                        }
                    )
                ).show()
            },
            onPerfInfo = {
                SettingsSheetDialog(
                    this,
                    "📊 Desempenho",
                    listOf(
                        SettingsSheetDialog.Item("FPS", { "${runtime.fps.toInt()}" }, emptyList()) {},
                        SettingsSheetDialog.Item("CPU", { "${PerfManager.cpuUsagePercent()}%" }, emptyList()) {},
                        SettingsSheetDialog.Item("RAM usada", { "${PerfManager.ramUsedMb()} MB" }, emptyList()) {},
                        SettingsSheetDialog.Item("RAM total", { "${PerfManager.ramTotalMb()} MB" }, emptyList()) {},
                        SettingsSheetDialog.Item("Modo atual", { PerfManager.modeName(SettingsManager.performanceMode) }, listOf(-1, +1)) { d ->
                            val m = (SettingsManager.performanceMode + d + 3) % 3
                            PerfManager.applyMode(m)
                            runtime.setPerformanceMode(m)
                        }
                    )
                ).show()
            },
            onRestart = {
                runtime.reset()
                runtime.resumeLoop()
            },
            onSaveConfig = {
                val snap = SettingsManager.snapshot()
                val json = com.davimikeben.sm64emulator.util.JsonHelper.toJson(snap)
                openFileOutput("settings_backup.json", MODE_PRIVATE).use { it.write(json.toByteArray()) }
                Toast.makeText(this, "✓ Salvo", Toast.LENGTH_SHORT).show()
            },
            onBackToMenu = {
                runtime.deinit()
                finish()
            },
            onResumeGame = {
                runtime.resumeLoop()
            }
        ).show()
    }
}
