package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalisisDecisionLayoutTest {

    private fun analisis(
        ventas: Double = 40_000.0,
        costo: Double = 22_000.0,
        gastos: Double = 8_000.0,
        cobros: Double = 32_000.0,
        saldo: Double = 12_000.0,
        ticket: Double = 500.0,
        roPct: Double = 25.0,
        bruto: Double = 18_000.0,
        pe: Double? = 30_000.0,
        proyeccion: ProyeccionCaja? = ProyeccionCaja(10_000.0, 4_000.0, 6_000.0, 6),
    ) = AnalisisMensual(
        ventasMes = ventas,
        cobrosMes = cobros,
        margenNetoPct = roPct,
        margenPorCategoria = emptyList(),
        deudores = DeudoresResumen(3, saldo),
        proyeccionCaja = proyeccion,
        stockEstancado = emptyList(),
        valorInventario = 0.0,
        ventasMesAnterior = 0.0,
        variacionVentasPct = null,
        gastosMes = gastos,
        saldoPendiente = saldo,
        ticketPromedio = ticket,
        cantidadVentas = 80,
        costoMes = costo,
        marginFacts = MarginFacts(
            ventasMes = ventas,
            costoMes = costo,
            margenBruto = bruto,
            margenBrutoPct = 45.0,
            margenNetoPct = roPct,
            puntoEquilibrio = pe,
            cobertura = CoberturaCosto(lineasTotal = 10, lineasReales = 9, pctReal = 90.0),
        ),
    )

    @Test
    fun waterfall_usesRpcAmountsInOrder() {
        val wf = AnalisisDecisionLayout.from(analisis()).waterfall
        assertEquals(listOf("ventas", "cogs", "bruto", "opex", "resultado"), wf.map { it.key })
        assertEquals(40_000.0, wf[0].monto, 0.001)
        assertEquals(22_000.0, wf[1].monto, 0.001)
        assertEquals(18_000.0, wf[2].monto, 0.001)
        assertEquals(8_000.0, wf[3].monto, 0.001)
        assertEquals(10_000.0, wf[4].monto, 0.001)
    }

    @Test
    fun waterfall_resultadoIsVentasMinusCogsMinusOpex() {
        val wf = AnalisisDecisionLayout.from(analisis(ventas = 100.0, costo = 40.0, gastos = 25.0, bruto = 60.0)).waterfall
        assertEquals(35.0, wf.last().monto, 0.001)
    }

    @Test
    fun kpis_exposeBrutoRoTicketCxcAndPe() {
        val kpis = AnalisisDecisionLayout.from(analisis()).kpis
        assertEquals(18_000.0, kpis.utilidadBruta, 0.001)
        assertEquals(25.0, kpis.resultadoOperativoPct, 0.001)
        assertEquals(500.0, kpis.ticket, 0.001)
        assertEquals(12_000.0, kpis.cxc, 0.001)
        assertEquals(30_000.0, kpis.puntoEquilibrio)
        assertTrue(!kpis.resultadoIncompleto)
    }

    @Test
    fun caja_exposesCobrosSaldoAndProyeccion() {
        val caja = AnalisisDecisionLayout.from(analisis()).caja
        assertEquals(32_000.0, caja.cobrado, 0.001)
        assertEquals(12_000.0, caja.saldoPendiente, 0.001)
        assertEquals(6_000.0, caja.proyeccionSaldoNeto)
    }

    @Test
    fun caja_nullProyeccionWhenAbsent() {
        val caja = AnalisisDecisionLayout.from(analisis(proyeccion = null)).caja
        assertNull(caja.proyeccionSaldoNeto)
    }
}
