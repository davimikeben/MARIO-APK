package com.davimikeben.sm64emulator.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Gerenciador central de preferências. Salva/carrega configurações de teclado,
 * gráficos, áudio, desempenho, tela, monitoramento.
 */
object SettingsManager {
    private const val PREFS = "sm64_settings"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // —— Teclado ——
    var keyboardOpacity: Int  // 0-100
        get() = prefs.getInt("kbd_opacity", 70)
        set(v) = prefs.edit().putInt("kbd_opacity", v.coerceIn(10, 100)).apply()

    var keyboardScale: Int    // 50-200 (%)
        get() = prefs.getInt("kbd_scale", 100)
        set(v) = prefs.edit().putInt("kbd_scale", v.coerceIn(50, 200)).apply()

    var keyboardStyle: Int    // 0=classic 1=modern 2=neon
        get() = prefs.getInt("kbd_style", 1)
        set(v) = prefs.edit().putInt("kbd_style", v).apply()

    var keyboardProfile: String
        get() = prefs.getString("kbd_profile", "default") ?: "default"
        set(v) = prefs.edit().putString("kbd_profile", v).apply()

    var buttonPositions: String  // JSON map buttonId→[x,y]
        get() = prefs.getString("kbd_positions", "{}") ?: "{}"
        set(v) = prefs.edit().putString("kbd_positions", v).apply()

    var enabledButtons: String   // JSON list of enabled button ids
        get() = prefs.getString("kbd_enabled", "all") ?: "all"
        set(v) = prefs.edit().putString("kbd_enabled", v).apply()

    var hapticEnabled: Boolean
        get() = prefs.getBoolean("kbd_haptic", true)
        set(v) = prefs.edit().putBoolean("kbd_haptic", v).apply()

    // —— Gráficos ——
    var renderApi: Int  // 0=OpenGLES 1=Vulkan (se suportado)
        get() = prefs.getInt("gfx_api", 0)
        set(v) = prefs.edit().putInt("gfx_api", v).apply()

    var internalResolution: Int  // 1x,2x,...
        get() = prefs.getInt("gfx_resolution", 1)
        set(v) = prefs.edit().putInt("gfx_resolution", v.coerceIn(1, 4)).apply()

    var filtering: Int  // 0=nearest 1=linear 2=bilinear
        get() = prefs.getInt("gfx_filtering", 1)
        set(v) = prefs.edit().putInt("gfx_filtering", v).apply()

    var vsync: Boolean
        get() = prefs.getBoolean("gfx_vsync", true)
        set(v) = prefs.edit().putBoolean("gfx_vsync", v).apply()

    // —— Desempenho ——
    var performanceMode: Int  // 0=performance 1=balanced 2=quality
        get() = prefs.getInt("perf_mode", 1)
        set(v) = prefs.edit().putInt("perf_mode", v.coerceIn(0, 2)).apply()

    // —— Áudio ——
    var audioEnabled: Boolean
        get() = prefs.getBoolean("audio_enabled", true)
        set(v) = prefs.edit().putBoolean("audio_enabled", v).apply()

    var audioVolume: Int  // 0-100
        get() = prefs.getInt("audio_volume", 80)
        set(v) = prefs.edit().putInt("audio_volume", v.coerceIn(0, 100)).apply()

    // —— Tela ——
    var keepScreenOn: Boolean
        get() = prefs.getBoolean("screen_keep_on", true)
        set(v) = prefs.edit().putBoolean("screen_keep_on", v).apply()

    var stretchMode: Int  // 0=aspect 1=stretch 2=4:3
        get() = prefs.getInt("screen_stretch", 0)
        set(v) = prefs.edit().putInt("screen_stretch", v).apply()

    var brightness: Int
        get() = prefs.getInt("screen_brightness", 100)
        set(v) = prefs.edit().putInt("screen_brightness", v.coerceIn(50, 100)).apply()

    // —— Monitoramento ——
    var showFpsOnly: Boolean
        get() = prefs.getBoolean("mon_fps_only", true)
        set(v) = prefs.edit().putBoolean("mon_fps_only", v).apply()

    var showCpuGpu: Boolean
        get() = prefs.getBoolean("mon_cpu_gpu", false)
        set(v) = prefs.edit().putBoolean("mon_cpu_gpu", v).apply()

    var showRam: Boolean
        get() = prefs.getBoolean("mon_ram", false)
        set(v) = prefs.edit().putBoolean("mon_ram", v).apply()

    var showTemp: Boolean
        get() = prefs.getBoolean("mon_temp", false)
        set(v) = prefs.edit().putBoolean("mon_temp", v).apply()

    // —— Backup/Restore completo ——
    fun snapshot(): Map<String, *> = prefs.all

    fun restore(snapshot: Map<String, *>) {
        val ed = prefs.edit()
        ed.clear()
        snapshot.forEach { (k, v) ->
            when (v) {
                is Boolean -> ed.putBoolean(k, v)
                is Int -> ed.putInt(k, v)
                is Long -> ed.putLong(k, v)
                is Float -> ed.putFloat(k, v)
                is String -> ed.putString(k, v)
                is Set<*> -> @Suppress("UNCHECKED_CAST") ed.putStringSet(k, v as Set<String>)
            }
        }
        ed.apply()
    }
}
