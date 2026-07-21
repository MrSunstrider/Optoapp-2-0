package com.example.optoapp

import com.example.optoapp.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.TimeZone

class DateUtilsTest {

    @Test
    fun fromDisplayFormat_validDate_returnsLocalDate() {
        val result = DateUtils.fromDisplayFormat("15/03/1990")
        assertEquals(LocalDate.of(1990, 3, 15), result)
    }

    @Test
    fun fromDisplayFormat_firstJanuary_returnsLocalDate() {
        val result = DateUtils.fromDisplayFormat("01/01/2000")
        assertEquals(LocalDate.of(2000, 1, 1), result)
    }

    @Test
    fun fromDisplayFormat_emptyString_returnsNull() {
        assertNull(DateUtils.fromDisplayFormat(""))
    }

    @Test
    fun fromDisplayFormat_isoFormat_stillParsedCorrectly() {
        val result = DateUtils.fromDisplayFormat("25/12/2024")
        assertEquals(LocalDate.of(2024, 12, 25), result)
    }

    @Test
    fun formatDateInput_empty_returnsEmpty() {
        assertEquals("", DateUtils.formatDateInput(""))
    }

    @Test
    fun formatDateInput_oneDigit_returnsOneDigit() {
        assertEquals("1", DateUtils.formatDateInput("1"))
    }

    @Test
    fun formatDateInput_twoDigits_returnsTwoDigitsNoSlash() {
        assertEquals("15", DateUtils.formatDateInput("15"))
    }

    @Test
    fun formatDateInput_slashStrippedToDigits() {
        // "15/" → digits "15" → reformats to "15"
        assertEquals("15", DateUtils.formatDateInput("15/"))
    }

    @Test
    fun formatDateInput_threeDigitsNoSlash_insertsSlashAfterDay() {
        assertEquals("15/0", DateUtils.formatDateInput("150"))
    }

    @Test
    fun formatDateInput_fourDigits_daySlashMonth() {
        assertEquals("15/03", DateUtils.formatDateInput("1503"))
    }

    @Test
    fun formatDateInput_fiveDigits_noSecondSlashYet() {
        assertEquals("15/03", DateUtils.formatDateInput("1503"))
    }

    @Test
    fun formatDateInput_sixDigits_insertsSecondSlash() {
        // "15/03/" → digits "1503" → reformats to "15/03"
        assertEquals("15/03", DateUtils.formatDateInput("15/03/"))
    }

    @Test
    fun formatDateInput_sixDigitsNoSlash_insertsSecondSlash() {
        assertEquals("15/03/1", DateUtils.formatDateInput("15031"))
    }

    @Test
    fun formatDateInput_sixDigits_yearPartial() {
        assertEquals("15/03/19", DateUtils.formatDateInput("150319"))
    }

    @Test
    fun formatDateInput_fullDate() {
        assertEquals("15/03/1990", DateUtils.formatDateInput("15031990"))
    }

    @Test
    fun formatDateInput_fullDateWithExistingSlashes() {
        assertEquals("15/03/1990", DateUtils.formatDateInput("15/03/1990"))
    }

    @Test
    fun formatDateInput_maxTenChars() {
        assertEquals("15/03/1990", DateUtils.formatDateInput("1503199012"))
    }

    @Test
    fun formatDateInput_stripsNonDigitExceptSlash() {
        assertEquals("15/03/1990", DateUtils.formatDateInput("15-03.1990"))
    }

    @Test
    fun formatDateInput_deletionFromFull_removesCorrectly() {
        // Simulate backspace from "15/03/1990" → "15/03/199"
        assertEquals("15/03/199", DateUtils.formatDateInput("15/03/199"))
    }

    @Test
    fun formatDateInput_deletionPastSlash_removesSlash() {
        // Simulate backspace from "15/03" → "15/0" → "15/0"
        assertEquals("15/0", DateUtils.formatDateInput("15/0"))
    }

    @Test
    fun testDateOffset() {
        val out = StringBuilder()
        out.append("Default timezone: ${TimeZone.getDefault().id}\n")

        val format = SimpleDateFormat("dd/MM/yyyy")
        format.timeZone = TimeZone.getTimeZone("UTC")
        val selectedUtcMillis = format.parse("25/04/2025")!!.time

        out.append("Selected UTC millis: $selectedUtcMillis\n")
        val dateUTC = Date(selectedUtcMillis)
        val localFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss") // local default formatter
        out.append("If formatted directly locally: ${localFmt.format(dateUTC)}\n")

        val selectedLocalDate = DateUtils.pickerMillisToLocalDate(selectedUtcMillis)
        out.append("Selected as LocalDate: $selectedLocalDate\n")

        val toPickerUtc = DateUtils.localDateToPickerMillis(selectedLocalDate)
        out.append("Back to picker UTC millis: $toPickerUtc\n")
        out.append("Picker reads as: ${format.format(Date(toPickerUtc))}\n")

        val roundTrip = DateUtils.pickerMillisToLocalDate(toPickerUtc)
        assertEquals(selectedLocalDate, roundTrip)

        System.out.println(out.toString())
    }
}
