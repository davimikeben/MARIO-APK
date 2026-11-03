package com.davimikeben.sm64emulator.util

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converte o snapshot do SettingsManager (Map<String,*>) para JSON e vice-versa.
 */
object JsonHelper {
    fun toJson(map: Map<String, *>): String {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            when (v) {
                is Boolean -> obj.put(k, v)
                is Int -> obj.put(k, v)
                is Long -> obj.put(k, v)
                is Float -> obj.put(k, v.toDouble())
                is String -> obj.put(k, v)
                is Set<*> -> obj.put(k, JSONArray().also { arr -> v.forEach { arr.put(it.toString()) } })
                else -> obj.put(k, v.toString())
            }
        }
        return obj.toString(2)
    }

    fun fromJsonToSettings(json: String): Map<String, Any?> {
        val obj = JSONObject(json)
        val out = mutableMapOf<String, Any?>()
        for (key in obj.keys()) {
            val v = obj.get(key)
            when (v) {
                is Boolean, is Int, is Long, is String -> out[key] = v
                is Number -> out[key] = v.toDouble()
                else -> out[key] = v?.toString()
            }
        }
        return out
    }
}
