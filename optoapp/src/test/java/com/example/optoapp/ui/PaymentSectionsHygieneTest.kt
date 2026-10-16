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
    fun `state remembered per payment row is keyed by the payment id`() {
        val loopStart = Regex("""\.pagos\.forEach\s*\{""")
        val unkeyed = paymentSections.flatMap { file ->
            val text = file.readText(Charsets.UTF_8)
            loopStart.findAll(text).mapNotNull { match ->
                val body = blockFrom(text, match.range.last)
                file.name.takeIf { body.contains("remember") && !body.contains("key(pago.id)") }
            }.toList()
        }
        assertEquals(emptyList<String>(), unkeyed)
    }

    private fun blockFrom(text: String, openBrace: Int): String {
        var depth = 0
        for (i in openBrace until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return text.substring(openBrace, i + 1)
            }
        }
        return text.substring(openBrace)
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
