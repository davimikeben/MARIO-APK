package com.davimikeben.extremen64

import android.content.Context
import android.content.SharedPreferences

enum class PerformanceMode(val label: String, val description: String) {
    DESEMPENHO("⚡ DESEMPENHO", "Prioridade total à fluidez (ideal Mali-G52)"),
    EQUILIBRADO("⚖️ EQUILIBRADO", "Equilíbrio entre imagem e desempenho"),
    QUALIDADE("🎨 QUALIDADE", "Melhor imagem possível, resolução cheia")
}

/**
 * Persists user preferences. "Salvar" writes the current state, "Restaurar"
 * brings the defaults back.
 */
object SettingsManager {
    private const val PREFS = "ExtremeN64Settings"

    private const val K_MODE = "performance_mode"
    private const val K_SHOW_FPS = "show_fps"
    private const val K_OPACITY = "controls_opacity"
    private const val K_SCALE = "controls_scale"
    private const val K_VIBRATION = "vibration"
    private const val K_AUDIO = "audio_enabled"
    private const val K_VFMODE = "video_filter"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var performanceMode: PerformanceMode
        get() {
            val raw = prefs.getString(K_MODE, PerformanceMode.EQUILIBRADO.name)
            return runCatching { PerformanceMode.valueOf(raw!!) }.getOrDefault(PerformanceMode.EQUILIBRADO)
        }
        set(value) = prefs.edit().putString(K_MODE, value.name).apply()

    var showFps: Boolean
        get() = prefs.getBoolean(K_SHOW_FPS, true)
        set(value) = prefs.edit().putBoolean(K_SHOW_FPS, value).apply()

    var controlsOpacity: Float
        get() = prefs.getFloat(K_OPACITY, 0.45f)
        set(value) = prefs.edit().putFloat(K_OPACITY, value).apply()

    var controlsScale: Float
        get() = prefs.getFloat(K_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(K_SCALE, value).apply()

    var vibration: Boolean
        get() = prefs.getBoolean(K_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(K_VIBRATION, value).apply()

    var audioEnabled: Boolean
        get() = prefs.getBoolean(K_AUDIO, true)
        set(value) = prefs.edit().putBoolean(K_AUDIO, value).apply()

    var videoFilter: String
        get() = prefs.getString(K_VFMODE, "Suave") ?: "Suave"
        set(value) = prefs.edit().putString(K_VFMODE, value).apply()

    /** Renderer the device will use - chosen from the performance profile. */
    fun preferredGLESVersion(): Int =
        if (performanceMode == PerformanceMode.DESEMPENHO) 2 else 3

    fun saveSnapshot() {
        // Values are written eagerly; this simply confirms persistence.
        prefs.edit().putBoolean("snapshot_saved", true).apply()
    }

    // Generic accessors used by the on-screen control layout editor.
    fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    fun putFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }

    fun restoreDefaults() {
        prefs.edit().clear().apply()
    }

    fun snapshotSummary(): String = buildString {
        appendLine("Perfil: ${performanceMode.label}")
        appendLine("FPS visível: ${if (showFps) "sim" else "não"}")
        appendLine("Opacidade dos controles: ${(controlsOpacity * 100).toInt()}%")
        appendLine("Escala dos controles: ${"%.2f".format(controlsScale)}x")
        appendLine("Vibração: ${if (vibration) "ativada" else "desativada"}")
        appendLine("Áudio: ${if (audioEnabled) "ativado" else "desativado"}")
        append("Filtro de vídeo: $videoFilter")
    }
}
