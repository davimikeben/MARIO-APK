package com.davimikeben.extremen64

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass

// ---------------------------------------------------------------------------
// Libretro joypad IDs (must match libretro.h)
// ---------------------------------------------------------------------------
object Pad {
    const val B = 0
    const val Y = 1
    const val SELECT = 2
    const val START = 3
    const val UP = 4
    const val DOWN = 5
    const val LEFT = 6
    const val RIGHT = 7
    const val A = 8
    const val X = 9
    const val L = 10
    const val R = 11
    const val L2 = 12   // mupen64plus-next uses L2 for the N64 "Z" trigger
    const val R2 = 13
}

private const val AXIS_LX = 0
private const val AXIS_LY = 1
private const val AXIS_RX = 2
private const val AXIS_RY = 3

private val ACCENT = Color(0xFFE91E63)
private val ACCENT_SOFT = Color(0xFF1E2238)

/**
 * Complete on-screen pad tuned for the N64 controller layout:
 * analog stick, D-pad, A/B, C-buttons (right analog), L/R, Z and Start.
 * Fully multitouch, resizable and repositionable.
 */
@Composable
fun VirtualControls(editMode: Boolean) {
    val opacity = SettingsManager.controlsOpacity
    val scale = SettingsManager.controlsScale

    var joyOffset by remember { mutableStateOf(Offset(
        SettingsManager.getFloat("layout_joy_x", 0f),
        SettingsManager.getFloat("layout_joy_y", 0f))) }
    var actOffset by remember { mutableStateOf(Offset(
        SettingsManager.getFloat("layout_act_x", 0f),
        SettingsManager.getFloat("layout_act_y", 0f))) }
    var cpadOffset by remember { mutableStateOf(Offset(
        SettingsManager.getFloat("layout_cpad_x", 0f),
        SettingsManager.getFloat("layout_cpad_y", 0f))) }
    var dpadOffset by remember { mutableStateOf(Offset(
        SettingsManager.getFloat("layout_dpad_x", 0f),
        SettingsManager.getFloat("layout_dpad_y", 0f))) }

    fun persist(key: String, value: Offset) {
        SettingsManager.putFloat("${key}_x", value.x)
        SettingsManager.putFloat("${key}_y", value.y)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ---------------- Analog stick (bottom-left) ----------------
        DraggableCluster(
            offset = joyOffset,
            enabled = editMode,
            onOffsetChange = { joyOffset = it; persist("layout_joy", it) },
            alignment = Alignment.BottomStart,
            basePadding = 24.dp
        ) {
            AnalogStick(
                modifier = Modifier.size((150 * scale).dp),
                alpha = opacity,
            )
        }

        // ---------------- Action cluster (bottom-right) ----------------
        DraggableCluster(
            offset = actOffset,
            enabled = editMode,
            onOffsetChange = { actOffset = it; persist("layout_act", it) },
            alignment = Alignment.BottomEnd,
            basePadding = 24.dp
        ) {
            ActionCluster(scale = scale, opacity = opacity)
        }

        // ---------------- C-buttons pad (right) ----------------
        DraggableCluster(
            offset = cpadOffset,
            enabled = editMode,
            onOffsetChange = { cpadOffset = it; persist("layout_cpad", it) },
            alignment = Alignment.CenterEnd,
            basePadding = 24.dp
        ) {
            CPad(scale = scale, opacity = opacity)
        }

        // ---------------- D-pad (bottom-center) ----------------
        DraggableCluster(
            offset = dpadOffset,
            enabled = editMode,
            onOffsetChange = { dpadOffset = it; persist("layout_dpad", it) },
            alignment = Alignment.BottomCenter,
            basePadding = 12.dp
        ) {
            DPad(scale = scale, opacity = opacity)
        }

        // ---------------- Shoulder + system buttons (top) ----------------
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PadButton("L1", "Ombro esquerdo", Pad.L, opacity, scale * 0.9f)
            Spacer(Modifier.width(10.dp))
            PadButton("Z", "Gatilho Z (L2)", Pad.L2, opacity, scale * 0.9f)
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PadButton("R1", "Ombro direito", Pad.R, opacity, scale * 0.9f)
            Spacer(Modifier.width(10.dp))
            PadButton("START", "Iniciar / Pausar", Pad.START, opacity, scale * 0.9f)
        }
    }
}

// ---------------------------------------------------------------------------
// Editable cluster wrapper
// ---------------------------------------------------------------------------
@Composable
private fun DraggableCluster(
    offset: Offset,
    enabled: Boolean,
    onOffsetChange: (Offset) -> Unit,
    alignment: Alignment,
    basePadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val latestOffset by rememberUpdatedState(offset)
    val latestCallback by rememberUpdatedState(onOffsetChange)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(basePadding)
    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(x = offset.x.dp, y = offset.y.dp)
                .then(
                    if (enabled) Modifier
                        .border(2.dp, ACCENT, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            var acc = latestOffset
                            detectDragGestures(
                                onDragStart = { acc = latestOffset },
                                onDrag = { change, drag ->
                                    change.consume()
                                    acc += Offset(drag.x, drag.y)
                                    latestCallback(acc)
                                }
                            )
                        }
                    else Modifier
                )
        ) {
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------
@Composable
private fun ActionCluster(scale: Float, opacity: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PadButton("B", "Botão B (ação secundária)", Pad.B, opacity, scale * 1.15f)
        Spacer(Modifier.width(14.dp))
        PadButton("A", "Botão A (ação principal)", Pad.A, opacity, scale * 1.15f)
    }
}

