package com.example.optoapp.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class RemovedRoutesSweepTest {

    private val sources = MainSources.kotlinFilesUnder("java/com/example/optoapp")

    private val removedReferences = listOf(
        Regex("""\bRoute\.Gastos\b"""),
        Regex("""\bCostosYGastosDisp\b"""),
        Regex("""\bGastosScreen\b"""),
        Regex("""\bMainDrawerContent\b"""),
    )

    @Test
    fun noReferenceToRemovedRoutesOrScreensRemains() {
        val offenders = sources.flatMap { file ->
            val text = file.readText()
            removedReferences.filter { it.containsMatchIn(text) }.map { "${file.name}: ${it.pattern}" }
        }

        assertTrue(offenders.toString(), offenders.isEmpty())
    }

    @Test
    fun removedScreenFilesNoLongerExist() {
        val remaining = sources.filter { it.name == "GastosScreen.kt" || it.name == "MainDrawerContent.kt" }

        assertTrue(remaining.map { it.path }.toString(), remaining.isEmpty())
    }
}
