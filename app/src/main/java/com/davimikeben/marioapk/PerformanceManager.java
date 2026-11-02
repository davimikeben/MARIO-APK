package com.davimikeben.marioapk;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

/**
 * Gerenciador de desempenho otimizado para GPUs Mali-G52.
 * Três modos: DESEMPENHO, EQUILIBRADO, QUALIDADE.
 * Detecta capacidades reais do dispositivo; não presume uniformidade.
 */
public class PerformanceManager {
    public static final int MODE_PERFORMANCE = 0;
    public static final int MODE_BALANCED = 1;
    public static final int MODE_QUALITY = 2;

    private final Context context;
    private int mode = MODE_BALANCED;

    public final String gpuRenderer;
    public final String gpuVendor;
    public final int glEsVersion;
    public final int maxTextureSize;
    public final boolean isMaliG52;
    public final int availableProcessors;
    public final long totalMemoryBytes;

    public PerformanceManager(Context context) {
        this.context = context.getApplicationContext();
        this.gpuRenderer = safe(() -> GLES20.glGetString(GLES20.GL_RENDERER), "unknown");
        this.gpuVendor = safe(() -> GLES20.glGetString(GLES20.GL_VENDOR), "unknown");
        this.glEsVersion = detectGlEsVersion();
        this.maxTextureSize = detectMaxTextureSize();
        this.isMaliG52 = detectMaliG52();
        this.availableProcessors = Runtime.getRuntime().availableProcessors();
        this.totalMemoryBytes = detectTotalMemory();
        Log.i("Perf", "GPU=" + gpuRenderer + " Vendor=" + gpuVendor +
                " GLES=" + glEsVersion + " maxTex=" + maxTextureSize +
                " MaliG52=" + isMaliG52 + " cores=" + availableProcessors +
                " memMB=" + (totalMemoryBytes / 1048576));
    }

    private static String safe(java.util.function.Supplier<String> s, String def) {
        try { String v = s.get(); return v == null ? def : v; }
        catch (Throwable t) { return def; }
    }

    public void setMode(int mode) { this.mode = mode; }
    public int getMode() { return mode; }
    public String getModeName() {
        switch (mode) {
            case MODE_PERFORMANCE: return "Desempenho";
            case MODE_QUALITY: return "Qualidade";
            default: return "Equilibrado";
        }
    }
    public String getRecommendedResolution() {
        switch (mode) {
            case MODE_PERFORMANCE: return "960x540";
            case MODE_QUALITY: return "1920x1080";
            default: return "1280x720";
        }
    }
    public int getTargetFps() {
        return (mode == MODE_QUALITY) ? 30 : 60;
    }
    public boolean isFramePacingEnabled() { return true; }
    public boolean isVulkanPreferred() { return glEsVersion >= 0x30000 && !isMaliG52; }
    public int getGlEsVersion() { return glEsVersion; }

    private int detectGlEsVersion() {
        try {
            String v = GLES20.glGetString(GLES20.GL_VERSION);
            if (v != null) {
                String head = v.split(" ")[0].replace("OpenGL", "").trim();
                String[] p = head.split("\\.");
                if (p.length >= 2) {
                    return (Integer.parseInt(p[0]) << 16) | (Integer.parseInt(p[1]) << 8);
                }
            }
        } catch (Throwable t) {}
        return 0x20000;
    }
    private int detectMaxTextureSize() {
        try {
            int[] sz = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, sz, 0);
            return sz[0];
        } catch (Throwable t) { return 2048; }
    }
    private boolean detectMaliG52() {
        String r = gpuRenderer.toLowerCase();
        String v = gpuVendor.toLowerCase();
        return r.contains("mali-g52") || v.contains("mali-g52");
    }
    private long detectTotalMemory() {
        try { return android.os.Process.getTotalMemory(); }
        catch (Throwable t) { return Runtime.getRuntime().maxMemory(); }
    }
    public String capabilitiesReport() {
        return "=== Capacidades do dispositivo ===\n" +
                "GPU Renderer: " + gpuRenderer + "\n" +
                "GPU Vendor: " + gpuVendor + "\n" +
                "OpenGL ES: " + glEsVersion + "\n" +
                "Max Texture Size: " + maxTextureSize + "\n" +
                "Mali-G52 detectado: " + isMaliG52 + "\n" +
                "CPUs: " + availableProcessors + "\n" +
                "Memória total: " + (totalMemoryBytes / 1048576) + " MB\n" +
                "Modo: " + getModeName() + "\n" +
                "Resolução: " + getRecommendedResolution() + "\n" +
                "Frame pacing: " + isFramePacingEnabled() + "\n" +
                "Vulkan preferido: " + isVulkanPreferred() + "\n" +
                "Android: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ")";
    }
}
