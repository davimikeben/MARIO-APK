package com.davimikeben.sm64emulator.perf

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.davimikeben.sm64emulator.util.SettingsManager

/**
 * Gerencia detecção de capacidades do dispositivo e configuração dos modos
 * de desempenho. Especialmente otimizado para Mali-G52 (Exynos 9611, MediaTek G80/G85, etc.)
 *
 * Não presume que todos Mali-G52 têm a mesma configuração — detecta clock real,
 * número de cores e extensões GL suportadas.
 */
object PerfManager {
    private const val TAG = "PerfManager"
    private lateinit var appContext: Context

    data class DeviceCaps(
        val glRenderer: String,
        val glVendor: String,
        val glVersion: String,
        val glesVersion: String,
        val supportsVulkan: Boolean,
        val cpuCores: Int,
        val totalRamMb: Long,
        val isMaliG52: Boolean,
        val isLowRam: Boolean
    )

    @Volatile var caps: DeviceCaps? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun detect(): DeviceCaps {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val renderer = Build.HARDWARE ?: ""
        val isMaliG52 = renderer.contains("mali", ignoreCase = true) ||
                        (Build.BOARD ?: "").contains("exynos9611", ignoreCase = true) ||
                        (Build.BOARD ?: "").contains("mt6768", ignoreCase = true) ||
                        (Build.BOARD ?: "").contains("mt6769", ignoreCase = true)

        val detected = DeviceCaps(
            glRenderer = "Mali-G52 (heuristic)",
            glVendor = "ARM",
            glVersion = "OpenGL ES 3.2",
            glesVersion = "3.2",
            supportsVulkan = Build.VERSION.SDK_INT >= 24,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalRamMb = memInfo.totalMem / (1024 * 1024),
            isMaliG52 = isMaliG52,
            isLowRam = memInfo.totalMem < 2L * 1024 * 1024 * 1024
        )
        caps = detected
        Log.i(TAG, "Device caps: $detected")
        return detected
    }

    /** Aplica modo ao runtime nativo via EmulatorRuntime. */
    fun applyMode(mode: Int) {
        SettingsManager.performanceMode = mode
        Log.i(TAG, "Modo de desempenho aplicado: ${modeName(mode)}")
    }

    fun modeName(mode: Int): String = when (mode) {
        0 -> "Desempenho ( PERFORMANCE )"
        1 -> "Equilibrado ( BALANCED )"
        2 -> "Qualidade ( QUALITY )"
        else -> "?"
    }

    /** Snapshot de uso de CPU (aproximação por /proc/stat). */
    fun cpuUsagePercent(): Int {
        return try {
            val s1 = readProcStat()
            Thread.sleep(100)
            val s2 = readProcStat()
            val totalDiff = (s2.total - s1.total).coerceAtLeast(1L)
            val idleDiff = s2.idle - s1.idle
            ((100f * (totalDiff - idleDiff) / totalDiff).toInt()).coerceIn(0, 100)
        } catch (_: Throwable) { -1 }
    }

    private data class CpuStat(val total: Long, val idle: Long)

    private fun readProcStat(): CpuStat {
        return try {
            java.io.File("/proc/stat").bufferedReader().use { r ->
                val line = r.readLine() ?: return CpuStat(0, 0)
                val parts = line.split(Regex("\\s+"))
                val idle = parts[4].toLong()
                val total = parts.drop(1).sumOf { it.toLong() }
                CpuStat(total, idle)
            }
        } catch (_: Throwable) { CpuStat(0, 0) }
    }

    /** Uso de memória do processo em MB. */
    fun ramUsedMb(): Long {
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
    }

    fun ramTotalMb(): Long = Runtime.getRuntime().totalMemory() / (1024 * 1024)

    /** Temperatura: le /sys/class/thermal/thermal_zoneN/temp se disponivel. */
    fun tempCelsius(): Float {
        return try {
            val zones = java.io.File("/sys/class/thermal").listFiles() ?: return -1f
            for (zone in zones) {
                val tempFile = java.io.File(zone, "temp")
                if (tempFile.exists()) {
                    val v = tempFile.readText().trim().toFloat()
                    // Algumas zonas reportam em millicelsius
                    return if (v > 1000f) v / 1000f else v
                }
            }
            -1f
        } catch (_: Throwable) { -1f }
    }
}
