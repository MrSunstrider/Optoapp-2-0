package com.example.optoapp.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

/**
 * Characterization tests for RecoveryState and password validation logic.
 *
 * These tests capture the state machine and data contracts introduced for the
 * password recovery flow. No mocking library is used — only observable state
 * patterns and pure validation logic.
 */
class RecoveryStateTest {

    // ─── RecoveryState sealed class behavior ─────────────────────────────

    @Test
    fun recoveryStateIdle_dataObject() {
        val s1: RecoveryState = RecoveryState.Idle
        val s2: RecoveryState = RecoveryState.Idle
        assertEquals(s1, s2)
        assertFalse(s1 is RecoveryState.Loading)
        assertFalse(s1 is RecoveryState.Error)
    }

    @Test
    fun recoveryStateLoading_isNotIdle() {
        val loading: RecoveryState = RecoveryState.Loading
        assertNotEquals(RecoveryState.Idle, loading)
        assertFalse(loading is RecoveryState.EmailSent)
        assertFalse(loading is RecoveryState.Error)
    }

    @Test
    fun recoveryStateError_holdsMessage() {
        val error: RecoveryState = RecoveryState.Error("Test error")
        assertTrue(error is RecoveryState.Error)
        assertEquals("Test error", (error as RecoveryState.Error).message)
    }

    @Test
    fun recoveryStateError_dataClass() {
        val e1 = RecoveryState.Error("msg")
        val e2 = RecoveryState.Error("msg")
        assertEquals(e1, e2)
        assertEquals(e1.hashCode(), e2.hashCode())
    }

    @Test
    fun recoveryStateError_differentMessages_notEqual() {
        val e1 = RecoveryState.Error("msg1")
        val e2 = RecoveryState.Error("msg2")
        assertNotEquals(e1, e2)
    }

    @Test
    fun recoveryStateEmailSent_dataObject() {
        val s1: RecoveryState = RecoveryState.EmailSent
        val s2: RecoveryState = RecoveryState.EmailSent
        assertEquals(s1, s2)
        assertFalse(s1 is RecoveryState.Loading)
    }

    @Test
    fun recoveryStateLinkReceived_dataObject() {
        val s1: RecoveryState = RecoveryState.LinkReceived
        val s2: RecoveryState = RecoveryState.LinkReceived
        assertEquals(s1, s2)
        assertFalse(s1 is RecoveryState.PasswordUpdated)
    }

    @Test
    fun recoveryStatePasswordUpdated_dataObject() {
        val s1: RecoveryState = RecoveryState.PasswordUpdated
        val s2: RecoveryState = RecoveryState.PasswordUpdated
        assertEquals(s1, s2)
        assertFalse(s1 is RecoveryState.Error)
    }

    // ─── State machine: MutableStateFlow<RecoveryState> pattern ──────────

    @Test
    fun recoveryStateFlow_startsAsIdle() {
        val flow = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
        assertEquals(RecoveryState.Idle, flow.value)
    }

    @Test
    fun recoveryStateFlow_transitionsIdleToLoading() {
        val flow = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
        flow.value = RecoveryState.Loading
        assertEquals(RecoveryState.Loading, flow.value)
    }

    @Test
    fun recoveryStateFlow_transitionsLoadingToEmailSent() {
        val flow = MutableStateFlow<RecoveryState>(RecoveryState.Loading)
        flow.value = RecoveryState.EmailSent
        assertEquals(RecoveryState.EmailSent, flow.value)
    }

    @Test
    fun recoveryStateFlow_transitionsLoadingToLinkReceived() {
        val flow = MutableStateFlow<RecoveryState>(RecoveryState.Loading)
        flow.value = RecoveryState.LinkReceived
        assertEquals(RecoveryState.LinkReceived, flow.value)
    }

    @Test
    fun recoveryStateFlow_transitionsLoadingToPasswordUpdated() {
        val flow = MutableStateFlow<RecoveryState>(RecoveryState.Loading)
        flow.value = RecoveryState.PasswordUpdated
        assertEquals(RecoveryState.PasswordUpdated, flow.value)
    }

    @Test
    fun recoveryStateFlow_transitionsLoadingToError() {
        val flow = MutableStateFlow<RecoveryState>(RecoveryState.Loading)
        flow.value = RecoveryState.Error("error")
        assertTrue(flow.value is RecoveryState.Error)
        assertEquals("error", (flow.value as RecoveryState.Error).message)
    }

    // ─── Password validation logic (same rules as RegisterScreen) ────────

    private fun validateNewPassword(password: String, confirmPassword: String): String? = when {
        password.length < 6 -> "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo."
        !password.any { it.isLowerCase() } -> "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo."
        !password.any { it.isUpperCase() } -> "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo."
        !password.any { it.isDigit() } -> "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo."
        !password.any { !it.isLetterOrDigit() } -> "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo."
        password != confirmPassword -> "Las contraseñas no coinciden."
        else -> null
    }

    @Test
    fun validateNewPassword_validPassword_returnsNull() {
        assertNull(validateNewPassword("Password1!", "Password1!"))
    }

    @Test
    fun validateNewPassword_tooShort_returnsWeakError() {
        val error = validateNewPassword("Pwd1!", "Pwd1!")
        assertEquals(
            "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo.",
            error
        )
    }

    @Test
    fun validateNewPassword_missingLowercase_returnsWeakError() {
        val error = validateNewPassword("PASSWORD1!", "PASSWORD1!")
        assertEquals(
            "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo.",
            error
        )
    }

    @Test
    fun validateNewPassword_missingUppercase_returnsWeakError() {
        val error = validateNewPassword("password1!", "password1!")
        assertEquals(
            "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo.",
            error
        )
    }

    @Test
    fun validateNewPassword_missingDigit_returnsWeakError() {
        val error = validateNewPassword("Password!", "Password!")
        assertEquals(
            "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo.",
            error
        )
    }

    @Test
    fun validateNewPassword_missingSymbol_returnsWeakError() {
        val error = validateNewPassword("Password1", "Password1")
        assertEquals(
            "Debe tener al menos 6 caracteres, una mayúscula, una minúscula, un número y un símbolo.",
            error
        )
    }

    @Test
    fun validateNewPassword_mismatch_returnsMismatchError() {
        val error = validateNewPassword("Password1!", "Password2!")
        assertEquals("Las contraseñas no coinciden.", error)
    }
}
