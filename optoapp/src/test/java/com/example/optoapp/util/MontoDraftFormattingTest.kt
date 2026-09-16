package com.example.optoapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MontoDraftFormattingTest {

    @Test
    fun formatDraft_nullOrZero_returnsEmpty() {
        assertEquals("", MontoDraftFormatting.formatDraft(null))
        assertEquals("", MontoDraftFormatting.formatDraft(0.0))
        assertEquals("", MontoDraftFormatting.formatDraft(-0.0))
    }

    @Test
    fun formatDraft_wholeNumber_noTrailingDotZero() {
        assertEquals("150", MontoDraftFormatting.formatDraft(150.0))
        assertEquals("1", MontoDraftFormatting.formatDraft(1.0))
    }

    @Test
    fun formatDraft_fraction_usesTwoDecimals() {
        assertEquals("150.50", MontoDraftFormatting.formatDraft(150.5))
        assertEquals("99.99", MontoDraftFormatting.formatDraft(99.99))
    }

    @Test
    fun parseDraft_empty_null() {
        assertNull(MontoDraftFormatting.parseDraft(""))
        assertNull(MontoDraftFormatting.parseDraft("   "))
    }

    @Test
    fun parseDraft_acceptsCommaAndDot() {
        assertEquals(150.0, MontoDraftFormatting.parseDraft("150")!!, 0.0)
        assertEquals(150.5, MontoDraftFormatting.parseDraft("150,50")!!, 0.0)
        assertEquals(150.5, MontoDraftFormatting.parseDraft("150.50")!!, 0.0)
    }
}
