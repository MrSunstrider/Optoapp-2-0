package com.example.optoapp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FinanceUiHygieneTest {

    private val costosScreen = MainSources.read("java/com/example/optoapp/ui/screens/CostosYGastosScreen.kt")

    private fun offendingLines(files: List<File>, matches: (String) -> Boolean): List<String> =
        files.flatMap { file ->
            file.readLines(Charsets.UTF_8).filter(matches).map { "${file.name}: ${it.trim()}" }
        }

    @Test
    fun `cost values are never painted with the positive green`() {
        val offending = offendingLines(FinanceSources.files()) { line ->
            line.contains("positiveGreen") && line.contains("costo", ignoreCase = true)
        }
        assertEquals(emptyList<String>(), offending)
    }

    @Test
    fun `costos top bar shows a described back arrow instead of the menu icon`() {
        assertTrue(costosScreen.contains("Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.costos_atras_cd)"))
        assertFalse(costosScreen.contains("Icons.Default.Menu"))
        assertNotNull(MainSources.stringValue("costos_atras_cd"))
    }

    @Test
    fun `long-press delete always has a visible delete button`() {
        val longPressOnly = FinanceSources.files().filter { file ->
            val text = file.readText(Charsets.UTF_8)
            Regex("""onLongClick\s*=\s*\{[^}]*[Dd]elete""").containsMatchIn(text) && !text.contains("Icons.Default.Delete")
        }.map { it.name }
        assertEquals(emptyList<String>(), longPressOnly)
    }

    @Test
    fun `finance composables have no hardcoded user-facing copy`() {
        val literalCopy = Regex("""(Text\(\s*"|text\s*=\s*"|contentDescription\s*=\s*(if\b[^"]*)?"|label\s*=\s*"|title\s*=\s*")""")
        val offending = offendingLines(FinanceSources.files()) { literalCopy.containsMatchIn(it) }
        assertEquals(emptyList<String>(), offending)
    }

    @Test
    fun `copy uses neutral Spanish without voseo or emoji`() {
        val voseo = Regex(
            """(?U)\b(vos|tenés|podés|querés|sabés|debés|necesitás|usás|hacé|poné|elegí|agregá|revisá|ingresá|seleccioná|confirmá|tocá|guardá|completá|intentá|probá|creá|editá|eliminá|cargá|registrá|asigná|escribí|definí|abrí|volvé)\b""",
            RegexOption.IGNORE_CASE,
        )
        val emoji = Regex("""[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]""")
        val sources = FinanceSources.files() + MainSources.file(MainSources.STRINGS_XML)
        val offending = offendingLines(sources) { voseo.containsMatchIn(it) || emoji.containsMatchIn(it) }
        assertEquals(emptyList<String>(), offending)
    }
}
