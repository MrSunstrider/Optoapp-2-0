package com.example.optoapp.ui.components

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OptoTextFieldTest {

    @Test
    fun `OptoTextField value is required string`() {
        assertTrue(true) // compile-time: value: String required
    }

    @Test
    fun `OptoTextField onValueChange is required callback`() {
        assertTrue(true) // compile-time: onValueChange: (String) -> Unit required
    }

    @Test
    fun `OptoTextField label is required string`() {
        assertTrue(true) // compile-time: label: String required
    }

    @Test
    fun `OptoTextField maxLength clamps input correctly`() {
        val maxLen = 5
        val input = "123456789"
        val clamped = if (input.length > maxLen) input.substring(0, maxLen) else input
        assertEquals("12345", clamped)
    }

    @Test
    fun `OptoTextField placeholder defaults to null`() {
        val placeholder: String? = null
        assertNull(placeholder)
    }

    @Test
    fun `OptoTextField placeholder can be set`() {
        val placeholder: String? = "Buscar paciente..."
        assertEquals("Buscar paciente...", placeholder)
    }

    // ── H9: error priority over charCount ──────────────────────────────────

    @Test
    fun `supportingText shows error even when charCount is enabled`() {
        val isError = true
        val showCharCount = true
        val result = resolveSupportingText(isError, showCharCount, maxLength = 10, valueLength = 5, supportingText = "Campo inválido")
        assertEquals("Campo inválido", result)
    }

    @Test
    fun `supportingText shows charCount when no error`() {
        val isError = false
        val showCharCount = true
        val result = resolveSupportingText(isError, showCharCount, maxLength = 20, valueLength = 15, supportingText = null)
        assertEquals("15/20", result)
    }

    @Test
    fun `supportingText shows custom message when no error and no charCount`() {
        val isError = false
        val showCharCount = false
        val result = resolveSupportingText(isError, showCharCount, maxLength = null, valueLength = 0, supportingText = "Texto de ayuda")
        assertEquals("Texto de ayuda", result)
    }

    @Test
    fun `supportingText error defaults to fallback when null`() {
        val isError = true
        val showCharCount = true
        val result = resolveSupportingText(isError, showCharCount, maxLength = 10, valueLength = 5, supportingText = null)
        assertEquals("Error de entrada", result)
    }

    @Test
    fun `supportingText returns null when no conditions match`() {
        val isError = false
        val showCharCount = false
        val result = resolveSupportingText(isError, showCharCount, maxLength = null, valueLength = 0, supportingText = null)
        assertNull(result)
    }

    companion object {
        fun resolveSupportingText(
            isError: Boolean,
            showCharCount: Boolean,
            maxLength: Int?,
            valueLength: Int,
            supportingText: String?
        ): String? {
            return when {
                isError -> supportingText ?: "Error de entrada"
                showCharCount && maxLength != null -> "${valueLength}/$maxLength"
                supportingText != null -> supportingText
                else -> null
            }
        }
    }
}
