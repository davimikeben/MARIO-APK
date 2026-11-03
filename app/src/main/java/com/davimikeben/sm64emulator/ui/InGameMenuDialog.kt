package com.davimikeben.sm64emulator.ui

import android.app.AlertDialog
import android.content.Context
import com.davimikeben.sm64emulator.R

/**
 * Menu flutuante durante a partida.
 * Não reinicializa o jogo — apenas pausa o loop enquanto o menu está aberto.
 */
class InGameMenuDialog(
    private val context: Context,
    private val onModifyControls: () -> Unit,
    private val onTransparency: () -> Unit,
    private val onGraphics: () -> Unit,
    private val onPerfInfo: () -> Unit,
    private val onRestart: () -> Unit,
    private val onSaveConfig: () -> Unit,
    private val onBackToMenu: () -> Unit,
    private val onResumeGame: () -> Unit
) {
    fun show() {
        val items = arrayOf(
            "🎮 Modificar controles (mover/redimensionar)",
            "🎨 Transparência e opacidade",
            "🖼️ Modificar gráficos",
            "📊 Informações de desempenho",
            "🔄 Reiniciar o jogo",
            "💾 Salvar configurações",
            "🏠 Voltar ao menu principal",
            "▶️ Voltar ao jogo"
        )
        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog)
            .setTitle("⚙️ Menu — SM64 Emulator")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> onModifyControls()
                    1 -> onTransparency()
                    2 -> onGraphics()
                    3 -> onPerfInfo()
                    4 -> onRestart()
                    5 -> onSaveConfig()
                    6 -> onBackToMenu()
                    7 -> onResumeGame()
                }
            }
            .setOnCancelListener { onResumeGame() }
            .create()
            .show()
    }
}
