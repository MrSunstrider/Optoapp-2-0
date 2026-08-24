package com.example.optoapp.data.opticasettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessHoursConfigJsonTest {

    @Test
    fun extract_returnsEmpty_forBlankOrMalformed() {
        assertEquals("", BusinessHoursConfigJson.extractBusinessHours(""))
        assertEquals("", BusinessHoursConfigJson.extractBusinessHours("{}"))
        assertEquals("", BusinessHoursConfigJson.extractBusinessHours("not-json"))
    }

    @Test
    fun extract_readsBusinessHoursKey() {
        val json = """{"business_hours":"Lun-Vie 9-18","theme":"dark"}"""
        assertEquals("Lun-Vie 9-18", BusinessHoursConfigJson.extractBusinessHours(json))
    }

    @Test
    fun merge_setsBusinessHours_preservingOtherKeys() {
        val merged = BusinessHoursConfigJson.mergeBusinessHours(
            """{"theme":"dark","business_hours":"old"}""",
            "Mar-Sab 10-19",
        )
        assertEquals("Mar-Sab 10-19", BusinessHoursConfigJson.extractBusinessHours(merged))
        assertTrue(merged.contains("\"theme\""))
        assertTrue(merged.contains("dark"))
        assertFalse(merged.contains("\"old\""))
    }

    @Test
    fun merge_createsObject_fromBlankOrMalformed() {
        val fromBlank = BusinessHoursConfigJson.mergeBusinessHours("", "9-18")
        assertEquals("9-18", BusinessHoursConfigJson.extractBusinessHours(fromBlank))

        val fromBad = BusinessHoursConfigJson.mergeBusinessHours("{broken", "10-20")
        assertEquals("10-20", BusinessHoursConfigJson.extractBusinessHours(fromBad))
    }
}
