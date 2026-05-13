package com.example.optoapp

import com.example.optoapp.domain.sync.SyncResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncResultTest {
    @Test
    fun successIsObject() {
        assertTrue(SyncResult.Success is SyncResult)
    }

    @Test
    fun partialSuccessHasCounts() {
        val r = SyncResult.PartialSuccess(3, 5)
        assertEquals(3, r.uploaded)
        assertEquals(5, r.downloaded)
    }

    @Test
    fun errorHasMessage() {
        val r = SyncResult.Error("timeout")
        assertEquals("timeout", r.message)
    }
}
