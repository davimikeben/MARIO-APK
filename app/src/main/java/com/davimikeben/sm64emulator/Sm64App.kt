package com.davimikeben.sm64emulator

import android.app.Application
import android.util.Log
import com.davimikeben.sm64emulator.perf.PerfManager
import com.davimikeben.sm64emulator.util.SettingsManager

/**
 * Ponto de entrada do aplicativo.
 * Inicializa gerenciadores globais antes da primeira Activity.
 */
class Sm64App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        SettingsManager.init(this)
        PerfManager.init(this)
        Log.i(TAG, "SM64Emulator initialized — davimikeben build")
    }

    companion object {
        private const val TAG = "Sm64App"
        lateinit var instance: Sm64App
            private set
    }
}
