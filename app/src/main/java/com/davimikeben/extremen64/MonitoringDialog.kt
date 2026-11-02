package com.davimikeben.extremen64

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun MonitoringDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

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
                Text("MONITORAMENTO", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                MonitoringItem("RAM Livre", "${memoryInfo.availMem / (1024 * 1024)} MB")
                MonitoringItem("RAM Total", "${memoryInfo.totalMem / (1024 * 1024)} MB")
                MonitoringItem("Arquitetura", Build.SUPPORTED_ABIS[0])
                MonitoringItem("Versão Android", Build.VERSION.RELEASE)
                MonitoringItem("Modelo", Build.MODEL)
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun MonitoringItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White)
    }
}
