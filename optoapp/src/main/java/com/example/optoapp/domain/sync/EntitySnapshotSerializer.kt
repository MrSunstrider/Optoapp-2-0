package com.example.optoapp.domain.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

/**
 * FR-08 / FR-09: Serialization helpers for full-entity snapshot capture.
 *
 * Converts entity DTOs to/from JSON strings for storage in [ConflictRecord]
 * fields (baseSnapshot, localData, remoteData).
 */
object EntitySnapshotSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    /**
     * Serializes any [@Serializable] entity to its JSON string representation.
     * Returns `"{}"` on serialization error.
     */
    fun serialize(entity: Any): String = try {
        json.encodeToString(json.encodeToJsonElement(entity))
    } catch (e: Exception) {
        "{}"
    }

    /**
     * Parses a JSON [snapshot] string into a [JsonObject].
     * Returns an empty `JsonObject()` on parse error.
     */
    fun parseSnapshot(snapshot: String): JsonObject = try {
        json.decodeFromString(snapshot)
    } catch (e: Exception) {
        JsonObject(emptyMap())
    }

    /**
     * Determines whether a baseline snapshot is available for three-way merging.
     * Returns `true` when [baseSnapshot] is non-blank and not the default `"{}"`.
     */
    fun hasSnapshotData(baseSnapshot: String): Boolean = baseSnapshot.isNotBlank() && baseSnapshot != "{}"
}
