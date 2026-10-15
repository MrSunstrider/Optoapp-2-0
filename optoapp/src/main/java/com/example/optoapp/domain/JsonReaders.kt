package com.example.optoapp.domain

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

// Reads go through `as?` instead of `jsonObject`/`jsonPrimitive` so a null or wrongly typed
// value from an older server degrades to the default instead of throwing.
internal fun JsonObject.optDouble(key: String): Double = optDoubleNullable(key) ?: 0.0

internal fun JsonObject.optInt(key: String): Int = (this[key] as? JsonPrimitive)?.intOrNull ?: 0

internal fun JsonObject.optDoubleNullable(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull

internal fun JsonObject.optString(key: String): String = optStringNullable(key) ?: ""

internal fun JsonObject.optStringNullable(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

internal fun JsonObject.optObject(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.optArray(key: String): JsonArray? = this[key] as? JsonArray
