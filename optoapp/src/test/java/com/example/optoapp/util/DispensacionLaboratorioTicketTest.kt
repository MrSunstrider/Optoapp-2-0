package com.example.optoapp.util

import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.EvaluacionClinica
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DispensacionLaboratorioTicketTest {

    private fun testCtx(
        ot: String = "OT-001",
        tipoLente: String = "Progresivo",
        altura: String = "22",
        pacienteNombre: String = "Juan Pérez",
    ) = LaboratorioTicketContext(
        ot = ot,
        fecha = LocalDate.of(2025, 3, 15),
        pacienteNombre = pacienteNombre,
        tipoLente = tipoLente,
        subTipoBifocal = "",
        distanciaLente = "",
        altura = altura,
        materialLente = "Policarbonato",
        tratamientos = listOf("Antireflejo", "Blue Block"),
        colorLente = "",
        notasDiseno = "",
        origenMontura = "Propia",
        tipoAro = "Completo",
        materialMontura = "Acetato",
        descripcionMontura = "Ray-Ban Aviator",
        tipoMontura = "Marcada",
    )

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

    // ── LaboratorioTicketContext.fromDispensacion ───────────────────────

    @Test
    fun `fromDispensacion maps all fields`() {
        val disp = DispensacionOptica(
            id = "d1",
            pacienteId = "p1",
            fecha = LocalDate.of(2025, 3, 15),
            ot = "OT-001",
            tipoLente = "Progresivo",
            altura = "22",
            materialLente = "Policarbonato",
            tratamientos = listOf("Antireflejo"),
            tipoMontura = "Marcada",
        )
        val ctx = LaboratorioTicketContext.fromDispensacion(disp, "Juan")
        assertEquals("OT-001", ctx.ot)
        assertEquals("Juan", ctx.pacienteNombre)
        assertEquals("Progresivo", ctx.tipoLente)
        assertEquals("22", ctx.altura)
        assertEquals("Policarbonato", ctx.materialLente)
    }

    // ── textoCompleto ───────────────────────────────────────────────────

    @Test
    fun `textoCompleto null eval generates ticket without formula`() {
        val ctx = testCtx()
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("OT: OT-001"))
        assertTrue(result.contains("Paciente: Juan Pérez"))
        assertTrue(result.contains("Lente: Progresivo"))
    }

    @Test
    fun `textoCompleto with eval includes formula`() {
        val ctx = testCtx()
        val ev = minimalEval()
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, ev)
        assertTrue(result.contains("OD: -2.00 / -0.50  × 180°"))
        assertTrue(result.contains("OI: -1.50 / -0.25  × 170°"))
    }

    @Test
    fun `textoCompleto includes montura section`() {
        val ctx = testCtx()
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("Montura"))
        assertTrue(result.contains("Origen: Propia"))
        assertTrue(result.contains("Tipo: Marcada"))
        assertTrue(result.contains("Aro: Completo"))
        assertTrue(result.contains("Mat: Acetato"))
        assertTrue(result.contains("Desc: Ray-Ban Aviator"))
    }

    @Test
    fun `textoCompleto includes tratamientos`() {
        val ctx = testCtx().copy(tratamientos = listOf("Antireflejo", "Blue Block"))
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("Tratamientos: Antireflejo, Blue Block"))
    }

    @Test
    fun `textoCompleto filters out Ninguno from tratamientos`() {
        val ctx = testCtx().copy(tratamientos = listOf("Ninguno", "Antireflejo"))
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("Antireflejo"))
        assertTrue(!result.contains("Ninguno"))
    }

    @Test
    fun `textoCompleto with DIP values`() {
        val ctx = testCtx()
        val ev = minimalEval().copy(dipLejos = "32", dipCerca = "30")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, ev)
        assertTrue(result.contains("32 (lejos) / 30 (cerca)"))
    }

    @Test
    fun `textoCompleto with add cerca OD and OI`() {
        val ctx = testCtx()
        val ev = minimalEval().copy(addCercaOd = "+2.00", addCercaOi = "+2.00")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, ev)
        assertTrue(result.contains("Add: OD +2.00 · OI +2.00"))
    }

    @Test
    fun `textoCompleto with AV CC values`() {
        val ctx = testCtx()
        val ev = minimalEval().copy(avCcOdLejos = "20/20", avCcOiLejos = "20/25")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, ev)
        assertTrue(result.contains("AVCC OD 20/20"))
        assertTrue(result.contains("AVCC OI 20/25"))
    }

    @Test
    fun `textoCompleto bifocal includes subTipo`() {
        val ctx = testCtx(tipoLente = "Bifocal").copy(subTipoBifocal = "Kriptok")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("Bifocal (Kriptok)"))
    }

    @Test
    fun `textoCompleto monofocal includes distancia`() {
        val ctx = testCtx(tipoLente = "Monofocal").copy(distanciaLente = "Lejos")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("Monofocal - Lejos"))
    }

    @Test
    fun `textoCompleto blank OT shows dash`() {
        val ctx = testCtx(ot = "")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("OT: —"))
    }

    @Test
    fun `textoCompleto blank paciente shows dash`() {
        val ctx = testCtx(pacienteNombre = "")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("Paciente: —"))
    }

    @Test
    fun `textoCompleto without altura when lente does not require it`() {
        val ctx = testCtx(tipoLente = "Monofocal", altura = "")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(!result.contains("h:"))
    }

    @Test
    fun `textoCompleto with altura for progresivo`() {
        val ctx = testCtx(tipoLente = "Progresivo", altura = "22")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("h: 22 mm"))
    }

    @Test
    fun `textoCompleto with altura for ocupacional`() {
        val ctx = testCtx(tipoLente = "Ocupacional", altura = "18")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("h: 18 mm"))
    }

    @Test
    fun `textoCompleto with color and notas`() {
        val ctx = testCtx().copy(colorLente = "Gris", notasDiseno = "Bisel pulido")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, null)
        assertTrue(result.contains("Color: Gris"))
        assertTrue(result.contains("Notas: Bisel pulido"))
    }

    @Test
    fun `textoCompleto with eval `() {
        val ctx = testCtx()
        val ev = minimalEval(odEsf = "", odCil = "", odEje = "")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, ev)
        assertTrue(result.contains("OD: — / —  × —°"))
    }

    @Test
    fun `textoCompacto has same output as textoCompleto`() {
        val ctx = testCtx()
        val ev = minimalEval()
        val completo = DispensacionLaboratorioTicket.textoCompleto(ctx, ev)
        val compacto = DispensacionLaboratorioTicket.textoCompacto(ctx, ev)
        assertEquals(completo, compacto)
    }

    @Test
    fun `textoCompleto with add only OD`() {
        val ctx = testCtx()
        val ev = minimalEval().copy(addCercaOd = "+2.00", addCercaOi = "")
        val result = DispensacionLaboratorioTicket.textoCompleto(ctx, ev)
        assertTrue(result.contains("Add: OD +2.00"))
        // Should NOT contain "OI" in the Add line specifically
        assertTrue(!result.contains("Add: OD +2.00 · OI"))
    }
}
