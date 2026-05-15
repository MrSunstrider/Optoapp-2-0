package com.example.optoapp.util

import com.example.optoapp.data.EvaluacionClinica
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for [RefraccionTableBuilder.prepareData] — pure logic, no Canvas/Paint.
 */
class RecetaRefraccionTableTest {

    private fun eval(
        recetaOdEsf: String = "", recetaOdCil: String = "", recetaOdEje: String = "",
        recetaOiEsf: String = "", recetaOiCil: String = "", recetaOiEje: String = "",
        avCcOdLejos: String = "", avCcOiLejos: String = "", avCcAoPx: String = "",
        dipLejos: String = "", dipCerca: String = "",
        addCercaOd: String = "", addCercaOi: String = "",
        addIntermediaOd: String = "", addIntermediaOi: String = "",
        avCcAoCerca: String = "",
        prismaOdValor: String = "", prismaOdBase: String = "",
        prismaOiValor: String = "", prismaOiBase: String = ""
    ): EvaluacionClinica {
        val today = LocalDate.now()
        return EvaluacionClinica(
            id = "test", pacienteId = "p1", fecha = today, motivoConsulta = "test",
            recetaOdEsf = recetaOdEsf, recetaOdCil = recetaOdCil, recetaOdEje = recetaOdEje,
            recetaOiEsf = recetaOiEsf, recetaOiCil = recetaOiCil, recetaOiEje = recetaOiEje,
            avCcOdLejos = avCcOdLejos, avCcOiLejos = avCcOiLejos, avCcAoPx = avCcAoPx,
            dipLejos = dipLejos, dipCerca = dipCerca,
            addCercaOd = addCercaOd, addCercaOi = addCercaOi,
            addIntermediaOd = addIntermediaOd, addIntermediaOi = addIntermediaOi,
            avCcAoCerca = avCcAoCerca,
            prismaOdValor = prismaOdValor, prismaOdBase = prismaOdBase,
            prismaOiValor = prismaOiValor, prismaOiBase = prismaOiBase
        )
    }

    @Test
    fun `prepareData hasRx false when no refraction data`() {
        val data = RefraccionTableBuilder.prepareData(eval())
        assertEquals(false, data.hasRx)
    }

    @Test
    fun `prepareData hasRx true with refraction data`() {
        val data = RefraccionTableBuilder.prepareData(eval(recetaOdEsf = "-1.00"))
        assertEquals(true, data.hasRx)
    }

    @Test
    fun `distRows contains OD and OI with correct values`() {
        val data = RefraccionTableBuilder.prepareData(eval(
            recetaOdEsf = "-1.00", recetaOdCil = "-0.50", recetaOdEje = "90",
            recetaOiEsf = "-2.00", recetaOiCil = "-0.75", recetaOiEje = "85",
            avCcOdLejos = "0.8", avCcOiLejos = "0.6", avCcAoPx = "0.7",
            dipLejos = "32", dipCerca = "30"
        ))
        assertEquals(2, data.distRows.size)
        assertEquals("OD", data.distRows[0].label)
        assertEquals("OI", data.distRows[1].label)

        // OD: esf, cil, eje, dip, av, avao
        with(data.distRows[0]) {
            assertEquals("-1.00", cells[0].text)
            assertEquals("-0.50", cells[1].text)
            assertEquals("90", cells[2].text)
            assertEquals("32", cells[3].text)
            assertEquals("0.8", cells[4].text)
            assertEquals("0.7", cells[5].text)
        }
        // OI
        with(data.distRows[1]) {
            assertEquals("-2.00", cells[0].text)
            assertEquals("-0.75", cells[1].text)
            assertEquals("85", cells[2].text)
            assertEquals("30", cells[3].text)
            assertEquals("0.6", cells[4].text)
            assertEquals("0.7", cells[5].text)
        }
    }

    @Test
    fun `nearRows contains near vision data`() {
        val data = RefraccionTableBuilder.prepareData(eval(
            addCercaOd = "+1.50", addCercaOi = "+1.50",
            addIntermediaOd = "+1.25", addIntermediaOi = "+1.25",
            dipCerca = "30",
            avCcAoCerca = "0.8"
        ))
        assertEquals(2, data.nearRows.size)
        with(data.nearRows[0]) {
            assertEquals("OD", label)
            assertEquals("+1.50", cells[0].text)
            assertEquals("+1.25", cells[1].text)
            assertEquals("30", cells[2].text)
            assertEquals("0.8", cells[3].text)
        }
        with(data.nearRows[1]) {
            assertEquals("OI", label)
            assertEquals("+1.50", cells[0].text)
            assertEquals("+1.25", cells[1].text)
            assertEquals("30", cells[2].text)
            assertEquals("0.8", cells[3].text)
        }
    }

    @Test
    fun `prismaRows contains prism data`() {
        val data = RefraccionTableBuilder.prepareData(eval(
            prismaOdValor = "2", prismaOdBase = "Nasal",
            prismaOiValor = "1", prismaOiBase = "Temporal"
        ))
        assertEquals(2, data.prismaRows.size)
        with(data.prismaRows[0]) {
            assertEquals("OD", label)
            assertEquals("2", cells[0].text)
            assertEquals("Nasal", cells[1].text)
        }
        with(data.prismaRows[1]) {
            assertEquals("OI", label)
            assertEquals("1", cells[0].text)
            assertEquals("Temporal", cells[1].text)
        }
    }

    @Test
    fun `blank fields show dash`() {
        val data = RefraccionTableBuilder.prepareData(eval(recetaOdEsf = "-1.00"))
        // OD has data, OI is blank
        assertEquals("-1.00", data.distRows[0].cells[0].text)
        assertEquals("—", data.distRows[1].cells[0].text)
        assertEquals("—", data.distRows[1].cells[1].text)
    }
}
