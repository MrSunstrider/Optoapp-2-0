package com.example.optoapp.ui.components.config

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: RED phase — tests for ConfigAboutSection.
 *
 * The about section must exist as a public top-level composable function
 * with no required parameters, showing version and legal links.
 */
class ConfigAboutSectionTest {

    @Test
    fun configAboutSection_isPublicTopLevelFunction_exists() {
        val className = "com.example.optoapp.ui.components.config.ConfigAboutSectionKt"
        val methodName = "ConfigAboutSection"
        val clazz = Class.forName(className)
        val methods = clazz.declaredMethods.map { it.name }
        assertTrue(
            "ConfigAboutSection debe ser función top-level en ConfigAboutSectionKt. " +
                "Encontrados: $methods",
            methodName in methods,
        )
    }

    @Test
    fun configAboutSection_hasNoRequiredParameters() {
        val className = "com.example.optoapp.ui.components.config.ConfigAboutSectionKt"
        val clazz = Class.forName(className)
        val method = clazz.declaredMethods.first { it.name == "ConfigAboutSection" }
        val paramCount = method.parameterCount
        // Composable default parameters + modifier pattern
        assertTrue(
            "ConfigAboutSection debe tener pocos o ningún parámetro. Encontrados: $paramCount",
            paramCount <= 2,
        )
    }
}
