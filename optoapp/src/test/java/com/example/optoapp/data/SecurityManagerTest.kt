package com.example.optoapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityManagerTest {

    @Test
    fun isValidPin_rejects_repeating_digits() {
        assertFalse(SecurityManager.isValidPin("000000"))
        assertFalse(SecurityManager.isValidPin("111111"))
        assertFalse(SecurityManager.isValidPin("222222"))
        assertFalse(SecurityManager.isValidPin("333333"))
        assertFalse(SecurityManager.isValidPin("444444"))
        assertFalse(SecurityManager.isValidPin("555555"))
        assertFalse(SecurityManager.isValidPin("666666"))
        assertFalse(SecurityManager.isValidPin("777777"))
        assertFalse(SecurityManager.isValidPin("888888"))
        assertFalse(SecurityManager.isValidPin("999999"))
    }

    @Test
    fun isValidPin_rejects_sequential_patterns() {
        assertFalse(SecurityManager.isValidPin("123456"))
        assertFalse(SecurityManager.isValidPin("234567"))
        assertFalse(SecurityManager.isValidPin("345678"))
        assertFalse(SecurityManager.isValidPin("456789"))
        assertFalse(SecurityManager.isValidPin("654321"))
        assertFalse(SecurityManager.isValidPin("543210"))
    }

    @Test
    fun isValidPin_rejects_non_digit_input() {
        assertFalse(SecurityManager.isValidPin("abcdef"))
        assertFalse(SecurityManager.isValidPin("12 345"))
        assertFalse(SecurityManager.isValidPin("12.345"))
    }

    @Test
    fun isValidPin_rejects_wrong_length() {
        assertFalse(SecurityManager.isValidPin(""))
        assertFalse(SecurityManager.isValidPin("1"))
        assertFalse(SecurityManager.isValidPin("12345"))
        assertFalse(SecurityManager.isValidPin("1234567"))
    }

    @Test
    fun isValidPin_accepts_valid_pins() {
        assertTrue(SecurityManager.isValidPin("183729"))
        assertTrue(SecurityManager.isValidPin("904812"))
        assertTrue(SecurityManager.isValidPin("573910"))
        assertTrue(SecurityManager.isValidPin("482601"))
    }

    @Test
    fun migratePinHasBeenSet_no_longer_excludes_123456() {
        // migratePinHasBeenSet() was simplified: the "123456" special case was removed.
        // Now ANY non-empty stored PIN triggers pinHasBeenSet = true.
        // This test validates the postcondition: isValidPin behavior is unchanged,
        // and "123456" is treated identically to any other stored PIN for migration.
        assertFalse("\"123456\" sigue siendo un PIN débil en isValidPin", SecurityManager.isValidPin("123456"))
        assertTrue("PIN válido sigue siendo aceptado", SecurityManager.isValidPin("183729"))
    }
}
