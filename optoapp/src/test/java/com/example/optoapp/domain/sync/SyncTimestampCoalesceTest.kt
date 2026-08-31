package com.example.optoapp.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTimestampCoalesceTest {

    @Test
    fun coalesceUploadUpdatedAt_preservesNonBlank() {
        assertEquals("2026-08-31T10:00:00Z", coalesceUploadUpdatedAt("2026-08-31T10:00:00Z"))
    }

    @Test
    fun coalesceUploadUpdatedAt_stampsNull() {
        val stamped = coalesceUploadUpdatedAt(null)
        assertNotNull(stamped)
        assertTrue(stamped.isNotBlank())
    }

    @Test
    fun coalesceUploadUpdatedAt_stampsBlankAndWhitespace() {
        assertTrue(coalesceUploadUpdatedAt("").isNotBlank())
        assertTrue(coalesceUploadUpdatedAt("   ").isNotBlank())
    }
}
