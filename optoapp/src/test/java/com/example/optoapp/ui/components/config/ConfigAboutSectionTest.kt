package com.example.optoapp.ui.components.config

import org.junit.Assert.assertNull
import org.junit.Test

/** ConfigAboutSection was dead UI — removed in config Oleada 1. */
class ConfigAboutSectionTest {
    @Test
    fun `ConfigAboutSection class must not exist`() {
        val clazz = runCatching {
            Class.forName("com.example.optoapp.ui.components.config.ConfigAboutSectionKt")
        }.getOrNull()
        assertNull("ConfigAboutSectionKt must be deleted", clazz)
    }
}
