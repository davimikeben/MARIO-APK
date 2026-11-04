package com.davimikeben.sm64emulator.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.davimikeben.sm64emulator.core.LibretroConstants
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Teclado virtual v3 — design moderno estilo gamepad de emulador premium.
 *
 * Visual:
 *  • Botões com gradiente radial, rim light, gloss superior e sombra projetada
 *  • Feedback de pressionado: escala 0.92 + brilho, com animação suave (~110 ms)
 *  • Stick analógico com polegar VISÍVEL que segue o dedo e volta ao centro
 *    com animação de mola
 *  • Multitouch real (cada dedo independente)
 *
 * Mapeamento OFICIAL do Mupen64Plus-Next (docs.libretro.com mupen64plus.md):
 *   N64 A (pular)      → RetroPad B   (id 0)
 *   N64 B (atacar)     → RetroPad Y   (id 1)
 *   N64 Z (crouch)     → RetroPad L2  (id 12)
 *   N64 L              → RetroPad L1  (id 10)
 *   N64 R              → RetroPad R1  (id 11)
 *   N64 Start          → RetroPad Start (id 3)
 *   N64 C-Up           → RetroPad X   (id 9)
 *   N64 C-Right        → RetroPad A   (id 8)
 *   N64 C-Left         → stick DIREITO X negativo
 *   N64 C-Down         → stick DIREITO Y positivo
 *   N64 Control Stick  → stick ESQUERDO analógico
 */
class VirtualKeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    interface KeyboardListener {
        fun onButton(buttonId: Int, pressed: Boolean)
        /** stick: 0=esquerdo, 1=direito. axis: 0=X, 1=Y. value: -1..1 */
        fun onAnalog(stick: Int, axis: Int, value: Float)
    }

    var listener: KeyboardListener? = null
    var opacity: Int = 78
        set(v) { field = v.coerceIn(10, 100); invalidate() }
    var scalePercent: Int = 100
        set(v) { field = v.coerceIn(50, 200); invalidate() }
    var style: Int = 1
        set(v) { field = v; invalidate() }
    var hapticEnabled: Boolean = true
    var editMode: Boolean = false

    // ===== Definição de um elemento do gamepad =====
    private class Elem(
        val key: String,
        val label: String,
        // Mapeamento digital: retro joypad id (-1 se não for digital)
        val joyId: Int,
        // Mapeamento analógico (C-Left/C-Down): stick, axis, signed value
        val analogStick: Int = -1,
        val analogAxis: Int = -1,
        val analogValue: Int = 0,
        // Posição relativa: x % da largura, y % da altura, raio % da altura
        var xRel: Float, var yRel: Float, val rRel: Float,
        val themeId: Int          // paleta de destaque (avaliada no draw)
    ) {
        var pressT = 0f             // 0..1 animação de pressionado
        // ====== SHADER CACHE ======
        var cachedShaderKey = -1L
        var bodyShader: RadialGradient? = null
    }

    // ===== Paleta por estilo (0=clássico 1=moderno 2=neon) — avaliada no draw =====
    private fun themeColor(themeId: Int): Int = when (style) {
        0 -> when (themeId) {   // clássico: sóbrio
            0 -> Color.parseColor("#15803D"); 1 -> Color.parseColor("#1D4ED8")
            2 -> Color.parseColor("#B45309"); 3 -> Color.parseColor("#475569")
            4 -> Color.parseColor("#3F4C5E"); 5 -> Color.parseColor("#1E293B")
            else -> Color.parseColor("#0891B2")
        }
        1 -> when (themeId) {   // moderno (default)
            0 -> Color.parseColor("#22C55E"); 1 -> Color.parseColor("#3B82F6")
            2 -> Color.parseColor("#F59E0B"); 3 -> Color.parseColor("#64748B")
            4 -> Color.parseColor("#546070"); 5 -> Color.parseColor("#252B36")
            else -> Color.parseColor("#00B8D4")
        }
        else -> when (themeId) { // neon
            0 -> Color.parseColor("#FF2D78"); 1 -> Color.parseColor("#8B5CF6")
            2 -> Color.parseColor("#FBBF24"); 3 -> Color.parseColor("#14B8A6")
            4 -> Color.parseColor("#6366F1"); 5 -> Color.parseColor("#0F172A")
            else -> Color.parseColor("#22D3EE")
        }
    }

    // ===== Layout do gamepad (coordenadas relativas; x % w, y % h, r % h) =====
    // themeId: 0=A 1=B 2=C 3=Z 4=L/R 5=Start 6=stick
    private val elems = mutableListOf(
        // --- Stick analógico esquerdo (N64 Control Stick) ---
        Elem("stick", "", -1, xRel = 0.115f, yRel = 0.66f, rRel = 0.155f, themeId = 6),
        // --- Gatilhos superiores ---
        Elem("Z", "Z", LibretroConstants.RETRO_DEVICE_ID_JOYPAD_L2,
             xRel = 0.210f, yRel = 0.14f, rRel = 0.048f, themeId = 3),
        Elem("L", "L", LibretroConstants.RETRO_DEVICE_ID_JOYPAD_L,
             xRel = 0.295f, yRel = 0.14f, rRel = 0.040f, themeId = 4),
        Elem("START", "START", LibretroConstants.RETRO_DEVICE_ID_JOYPAD_START,
             xRel = 0.500f, yRel = 0.13f, rRel = 0.038f, themeId = 5),
        Elem("R", "R", LibretroConstants.RETRO_DEVICE_ID_JOYPAD_R,
             xRel = 0.710f, yRel = 0.14f, rRel = 0.040f, themeId = 4),
        // --- C-buttons (câmera) — cluster diamante ---
        Elem("CUp",    "C▲", LibretroConstants.RETRO_DEVICE_ID_JOYPAD_X,
             xRel = 0.800f, yRel = 0.205f, rRel = 0.038f, themeId = 2),
        Elem("CRight", "C▶", LibretroConstants.RETRO_DEVICE_ID_JOYPAD_A,
             xRel = 0.880f, yRel = 0.300f, rRel = 0.038f, themeId = 2),
        Elem("CDown",  "C▼", -1, analogStick = 1, analogAxis = 1, analogValue = 32767,
             xRel = 0.800f, yRel = 0.395f, rRel = 0.038f, themeId = 2),
        Elem("CLeft",  "C◀", -1, analogStick = 1, analogAxis = 0, analogValue = -32767,
             xRel = 0.720f, yRel = 0.300f, rRel = 0.038f, themeId = 2),
        // --- Ações principais (direita) ---
        Elem("B", "B", LibretroConstants.RETRO_DEVICE_ID_JOYPAD_Y,
             xRel = 0.775f, yRel = 0.750f, rRel = 0.080f, themeId = 1),
        Elem("A", "A", LibretroConstants.RETRO_DEVICE_ID_JOYPAD_B,
             xRel = 0.905f, yRel = 0.660f, rRel = 0.095f, themeId = 0)
    )

    private fun elemByKey(key: String): Elem? = elems.firstOrNull { it.key == key }
    private val stickElem get() = elemByKey("stick")!!

    // ===== Estado de toque =====
    private class TouchState(
        var elem: Elem?, var downX: Float = 0f, var downY: Float = 0f
    )
    private val pointerStates = HashMap<Int, TouchState>()

    // ===== Polegar do stick (px) =====
    private var thumbX = 0f   // offset relativo ao centro, em px
    private var thumbY = 0f
    private var stickTargetX = 0f
    private var stickTargetY = 0f

    // ===== Animação =====
    private var animRunning = false
    private var lastFrameNanos = 0L

    // ===== Paints =====
    private val pBody = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pRim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pGloss = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pShadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val pThumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pEdit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.argb(200, 250, 204, 21)
    }

    // ===== Cache de dimensões =====
    private var viewW = 1f
    private var viewH = 1f
    private var shaderKeyBase = 0L

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        viewW = w.coerceAtLeast(1).toFloat()
        viewH = h.coerceAtLeast(1).toFloat()
        shaderKeyBase = 0L // invalida shaders
    }

    fun toggleEditMode() {
        editMode = !editMode
        invalidate()
    }

    // ===== Loop de animação (só roda quando há algo animando) =====
    private fun kickAnim() {
        if (animRunning) return
        animRunning = true
        lastFrameNanos = System.nanoTime()
        post(animTick)
    }

    private val animTick = object : Runnable {
        override fun run() {
            val now = System.nanoTime()
            val dt = ((now - lastFrameNanos).coerceAtLeast(1) / 1_000_000_000f)
            lastFrameNanos = now
            val speed = 9f // ~110ms para transição completa
            var active = false

            for (e in elems) {
                val target = if (pointerStates.values.any { it.elem === e }) 1f else 0f
                val d = target - e.pressT
                if (abs(d) > 0.002f) {
                    e.pressT = (e.pressT + d.coerceIn(-dt * speed, dt * speed)).coerceIn(0f, 1f)
                    active = true
                } else e.pressT = target
            }
            // Mola do stick: thumb → target (exponencial suave)
            val k = 1f - 0.0001f.coerceAtLeast(1f - dt * 22f)
            val dx = stickTargetX - thumbX
            val dy = stickTargetY - thumbY
            if (hypot(dx, dy) > 0.5f) {
                thumbX += dx * k
                thumbY += dy * k
                active = true
            } else { thumbX = stickTargetX; thumbY = stickTargetY }

            invalidate()
            if (active) postOnAnimation(this) else animRunning = false
        }
    }

    // ===== Geometria =====
    private fun elemCx(e: Elem): Float {
        val scale = scalePercent / 100f
        // ancoragem: aproxima tudo do centro quando escala < 100
        return e.xRel * viewW + (1f - scale) * (viewW * 0.5f - e.xRel * viewW) * 0.5f
    }
    private fun elemCy(e: Elem): Float {
        val scale = scalePercent / 100f
        return e.yRel * viewH + (1f - scale) * (viewH * 0.5f - e.yRel * viewH) * 0.5f
    }
    private fun elemR(e: Elem): Float = e.rRel * viewH * (scalePercent / 100f)

    private fun findElemAt(x: Float, y: Float): Elem? {
        var best: Elem? = null
        var bestD = Float.MAX_VALUE
        for (e in elems) {
            // área de toque ~25% maior que o visual
            val r = elemR(e) * (if (e.key == "stick") 1.10f else 1.25f)
            val d = hypot(x - elemCx(e), y - elemCy(e))
            if (d <= r && d < bestD) { best = e; bestD = d }
        }
        return best
    }

    // ===== Desenho =====
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val alpha = (opacity * 255 / 100)
        val key = shaderKeyBase + viewW.roundToInt() * 31 + viewH.roundToInt() * 997 +
                  style * 104729 + scalePercent * 31_063

        for (e in elems) {
            val cx = elemCx(e)
            val cy = elemCy(e)
            val r = elemR(e)

            if (e.key == "stick") { drawStick(canvas, e, cx, cy, r, alpha); continue }

            // --- Botão circular com profundidade ---
            val press = e.pressT
            val scale = 1f - 0.08f * press
            val drawR = r * scale

            // Sombra projetada (fake, hardware-friendly)
            pShadow.color = Color.argb((alpha * 0.35f).roundToInt(), 0, 0, 0)
            canvas.drawCircle(cx, cy + drawR * 0.14f + 2f, drawR, pShadow)

            // Corpo: gradiente radial (centro claro → base)
            val accent = themeColor(e.themeId)
            val sk = key * 13 + e.key.hashCode()
            if (e.cachedShaderKey != sk || e.bodyShader == null) {
                e.bodyShader = RadialGradient(
                    cx - drawR * 0.25f, cy - drawR * 0.35f, drawR * 1.6f,
                    intArrayOf(lighten(accent, 0.35f), accent, darken(accent, 0.45f)),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
                )
                e.cachedShaderKey = sk
            }
            pBody.shader = e.bodyShader
            pBody.alpha = alpha
            canvas.drawCircle(cx, cy, drawR, pBody)

            // Gloss superior (elipse clara)
            pGloss.shader = LinearGradient(
                0f, cy - drawR, 0f, cy,
                intArrayOf(Color.argb((alpha * 0.42f).roundToInt(), 255, 255, 255), Color.argb(0, 255, 255, 255)),
                null, Shader.TileMode.CLAMP
            )
            val gloss = RectF(cx - drawR * 0.72f, cy - drawR * 0.88f,
                              cx + drawR * 0.72f, cy - drawR * 0.08f)
            canvas.drawOval(gloss, pGloss)

            // Rim light (borda interna clara) + contorno escuro
            pRim.color = Color.argb((alpha * (0.45f + 0.3f * press)).roundToInt(), 255, 255, 255)
            pRim.strokeWidth = drawR * 0.055f
            canvas.drawCircle(cx, cy, drawR - pRim.strokeWidth, pRim)

            // Anel de brilho quando pressionado
            if (press > 0.01f) {
                pRing.color = Color.argb((press * 200).roundToInt(), 255, 255, 255)
                pRing.strokeWidth = drawR * 0.10f
                canvas.drawCircle(cx, cy, drawR - pRing.strokeWidth * 0.5f, pRing)
            }

            // Rótulo
            pText.alpha = ((alpha + 60).coerceAtMost(255))
            pText.textSize = drawR * (if (e.key == "START") 0.52f else 0.66f)
            pText.color = if (e.key == "START") Color.WHITE else Color.WHITE
            pText.setShadowLayer(2f, 0f, 2f, Color.argb(140, 0, 0, 0))
            canvas.drawText(e.label, cx, cy + pText.textSize * 0.35f, pText)

            if (editMode) canvas.drawCircle(cx, cy, drawR + 6f, pEdit)
        }
    }

    private fun drawStick(canvas: Canvas, e: Elem, cx: Float, cy: Float, r: Float, alpha: Int) {
        // Base côncava: gradiente invertido (borda clara, centro escuro)
        pBody.shader = RadialGradient(
            cx, cy, r,
            intArrayOf(darken(Color.parseColor("#1B1F27"), 0.35f), Color.parseColor("#2A303C"), lighten(Color.parseColor("#39404E"), 0.15f)),
            floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP
        )
        pBody.alpha = alpha
        canvas.drawCircle(cx, cy, r, pBody)

        // Anel externo com acento
        val accent = themeColor(e.themeId)
        pRing.color = Color.argb((alpha * 0.55f).roundToInt(),
            Color.red(accent), Color.green(accent), Color.blue(accent))
        pRing.strokeWidth = r * 0.06f
        canvas.drawCircle(cx, cy, r - pRing.strokeWidth * 0.5f, pRing)

        // Marcas de direção (N/S/L/O)
        pText.textSize = r * 0.22f
        pText.color = Color.argb((alpha * 0.45f).roundToInt(), 255, 255, 255)
        pText.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        canvas.drawText("▲", cx, cy - r * 0.62f, pText)
        canvas.drawText("▼", cx, cy + r * 0.78f, pText)
        canvas.drawText("◀", cx - r * 0.70f, cy + pText.textSize * 0.35f, pText)
        canvas.drawText("▶", cx + r * 0.70f, cy + pText.textSize * 0.35f, pText)

        // Polegar — VISÍVEL e que se move
        val maxOff = r - r * 0.34f
        val tx = cx + thumbX.coerceIn(-maxOff, maxOff)
        val ty = cy + thumbY.coerceIn(-maxOff, maxOff)
        val thumbR = r * 0.34f

        // sombra do polegar
        pShadow.color = Color.argb((alpha * 0.45f).roundToInt(), 0, 0, 0)
        canvas.drawCircle(tx, ty + thumbR * 0.18f + 2f, thumbR, pShadow)

        pThumb.shader = RadialGradient(
            tx - thumbR * 0.3f, ty - thumbR * 0.4f, thumbR * 1.5f,
            intArrayOf(lighten(Color.parseColor("#3A4150"), 0.30f), Color.parseColor("#2E3542"), darken(Color.parseColor("#20242E"), 0.2f)),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        pThumb.alpha = alpha
        canvas.drawCircle(tx, ty, thumbR, pThumb)

        // brilho central do polegar com acento
        pRim.color = Color.argb((alpha * 0.7f).roundToInt(),
            Color.red(accent), Color.green(accent), Color.blue(accent))
        pRim.strokeWidth = thumbR * 0.10f
        canvas.drawCircle(tx, ty, thumbR - pRim.strokeWidth * 0.5f, pRim)

        // Gloss pequeno
        pGloss.shader = LinearGradient(
            0f, ty - thumbR, 0f, ty,
            intArrayOf(Color.argb((alpha * 0.35f).roundToInt(), 255, 255, 255), Color.argb(0, 255, 255, 255)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(tx - thumbR * 0.7f, ty - thumbR * 0.85f,
                              tx + thumbR * 0.7f, ty - thumbR * 0.05f), pGloss)
    }

    // ===== Cores =====
    private fun lighten(c: Int, f: Float): Int = mix(c, Color.WHITE, f)
    private fun darken(c: Int, f: Float): Int = mix(c, Color.BLACK, f)
    private fun mix(a: Int, b: Int, t: Float): Int {
        val ar = Color.red(a); val ag = Color.green(a); val ab = Color.blue(a)
        val br = Color.red(b); val bg = Color.green(b); val bb = Color.blue(b)
        return Color.rgb(
            (ar + (br - ar) * t).roundToInt(),
            (ag + (bg - ag) * t).roundToInt(),
            (ab + (bb - ab) * t).roundToInt()
        )
    }

    // ===== Toque / Multitouch =====
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val pid = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)
                if (editMode) {
                    val e = findElemAt(x, y) ?: return true
                    pointerStates[pid] = TouchState(e, x, y)
                } else {
                    val e = findElemAt(x, y) ?: return true
                    pointerStates[pid] = TouchState(e, x, y)
                    pressElem(e, true)
                    if (hapticEnabled) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    if (e.key == "stick") updateStickFrom(x, y)
                    else if (e.analogStick >= 0) sendAnalog(e, true)
                }
                kickAnim()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val st = pointerStates[pid] ?: continue
                    val e = st.elem ?: continue
                    if (editMode) {
                        e.xRel = (event.getX(i) / viewW).coerceIn(0f, 1f)
                        e.yRel = (event.getY(i) / viewH).coerceIn(0f, 1f)
                        invalidate()
                    } else if (e.key == "stick") {
                        updateStickFrom(event.getX(i), event.getY(i))
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = if (event.actionMasked == MotionEvent.ACTION_CANCEL) 0 else event.actionIndex
                val pid = event.getPointerId(idx.coerceIn(0, event.pointerCount - 1))
                val st = pointerStates.remove(pid) ?: return true
                val e = st.elem ?: return true
                if (!editMode) {
                    pressElem(e, false)
                    if (e.key == "stick") { stickTargetX = 0f; stickTargetY = 0f; kickAnim() }
                    else if (e.analogStick >= 0) sendAnalog(e, false)
                }
            }
        }
        return true
    }

    private fun pressElem(e: Elem, pressed: Boolean) {
        if (e.joyId >= 0) listener?.onButton(e.joyId, pressed)
        // elementos só-analógicos (C-Left/C-Down) tratados em sendAnalog
    }

    private fun sendAnalog(e: Elem, pressed: Boolean) {
        val v = if (pressed) e.analogValue else 0
        listener?.onAnalog(e.analogStick, e.analogAxis, v / 32767f)
    }

    private fun updateStickFrom(x: Float, y: Float) {
        val cx = elemCx(stickElem)
        val cy = elemCy(stickElem)
        val r = elemR(stickElem)
        val maxOff = r - r * 0.34f
        var dx = x - cx
        var dy = y - cy
        val dist = hypot(dx, dy)
        if (dist > maxOff) { dx = dx / dist * maxOff; dy = dy / dist * maxOff }
        stickTargetX = dx
        stickTargetY = dy
        // valor normalizado -1..1 com deadzone 5%
        val nx = (dx / maxOff)
        val ny = (dy / maxOff)
        val dz = 0.05f
        val sx = if (abs(nx) < dz) 0f else (nx - dz * nx / abs(nx).coerceAtLeast(dz)) / (1f - dz)
        val sy = if (abs(ny) < dz) 0f else (ny - dz * ny / abs(ny).coerceAtLeast(dz)) / (1f - dz)
        listener?.onAnalog(0, 0, sx.coerceIn(-1f, 1f))
        listener?.onAnalog(0, 1, sy.coerceIn(-1f, 1f))
        kickAnim()
    }

    // ===== Manutenção de estado =====
    fun releaseAll() {
        pointerStates.clear()
        stickTargetX = 0f; stickTargetY = 0f
        for (e in elems) {
            if (e.joyId >= 0) listener?.onButton(e.joyId, false)
            if (e.analogStick >= 0) listener?.onAnalog(e.analogStick, e.analogAxis, 0f)
        }
        listener?.onAnalog(0, 0, 0f)
        listener?.onAnalog(0, 1, 0f)
        kickAnim()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animTick)
        super.onDetachedFromWindow()
    }
}
