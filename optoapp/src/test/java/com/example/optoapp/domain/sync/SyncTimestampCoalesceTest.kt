package com.example.optoapp.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTimestampCoalesceTest {

    @Test
    fun coalesceUpdatedAt_preservesNonBlank() {
        assertEquals("2026-08-31T10:00:00Z", coalesceUpdatedAt("2026-08-31T10:00:00Z"))
    }

    @Test
    fun coalesceUpdatedAt_stampsNull() {
        val stamped = coalesceUpdatedAt(null)
        assertNotNull(stamped)
        assertTrue(stamped.isNotBlank())
    }

    @Test
    fun coalesceUpdatedAt_stampsBlankAndWhitespace() {
        assertTrue(coalesceUpdatedAt("").isNotBlank())
        assertTrue(coalesceUpdatedAt("   ").isNotBlank())
    }
}
