package com.example.optoapp.viewmodel.dip

import org.junit.Assert.*
import org.junit.Test

/**
 * Characterization tests for DipParser — pure functions extracted from
 * EvaluacionViewModel. These tests capture the EXACT current behavior of
 * parseDipOrDnp and formatDipForUi so that extraction preserves semantics.
 */
class DipParserTest {

    // ─── parseDipOrDnp: blank / edge inputs ────────────────────────────

    @Test
    fun `parseDipOrDnp empty string returns all nulls`() {
        val result = DipParser.parseDipOrDnp("")
        assertNull(result.dipTotalMm)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    @Test
    fun `parseDipOrDnp whitespace string returns all nulls`() {
        val result = DipParser.parseDipOrDnp("   ")
        assertNull(result.dipTotalMm)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    // ─── parseDipOrDnp: single DIP value ──────────────────────────────

    @Test
    fun `parseDipOrDnp integer dip returns dipTotalMm only`() {
        val result = DipParser.parseDipOrDnp("62")
        assertEquals(62.0, result.dipTotalMm!!, 0.001)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    @Test
    fun `parseDipOrDnp decimal dip with dot returns dipTotalMm only`() {
        val result = DipParser.parseDipOrDnp("61.5")
        assertEquals(61.5, result.dipTotalMm!!, 0.001)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    @Test
    fun `parseDipOrDnp decimal dip with comma returns dipTotalMm only`() {
        val result = DipParser.parseDipOrDnp("61,5")
        assertEquals(61.5, result.dipTotalMm!!, 0.001)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    // ─── parseDipOrDnp: DNP pair (OD/OI) ──────────────────────────────

    @Test
    fun `parseDipOrDnp dnp pair with slash returns both values and sum`() {
        val result = DipParser.parseDipOrDnp("30/31")
        assertEquals(61.0, result.dipTotalMm!!, 0.001)
        assertEquals(30.0, result.dnpOdMm!!, 0.001)
        assertEquals(31.0, result.dnpOiMm!!, 0.001)
    }

    @Test
    fun `parseDipOrDnp dnp pair with decimals and comma`() {
        val result = DipParser.parseDipOrDnp("29,5/30,5")
        assertEquals(60.0, result.dipTotalMm!!, 0.001)
        assertEquals(29.5, result.dnpOdMm!!, 0.001)
        assertEquals(30.5, result.dnpOiMm!!, 0.001)
    }

    @Test
    fun `parseDipOrDnp dnp pair with spaces is trimmed`() {
        val result = DipParser.parseDipOrDnp("  30 / 31  ")
        assertEquals(61.0, result.dipTotalMm!!, 0.001)
        assertEquals(30.0, result.dnpOdMm!!, 0.001)
        assertEquals(31.0, result.dnpOiMm!!, 0.001)
    }

    // ─── parseDipOrDnp: invalid inputs ─────────────────────────────────

    @Test
    fun `parseDipOrDnp non numeric string returns all nulls`() {
        val result = DipParser.parseDipOrDnp("abc")
        assertNull(result.dipTotalMm)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    @Test
    fun `parseDipOrDnp invalid pair with non numeric part returns all nulls`() {
        val result = DipParser.parseDipOrDnp("30/xx")
        assertNull(result.dipTotalMm)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    @Test
    fun `parseDipOrDnp triple slash returns all nulls`() {
        val result = DipParser.parseDipOrDnp("30/31/32")
        assertNull(result.dipTotalMm)
        assertNull(result.dnpOdMm)
        assertNull(result.dnpOiMm)
    }

    // ─── formatDipForUi: raw input takes precedence ────────────────────

    @Test
    fun `formatDipForUi returns raw input when dipLejosRaw is not blank`() {
        val result = DipParser.formatDipForUi(
            dipLejosRaw = "65",
            dipTotalMm = 62.0,
            dnpOdMm = 30.0,
            dnpOiMm = 31.0
        )
        assertEquals("65", result)
    }

    @Test
    fun `formatDipForUi returns empty when all nulls and raw blank`() {
        val result = DipParser.formatDipForUi(
            dipLejosRaw = "",
            dipTotalMm = null,
            dnpOdMm = null,
            dnpOiMm = null
        )
        assertEquals("", result)
    }

    // ─── formatDipForUi: DNP pair format ───────────────────────────────

    @Test
    fun `formatDipForUi formats od slash oi when both dnp values present`() {
        val result = DipParser.formatDipForUi(
            dipLejosRaw = "",
            dipTotalMm = 61.0,
            dnpOdMm = 30.0,
            dnpOiMm = 31.0
        )
        assertEquals("30/31", result)
    }

    @Test
    fun `formatDipForUi formats od slash oi with integer rounding`() {
        val result = DipParser.formatDipForUi(
            dipLejosRaw = "",
            dipTotalMm = 60.0,
            dnpOdMm = 29.5,
            dnpOiMm = 30.5
        )
        assertEquals("29.5/30.5", result)
    }

    // ─── formatDipForUi: single DIP total ──────────────────────────────

    @Test
    fun `formatDipForUi shows dipTotalMm when only total is available`() {
        val result = DipParser.formatDipForUi(
            dipLejosRaw = "",
            dipTotalMm = 62.0,
            dnpOdMm = null,
            dnpOiMm = null
        )
        assertEquals("62", result)
    }

    @Test
    fun `formatDipForUi shows dipTotalMm decimal`() {
        val result = DipParser.formatDipForUi(
            dipLejosRaw = "",
            dipTotalMm = 61.5,
            dnpOdMm = null,
            dnpOiMm = null
        )
        assertEquals("61.5", result)
    }

    @Test
    fun `formatDipForUi prefers dnp pair over dipTotal when both available`() {
        val result = DipParser.formatDipForUi(
            dipLejosRaw = "",
            dipTotalMm = 61.0,
            dnpOdMm = 30.0,
            dnpOiMm = 31.0
        )
        assertEquals("30/31", result)
    }
}
