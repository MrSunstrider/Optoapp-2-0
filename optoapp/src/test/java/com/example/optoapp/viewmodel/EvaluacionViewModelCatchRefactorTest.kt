package com.example.optoapp.viewmodel

import org.junit.Assert.*
import org.junit.Test

/**
 * Approval tests for [parseSnellenToLogMar] behavior
 * BEFORE catch refactoring. These must pass before AND after changing
 * `catch (e: Exception)` to `catch (e: NumberFormatException)`.
 *
 * The function is a pure string→Double? parser extracted to
 * EvaluacionDiagnosticoHelper specifically to enable direct unit testing.
 */
class EvaluacionViewModelCatchRefactorTest {

    // ─── parseSnellenToLogMar (line 486, current: catch(e:Exception) → catch(e:NumberFormatException)) ───

    @Test
    fun `parseSnellenToLogMar valid input returns logMAR`() {
        // 20/20 = 1.0 decimal → -log10(1.0) = 0.0
        val result = parseSnellenToLogMar("20/20")
        assertNotNull(result)
        assertEquals(0.0, result!!, 0.001)
    }

    @Test
    fun `parseSnellenToLogMar 20 divided by 40 returns correct logMAR`() {
        // 20/40 = 0.5 → -log10(0.5) ≈ 0.301
        val result = parseSnellenToLogMar("20/40")
        assertNotNull(result)
        assertEquals(-Math.log10(0.5), result!!, 0.001)
    }

    @Test
    fun `parseSnellenToLogMar 20 divided by 200 returns logMAR 1`() {
        // 20/200 = 0.1 → -log10(0.1) = 1.0
        val result = parseSnellenToLogMar("20/200")
        assertNotNull(result)
        assertEquals(1.0, result!!, 0.001)
    }

    @Test
    fun `parseSnellenToLogMar empty string returns null`() {
        assertNull(parseSnellenToLogMar(""))
    }

    @Test
    fun `parseSnellenToLogMar blank string returns null`() {
        assertNull(parseSnellenToLogMar("   "))
    }

    @Test
    fun `parseSnellenToLogMar no slash returns null`() {
        assertNull(parseSnellenToLogMar("2020"))
    }

    @Test
    fun `parseSnellenToLogMar non numeric returns null`() {
        assertNull(parseSnellenToLogMar("20/abc"))
    }

    @Test
    fun `parseSnellenToLogMar zero denominator returns null`() {
        assertNull(parseSnellenToLogMar("20/0"))
    }

    @Test
    fun `parseSnellenToLogMar negative denominator returns null`() {
        assertNull(parseSnellenToLogMar("20/-5"))
    }

    @Test
    fun `parseSnellenToLogMar three parts returns null`() {
        assertNull(parseSnellenToLogMar("20/20/20"))
    }

    @Test
    fun `parseSnellenToLogMar 20 divided by 63 returns approx logMAR 0_5`() {
        // 20/63 ≈ 0.317 → -log10(0.317) ≈ 0.498
        val result = parseSnellenToLogMar("20/63")
        assertNotNull(result)
        assertEquals(-Math.log10(20.0 / 63.0), result!!, 0.01)
    }

    @Test
    fun `parseSnellenToLogMar 20 divided by 400 returns logMAR 1_3`() {
        // 20/400 = 0.05 → -log10(0.05) ≈ 1.301
        val result = parseSnellenToLogMar("20/400")
        assertNotNull(result)
        assertEquals(-Math.log10(0.05), result!!, 0.001)
    }
}
