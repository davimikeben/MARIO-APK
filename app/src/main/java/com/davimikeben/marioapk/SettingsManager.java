package com.davimikeben.marioapk;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gerenciamento de configurações salvas localmente (SharedPreferences).
 * Inclui: desempenho, gráficos, áudio, teclado (opacidade, tamanho, posição)
 * e perfis de controle.
 */
public class SettingsManager {
    private static final String PREFS_NAME = "marioapk_settings";

    // Chaves
    private static final String KEY_PERF_MODE = "perf_mode";            // 0 desempenho,1 equilibrado,2 qualidade
    private static final String KEY_GFX_RESOLUTION = "gfx_resolution";
    private static final String KEY_AUDIO_ENABLED = "audio_enabled";
    private static final String KEY_AUDIO_VOLUME = "audio_volume";
    private static final String KEY_KEYBOARD_OPACITY = "kbd_opacity";   // 0-100
    private static final String KEY_KEYBOARD_SCALE = "kbd_scale";       // 0-100
    private static final String KEY_KEYBOARD_ENABLED = "kbd_enabled";
    private static final String KEY_FPS_SHOW = "fps_show";
    private static final String KEY_MALI_MODE = "mali_mode";

    // Modos de desempenho (Mali-G52)
    public static final int PERF_PERFORMANCE = 0;
    public static final int PERF_BALANCED = 1;
    public static final int PERF_QUALITY = 2;

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getPerfMode() {
        return prefs.getInt(KEY_PERF_MODE, PERF_BALANCED);
    }
    public void setPerfMode(int mode) {
        prefs.edit().putInt(KEY_PERF_MODE, mode).apply();
    }

    public String getGfxResolution() {
        return prefs.getString(KEY_GFX_RESOLUTION, "1280x720");
    }
    public void setGfxResolution(String res) {
        prefs.edit().putString(KEY_GFX_RESOLUTION, res).apply();
    }

    public boolean isAudioEnabled() {
        return prefs.getBoolean(KEY_AUDIO_ENABLED, true);
    }
    public void setAudioEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUDIO_ENABLED, enabled).apply();
    }

    public float getAudioVolume() {
        return prefs.getFloat(KEY_AUDIO_VOLUME, 1.0f);
    }
    public void setAudioVolume(float vol) {
        prefs.edit().putFloat(KEY_AUDIO_VOLUME, vol).apply();
    }

    public int getKeyboardOpacity() {
        return prefs.getInt(KEY_KEYBOARD_OPACITY, 80);
    }
    public void setKeyboardOpacity(int opacity) {
        prefs.edit().putInt(KEY_KEYBOARD_OPACITY, opacity).apply();
    }

    public int getKeyboardScale() {
        return prefs.getInt(KEY_KEYBOARD_SCALE, 100);
    }
    public void setKeyboardScale(int scale) {
        prefs.edit().putInt(KEY_KEYBOARD_SCALE, scale).apply();
    }

    public boolean isKeyboardEnabled() {
        return prefs.getBoolean(KEY_KEYBOARD_ENABLED, true);
    }
    public void setKeyboardEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_KEYBOARD_ENABLED, enabled).apply();
    }

    public boolean isFpsShown() {
        return prefs.getBoolean(KEY_FPS_SHOW, true);
    }
    public void setFpsShown(boolean show) {
        prefs.edit().putBoolean(KEY_FPS_SHOW, show).apply();
    }

    public String getMaliMode() {
        return prefs.getString(KEY_MALI_MODE, "balanced");
    }
    public void setMaliMode(String mode) {
        prefs.edit().putString(KEY_MALI_MODE, mode).apply();
    }

    /** Restaura todos os valores padrão (restaurar configurações). */
    public void restoreDefaults() {
        prefs.edit()
                .putInt(KEY_PERF_MODE, PERF_BALANCED)
                .putString(KEY_GFX_RESOLUTION, "1280x720")
                .putBoolean(KEY_AUDIO_ENABLED, true)
                .putFloat(KEY_AUDIO_VOLUME, 1.0f)
                .putInt(KEY_KEYBOARD_OPACITY, 80)
                .putInt(KEY_KEYBOARD_SCALE, 100)
                .putBoolean(KEY_KEYBOARD_ENABLED, true)
                .putBoolean(KEY_FPS_SHOW, true)
                .putString(KEY_MALI_MODE, "balanced")
                .apply();
    }
}
