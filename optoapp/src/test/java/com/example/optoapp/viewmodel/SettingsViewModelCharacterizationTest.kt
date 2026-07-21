package com.example.optoapp.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Characterization tests for SettingsViewModel.
 *
 * Approval tests that capture existing and new contract behavior.
 * Full ViewModel instantiation requires Hilt + Android Context (no mocking library),
 * so we test contracts via Java reflection on declared fields/methods.
 */
class SettingsViewModelCharacterizationTest {

    @Test
    fun settingsViewModel_classExists() {
        val clazz: Class<*> = SettingsViewModel::class.java
        assertNotNull("SettingsViewModel debe existir", clazz)
        assertEquals("SettingsViewModel", clazz.simpleName)
    }

    // ─── EXISTING members (approval tests) ─────────────────────────────

    @Test
    fun remindersEnabled_isDeclaredMember() {
        val methods = SettingsViewModel::class.java.declaredMethods.map { it.name }
        val fields = SettingsViewModel::class.java.declaredFields.map { it.name }
        assertTrue(
            "remindersEnabled debe ser miembro (field o method)",
            "remindersEnabled" in methods || "remindersEnabled" in fields,
        )
    }

    @Test
    fun userTimeZone_isDeclaredMember() {
        val methods = SettingsViewModel::class.java.declaredMethods.map { it.name }
        val fields = SettingsViewModel::class.java.declaredFields.map { it.name }
        assertTrue(
            "userTimeZone debe ser miembro",
            "userTimeZone" in methods || "userTimeZone" in fields,
        )
    }

    @Test
    fun setRemindersEnabled_isDeclaredMember() {
        val names = SettingsViewModel::class.java.declaredMethods.map { it.name }
        assertTrue("setRemindersEnabled debe ser miembro", "setRemindersEnabled" in names)
    }

    @Test
    fun setUserTimeZone_isDeclaredMember() {
        val names = SettingsViewModel::class.java.declaredMethods.map { it.name }
        assertTrue("setUserTimeZone debe ser miembro", "setUserTimeZone" in names)
    }

    @Test
    fun sendTestNotification_isDeclaredMember() {
        val names = SettingsViewModel::class.java.declaredMethods.map { it.name }
        assertTrue("sendTestNotification debe ser miembro", "sendTestNotification" in names)
    }

    // ─── NEW PIN method signatures (RED tests - will fail first) ───────

    @Test
    fun pinHasBeenSet_isDeclaredMember() {
        val methods = SettingsViewModel::class.java.declaredMethods.map { it.name }
        val fields = SettingsViewModel::class.java.declaredFields.map { it.name }
        assertTrue(
            "pinHasBeenSet debe ser miembro de SettingsViewModel",
            "pinHasBeenSet" in methods || "pinHasBeenSet" in fields,
        )
    }

    @Test
    fun isPinRequired_isDeclaredMember() {
        val methods = SettingsViewModel::class.java.declaredMethods.map { it.name }
        val fields = SettingsViewModel::class.java.declaredFields.map { it.name }
        assertTrue(
            "isPinRequired debe ser miembro de SettingsViewModel",
            "isPinRequired" in methods || "isPinRequired" in fields,
        )
    }

    @Test
    fun togglePinRequired_isDeclaredMember() {
        val names = SettingsViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "togglePinRequired debe ser miembro de SettingsViewModel",
            "togglePinRequired" in names,
        )
    }

    @Test
    fun updatePin_isDeclaredMember() {
        val names = SettingsViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "updatePin debe ser miembro de SettingsViewModel",
            "updatePin" in names,
        )
    }

    // ─── Method parameter contract tests via Java reflection ────────────

    @Test
    fun setRemindersEnabled_acceptsBooleanParameter() {
        val method = SettingsViewModel::class.java.declaredMethods
            .firstOrNull { it.name == "setRemindersEnabled" }
        assertNotNull("setRemindersEnabled debe existir", method)
        assertEquals("setRemindersEnabled debe tener 1 parámetro", 1, method?.parameterCount)
        assertEquals(
            "setRemindersEnabled debe aceptar Boolean/boolean",
            "boolean",
            method?.parameterTypes?.first()?.name,
        )
    }

    @Test
    fun togglePinRequired_acceptsBooleanParameter() {
        val method = SettingsViewModel::class.java.declaredMethods
            .firstOrNull { it.name == "togglePinRequired" }
        assertNotNull("togglePinRequired debe existir", method)
        assertEquals("togglePinRequired debe tener 1 parámetro", 1, method?.parameterCount)
        assertEquals(
            "togglePinRequired debe aceptar Boolean/boolean",
            "boolean",
            method?.parameterTypes?.first()?.name,
        )
    }

    @Test
    fun updatePin_acceptsTwoStrings() {
        val method = SettingsViewModel::class.java.declaredMethods
            .firstOrNull { it.name == "updatePin" }
        assertNotNull("updatePin debe existir", method)
        assertEquals("updatePin debe tener 2 parámetros", 2, method?.parameterCount)
        method?.parameterTypes?.forEach { paramType ->
            assertEquals("updatePin parámetros deben ser String", "java.lang.String", paramType.name)
        }
    }
}
