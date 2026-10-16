package com.example.optoapp.data.opticasettings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Commission percent for Reportes Equipo, stored in optica_settings.configJson.
 * Default 0 — do not invent a business rate without dueño configuration.
 */
object CommissionConfigJson {
    const val KEY = "commission_pct"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun extractPct(configJson: String): Double {
        if (configJson.isBlank()) return 0.0
        return runCatching {
            json.parseToJsonElement(configJson).jsonObject[KEY]
                ?.jsonPrimitive
                ?.doubleOrNull
                ?: json.parseToJsonElement(configJson).jsonObject[KEY]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.toDoubleOrNull()
                ?: 0.0
        }.getOrDefault(0.0).coerceIn(0.0, 100.0)
    }

    fun mergePct(configJson: String, pct: Double): String {
        val base = runCatching {
            json.parseToJsonElement(configJson.ifBlank { "{}" }).jsonObject.toMutableMap()
        }.getOrElse { mutableMapOf() }
        base[KEY] = JsonPrimitive(pct.coerceIn(0.0, 100.0))
        return JsonObject(base).toString()
    }
}
