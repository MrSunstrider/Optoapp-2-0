package com.example.optoapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterAdoptionSweepTest {

    @Test
    fun `finance composables format amounts only through MoneyFormatter`() {
        val adHocMoney = Regex("""fun\s+fmt\(|\.fmt\(\)|\bfmt\(|s/\.|formatCurrency""")
        val offending = FinanceSources.files().flatMap { file ->
            file.readLines().filter { adHocMoney.containsMatchIn(it) }.map { "${file.name}: ${it.trim()}" }
        }
        assertEquals(emptyList<String>(), offending)
    }

    @Test
    fun `legacy formatCurrency is gone from main sources`() {
        val callers = MainSources.kotlinFilesUnder("java/com/example/optoapp")
            .filter { it.readText(Charsets.UTF_8).contains("formatCurrency") }
            .map { it.name }
        assertEquals(emptyList<String>(), callers)
    }

    @Test
    fun `string resources never embed the legacy currency prefix`() {
        val legacy = MainSources.read(MainSources.STRINGS_XML).lines().filter { it.contains("s/.") }
        assertEquals(emptyList<String>(), legacy)
    }
}
