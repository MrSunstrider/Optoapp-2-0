package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PeriodComparisonTest {

    private fun mov(monto: Double, costo: Double, incompleto: Boolean = false) = MovimientoFinanciero(
        id = monto.toString(),
        fecha = LocalDate.of(2026, 10, 1),
        tipo = TipoMovimiento.VENTA,
        origen = Origen.DISPENSACION,
        origenId = "x",
        montoTotal = monto,
        montoPagado = monto,
        costo = costo,
        pacienteId = "p1",
        opticaId = "o1",
        descripcion = "",
        vinculadoA = null,
        costoIncompleto = incompleto,
    )

    @Test
    fun fromMovimientos_sumsVendidoAndExcludesIncompleteMargen() {
        val current = listOf(mov(100.0, 40.0), mov(50.0, 0.0, incompleto = true))
        val previous = listOf(mov(80.0, 30.0))
        val cmp = PeriodComparison.fromMovimientos(current, previous)
        assertEquals(150.0, cmp.currentVendido, 0.0)
        assertEquals(80.0, cmp.previousVendido, 0.0)
        assertEquals(60.0, cmp.currentMargen, 0.0)
        assertEquals(50.0, cmp.previousMargen, 0.0)
        assertEquals(2, cmp.currentTransacciones)
        assertEquals(1, cmp.previousTransacciones)
    }

    @Test
    fun pct_handlesZeroPrevious() {
        assertNull(PeriodComparison.pct(10.0, 0.0))
        assertEquals(0.0, PeriodComparison.pct(0.0, 0.0)!!, 0.0)
        assertEquals(25.0, PeriodComparison.pct(125.0, 100.0)!!, 0.0)
    }
}
