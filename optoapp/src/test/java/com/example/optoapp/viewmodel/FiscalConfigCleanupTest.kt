package com.example.optoapp.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: RED phase — fiscal fields removal verification.
 *
 * After cleanup, FiscalConfigUi, FiscalDraft, and FiscalDraftUpdate
 * must NOT contain: moneda, pais, distritoCiudadDepartamento, contactoWhatsappTelefono.
 */
class FiscalConfigCleanupTest {

    @Test
    fun `FiscalConfigUi has no removed fields`() {
        val fields = FiscalConfigUi::class.java.declaredFields.map { it.name }
        val removed = listOf("moneda", "pais", "distritoCiudadDepartamento", "contactoWhatsappTelefono")
        val violations = removed.filter { it in fields }
        assertTrue(
            "FiscalConfigUi no debe contener: $violations. Campos actuales: $fields",
            violations.isEmpty()
        )
    }

    @Test
    fun `FiscalDraft has no removed fields`() {
        val fields = FiscalDraft::class.java.declaredFields.map { it.name }
        val removed = listOf("moneda", "pais", "distritoCiudadDepartamento", "contactoWhatsappTelefono")
        val violations = removed.filter { it in fields }
        assertTrue(
            "FiscalDraft no debe contener: $violations. Campos actuales: $fields",
            violations.isEmpty()
        )
    }

    @Test
    fun `FiscalDraftUpdate has no removed fields`() {
        val clazz = Class.forName("com.example.optoapp.ui.components.config.FiscalDraftUpdate")
        val fields = clazz.declaredFields.map { it.name }
        val removed = listOf("moneda", "pais", "distritoCiudadDepartamento", "contactoWhatsappTelefono")
        val violations = removed.filter { it in fields }
        assertTrue(
            "FiscalDraftUpdate no debe contener: $violations",
            violations.isEmpty()
        )
    }

    @Test
    fun `OpticaFiscalSettings has no removed fields`() {
        val clazz = Class.forName("com.example.optoapp.data.OpticaFiscalSettings")
        val fields = clazz.declaredFields.map { it.name }
        val removed = listOf("moneda", "pais", "distritoCiudadDepartamento", "contactoWhatsappTelefono")
        val violations = removed.filter { it in fields }
        assertTrue(
            "OpticaFiscalSettings no debe contener: $violations. Campos actuales: $fields",
            violations.isEmpty()
        )
    }
}
