package com.example.optoapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotivoDialogStateTest {

    @Test
    fun `confirm requires a non blank motivo`() {
        assertFalse(canConfirmMotivo("", submitting = false))
        assertFalse(canConfirmMotivo("   ", submitting = false))
        assertTrue(canConfirmMotivo("Cliente desistió", submitting = false))
    }

    @Test
    fun `confirm is disabled while submitting`() {
        assertFalse(canConfirmMotivo("Cliente desistió", submitting = true))
    }

    @Test
    fun `motivo input is capped at 500 characters`() {
        assertEquals(500, limitMotivo("x".repeat(520)).length)
        assertEquals("Lente rayado", limitMotivo("Lente rayado"))
    }

    @Test
    fun `counter shows used and maximum characters`() {
        assertEquals("12/500", motivoCounter("Lente rayado"))
        assertEquals("0/500", motivoCounter(""))
    }
}
