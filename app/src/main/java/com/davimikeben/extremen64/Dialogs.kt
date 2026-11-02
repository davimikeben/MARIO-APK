package com.davimikeben.extremen64

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private val PANEL = Color(0xF2141A33)
private val ACCENT = Color(0xFFE91E63)

@Composable
private fun Panel(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { }) {
        Surface(shape = RoundedCornerShape(18.dp), color = PANEL, modifier = Modifier.width(460.dp)) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(title, color = ACCENT, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, color = Color(0xFF5C6BC0), fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun CloseRow(onClose: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(
            onClick = onClose,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ACCENT)
        ) { Text("FECHAR", color = Color.White) }
    }
}

// ---------------------------------------------------------------------------
@Composable
fun ControlsConfigDialog(onClose: () -> Unit) {
    var opacity by remember { mutableStateOf(SettingsManager.controlsOpacity) }
    var scale by remember { mutableStateOf(SettingsManager.controlsScale) }
    var vibration by remember { mutableStateOf(SettingsManager.vibration) }

    Panel(
        title = "🎮 TECLADO VIRTUAL",
        subtitle = "Cada tecla mostra nome e descrição. Multitouch real."
    ) {
        Text("Transparência: ${(opacity * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
        Slider(value = opacity, onValueChange = {
            opacity = it; SettingsManager.controlsOpacity = it
        }, valueRange = 0.05f..1f)

        Text("Tamanho: ${"%.2f".format(scale)}x", color = Color.White, fontSize = 12.sp)
        Slider(value = scale, onValueChange = {
            scale = it; SettingsManager.controlsScale = it
        }, valueRange = 0.6f..1.8f)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Vibração ao pressionar", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = vibration, onCheckedChange = {
                vibration = it; SettingsManager.vibration = it
            })
        }

        Spacer(Modifier.height(8.dp))
        Text("Mapa de teclas", color = ACCENT, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        KeyRow("🎮 Direcional analógico", "Move o personagem (eixo analógico + digital)")
        KeyRow("⬆️⬇️⬅️➡️ Direcional digital", "Menus e movimentos alternativos")
        KeyRow("🅰️ Botão A", "Ação principal (pular)")
        KeyRow("🅱️ Botão B", "Ação secundária (agachar/agarrar)")
        KeyRow("🅾️ Botão Z (L2)", "Gatilho central / agachar")
        KeyRow("L1 / R1", "Ombros esquerdo e direito")
        KeyRow("✳️ C-buttons (C↑ C↓ C← C→)", "Controle de câmera (eixo analógico direito)")
        KeyRow("⏸️ START", "Inicia e pausa a partida")

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                listOf("layout_joy", "layout_act", "layout_cpad", "layout_dpad").forEach {
                    SettingsManager.putFloat("${it}_x", 0f)
                    SettingsManager.putFloat("${it}_y", 0f)
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2238))
        ) { Text("♻️ Reposicionar teclas no padrão", color = Color.White, fontSize = 11.sp) }

        CloseRow(onClose)
    }
}

@Composable
private fun KeyRow(name: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(name, color = Color.White, fontSize = 11.sp, modifier = Modifier.width(190.dp))
        Text(description, color = Color(0xFFB0B6D0), fontSize = 11.sp)
    }
}

// ---------------------------------------------------------------------------
@Composable
fun GraphicsDialog(onClose: () -> Unit) {
    var filter by remember { mutableStateOf(SettingsManager.videoFilter) }

    Panel(title = "🎨 GRÁFICOS", subtitle = "Filtro de imagem e escala de saída") {
        listOf("Nítido", "Suave", "Suave + Scanlines").forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = filter == option, onClick = {
                    filter = option; SettingsManager.videoFilter = option
                })
                Text(option, color = Color.White, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "A resolução interna é definida pelo perfil escolhido em DESEMPENHO.\n" +
                "No perfil DESEMPENHO o app usa OpenGL ES 2.0; nos demais, OpenGL ES 3.0.",
            color = Color(0xFFB0B6D0), fontSize = 11.sp
        )
        CloseRow(onClose)
    }
}

// ---------------------------------------------------------------------------
@Composable
fun AudioDialog(onClose: () -> Unit) {
    var audio by remember { mutableStateOf(SettingsManager.audioEnabled) }

    Panel(title = "🔊 ÁUDIO", subtitle = "Saída de som do núcleo (OpenSL ES)") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Som ligado", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = audio, onCheckedChange = {
                audio = it; SettingsManager.audioEnabled = it
            })
        }
        Text(
            "A taxa de amostragem é sincronizada automaticamente com o núcleo.",
            color = Color(0xFFB0B6D0), fontSize = 11.sp
        )
        CloseRow(onClose)
    }
}

// ---------------------------------------------------------------------------
@Composable
fun ScreenDialog(onClose: () -> Unit) {
    var showFps by remember { mutableStateOf(SettingsManager.showFps) }

    Panel(title = "📱 TELA", subtitle = "HUD e apresentação") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar contador de FPS", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Switch(checked = showFps, onCheckedChange = {
                showFps = it; SettingsManager.showFps = it
            })
        }
        Text(
            "Durante o jogo somente o FPS é exibido. Todas as demais métricas\n" +
                "ficam em MONITORAMENTO.",
            color = Color(0xFFB0B6D0), fontSize = 11.sp
        )
        CloseRow(onClose)
    }
}

// ---------------------------------------------------------------------------
@Composable
fun InfoDialog(title: String, body: String, onClose: () -> Unit) {
    Panel(title = title, subtitle = null) {
        Text(body, color = Color.White, fontSize = 12.sp)
        CloseRow(onClose)
    }
}
