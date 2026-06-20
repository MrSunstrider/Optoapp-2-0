package com.example.optoapp.ui.components.config

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: Tests for SectionHeader with optional icon parameter.
 *
 * Verifies that SectionHeader accepts an optional ImageVector icon
 * and that the function signature is correct after the enhancement.
 */
class ConfigSectionHeaderTest {

    @Test
    fun sectionHeader_isPublicTopLevelFunction_exists() {
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
    fun sectionHeader_acceptsStringParameter() {
        val className = "com.example.optoapp.ui.components.config.ConfigSectionHeaderKt"
        val clazz = Class.forName(className)
        val method = clazz.declaredMethods.first { it.name == "SectionHeader" }
        val paramTypes = method.parameterTypes.map { it.simpleName }
        assertTrue(
            "Debe aceptar String como parámetro. Encontrados: $paramTypes",
            paramTypes.contains("String")
        )
    }

    @Test
    fun sectionHeader_acceptsOptionalImageVectorParameter() {
        val className = "com.example.optoapp.ui.components.config.ConfigSectionHeaderKt"
        val clazz = Class.forName(className)
        val method = clazz.declaredMethods.first { it.name == "SectionHeader" }
        val paramTypes = method.parameterTypes.map { it.simpleName }
        assertTrue(
            "Debe aceptar ImageVector como parámetro opcional. Encontrados: $paramTypes",
            paramTypes.contains("ImageVector")
        )
    }
}
