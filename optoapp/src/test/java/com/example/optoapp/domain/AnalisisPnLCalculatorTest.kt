package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalisisPnLCalculatorTest {

    @Test
    fun online_pnl_lines_match_analisisMensual() {
        val analisis = AnalisisMensual(
            ventasMes = 1000.0,
            cobrosMes = 900.0,
            margenNetoPct = 40.0,
            margenPorCategoria = listOf(
                MargenCategoria("Lentes", ventas = 1000.0, costos = 400.0, margenPct = 60.0),
            ),
            deudores = DeudoresResumen(0, 0.0),
            proyeccionCaja = null,
            stockEstancado = emptyList(),
            valorInventario = 0.0,
            ventasMesAnterior = 0.0,
            variacionVentasPct = null,
            gastosMes = 200.0,
        )

        val pnl = AnalisisPnLCalculator.fromAnalisis(analisis)
        assertEquals(1000.0, pnl.ventas, 0.001)
        assertEquals(400.0, pnl.cogs, 0.001)
        assertEquals(200.0, pnl.gastos, 0.001)
        assertEquals(400.0, pnl.utilidad, 0.001)
    }

    @Test
    fun costoMes_overrides_categoria_sum_for_cogs() {
        val analisis = AnalisisMensual(
            ventasMes = 1000.0,
            cobrosMes = 900.0,
            margenNetoPct = 0.0,
            margenPorCategoria = emptyList(),
            deudores = DeudoresResumen(0, 0.0),
            proyeccionCaja = null,
            stockEstancado = emptyList(),
            valorInventario = 0.0,
            ventasMesAnterior = 0.0,
            variacionVentasPct = null,
            gastosMes = 100.0,
            costoMes = 300.0,
            esOffline = true,
        )
        val pnl = AnalisisPnLCalculator.fromAnalisis(analisis)
        assertEquals(300.0, pnl.cogs, 0.001)
        assertEquals(600.0, pnl.utilidad, 0.001)
    }
}
