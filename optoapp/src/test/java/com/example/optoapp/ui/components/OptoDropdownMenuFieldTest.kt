package com.example.optoapp.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OptoDropdownMenuFieldTest {

    @Test
    fun shouldUseLazyColumnForDropdown_falseForPendienteEntregadoPair() {
        assertFalse(shouldUseLazyColumnForDropdown(2))
    }

    @Test
    fun shouldUseLazyColumnForDropdown_falseAtThreshold() {
        assertFalse(shouldUseLazyColumnForDropdown(DROPDOWN_LAZY_LIST_THRESHOLD))
    }

    @Test
    fun shouldUseLazyColumnForDropdown_trueAboveThreshold() {
        assertTrue(shouldUseLazyColumnForDropdown(DROPDOWN_LAZY_LIST_THRESHOLD + 1))
    }
}
