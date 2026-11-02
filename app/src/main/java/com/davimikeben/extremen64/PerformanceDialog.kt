package com.davimikeben.extremen64

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PerformanceDialog(onClose: () -> Unit) {
    var selectedMode by remember { mutableStateOf(SettingsManager.performanceMode) }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = Color.DarkGray,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("OTIMIZAÇÃO EXTREMA", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text("Foco: Mali-G52", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                PerformanceOption("⚡ DESEMPENHO", "Fluidez máxima, menor resolução", selectedMode == PerformanceMode.DESEMPENHO) {
                    selectedMode = PerformanceMode.DESEMPENHO
                    SettingsManager.performanceMode = PerformanceMode.DESEMPENHO
                }
                
                PerformanceOption("⚖️ EQUILIBRADO", "Equilíbrio entre imagem e FPS", selectedMode == PerformanceMode.EQUILIBRADO) {
                    selectedMode = PerformanceMode.EQUILIBRADO
                    SettingsManager.performanceMode = PerformanceMode.EQUILIBRADO
                }
                
                PerformanceOption("🎨 QUALIDADE", "Melhor imagem, resolução nativa", selectedMode == PerformanceMode.QUALIDADE) {
                    selectedMode = PerformanceMode.QUALIDADE
                    SettingsManager.performanceMode = PerformanceMode.QUALIDADE
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun PerformanceOption(label: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(label, color = Color.White)
            Text(desc, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
