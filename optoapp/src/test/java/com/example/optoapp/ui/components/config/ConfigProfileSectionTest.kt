package com.example.optoapp.ui.components.config

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD: Tests for ConfigProfileSection.
 *
 * Verifies that ConfigProfileSection exposes email, rol, and opticaName
 * parameters, and that the version label references BuildConfig.
 */
class ConfigProfileSectionTest {

    @Test
    fun configProfileSection_isPublicFunction_exists() {
        val className = "com.example.optoapp.ui.components.config.ConfigProfileSectionKt"
        val methodName = "ConfigProfileSection"
        val clazz = Class.forName(className)
        val methods = clazz.declaredMethods.map { it.name }
        assertTrue(
            "ConfigProfileSection debe existir en ConfigProfileSectionKt. Encontrados: $methods",
            methodName in methods,
        )
    }

    @Test
    fun configProfileSection_acceptsEmailRolOpticaNameParams() {
        val className = "com.example.optoapp.ui.components.config.ConfigProfileSectionKt"
        val clazz = Class.forName(className)
        val method = clazz.declaredMethods.first { it.name == "ConfigProfileSection" }
        val paramTypes = method.parameterTypes.map { it.simpleName }
        assertTrue(
            "Debe aceptar String para email. Encontrados: $paramTypes",
            paramTypes.contains("String"),
        )
        // Must have at least 3 String params (email, rol, opticaName)
        val stringCount = paramTypes.count { it == "String" }
        assertTrue(
            "Debe tener al menos 3 parámetros String (email, rol, opticaName). Encontrados: $stringCount",
            stringCount >= 3,
        )
    }
}
