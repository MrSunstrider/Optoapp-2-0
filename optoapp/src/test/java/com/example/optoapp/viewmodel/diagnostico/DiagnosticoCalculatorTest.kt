package com.example.optoapp.viewmodel.diagnostico

import org.junit.Assert.*
import org.junit.Test

/**
 * Characterization tests for DiagnosticoCalculator — pure functions extracted
 * from EvaluacionViewModel. Captures exact current behavior of parseRefraction,
 * calcularDiagnostico, and parseSnellenToLogMar.
 */
class DiagnosticoCalculatorTest {

    @Test
    fun `parseRefraction plano returns 0`() {
        assertEquals(0.0, DiagnosticoCalculator.parseRefraction("plano")!!, 0.001)
    }

    @Test
    fun `parseRefraction neutro returns 0`() {
        assertEquals(0.0, DiagnosticoCalculator.parseRefraction("neutro")!!, 0.001)
    }

    @Test
    fun `parseRefraction pl abbreviation returns 0`() {
        assertEquals(0.0, DiagnosticoCalculator.parseRefraction("pl")!!, 0.001)
    }

    @Test
    fun `parseRefraction nt abbreviation returns 0`() {
        assertEquals(0.0, DiagnosticoCalculator.parseRefraction("nt")!!, 0.001)
    }

    @Test
    fun `parseRefraction uppercase PLANO returns 0`() {
        assertEquals(0.0, DiagnosticoCalculator.parseRefraction("PLANO")!!, 0.001)
    }

    @Test
    fun `parseRefraction mixed case returns 0`() {
        assertEquals(0.0, DiagnosticoCalculator.parseRefraction("NeuTro")!!, 0.001)
    }

    @Test
    fun `parseRefraction positive integer`() {
        assertEquals(2.0, DiagnosticoCalculator.parseRefraction("2")!!, 0.001)
    }

    @Test
    fun `parseRefraction positive with plus sign`() {
        assertEquals(1.0, DiagnosticoCalculator.parseRefraction("+1.00")!!, 0.001)
    }

    @Test
    fun `parseRefraction negative value`() {
        assertEquals(-3.5, DiagnosticoCalculator.parseRefraction("-3.50")!!, 0.001)
    }

    @Test
    fun `parseRefraction decimal with comma`() {
        assertEquals(1.5, DiagnosticoCalculator.parseRefraction("1,50")!!, 0.001)
    }

    @Test
    fun `parseRefraction zero`() {
        assertEquals(0.0, DiagnosticoCalculator.parseRefraction("0")!!, 0.001)
    }

    @Test
    fun `parseRefraction empty string returns null`() {
        assertNull(DiagnosticoCalculator.parseRefraction(""))
    }

    @Test
    fun `parseRefraction whitespace returns null`() {
        assertNull(DiagnosticoCalculator.parseRefraction("   "))
    }

    @Test
    fun `parseRefraction invalid text returns null`() {
        assertNull(DiagnosticoCalculator.parseRefraction("abc"))
    }

    @Test
    fun `parseRefraction trimmed whitespace works`() {
        assertEquals(1.5, DiagnosticoCalculator.parseRefraction("  1.50  ")!!, 0.001)
    }

    @Test
    fun `calcularDiagnostico both null returns empty`() {
        assertEquals("", DiagnosticoCalculator.calcularDiagnostico("", ""))
    }

    @Test
    fun `calcularDiagnostico esf null cil plano returns Emetropia`() {
        // Both resolve to 0.0 → Emetropía
        assertEquals("Emetropía", DiagnosticoCalculator.calcularDiagnostico("", "pl"))
    }

    @Test
    fun `calcularDiagnostico plano and plano returns Emetropia`() {
        assertEquals("Emetropía", DiagnosticoCalculator.calcularDiagnostico("plano", "plano"))
    }

    @Test
    fun `calcularDiagnostico zero esf zero cil returns Emetropia`() {
        assertEquals("Emetropía", DiagnosticoCalculator.calcularDiagnostico("0", "0"))
    }

    @Test
    fun `calcularDiagnostico negative esf zero cil returns Miopia`() {
        assertEquals("Miopía", DiagnosticoCalculator.calcularDiagnostico("-2.00", "0"))
    }

    @Test
    fun `calcularDiagnostico positive esf zero cil returns Hipermetropia`() {
        assertEquals("Hipermetropía", DiagnosticoCalculator.calcularDiagnostico("+1.00", "0"))
    }

