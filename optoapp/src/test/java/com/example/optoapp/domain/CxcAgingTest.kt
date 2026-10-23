package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CxcAgingTest {

    private fun deudor(dias: Int, saldo: Double) = Deudor(
        pacienteNombre = "A",
        pacienteTelefono = "999",
        ventaId = "v$dias",
        ventaFecha = java.time.LocalDate.of(2026, 1, 1),
        montoTotal = saldo,
        totalPagado = 0.0,
        saldo = saldo,
        diasDeuda = dias,
        pacienteId = "p",
    )

    @Test
    fun empty_deudores_returnsVacio() {
        val r = CxcAging.from(emptyList(), saldoPendiente = 100.0, ventasMes = 1000.0, mes = YearMonth.of(2026, 7))
        assertTrue(r.vacio)
        assertNull(r.dsoIlustrativo)
        assertNull(r.diasPromedioPonderado)
    }

    @Test
    fun buckets_splitByDias() {
        val r = CxcAging.from(
            listOf(
                deudor(10, 100.0),
                deudor(45, 200.0),
                deudor(70, 50.0),
                deudor(120, 300.0),
            ),
            saldoPendiente = 650.0,
            ventasMes = 3100.0,
            mes = YearMonth.of(2026, 7),
        )
        assertEquals(100.0, r.buckets[0].saldo, 0.001)
        assertEquals(200.0, r.buckets[1].saldo, 0.001)
        assertEquals(50.0, r.buckets[2].saldo, 0.001)
        assertEquals(300.0, r.buckets[3].saldo, 0.001)
        // DSO = 650 / (3100/31) = 650 * 31 / 3100 = 6.5
        assertEquals(6.5, r.dsoIlustrativo!!, 0.001)
        // weighted = (100*10+200*45+50*70+300*120)/650 = 49500/650
        assertEquals(49_500.0 / 650.0, r.diasPromedioPonderado!!, 0.001)
    }

    @Test
    fun zeroVentas_hidesDso() {
        val r = CxcAging.from(
            listOf(deudor(5, 100.0)),
            saldoPendiente = 100.0,
            ventasMes = 0.0,
            mes = YearMonth.of(2026, 7),
        )
        assertNull(r.dsoIlustrativo)
    }
}
