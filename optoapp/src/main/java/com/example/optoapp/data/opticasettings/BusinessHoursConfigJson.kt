package com.example.optoapp.data.opticasettings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pure helpers for optica_settings.configJson business_hours key.
 * Preserves sibling keys on merge so fiscal/theme/etc. are not wiped.
 */
object BusinessHoursConfigJson {
    const val KEY = "business_hours"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun extractBusinessHours(configJson: String): String {
        if (configJson.isBlank()) return ""
        return runCatching {
            json.parseToJsonElement(configJson).jsonObject[KEY]
                ?.jsonPrimitive
                ?.contentOrNull
                .orEmpty()
        }.getOrDefault("")
    }

    fun mergeBusinessHours(configJson: String, hours: String): String {
        val base = runCatching {
            json.parseToJsonElement(configJson.ifBlank { "{}" }).jsonObject.toMutableMap()
        }.getOrElse { mutableMapOf() }
        base[KEY] = JsonPrimitive(hours)
        return JsonObject(base).toString()
    }
}
