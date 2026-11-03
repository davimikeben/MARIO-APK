package com.davimikeben.sm64emulator.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.davimikeben.sm64emulator.R
import com.davimikeben.sm64emulator.core.LibretroConstants
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Teclado virtual completo para Super Mario 64 (Nintendo 64 controller):
 *
 *  • Direcional analógico (substitui o thumbstick N64)
 *  • D-Pad (cima/baixo/esquerda/direita) — usado para câmera em SM64
 *  • 🅰️ Botão A — pulo
 *  • 🅱️ Botão B — soco/chutar/atak
 *  • 🆇 Botão X (mapeado para C-Left em SM64)
 *  • 🆈 Botão Y (mapeado para C-Up em SM64 — câmera)
 *  • L (Z-trigger do N64 — câmera/lock)
 *  • R (R do N64)
 *  • ⏸️ Start — pausa do jogo
 *  • 🎯 C-buttons (C-Up/C-Down/C-Left/C-Right) — controle de câmera
 *
 * Multitouch real (cada ponteiro independente). Redimensionável por SettingsManager.
 * Opacidade ajustável. Posições customizáveis (modo edição: arrasta botão).
 *
 * Não inventa comandos: o controle N64 é padronizado pela Nintendo, e SM64 usa
 * A=pular, B=atacar, Z=câmera lock, C-buttons=olhar, Stick=mover.
 */
class VirtualKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    interface KeyboardListener {
        fun onButton(buttonId: Int, pressed: Boolean)
        fun onAnalog(axis: Int, value: Float)
    }

    var listener: KeyboardListener? = null
    var opacity: Int = 70
        set(v) { field = v.coerceIn(10, 100); invalidate() }
    var scalePercent: Int = 100
        set(v) { field = v.coerceIn(50, 200); invalidate() }
    var style: Int = 1  // 0=classic 1=modern 2=neon
        set(v) { field = v; invalidate() }
    var hapticEnabled: Boolean = true
    var editMode: Boolean = false

    private data class ButtonDef(
        val id: Int, val label: String, val desc: String,
        var xRel: Float, var yRel: Float, val rRel: Float
    )

    // Posições em coordenadas relativas (0..1) dentro da View
    private val buttons = mutableListOf(
        // D-Pad no canto inferior esquerdo
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_UP,    "⬆️", "Câmera cima",      0.06f, 0.78f, 0.06f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_DOWN,  "⬇️", "Câmera baixo",     0.06f, 0.92f, 0.06f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_LEFT,  "⬅️", "Câmera esquerda",  0.0f,  0.85f, 0.06f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_RIGHT, "➡️", "Câmera direita",   0.12f, 0.85f, 0.06f),

        // Stick analógico à direita do D-pad
        ButtonDef(-1, "🎮", "Movimento (analógico)", 0.30f, 0.85f, 0.13f),

        // Botões de ação no canto inferior direito
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_A, "🅰️", "Pular",   0.88f, 0.85f, 0.07f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_B, "🅱️", "Atacar",  0.78f, 0.90f, 0.06f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_X, "🆇", "C-Left",  0.68f, 0.85f, 0.05f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_Y, "🆈", "C-Up",    0.74f, 0.78f, 0.05f),

        // Botões auxiliares
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_L, "L", "Z (lock câmera)", 0.40f, 0.95f, 0.05f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_R, "R", "R",                0.55f, 0.95f, 0.05f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_START, "⏸️", "Pausar",     0.50f, 0.05f, 0.05f),
        ButtonDef(LibretroConstants.RETRO_DEVICE_ID_JOYPAD_SELECT, "🎯", "C-Down",    0.92f, 0.95f, 0.05f)
    )

    // Estado de toque por pointerId
    private data class TouchState(var buttonId: Int = -99, var downX: Float = 0f, var downY: Float = 0f)
    private val pointerStates = HashMap<Int, TouchState>()

    private val paintBase = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintPressed = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintEdit = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paintText.textAlign = Paint.Align.CENTER
        paintText.color = Color.WHITE
    }

    fun toggleEditMode() {
        editMode = !editMode
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val alpha = (opacity * 255 / 100)
        val baseColor = when (style) {
            0 -> Color.parseColor("#1F2937")  // classic dark gray
            1 -> Color.parseColor("#3B82F6")  // modern blue
            2 -> Color.parseColor("#A21CAF")  // neon purple
            else -> Color.GRAY
        }
        val pressedColor = when (style) {
            0 -> Color.parseColor("#F59E0B")
            1 -> Color.parseColor("#F97316")
            2 -> Color.parseColor("#22D3EE")
            else -> Color.RED
        }
        paintBase.color = baseColor
        paintBase.alpha = alpha
        paintPressed.color = pressedColor
        paintPressed.alpha = min(255, alpha + 40)
        paintEdit.color = Color.YELLOW
        paintEdit.alpha = 180
        paintEdit.style = Paint.Style.STROKE
        paintEdit.strokeWidth = 4f
        paintText.textSize = 36f * scalePercent / 100f
        paintText.alpha = alpha + 60

        val w = width.toFloat()
        val h = height.toFloat()
        val scale = scalePercent / 100f

        buttons.forEach { b ->
            val cx = b.xRel * w + (1 - scale) * b.rRel * w * 0.5f
            val cy = b.yRel * h + (1 - scale) * b.rRel * h * 0.5f
            val r = b.rRel * min(w, h) * scale
            val isPressed = pointerStates.values.any { it.buttonId == b.id }
            val paint = if (isPressed) paintPressed else paintBase
            canvas.drawCircle(cx, cy, r, paint)
            // Label
            paintText.color = Color.WHITE
            canvas.drawText(b.label, cx, cy + paintText.textSize / 3f, paintText)
            if (editMode) {
                canvas.drawCircle(cx, cy, r + 6f, paintEdit)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)
                if (editMode) {
                    // Em modo edição: toque curto move botão para nova posição
                    val b = findButtonAt(x, y) ?: return true
                    pointerStates[pid] = TouchState(b.id, x, y)
                } else {
                    val b = findButtonAt(x, y)
                    if (b != null) {
                        pointerStates[pid] = TouchState(b.id, x, y)
                        handleButton(b.id, true)
                        if (hapticEnabled) performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val st = pointerStates[pid] ?: continue
                    if (editMode) {
                        // Move o botão para onde o ponteiro está
                        val b = buttons.firstOrNull { it.id == st.buttonId } ?: continue
                        b.xRel = (event.getX(i) / width).coerceIn(0f, 1f)
                        b.yRel = (event.getY(i) / height).coerceIn(0f, 1f)
                        invalidate()
                    } else if (st.buttonId == -1) {
                        // Stick analógico: valor proporcional ao deslocamento
                        val dx = (event.getX(i) - st.downX) / (0.18f * min(width, height))
                        val dy = (event.getY(i) - st.downY) / (0.18f * min(width, height))
                        val mag = hypot(dx, dy)
                        val mx = if (mag > 1f) dx / mag else dx
                        val my = if (mag > 1f) dy / mag else dy
                        listener?.onAnalog(0, mx)  // X
                        listener?.onAnalog(1, my)  // Y
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val st = pointerStates.remove(pid)
                if (st != null && !editMode) {
                    handleButton(st.buttonId, false)
                    if (st.buttonId == -1) {
                        listener?.onAnalog(0, 0f)
                        listener?.onAnalog(1, 0f)
                    }
                }
            }
        }
        return true
    }

    private fun handleButton(buttonId: Int, pressed: Boolean) {
        if (buttonId == -1) return  // analógico tratado em MOVE
        listener?.onButton(buttonId, pressed)
        invalidate()
    }

    private fun findButtonAt(x: Float, y: Float): ButtonDef? {
        val w = width.toFloat()
        val h = height.toFloat()
        val scale = scalePercent / 100f
        var best: ButtonDef? = null
        var bestDist = Float.MAX_VALUE
        buttons.forEach { b ->
            val cx = b.xRel * w + (1 - scale) * b.rRel * w * 0.5f
            val cy = b.yRel * h + (1 - scale) * b.rRel * h * 0.5f
            val r = b.rRel * min(w, h) * scale
            val d = hypot(x - cx, y - cy)
            if (d <= r && d < bestDist) { best = b; bestDist = d }
        }
        return best
    }
}
