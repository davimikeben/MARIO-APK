package com.davimikeben.sm64emulator.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.davimikeben.sm64emulator.R

/**
 * Bottom-sheet-like dialog reutilizável para todas as seções de configuração.
 *
 * Cada item tem: nome, função que retorna o valor atual, lista de deltas
 * (alterações), callback. Delta vazio = item somente-leitura (informação).
 * A função de valor permite que o dialog mostre o estado atualizado após cada alteração.
 */
class SettingsSheetDialog(
    private val context: Context,
    private val title: String,
    private val items: List<Item>
) {
    data class Item(
        val name: String,
        val currentValue: () -> String,
        val deltas: List<Int>,
        val onChange: (delta: Int) -> Unit
    )

    private var currentDialog: AlertDialog? = null

    fun show() {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        items.forEach { item ->
            val row = LayoutInflater.from(context).inflate(R.layout.settings_row, container, false) as ViewGroup
            row.findViewById<TextView>(R.id.row_name).text = item.name
            row.findViewById<TextView>(R.id.row_value).text = item.currentValue()

            val minusBtn = row.findViewById<View>(R.id.btn_minus)
            val plusBtn = row.findViewById<View>(R.id.btn_plus)

            if (item.deltas.isEmpty()) {
                minusBtn.visibility = View.GONE
                plusBtn.visibility = View.GONE
            } else if (item.deltas == listOf(0)) {
                // toggle button
                minusBtn.visibility = View.GONE
                plusBtn.setOnClickListener {
                    item.onChange(0)
                    rebuild()
                }
            } else {
                if (-1 !in item.deltas) minusBtn.visibility = View.GONE else
                    minusBtn.setOnClickListener { item.onChange(-1); rebuild() }
                if (+1 !in item.deltas) plusBtn.visibility = View.GONE else
                    plusBtn.setOnClickListener { item.onChange(+1); rebuild() }
            }
            container.addView(row)
        }

        currentDialog = AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog)
            .setTitle(title)
            .setView(container)
            .setOnDismissListener {}
            .create()
        currentDialog?.show()
    }

    private fun rebuild() {
        currentDialog?.dismiss()
        show()
    }
}
