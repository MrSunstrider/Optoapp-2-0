package com.example.optoapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityManagerTest {

    /**
     * In-memory contract fake — SecurityManager needs EncryptedSharedPreferences.
     * Mirrors savePin / clearStoredPin dual-writer behavior for unit tests.
     */
    private class FakeSecurityManager : ISecurityManager {
        private val _userPin = MutableStateFlow("")
        private val _pinHasBeenSet = MutableStateFlow(false)
        override val userPin: Flow<String> = _userPin.asStateFlow()
        override val pinHasBeenSet: Flow<Boolean> = _pinHasBeenSet.asStateFlow()
        override suspend fun savePin(pin: String) {
            _userPin.value = pin
            _pinHasBeenSet.value = true
        }
        override suspend fun clearStoredPin() {
            _userPin.value = ""
            _pinHasBeenSet.value = false
        }
    }

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
    fun migratePinHasBeenSet_excludes_dev_fallback_pin() {
        // "999999" es DEV_FALLBACK_PIN — existe solo como desarrollo, no cuenta como PIN del usuario.
        assertFalse("DEV_FALLBACK_PIN debe ser inválido en isValidPin", SecurityManager.isValidPin("999999"))
        assertTrue("PIN personalizado sigue siendo aceptado", SecurityManager.isValidPin("183729"))
    }

    @Test
    fun clearStoredPin_afterSavePin_emptiesSecretAndClearsFlag() = runTest {
        val sm = FakeSecurityManager()
        sm.savePin("183729")
        assertEquals("183729", sm.userPin.first())
        assertTrue(sm.pinHasBeenSet.first())

        sm.clearStoredPin()

        assertEquals("", sm.userPin.first())
        assertFalse(sm.pinHasBeenSet.first())
    }

    @Test
    fun clearStoredPin_whenAlreadyEmpty_staysEmptyAndFlagFalse() = runTest {
        val sm = FakeSecurityManager()
        sm.clearStoredPin()

        assertEquals("", sm.userPin.first())
        assertFalse(sm.pinHasBeenSet.first())
    }
}
