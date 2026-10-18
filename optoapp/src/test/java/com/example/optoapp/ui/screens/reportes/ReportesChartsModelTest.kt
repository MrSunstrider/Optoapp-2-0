package com.example.optoapp.ui.screens.reportes

import com.example.optoapp.domain.ClinicoReporte
import com.example.optoapp.domain.EquipoReporte
import com.example.optoapp.domain.InventarioReporte
import com.example.optoapp.domain.PacientesReporte
import com.example.optoapp.domain.PeriodComparison
import com.example.optoapp.domain.VendedorVenta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportesChartsModelTest {

    @Test
    fun sexoSlices_mapsCounts() {
        val slices = ReportesChartsModel.sexoSlices(
            PacientesReporte(altas = 5, hombres = 2, mujeres = 2, sinSexo = 1),
            hombresLabel = "H",
            mujeresLabel = "M",
            sinDatoLabel = "S",
        )
        assertEquals(listOf(2.0, 2.0, 1.0), slices.map { it.amount })
    }

    @Test
    fun clinicoDiagnosticos_bucketsOthers() {
        val reporte = ClinicoReporte(
            evaluaciones = 10,
            porDiagnostico = (8 downTo 1).map { "D$it" to it },
        )
        val slices = ReportesChartsModel.clinicoDiagnosticos(reporte, top = 3, otrosLabel = "Otros")
        assertEquals(4, slices.size)
        assertEquals("D8", slices[0].label)
        assertEquals(8.0, slices[0].amount, 0.0)
        assertEquals("Otros", slices.last().label)
        assertEquals((1 + 2 + 3 + 4 + 5).toDouble(), slices.last().amount, 0.0)
    }

    @Test
    fun inventarioEstado_okVsBajo() {
        val slices = ReportesChartsModel.inventarioEstado(
            InventarioReporte(modelos = 10, stockTotal = 40, lowStock = 3, salidasVentaPeriodo = 2),
            okLabel = "OK",
            bajoLabel = "Bajo",
        )
        assertEquals(7.0, slices[0].amount, 0.0)
        assertEquals(3.0, slices[1].amount, 0.0)
    }

    @Test
    fun equipoVendido_includesSinAtribuir() {
        val series = ReportesChartsModel.equipoVendido(
            EquipoReporte(
                filas = listOf(VendedorVenta("u1", "Ana", 100.0, 2, 10.0)),
                sinAtribuirVendido = 40.0,
                sinAtribuirCount = 1,
                comisionPct = 10.0,
            ),
            serieName = "Vendido",
            sinAtribuirLabel = "Sin",
        )
        assertEquals(1, series.size)
        assertEquals(listOf("Ana", "Sin"), series[0].points.map { it.label })
    }

    @Test
    fun ventasComparativo_twoSeries() {
        val series = ReportesChartsModel.ventasComparativo(
            PeriodComparison(100.0, 80.0, 40.0, 30.0, 5, 4),
            actualLabel = "Act",
            anteriorLabel = "Ant",
            vendidoLabel = "V",
            margenLabel = "M",
        )
        assertEquals(2, series.size)
        assertEquals(100.0, series[0].points[0].value, 0.0)
        assertEquals(80.0, series[1].points[0].value, 0.0)
    }

    @Test
    fun ventasMix_bothChannels() {
        val slices = ReportesChartsModel.ventasMix(200.0, 50.0, "OT", "Serv")
        assertEquals(2, slices.size)
        assertTrue(slices.all { it.amount > 0 })
    }
}