@Composable
private fun CPad(scale: Float, opacity: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton("C↑", "Cima", null, opacity, scale * 0.8f, analog = AXIS_RY to -1f)
        Row {
            PadButton("C←", "Esquerda", null, opacity, scale * 0.8f, analog = AXIS_RX to -1f)
            Spacer(Modifier.width(6.dp))
            PadButton("C→", "Direita", null, opacity, scale * 0.8f, analog = AXIS_RX to 1f)
        }
        PadButton("C↓", "Baixo", null, opacity, scale * 0.8f, analog = AXIS_RY to 1f)
    }
}

@Composable
private fun DPad(scale: Float, opacity: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PadButton("⬆️", "Direcional cima", Pad.UP, opacity, scale * 0.65f)
        Row {
            PadButton("⬅️", "Direcional esquerda", Pad.LEFT, opacity, scale * 0.65f)
            Spacer(Modifier.width(4.dp))
            PadButton("➡️", "Direcional direita", Pad.RIGHT, opacity, scale * 0.65f)
        }
        PadButton("⬇️", "Direcional baixo", Pad.DOWN, opacity, scale * 0.65f)
    }
}

/**
 * A single pad key. Supports either a libretro joypad id or an analog axis
 * (used by the N64 C-buttons). Handles real multitouch press/release.
 */
@Composable
private fun PadButton(
    label: String,
    description: String,
    buttonId: Int?,
    opacity: Float,
    scale: Float,
    analog: Pair<Int, Float>? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val size = (56 * scale).dp

    fun apply(state: Boolean) {
        pressed = state
        if (analog != null) {
            LibretroBridge.setAnalog(analog.first, if (state) analog.second else 0f)
        }
        if (buttonId != null) {
            LibretroBridge.setButtonState(buttonId, state)
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (pressed) (opacity + 0.35f).coerceAtMost(1f) else opacity)
            .background(if (pressed) ACCENT else ACCENT_SOFT, CircleShape)
            .border(1.dp, ACCENT.copy(alpha = 0.6f), CircleShape)
            .pointerInput(buttonId, analog) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        apply(true)
                        var released = false
                        while (!released) {
                            val ev = awaitPointerEvent(PointerEventPass.Main)
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            if (ch == null || !ch.pressed) released = true else ch.consume()
                        }
                        apply(false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = if (label.length > 2) 10.sp else 16.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Analog stick
// ---------------------------------------------------------------------------
@Composable
private fun AnalogStick(modifier: Modifier, alpha: Float) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    val radiusPx = with(androidx.compose.ui.platform.LocalDensity.current) { 60.dp.toPx() }

    Box(
        modifier = modifier
            .alpha(alpha)
            .background(ACCENT_SOFT, CircleShape)
            .border(2.dp, ACCENT.copy(alpha = 0.7f), CircleShape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)

                        fun update(pos: Offset) {
                            var d = pos - center
                            val len = d.getDistance()
                            if (len > radiusPx) d = d * (radiusPx / len)
                            knob = d
                            LibretroBridge.setAnalog(AXIS_LX, d.x / radiusPx)
                            LibretroBridge.setAnalog(AXIS_LY, d.y / radiusPx)
                            // mirror onto the digital d-pad for games that need it
                            LibretroBridge.setButtonState(Pad.LEFT, d.x < -radiusPx * 0.5f)
                            LibretroBridge.setButtonState(Pad.RIGHT, d.x > radiusPx * 0.5f)
                            LibretroBridge.setButtonState(Pad.UP, d.y < -radiusPx * 0.5f)
                            LibretroBridge.setButtonState(Pad.DOWN, d.y > radiusPx * 0.5f)
                        }

                        update(down.position)
                        var released = false
                        while (!released) {
                            val ev = awaitPointerEvent(PointerEventPass.Main)
                            val ch = ev.changes.firstOrNull { it.id == down.id }
                            if (ch == null || !ch.pressed) {
                                released = true
                            } else {
                                ch.consume()
                                update(ch.position)
                            }
                        }
                        knob = Offset.Zero
                        LibretroBridge.setAnalog(AXIS_LX, 0f)
                        LibretroBridge.setAnalog(AXIS_LY, 0f)
                        LibretroBridge.setButtonState(Pad.LEFT, false)
                        LibretroBridge.setButtonState(Pad.RIGHT, false)
                        LibretroBridge.setButtonState(Pad.UP, false)
                        LibretroBridge.setButtonState(Pad.DOWN, false)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = (knob.x / 4f).dp, y = (knob.y / 4f).dp)
                .size(52.dp)
                .background(ACCENT, CircleShape)
        )
    }
}
