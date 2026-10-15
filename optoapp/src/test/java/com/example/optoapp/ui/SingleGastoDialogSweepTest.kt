package com.example.optoapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleGastoDialogSweepTest {

    private val sources = MainSources.kotlinFilesUnder("java/com/example/optoapp")

    @Test
    fun exactlyOneGastoFormDialogImplementationExists() {
        val implementaciones = sources.filter { Regex("""fun\s+GastoFormDialog\s*\(""").containsMatchIn(it.readText()) }

        assertEquals(listOf("GastoFormDialog.kt"), implementaciones.map { it.name })
    }

    @Test
    fun noLegacyGastoDialogRemains() {
        val legacy = sources.filter { it.readText().contains("GastoFormularioDialog") }

        assertTrue(legacy.map { it.name }.toString(), legacy.isEmpty())
    }
}