    @Test
    fun `calcularDiagnostico plano esf negative cil returns miopico simple`() {
        assertEquals(
            "Astigmatismo miópico simple",
            DiagnosticoCalculator.calcularDiagnostico("plano", "-0.75"),
        )
    }

    @Test
    fun `calcularDiagnostico negative esf with plano cil returns Miopia`() {
        // cil = 0 → c == 0 → Miopía (not astigmatism)
        assertEquals(
            "Miopía",
            DiagnosticoCalculator.calcularDiagnostico("-0.75", "plano"),
        )
    }

    @Test
    fun `calcularDiagnostico plano esf positive cil returns hipermetropico simple`() {
        assertEquals(
            "Astigmatismo hipermetrópico simple",
            DiagnosticoCalculator.calcularDiagnostico("plano", "+1.00"),
        )
    }

    @Test
    fun `calcularDiagnostico both negative returns miopico compuesto`() {
        assertEquals(
            "Astigmatismo miópico compuesto",
            DiagnosticoCalculator.calcularDiagnostico("-1.00", "-0.50"),
        )
    }

    @Test
    fun `calcularDiagnostico both positive via transpose returns hipermetropico compuesto`() {
        // +1.00 +1.00 → transpose: e = 2.0, c = -1.0 → meridians 2.0 and 1.0 (both +)
        assertEquals(
            "Astigmatismo hipermetrópico compuesto",
            DiagnosticoCalculator.calcularDiagnostico("+1.00", "+1.00"),
        )
    }

    @Test
    fun `calcularDiagnostico meridianos opuestos returns mixto`() {
        // esf = +1.00, cil = -2.00 → meridian1 = 1.0, meridian2 = -1.0
        assertEquals(
            "Astigmatismo mixto",
            DiagnosticoCalculator.calcularDiagnostico("+1.00", "-2.00"),
        )
    }

    @Test
    fun `calcularDiagnostico esf null cil negative returns miopico simple`() {
        // esf = null → 0, cil = -1.0 → meridian1 = 0, meridian2 = -1.0
        assertEquals(
            "Astigmatismo miópico simple",
            DiagnosticoCalculator.calcularDiagnostico("", "-1.00"),
        )
    }

    @Test
    fun `calcularDiagnostico transposes positive cylinder to negative`() {
        // esf = -2.00, cil = +1.00 → transpose: e = -1.0, c = -1.0
        // meridian1 = -1.0, meridian2 = -2.0 → both negative → compuesto
        assertEquals(
            "Astigmatismo miópico compuesto",
            DiagnosticoCalculator.calcularDiagnostico("-2.00", "+1.00"),
        )
    }

    @Test
    fun `parseSnellenToLogMar 20 over 20 returns 0`() {
        assertEquals(0.0, DiagnosticoCalculator.parseSnellenToLogMar("20/20")!!, 0.001)
    }

    @Test
    fun `parseSnellenToLogMar 20 over 40 returns approx 0_301`() {
        val expected = -Math.log10(20.0 / 40.0)
        assertEquals(expected, DiagnosticoCalculator.parseSnellenToLogMar("20/40")!!, 0.001)
    }

    @Test
    fun `parseSnellenToLogMar 20 over 200 returns 1`() {
        assertEquals(1.0, DiagnosticoCalculator.parseSnellenToLogMar("20/200")!!, 0.001)
    }

    @Test
    fun `parseSnellenToLogMar invalid format returns null`() {
        assertNull(DiagnosticoCalculator.parseSnellenToLogMar("abc"))
    }

    @Test
    fun `parseSnellenToLogMar no slash returns null`() {
        assertNull(DiagnosticoCalculator.parseSnellenToLogMar("2020"))
    }

    @Test
    fun `parseSnellenToLogMar denominator zero returns null`() {
        assertNull(DiagnosticoCalculator.parseSnellenToLogMar("20/0"))
    }

    @Test
    fun `parseSnellenToLogMar empty returns null`() {
        assertNull(DiagnosticoCalculator.parseSnellenToLogMar(""))
    }

    @Test
    fun `parseSnellenToLogMar 20 over 10 returns negative`() {
        val expected = -Math.log10(20.0 / 10.0)
        assertEquals(expected, DiagnosticoCalculator.parseSnellenToLogMar("20/10")!!, 0.001)
    }
}
