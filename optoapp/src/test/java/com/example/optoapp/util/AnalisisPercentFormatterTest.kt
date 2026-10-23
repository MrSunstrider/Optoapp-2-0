package com.example.optoapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalisisPercentFormatterTest {

    @Test
    fun format_oneDecimal_always() {
        assertEquals("25.5", AnalisisPercentFormatter.format(25.54))
        assertEquals("25.5", AnalisisPercentFormatter.format(25.5))
        assertEquals("8.0", AnalisisPercentFormatter.format(8.0))
        assertEquals("0.0", AnalisisPercentFormatter.format(0.0))
    }

    @Test
    fun formatWithSymbol_appendsPercent() {
        assertEquals("12.3%", AnalisisPercentFormatter.formatWithSymbol(12.34))
    }
}
