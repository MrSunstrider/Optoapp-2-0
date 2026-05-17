package com.example.optoapp.viewmodel

import com.example.optoapp.data.SecurityManager
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for AuthViewModel — baseline + static contracts.
 *
 * AuthViewModel requires Hilt for construction (AuthDelegate, BackupDelegate depend
 * on Android/Supabase deps). The delegates' pure logic is tested in their own
 * test classes (AuthDelegateTest, BackupDelegateTest, PinDelegateTest).
 */
class AuthViewModelTest {

    @Test
    fun isValidPin_validLengthAndDigits_returnsTrue() {
        assertTrue(SecurityManager.isValidPin("123789"))
    }

    @Test
    fun isValidPin_tooShort_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("123"))
    }

    @Test
    fun isValidPin_tooLong_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("1234567"))
    }

    @Test
    fun isValidPin_empty_returnsFalse() {
        assertFalse(SecurityManager.isValidPin(""))
    }

    @Test
    fun isValidPin_nonDigits_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("12345a"))
    }

    @Test
    fun isValidPin_weakPattern_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("123456"))
    }

    @Test
    fun isValidPin_repeatedPattern_returnsFalse() {
        assertFalse(SecurityManager.isValidPin("111111"))
    }

    @Test
    fun pinLength_isSix() {
        assertEquals(6, SecurityManager.PIN_LENGTH)
    }

    @Test
    fun authStateLoading_isNotIdle() {
        val loading = AuthState.Loading
        val idle = AuthState.Idle
        assertNotEquals(idle, loading)
    }

    @Test
    fun authStateError_holdsMessage() {
        val error = AuthState.Error("Test error")
        assertEquals("Test error", (error as AuthState.Error).message)
    }

    @Test
    fun authStateSuccess_isNotError() {
        val success = AuthState.Success
        assertFalse(success is AuthState.Error)
    }
}
