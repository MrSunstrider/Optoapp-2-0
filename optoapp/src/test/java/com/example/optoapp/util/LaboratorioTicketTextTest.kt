package com.example.optoapp.util

import com.example.optoapp.data.EvaluacionClinica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LaboratorioTicketTextTest {

    private fun minimalEval(
        odEsf: String = "-2.00",
        odCil: String = "-0.50",
        odEje: String = "180",
        oiEsf: String = "-1.50",
        oiCil: String = "-0.25",
        oiEje: String = "170",
    ) = EvaluacionClinica(
        id = "eval1",
        pacienteId = "p1",
        fecha = LocalDate.of(2025, 1, 15),
        recetaOdEsf = odEsf,
        recetaOdCil = odCil,
        recetaOdEje = odEje,
        recetaOiEsf = oiEsf,
        recetaOiCil = oiCil,
        recetaOiEje = oiEje,
    )

    // ── bloqueFormulaLaboratorio ────────────────────────────────────────

    @Test
    fun `bloqueFormulaLaboratorio null eval returns empty`() {
        assertEquals("", LaboratorioTicketText.bloqueFormulaLaboratorio(null))
    }

    @Test
    fun `bloqueFormulaLaboratorio with basic refraction`() {
        val ev = minimalEval()
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("OD: -2.00 / -0.50 × 180°"))
        assertTrue(result.contains("OI: -1.50 / -0.25 × 170°"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with blank values shows dash`() {
        val ev = minimalEval(odEsf = "", odCil = "", odEje = "")
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("OD: — / — × —°"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with DNP both eyes`() {
        val ev = minimalEval().copy(dnpOdMm = 32.0, dnpOiMm = 33.0)
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("DNP 32.0/33.0 mm"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with DIP values`() {
        val ev = minimalEval().copy(dipLejos = "32", dipCerca = "30")
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("32 (lejos)"))
        assertTrue(result.contains("30 (cerca)"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with prism OD only`() {
        val ev = minimalEval().copy(
            prismaOdValor = "2Δ", prismaOdBase = "nasal",
            prismaOiValor = "", prismaOiBase = ""
        )
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("OD 2Δ (base nasal)"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with add and altura`() {
        val ev = minimalEval().copy(addCercaOd = "+2.00", addCercaOi = "+2.00")
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev, alturaMontajeMm = "22")
        assertTrue(result.contains("Add: cerca OD +2.00 · cerca OI +2.00"))
        assertTrue(result.contains("h: 22 mm"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with altura only no add`() {
        val ev = minimalEval()
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev, alturaMontajeMm = "22")
        assertTrue(result.contains("Add: —"))
        assertTrue(result.contains("h: 22 mm"))
    }

    // ── lineaRefraccionFinalYDipNdP ─────────────────────────────────────

    @Test
    fun `lineaRefraccionFinalYDipNdP null eval returns empty`() {
        assertEquals("", LaboratorioTicketText.lineaRefraccionFinalYDipNdP(null))
    }

    @Test
    fun `lineaRefraccionFinalYDipNdP with basic refraction`() {
        val ev = minimalEval()
        val result = LaboratorioTicketText.lineaRefraccionFinalYDipNdP(ev)
        assertTrue(result.contains("OD -2.00 / -0.50 x 180°"))
        assertTrue(result.contains("OI -1.50 / -0.25 x 170°"))
    }

    @Test
    fun `lineaRefraccionFinalYDipNdP with add and DNP`() {
        val ev = minimalEval().copy(
            addCercaOd = "+2.00",
            dnpOdMm = 32.0,
            dnpOiMm = 33.0
        )
        val result = LaboratorioTicketText.lineaRefraccionFinalYDipNdP(ev)
        assertTrue(result.contains("cerca OD +2.00"))
        assertTrue(result.contains("DNP 32.0/33.0 mm"))
    }

    @Test
    fun `lineaRefraccionFinalYDipNdP with DIP only`() {
        val ev = minimalEval().copy(dipLejos = "32.5")
        val result = LaboratorioTicketText.lineaRefraccionFinalYDipNdP(ev)
        assertTrue(result.contains("DIP lejos 32.5"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with all blank refraction parameters`() {
        val ev = EvaluacionClinica(
            id = "eval2",
            pacienteId = "p2",
            fecha = LocalDate.of(2025, 1, 15),
        )
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("OD: — / — × —°"))
        assertTrue(result.contains("OI: — / — × —°"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with prism both eyes`() {
        val ev = minimalEval().copy(
            prismaOdValor = "3Δ", prismaOdBase = "temporal",
            prismaOiValor = "2Δ", prismaOiBase = "nasal"
        )
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("OD 3Δ (base temporal)"))
        assertTrue(result.contains("OI 2Δ (base nasal)"))
        assertTrue(result.contains("Prisma:"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with DNP OD only`() {
        val ev = minimalEval().copy(dnpOdMm = 32.0)
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("DNP OD 32.0 mm"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with DIP intermedio`() {
        val ev = minimalEval().copy(dipIntermedio = "31")
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("31 (interm.)"))
    }

    @Test
    fun `bloqueFormulaLaboratorio with DIP total only`() {
        val ev = minimalEval().copy(dipTotalMm = 64.0)
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("64.0 mm (DIP total)"))
    }

    @Test
    fun `lineaRefraccionFinalYDipNdP with empty eval all blank`() {
        val ev = EvaluacionClinica(
            id = "eval3",
            pacienteId = "p3",
            fecha = LocalDate.of(2025, 2, 1),
        )
        assertEquals("", LaboratorioTicketText.lineaRefraccionFinalYDipNdP(ev))
    }

    @Test
    fun `bloqueFormulaLaboratorio with DNP OI only and DIP`() {
        val ev = minimalEval().copy(dnpOiMm = 33.0, dipCerca = "30")
        val result = LaboratorioTicketText.bloqueFormulaLaboratorio(ev)
        assertTrue(result.contains("DNP OI 33.0 mm"))
        assertTrue(result.contains("30 (cerca)"))
    }
}
