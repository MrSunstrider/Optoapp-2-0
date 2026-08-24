package com.example.optoapp.util

import com.example.optoapp.data.Pago
import com.example.optoapp.viewmodel.CierreCajaUiState
import com.example.optoapp.viewmodel.PagoDisplayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CierreCajaCsvExporterTest {

    private val fecha = LocalDate.of(2026, 8, 22)

    /**
     * Fixture: PagoEffect net = 60 (Abono 100 − Reembolso 40), raw monto sum = 140.
     * Export MUST use the PagoEffect aggregates passed in, not raw sum.
     */
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
        val totales = mapOf("Efectivo" to 60.0)
        return state to totales
    }

    @Test
    fun csv_heroAndEfectivo_equalPagoEffectAggregates_notRawMontoSum() {
        val (state, totales) = fixtureState()
        val cobradoHoy = 60.0
        val rawSum = state.pagos.sumOf { it.monto }
        assertEquals(140.0, rawSum, 0.0)

        val csv = CierreCajaCsvExporter.toCsv(
            state = state,
            cobradoHoy = cobradoHoy,
            totalesPorMetodo = totales,
            contado = null,
        )

        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("60.00"))
        assertTrue(csv.contains("Efectivo"))
        assertFalse("CSV must not use raw monto sum as hero", csv.contains("140.00"))
        val methodLines = csv.lineSequence()
            .dropWhile { !it.startsWith("Metodo,") }
            .drop(1)
            .takeWhile { it.isNotBlank() && !it.startsWith("Cobros") }
            .toList()
        val methodSum = methodLines.sumOf { line ->
            line.substringAfterLast(',').toDouble()
        }
        assertEquals(cobradoHoy, methodSum, 0.0)
    }

    @Test
    fun csv_usesInvariantDecimalPoint_andUtf8Bom() {
        val state = CierreCajaUiState(
            fecha = fecha,
            ventasHoy = 12.5,
            isLoading = false,
        )
        val csv = CierreCajaCsvExporter.toCsv(
            state = state,
            cobradoHoy = 12.5,
            totalesPorMetodo = mapOf("Tarjeta" to 12.5),
            contado = null,
        )
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("12.50"))
        assertFalse(csv.contains("12,50"))
    }

    @Test
    fun csv_detailRows_reversoAndReembolso_appearSignedNegative() {
        val (state, totales) = fixtureState()
        val csv = CierreCajaCsvExporter.toCsv(
            state = state,
            cobradoHoy = 60.0,
            totalesPorMetodo = totales,
            contado = null,
        )

        val cobroLines = csv.lineSequence()
            .dropWhile { !it.startsWith("Fecha,Tipo") }
            .drop(1)
            .filter { it.isNotBlank() }
            .toList()

        val reembolsoLine = cobroLines.firstOrNull { it.contains("Reembolso") }
        assertFalse("Reembolso detail must not appear as raw positive", reembolsoLine?.contains(",40.00") == true)
        assertTrue("Reembolso detail must appear as negative signed amount", reembolsoLine?.contains("-40.00") == true)

        val abonoLine = cobroLines.firstOrNull { it.contains("Abono") }
        assertTrue("Abono detail must appear as positive signed amount", abonoLine?.contains(",100.00") == true)
    }

    @Test
    fun csv_includesContadoAndDiferencia_whenContadoProvided() {
        val (state, totales) = fixtureState()
        val csv = CierreCajaCsvExporter.toCsv(
            state = state,
            cobradoHoy = 60.0,
            totalesPorMetodo = totales,
            contado = 55.0,
        )
        assertTrue(csv.contains("55.00"))
        assertTrue(csv.contains("-5.00") || csv.contains(",-5.00"))
    }
}
