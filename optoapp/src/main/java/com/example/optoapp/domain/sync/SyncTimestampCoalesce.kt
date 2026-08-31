package com.example.optoapp.domain.sync

import java.time.Instant

/**
 * WHY: PostgREST encodeDefaults sends explicit null; remote monturas/movimientos
 * updated_at is NOT NULL and INSERT triggers do not fill nulls (23502).
 */
internal fun coalesceUploadUpdatedAt(existing: String?): String =
    if (existing.isNullOrBlank()) Instant.now().toString() else existing
