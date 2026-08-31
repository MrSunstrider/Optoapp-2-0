package com.example.optoapp.domain.sync

import java.time.Instant

/**
 * Sentinel for Room backfill of null/blank updatedAt.
 * WHY: wall-clock "now" would inflate LWW and overwrite newer remote rows on first sync after upgrade.
 */
internal const val LEGACY_NULL_UPDATED_AT = "1970-01-01T00:00:00.000Z"

/**
 * WHY: Shared null/blank → now for upload wire payload (PostgREST encodeDefaults → 23502) and
 * backup restore (stamp missing timestamps without clobbering valid ones).
 * Do NOT use this for conflict-filter timestamps — coalesce only after LWW decides upload is safe.
 */
internal fun coalesceUpdatedAt(existing: String?): String =
    if (existing.isNullOrBlank()) Instant.now().toString() else existing
