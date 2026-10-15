package com.example.optoapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureVicoIsolationTest {

    private val vicoImport = Regex("""^\s*import\s+com\.patrykandpatrick\.vico""", RegexOption.MULTILINE)

    @Test
    fun screensAndViewModels_doNotImportVico() {
        val offenders = listOf(SCREENS_DIR, VIEWMODELS_DIR)
            .flatMap { MainSources.kotlinFilesUnder(it) }
            .filter { vicoImport.containsMatchIn(it.readText(Charsets.UTF_8)) }
            .map { it.name }

        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun chartWrappers_areTheOnlyVicoConsumers() {
        val wrappers = MainSources.kotlinFilesUnder(CHARTS_DIR)
            .filter { vicoImport.containsMatchIn(it.readText(Charsets.UTF_8)) }
            .map { it.name }

        assertTrue("Expected the chart wrappers to use Vico, found none", wrappers.isNotEmpty())
    }

    private companion object {
        const val SCREENS_DIR = "java/com/example/optoapp/ui/screens"
        const val VIEWMODELS_DIR = "java/com/example/optoapp/viewmodel"
        const val CHARTS_DIR = "java/com/example/optoapp/ui/components/charts"
    }
}
