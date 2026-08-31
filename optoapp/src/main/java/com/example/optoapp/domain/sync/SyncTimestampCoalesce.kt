package com.example.optoapp.domain.sync

import java.time.Instant

/**
 * WHY: Shared null/blank → now for upload (PostgREST encodeDefaults → 23502) and
 * backup restore (stamp missing timestamps without clobbering valid ones).
 */
internal fun coalesceUpdatedAt(existing: String?): String =
    if (existing.isNullOrBlank()) Instant.now().toString() else existing
