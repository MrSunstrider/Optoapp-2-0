package com.example.optoapp.ui.components.config

import org.junit.Assert.*
import org.junit.Test

/**
 * Approval test for SectionHeader extraction.
 *
 * Verifies that SectionHeader is available as a public top-level function
 * in the config components package after extraction from ConfiguracionScreen.
 */
class ConfigSectionHeaderTest {

    @Test
    fun sectionHeader_isPublicTopLevelFunction_exists() {
        // After extraction: SectionHeader should be a top-level function in
        // com.example.optoapp.ui.components.config.ConfigSectionHeaderKt
        val className = "com.example.optoapp.ui.components.config.ConfigSectionHeaderKt"
        val methodName = "SectionHeader"
        val clazz = Class.forName(className)
        val methods = clazz.declaredMethods.map { it.name }
        assertTrue(
            "SectionHeader debe ser función top-level en ConfigSectionHeaderKt. " +
            "Encontrados: $methods",
            methodName in methods
        )
    }

    @Test
    fun sectionHeader_hasCorrectParameterTypes() {
        val className = "com.example.optoapp.ui.components.config.ConfigSectionHeaderKt"
        val clazz = Class.forName(className)
        val method = clazz.declaredMethods.first { it.name == "SectionHeader" }
        val paramTypes = method.parameterTypes.map { it.simpleName }
        assertTrue("Debe aceptar String como primer parámetro", paramTypes.contains("String"))
    }
}
