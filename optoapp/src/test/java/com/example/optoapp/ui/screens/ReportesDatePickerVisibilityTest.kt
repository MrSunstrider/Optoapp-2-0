package com.example.optoapp.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportesDatePickerVisibilityTest {
    @Test
    fun diarioAndSemanal_showDatePicker() {
        assertTrue(reportesShowsDatePicker("Diario"))
        assertTrue(reportesShowsDatePicker("Semanal"))
    }

    @Test
    fun mensualAnualTotal_hideDatePicker() {
        assertFalse(reportesShowsDatePicker("Mensual"))
        assertFalse(reportesShowsDatePicker("Anual"))
        assertFalse(reportesShowsDatePicker("Total"))
        assertFalse(reportesShowsDatePicker("Todo"))
    }
}
