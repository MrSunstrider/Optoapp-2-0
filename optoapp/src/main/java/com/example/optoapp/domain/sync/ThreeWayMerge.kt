package com.example.optoapp.domain.sync

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * FR-09: Field-Level Three-Way Merge Logic.
 *
 * Pure class with no dependencies. Compares base / local / remote JSON snapshots
 * field-by-field and classifies each field as:
 * - no change (both sides equal base)
 * - auto-merge (only one side changed)
 * - conflict (both sides changed, values differ)
 *
 * A field MISSING from one side is treated as "that side did not touch the field"
 * (kept at base value), not as "that side deleted the field".
 */
data class MergeInput(
    val baseJson: JsonObject,
    val localJson: JsonObject,
    val remoteJson: JsonObject,
)

data class MergeResult<T>(
    val mergedEntity: T,
    val conflictedFields: List<String>,
    val autoMergedFields: List<String>,
    val hasConflict: Boolean,
)

object ThreeWayMerge {

    /**
     * Merges [input] per FR-09 rules. The returned [MergeResult.mergedEntity] contains
     * auto-merged fields with their resolved values; conflicted fields retain the local
     * value as a starting point — the caller ([SyncViewModel]) applies the final
     * local-wins or remote-wins policy for conflicted fields.
     */
    fun merge(input: MergeInput): MergeResult<JsonObject> {
        val base = input.baseJson
        val local = input.localJson
        val remote = input.remoteJson

        val allFields = (base.keys + local.keys + remote.keys).toSortedSet()
        val merged = mutableMapOf<String, JsonElement>()
        val conflicted = mutableListOf<String>()
        val autoMerged = mutableListOf<String>()

        for (field in allFields) {
            val baseVal = base[field]
            val localVal = local[field]
            val remoteVal = remote[field]

            val localChanged = localVal != null && localVal != baseVal
            val remoteChanged = remoteVal != null && remoteVal != baseVal

            when {
                !localChanged && !remoteChanged -> {
                    // No change from either side — keep the value (prefer local, then base, then remote)
                    merged[field] = localVal ?: baseVal ?: remoteVal!!
                }
                localChanged && !remoteChanged -> {
                    // Auto-merge: apply local
                    merged[field] = localVal!!
                    autoMerged.add(field)
                }
                !localChanged && remoteChanged -> {
                    // Auto-merge: apply remote
                    merged[field] = remoteVal!!
                    autoMerged.add(field)
                }
                else -> {
                    if (localVal == remoteVal) {
                        merged[field] = localVal!!
                        autoMerged.add(field)
                    } else {
                        merged[field] = localVal ?: remoteVal!!
                        conflicted.add(field)
                    }
                }
            }
        }

        return MergeResult(
            mergedEntity = JsonObject(merged),
            conflictedFields = conflicted,
            autoMergedFields = autoMerged,
            hasConflict = conflicted.isNotEmpty(),
        )
    }
}
