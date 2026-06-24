package com.example.optoapp.util

import org.junit.Assert.assertEquals
import org.junit.Test

class InputFormattersTest {

    // ── formatPhoneInput (XXX XXX XXX) ─────────────────────────────────

    @Test
    fun formatPhoneInput_empty_returnsEmpty() {
        assertEquals("", InputFormatters.formatPhoneInput(""))
    }

    @Test
    fun formatPhoneInput_oneDigit() {
        assertEquals("9", InputFormatters.formatPhoneInput("9"))
    }

    @Test
    fun formatPhoneInput_twoDigits() {
        assertEquals("95", InputFormatters.formatPhoneInput("95"))
    }

    @Test
    fun formatPhoneInput_threeDigits_noTrailingSpace() {
        assertEquals("952", InputFormatters.formatPhoneInput("952"))
    }

    @Test
    fun formatPhoneInput_fourDigits_insertsSpace() {
        assertEquals("952 1", InputFormatters.formatPhoneInput("9521"))
    }

    @Test
    fun formatPhoneInput_sixDigits() {
        assertEquals("952 142", InputFormatters.formatPhoneInput("952142"))
    }

    @Test
    fun formatPhoneInput_sevenDigits_insertsSecondSpace() {
        assertEquals("952 142 2", InputFormatters.formatPhoneInput("9521422"))
    }

    @Test
    fun formatPhoneInput_nineDigits_fullNumber() {
        assertEquals("952 142 241", InputFormatters.formatPhoneInput("952142241"))
    }

    @Test
    fun formatPhoneInput_alreadyFormatted_unchanged() {
        assertEquals("952 142 241", InputFormatters.formatPhoneInput("952 142 241"))
    }

    @Test
    fun formatPhoneInput_maxNineDigits() {
        assertEquals("952 142 241", InputFormatters.formatPhoneInput("9521422415"))
    }

    @Test
    fun formatPhoneInput_stripsNonDigit() {
        assertEquals("952 142 241", InputFormatters.formatPhoneInput("952-142.241"))
    }

    @Test
    fun formatPhoneInput_deletionFromFull() {
        // Backspace from "952 142 241" → "952 142 24"
        assertEquals("952 142 24", InputFormatters.formatPhoneInput("952 142 24"))
    }

    @Test
    fun formatPhoneInput_deletionPastSpace() {
        // Backspace from "952 142" → "952 14"
        assertEquals("952 14", InputFormatters.formatPhoneInput("952 14"))
    }
}
