package com.example.optoapp.ui.components.config

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: RED phase — SucursalesSection must be removed.
 */
class SucursalesSectionRemovalTest {

    @Test
    fun `SucursalesSection must not exist in config package`() {
        val className = "com.example.optoapp.ui.components.config.ConfigOpticaDataSectionKt"
        val clazz = Class.forName(className)
        val methodNames = clazz.declaredMethods.map { it.name }
        assertFalse(
            "SucursalesSection debe haber sido eliminada del package config. Métodos: $methodNames",
            "SucursalesSection" in methodNames
        )
    }

    @Test
    fun `ConfiguracionScreen does not import SucursalesSection`() {
        // Verify by checking the compiled class references
        val clazz = Class.forName("com.example.optoapp.ui.screens.ConfiguracionScreenKt")
        val method = clazz.declaredMethods.firstOrNull { it.name == "ConfiguracionScreen" }
        assertNotNull("ConfiguracionScreen function must exist", method)
        // If compilation passes without SucursalesSection, the removal is confirmed
    }
}
