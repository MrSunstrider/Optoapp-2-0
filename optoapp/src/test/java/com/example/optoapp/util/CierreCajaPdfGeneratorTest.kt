package com.example.optoapp.util

import com.example.optoapp.data.Pago
import com.example.optoapp.viewmodel.CierreCajaUiState
import com.example.optoapp.viewmodel.PagoDisplayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CierreCajaPdfGeneratorTest {

    private val fecha = LocalDate.of(2026, 8, 22)

    private fun fixtureState(): Pair<CierreCajaUiState, Map<String, Double>> {
        val pagos = listOf(
            Pago(
                id = "p1",
                fecha = fecha,
                tipo = "Abono",
                monto = 100.0,
                metodoPago = "Efectivo",
            ),
            Pago(
                id = "p2",
                fecha = fecha,
                tipo = "Reembolso",
                monto = 40.0,
                metodoPago = "Efectivo",
            ),
        )
        val display = pagos.map {
            PagoDisplayItem(
                pago = it,
                label = "OT 1",
                tipoEntidad = "Dispensación",
                esCobroAtrasado = false,
                dispensacionId = null,
                servicioExtraId = null,
                pacienteId = null,
            )
        }
        val state = CierreCajaUiState(
            fecha = fecha,
            pagos = pagos,
            pagosDisplay = display,
            ventasHoy = 60.0,
            totalGeneral = 200.0,
            isLoading = false,
        )
        return state to mapOf("Efectivo" to 60.0)
    }

    @Test
    fun dayCloseLines_heroAndEfectivo_equalPagoEffectAggregates_notRawMontoSum() {
        val (state, totales) = fixtureState()
        val cobradoHoy = 60.0
        val rawSum = state.pagos.sumOf { it.monto }
        assertEquals(140.0, rawSum, 0.0)

        val lines = CierreCajaPdfGenerator.dayCloseLines(
            state = state,
            cobradoHoy = cobradoHoy,
            totalesPorMetodo = totales,
            contado = null,
        )

        assertTrue(lines.any { it.contains("Cobrado") && it.contains("60.00") })
        assertTrue(lines.any { it.contains("Efectivo") && it.contains("60.00") })
        assertFalse(lines.any { it.contains("140.00") })
    }

    @Test
    fun dayCloseLines_includesFechaAndMethodBreakdown() {
        val state = CierreCajaUiState(
            fecha = fecha,
            ventasHoy = 10.0,
            cobrosAtrasados = 5.0,
            isLoading = false,
        )
        val lines = CierreCajaPdfGenerator.dayCloseLines(
            state = state,
            cobradoHoy = 15.0,
            totalesPorMetodo = mapOf("Tarjeta" to 10.0, "Móvil" to 5.0),
            contado = null,
        )
        assertTrue(lines.any { it.contains("2026-08-22") || it.contains("Cierre de Caja") })
        assertTrue(lines.any { it.contains("Tarjeta") && it.contains("10.00") })
        assertTrue(lines.any { it.contains("Móvil") && it.contains("5.00") })
        assertTrue(lines.any { it.contains("15.00") })
    }
}
