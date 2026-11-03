package com.davimikeben.sm64emulator.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.davimikeben.sm64emulator.core.EmulatorRuntime
import com.davimikeben.sm64emulator.perf.PerfManager

/**
 * Overlay discreto que mostra FPS por padrão.
 * Pode opcionalmente mostrar CPU/GPU/RAM/Temp quando configurado.
 */
class FpsOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00FF66")
        textSize = 28f
        setShadowLayer(4f, 1f, 1f, Color.BLACK)
    }
    private val paintErr = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF3B30")
        textSize = 24f
    }

    private var fps: Float = 0f
    private var cpu: Int = 0
    private var ramUsed: Long = 0
    private var ramTotal: Long = 0
    private var temp: Float = -1f
    private var errorMsg: String? = null
    private var fpsOnly = true
    private var showCpuGpu = false
    private var showRam = false
    private var showTemp = false

    private val handler = Handler(Looper.getMainLooper())
    private var runtime: EmulatorRuntime? = null
    private val updateRunnable = object : Runnable {
        override fun run() {
            runtime?.let {
                fps = it.fps
                if (!fpsOnly) {
                    cpu = PerfManager.cpuUsagePercent()
                    ramUsed = PerfManager.ramUsedMb()
                    ramTotal = PerfManager.ramTotalMb()
                    temp = PerfManager.tempCelsius()
                }
                invalidate()
            }
            handler.postDelayed(this, 250)
        }
    }

    fun start(ctx: Context, runtime: EmulatorRuntime) {
        this.runtime = runtime
        handler.post(updateRunnable)
    }

    fun stop() {
        handler.removeCallbacks(updateRunnable)
    }

    fun setMonitorMode(fpsOnly: Boolean, showCpuGpu: Boolean, showRam: Boolean, showTemp: Boolean) {
        this.fpsOnly = fpsOnly
        this.showCpuGpu = showCpuGpu
        this.showRam = showRam
        this.showTemp = showTemp
        invalidate()
    }

    fun setError(msg: String) {
        errorMsg = msg
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val lines = mutableListOf<String>()
        if (errorMsg != null) {
            canvas.drawText("⚠ $errorMsg", 8f, 30f, paintErr)
            return
        }
        lines.add("FPS: ${fps.toInt()}")
        if (!fpsOnly) {
            if (showCpuGpu) lines.add("CPU: $cpu%")
            if (showRam) lines.add("RAM: ${ramUsed}/${ramTotal} MB")
            if (showTemp && temp >= 0) lines.add("Temp: ${temp.toInt()}°C")
        }
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, 8f, 30f + i * 30f, paint)
        }
    }
}
