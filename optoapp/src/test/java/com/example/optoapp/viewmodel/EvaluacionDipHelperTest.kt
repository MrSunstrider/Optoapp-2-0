package com.example.optoapp.viewmodel

import org.junit.Assert.*
import org.junit.Test

class EvaluacionDipHelperTest {

    // ─── parseDipOrDnp ──────────────────────────────────────────────

    @Test
    fun parseDipOrDnp_empty_returnsEmpty() {
        val result = parseDipOrDnp("")
        assertNull(result.dipTotalMm)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    @Test
    fun parseDipOrDnp_whitespace_returnsEmpty() {
        val result = parseDipOrDnp("   ")
        assertNull(result.dipTotalMm)
    }

    @Test
    fun parseDipOrDnp_singleNumber_returnsDipTotal() {
        val result = parseDipOrDnp("64")
        assertNotNull(result.dipTotalMm)
        assertEquals(64.0, result.dipTotalMm!!, 0.001)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    @Test
    fun parseDipOrDnp_decimalWithComma_returnsDipTotal() {
        val result = parseDipOrDnp("63,5")
        assertNotNull(result.dipTotalMm)
        assertEquals(63.5, result.dipTotalMm!!, 0.001)
    }

    @Test
    fun parseDipOrDnp_odOiPair_returnsBoth() {
        val result = parseDipOrDnp("32/31")
        assertNotNull(result.dipTotalMm)
        assertEquals(63.0, result.dipTotalMm!!, 0.001)
        assertNotNull(result.dnpOdMm)
        assertEquals(32.0, result.dnpOdMm!!, 0.001)
        assertNotNull(result.dnpOiMm)
        assertEquals(31.0, result.dnpOiMm!!, 0.001)
    }

    @Test
    fun parseDipOrDnp_invalidString_returnsEmpty() {
        val result = parseDipOrDnp("abc")
        assertNull(result.dipTotalMm)
    }

    @Test
    fun parseDipOrDnp_invalidPairFormat_returnsEmpty() {
        val result = parseDipOrDnp("32/x")
        assertNull(result.dipTotalMm)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    @Test
    fun parseDipOrDnp_singleNumberWithSpaces_parsedCorrectly() {
        val result = parseDipOrDnp(" 64 ")
        assertNotNull(result.dipTotalMm)
        assertEquals(64.0, result.dipTotalMm!!, 0.001)
    }

    @Test
    fun parseDipOrDnp_pairWithSpaces_parsedCorrectly() {
        val result = parseDipOrDnp(" 32 / 31 ")
        assertNotNull(result.dipTotalMm)
        assertEquals(63.0, result.dipTotalMm!!, 0.001)
        assertNotNull(result.dnpOdMm)
        assertEquals(32.0, result.dnpOdMm!!, 0.001)
        assertNotNull(result.dnpOiMm)
        assertEquals(31.0, result.dnpOiMm!!, 0.001)
    }

    // ─── formatDipForUi ──────────────────────────────────────────────

    @Test
    fun formatDipForUi_dipLejosRawNotEmpty_returnsRawValue() {
        val result = formatDipForUi("64", 60.0, 30.0, 30.0)
        assertEquals("64", result)
    }

    @Test
    fun formatDipForUi_bothDnpValues_returnsOdOiPair() {
        val result = formatDipForUi("", 60.0, 32.0, 31.0)
        assertEquals("32/31", result)
    }

    @Test
    fun formatDipForUi_onlyDipTotal_returnsSingleValue() {
        val result = formatDipForUi("", 64.0, null, null)
        assertEquals("64", result)
    }

    @Test
    fun formatDipForUi_nothingAvailable_returnsEmpty() {
        val result = formatDipForUi("", null, null, null)
        assertEquals("", result)
    }

    @Test
    fun formatDipForUi_dipTotalDecimal_prettyPrinted() {
        val result = formatDipForUi("", 63.5, null, null)
        assertEquals("63.5", result)
    }

    @Test
    fun formatDipForUi_dipTotalInteger_noDecimal() {
        val result = formatDipForUi("", 60.0, null, null)
        assertEquals("60", result)
    }

    @Test
    fun formatDipForUi_dnpDecimalValues_prettyPrinted() {
        val result = formatDipForUi("", 63.0, 31.5, 31.5)
        assertEquals("31.5/31.5", result)
    }
}
