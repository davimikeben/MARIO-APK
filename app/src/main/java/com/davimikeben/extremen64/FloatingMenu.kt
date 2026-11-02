package com.davimikeben.extremen64

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PANEL = Color(0xF2141A33)
private val ACCENT = Color(0xFFE91E63)

/**
 * Discrete in-game overlay. Opening it never restarts the game - the emulator
 * keeps running behind the panel.
 */
@Composable
fun FloatingMenu(
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    fps: Float,
    hardware: Boolean,
    onReset: () -> Unit,
    onBackToMenu: () -> Unit,
    onClose: () -> Unit
) {
    var opacity by remember { mutableStateOf(SettingsManager.controlsOpacity) }
    var scale by remember { mutableStateOf(SettingsManager.controlsScale) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .padding(12.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = PANEL,
            modifier = Modifier.width(330.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚙️ MENU RÁPIDO", color = ACCENT, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "FPS ${fps.toInt()}  •  ${if (hardware) "GPU" else "CPU"}",
                        color = Color(0xFF00E676), fontSize = 11.sp
                    )
                }
                Divider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎮 Mover botões", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(checked = editMode, onCheckedChange = onEditModeChange)
                }

                Text("Transparência: ${(opacity * 100).toInt()}%", color = Color(0xFFB0B6D0), fontSize = 11.sp)
                Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        SettingsManager.controlsOpacity = it
                    },
                    valueRange = 0.05f..1f
                )

                Text("Tamanho dos botões: ${"%.2f".format(scale)}x", color = Color(0xFFB0B6D0), fontSize = 11.sp)
                Slider(
                    value = scale,
                    onValueChange = {
                        scale = it
                        SettingsManager.controlsScale = it
                    },
                    valueRange = 0.6f..1.8f
                )

                Spacer(Modifier.height(8.dp))
                QuickButton("🔄 Reiniciar o jogo") { onReset() }
                QuickButton("💾 Salvar configurações") { SettingsManager.saveSnapshot() }
                QuickButton("🏠 Voltar ao menu principal") { onBackToMenu() }
                QuickButton("✖ Fechar") { onClose() }
            }
        }
    }
}

@Composable
private fun QuickButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2238)),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
