package com.example.optoapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    private val emDash = "\u2014"

    @Test
    fun `formats with thousands separator and two decimals`() {
        assertEquals("S/ 1,234.50", MoneyFormatter.format(1234.5))
    }

    @Test
    fun `formats zero with two decimals`() {
        assertEquals("S/ 0.00", MoneyFormatter.format(0.0))
    }

    @Test
    fun `rounds half up on the decimal value`() {
        assertEquals("S/ 0.13", MoneyFormatter.format(0.125))
    }

    @Test
    fun `places the minus sign before the symbol`() {
        assertEquals("-S/ 80.00", MoneyFormatter.format(-80.0))
    }

    @Test
    fun `negative amount that rounds to zero has no sign`() {
        assertEquals("S/ 0.00", MoneyFormatter.format(-0.001))
    }

    @Test
    fun `non finite amounts render an em dash`() {
        assertEquals(emDash, MoneyFormatter.format(Double.NaN))
        assertEquals(emDash, MoneyFormatter.format(Double.POSITIVE_INFINITY))
        assertEquals(emDash, MoneyFormatter.format(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `abbreviates thousands with one decimal`() {
        assertEquals("S/ 1.2k", MoneyFormatter.formatAbbreviated(1200.0))
    }

    @Test
    fun `keeps full format below one thousand`() {
        assertEquals("S/ 999.00", MoneyFormatter.formatAbbreviated(999.0))
    }

    @Test
    fun `abbreviates millions with one decimal`() {
        assertEquals("S/ 2.5M", MoneyFormatter.formatAbbreviated(2500000.0))
    }

    @Test
    fun `abbreviation starts exactly at one thousand and one million`() {
        assertEquals("S/ 1.0k", MoneyFormatter.formatAbbreviated(1000.0))
        assertEquals("S/ 1.0M", MoneyFormatter.formatAbbreviated(1_000_000.0))
    }

    @Test
    fun `thousands that round up to one thousand k are promoted to millions`() {
        assertEquals("S/ 1.0M", MoneyFormatter.formatAbbreviated(999_960.0))
    }

    @Test
    fun `abbreviated negative keeps the sign before the symbol`() {
        assertEquals("-S/ 1.5k", MoneyFormatter.formatAbbreviated(-1450.0))
    }

    @Test
    fun `abbreviated non finite amounts render an em dash`() {
        assertEquals(emDash, MoneyFormatter.formatAbbreviated(Double.NaN))
        assertEquals(emDash, MoneyFormatter.formatAbbreviated(Double.POSITIVE_INFINITY))
        assertEquals(emDash, MoneyFormatter.formatAbbreviated(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `legacy formatCurrency delegates to MoneyFormatter`() {
        assertEquals("S/ 1,234.50", NumberFormatter.formatCurrency(1234.5))
    }
}
