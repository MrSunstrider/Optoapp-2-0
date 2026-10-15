package com.example.optoapp.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class PaymentSectionsHygieneTest {

    private val paymentSections = listOf(
        "java/com/example/optoapp/ui/screens/DispensacionFormSections.kt",
        "java/com/example/optoapp/ui/components/paciente/PacienteServiciosTab.kt",
        "java/com/example/optoapp/ui/components/dispensacion/PagosSection.kt",
        "java/com/example/optoapp/ui/components/paciente/PacienteDispensacionesTab.kt",
        "java/com/example/optoapp/ui/components/financiera/FinancieraPagosSection.kt",
    ).map(MainSources::file)

    private fun offendingLines(files: List<File>, matches: (String) -> Boolean): List<String> =
        files.flatMap { file ->
            file.readLines(Charsets.UTF_8).filter(matches).map { "${file.name}: ${it.trim()}" }
        }

    @Test
    fun `payment sections have no literal copy in text parameters`() {
        val literalCopy = Regex("""(Text\(\s*"|text\s*=\s*"|contentDescription\s*=\s*(if\b[^"]*)?"|label\s*=\s*"|title\s*=\s*"|errorMsg\s*=\s*")""")
        assertEquals(emptyList<String>(), offendingLines(paymentSections) { literalCopy.containsMatchIn(it) })
    }

    @Test
    fun `payment sections have no capitalized string literals outside domain keys`() {
        val capitalizedLiteral = Regex(""""[A-ZÁÉÍÓÚ¿¡][a-záéíóúñ ]""")
        val nonCodeLine = Regex("""^(import\s|//|\*|/\*|@\w+|message\s*=|replaceWith\s*=)""")
        // Estado and lens-type keys are persisted domain values compared in code, not displayed copy.
        val domainKey = Regex("""==\s*"[^"]*"|"[^"]*"\s*->""")
        val offending = offendingLines(paymentSections) { line ->
            val code = line.trim()
            !nonCodeLine.containsMatchIn(code) && capitalizedLiteral.containsMatchIn(code.replace(domainKey, ""))
        }
        assertEquals(emptyList<String>(), offending)
    }

    @Test
    fun `payment sections format money only through MoneyFormatter`() {
        val adHocMoney = Regex("""s/\.|String\.format\(|"%\.2f"|fun\s+formatMonto\(|fun\s+fmt\(""", RegexOption.IGNORE_CASE)
        assertEquals(emptyList<String>(), offendingLines(paymentSections) { adHocMoney.containsMatchIn(it) })
    }

    @Test
    fun `no ui source paints a saldo with the tertiary color`() {
        val uiFiles = MainSources.kotlinFilesUnder("java/com/example/optoapp/ui")
        val offending = offendingLines(uiFiles) { line ->
            line.contains("saldo", ignoreCase = true) && line.contains("colorScheme.tertiary")
        }
        assertEquals(emptyList<String>(), offending)
    }
}
