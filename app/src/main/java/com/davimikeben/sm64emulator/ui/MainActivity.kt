package com.davimikeben.sm64emulator.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davimikeben.sm64emulator.core.LibretroConstants
import com.davimikeben.sm64emulator.databinding.ActivityMainBinding
import com.davimikeben.sm64emulator.perf.PerfManager
import com.davimikeben.sm64emulator.rom.RomLoader
import com.davimikeben.sm64emulator.util.SettingsManager

/**
 * Tela principal — menu próprio do aplicativo.
 * Não mostra UI tradicional de emulador: apenas o menu personalizado.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SettingsManager.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PerfManager.init(applicationContext)
        val caps = PerfManager.detect()

        // Cabeçalho dinâmico com info do dispositivo
        binding.deviceInfo.text = buildString {
            append("Device: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append('\n')
            append("CPU cores: ").append(caps.cpuCores).append("   RAM: ").append(caps.totalRamMb).append(" MB\n")
            append("GPU: ").append(caps.glRenderer).append(" (").append(caps.glesVersion).append(")\n")
            if (caps.isMaliG52) append("✓ Mali-G52 detectado — perfil otimizado ativo")
        }

        binding.btnPlay.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        binding.btnKeyboard.setOnClickListener { showKeyboardSettings() }
        binding.btnPerformance.setOnClickListener { showPerformanceSettings() }
        binding.btnGraphics.setOnClickListener { showGraphicsSettings() }
        binding.btnAudio.setOnClickListener { showAudioSettings() }
        binding.btnScreen.setOnClickListener { showScreenSettings() }
        binding.btnMonitor.setOnClickListener { showMonitorSettings() }
        binding.btnSave.setOnClickListener {
            val snap = SettingsManager.snapshot()
            val json = com.davimikeben.sm64emulator.util.JsonHelper.toJson(snap)
            openFileOutput("settings_backup.json", MODE_PRIVATE).use { it.write(json.toByteArray()) }
            Toast.makeText(this, "✓ Configurações salvas", Toast.LENGTH_SHORT).show()
        }
        binding.btnRestore.setOnClickListener {
            try {
                openFileInput("settings_backup.json").use { input ->
                    val json = input.bufferedReader().readText()
                    val map = com.davimikeben.sm64emulator.util.JsonHelper.fromJsonToSettings(json)
                    SettingsManager.restore(map)
                }
                Toast.makeText(this, "✓ Configurações restauradas", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Sem backup anterior", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnVerifyRom.setOnClickListener { verifyRomIntegrity() }

        // Pré-validação: confirma que a ROM está dentro do APK e é legível
        verifyRomIntegrity(silent = true)
    }

    private fun verifyRomIntegrity(silent: Boolean = false) {
        Thread {
            try {
                val romFile = RomLoader.ensureExtracted(this)
                val hash = RomLoader.sha256(romFile)
                val ok = hash.equals(RomLoader.ASSET_ROM_SHA256, ignoreCase = true)
                runOnUiThread {
                    binding.romInfo.text = buildString {
                        append("ROM: ").append(RomLoader.EXTRACTED_NAME).append('\n')
                        append("Tamanho: ").append(romFile.length()).append(" bytes (8 MiB)\n")
                        append("SHA-256:\n").append(hash).append('\n')
                        append("Status: ").append(if (ok) "✓ ÍNTEGRA" else "✗ CORROMPIDA")
                    }
                    if (!silent) {
                        Toast.makeText(this,
                            if (ok) "✓ ROM íntegra confirmada" else "✗ Falha de integridade",
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.romInfo.text = "ERRO: ${e.message}"
                    if (!silent) Toast.makeText(this, "ERRO: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showKeyboardSettings() {
        SettingsSheetDialog(
            context = this,
            title = "🎮 Configuração do teclado",
            items = listOf(
                SettingsSheetDialog.Item("Opacidade", { "${SettingsManager.keyboardOpacity}%" }, listOf(-10, +10)) { d ->
                    SettingsManager.keyboardOpacity = (SettingsManager.keyboardOpacity + d).coerceIn(10, 100)
                },
                SettingsSheetDialog.Item("Escala", { "${SettingsManager.keyboardScale}%" }, listOf(-10, +10)) { d ->
                    SettingsManager.keyboardScale = (SettingsManager.keyboardScale + d).coerceIn(50, 200)
                },
                SettingsSheetDialog.Item("Estilo", { keyboardStyleName(SettingsManager.keyboardStyle) }, listOf(-1, +1)) { d ->
                    SettingsManager.keyboardStyle = (SettingsManager.keyboardStyle + d + 3) % 3
                },
                SettingsSheetDialog.Item("Vibração", { if (SettingsManager.hapticEnabled) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.hapticEnabled = !SettingsManager.hapticEnabled
                },
                SettingsSheetDialog.Item("Perfil", { SettingsManager.keyboardProfile }, listOf(0)) {
                    SettingsManager.keyboardProfile = if (SettingsManager.keyboardProfile == "default") "alt" else "default"
                }
            )
        ).show()
    }

    private fun keyboardStyleName(s: Int) = when (s) { 0 -> "Clássico"; 1 -> "Moderno"; 2 -> "Neon"; else -> "?" }

    private fun showPerformanceSettings() {
        val caps = PerfManager.caps ?: PerfManager.detect()
        SettingsSheetDialog(
            context = this,
            title = "⚡ Desempenho (Mali-G52)",
            items = listOf(
                SettingsSheetDialog.Item("Modo", { PerfManager.modeName(SettingsManager.performanceMode) }, listOf(-1, +1)) { d ->
                    val newMode = (SettingsManager.performanceMode + d + 3) % 3
                    PerfManager.applyMode(newMode)
                },
                SettingsSheetDialog.Item("Resolução interna", { "${SettingsManager.internalResolution}x" }, listOf(-1, +1)) { d ->
                    SettingsManager.internalResolution = (SettingsManager.internalResolution + d).coerceIn(1, 4)
                },
                SettingsSheetDialog.Item("VSync", { if (SettingsManager.vsync) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.vsync = !SettingsManager.vsync
                },
                SettingsSheetDialog.Item("CPU cores detectadas", { "${caps.cpuCores}" }, emptyList()) {},
                SettingsSheetDialog.Item("RAM total", { "${caps.totalRamMb} MB" }, emptyList()) {},
                SettingsSheetDialog.Item("Vulkan suportado", { if (caps.supportsVulkan) "SIM" else "NÃO" }, emptyList()) {}
            )
        ).show()
    }

    private fun showGraphicsSettings() {
        SettingsSheetDialog(
            context = this,
            title = "🎨 Gráficos",
            items = listOf(
                SettingsSheetDialog.Item("API de renderização", { if (SettingsManager.renderApi == 0) "OpenGL ES" else "Vulkan" }, listOf(0)) {
                    SettingsManager.renderApi = if (SettingsManager.renderApi == 0) 1 else 0
                },
                SettingsSheetDialog.Item("Filtragem", { when (SettingsManager.filtering) { 0 -> "Nearest"; 1 -> "Linear"; 2 -> "Bilinear"; else -> "?" } }, listOf(-1, +1)) { d ->
                    SettingsManager.filtering = (SettingsManager.filtering + d + 3) % 3
                },
                SettingsSheetDialog.Item("VSync", { if (SettingsManager.vsync) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.vsync = !SettingsManager.vsync
                },
                SettingsSheetDialog.Item("Resolução interna", { "${SettingsManager.internalResolution}x" }, listOf(-1, +1)) { d ->
                    SettingsManager.internalResolution = (SettingsManager.internalResolution + d).coerceIn(1, 4)
                }
            )
        ).show()
    }

    private fun showAudioSettings() {
        SettingsSheetDialog(
            context = this,
            title = "🔊 Áudio",
            items = listOf(
                SettingsSheetDialog.Item("Som", { if (SettingsManager.audioEnabled) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.audioEnabled = !SettingsManager.audioEnabled
                },
                SettingsSheetDialog.Item("Volume", { "${SettingsManager.audioVolume}%" }, listOf(-10, +10)) { d ->
                    SettingsManager.audioVolume = (SettingsManager.audioVolume + d).coerceIn(0, 100)
                }
            )
        ).show()
    }

    private fun showScreenSettings() {
        SettingsSheetDialog(
            context = this,
            title = "📱 Tela",
            items = listOf(
                SettingsSheetDialog.Item("Manter tela acesa", { if (SettingsManager.keepScreenOn) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.keepScreenOn = !SettingsManager.keepScreenOn
                },
                SettingsSheetDialog.Item("Modo de tela", { when (SettingsManager.stretchMode) { 0 -> "Aspecto"; 1 -> "Esticar"; 2 -> "4:3"; else -> "?" } }, listOf(-1, +1)) { d ->
                    SettingsManager.stretchMode = (SettingsManager.stretchMode + d + 3) % 3
                },
                SettingsSheetDialog.Item("Brilho", { "${SettingsManager.brightness}%" }, listOf(-10, +10)) { d ->
                    SettingsManager.brightness = (SettingsManager.brightness + d).coerceIn(50, 100)
                }
            )
        ).show()
    }

    private fun showMonitorSettings() {
        SettingsSheetDialog(
            context = this,
            title = "📊 Monitoramento",
            items = listOf(
                SettingsSheetDialog.Item("Mostrar apenas FPS", { if (SettingsManager.showFpsOnly) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.showFpsOnly = !SettingsManager.showFpsOnly
                },
                SettingsSheetDialog.Item("Mostrar CPU/GPU", { if (SettingsManager.showCpuGpu) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.showCpuGpu = !SettingsManager.showCpuGpu
                },
                SettingsSheetDialog.Item("Mostrar RAM", { if (SettingsManager.showRam) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.showRam = !SettingsManager.showRam
                },
                SettingsSheetDialog.Item("Mostrar Temperatura", { if (SettingsManager.showTemp) "ON" else "OFF" }, listOf(0)) {
                    SettingsManager.showTemp = !SettingsManager.showTemp
                }
            )
        ).show()
    }
}
