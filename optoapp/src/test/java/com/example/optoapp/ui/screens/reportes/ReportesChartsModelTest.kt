package com.example.optoapp.ui.screens.reportes

import com.example.optoapp.domain.ClinicoReporte
import com.example.optoapp.domain.EquipoReporte
import com.example.optoapp.domain.InventarioBucket
import com.example.optoapp.domain.InventarioReporte
import com.example.optoapp.domain.PacientesReporte
import com.example.optoapp.domain.PeriodComparison
import com.example.optoapp.domain.PeriodSeriesComparison
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
    fun edadSlices_mapsAgeGroups() {
        val slices = ReportesChartsModel.edadSlices(
            PacientesReporte(
                altas = 6,
                hombres = 0,
                mujeres = 0,
                sinSexo = 0,
                pediatricos = 2,
                adultos = 3,
                adultosMayores = 1,
            ),
            pediatricosLabel = "P",
            adultosLabel = "A",
            adultosMayoresLabel = "M",
        )
        assertEquals(listOf("P", "A", "M"), slices.map { it.label })
        assertEquals(listOf(2.0, 3.0, 1.0), slices.map { it.amount })
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
    fun clinicoDiagnosticos_keepsPinnedMiopiaWhenLongTailWouldHideIt() {
        val reporte = ClinicoReporte(
            evaluaciones = 40,
            porDiagnostico = listOf(
                "Catarata" to 10,
                "Glaucoma" to 9,
                "Queratitis" to 8,
                "Blefaritis" to 7,
                "Pinguecula" to 6,
                "Pterigion" to 5,
                "Miopía" to 2,
                "Orzuelo" to 1,
            ),
        )
        val slices = ReportesChartsModel.clinicoDiagnosticos(reporte, top = 6, otrosLabel = "Otros")
        assertTrue(slices.any { it.label == "Miopía" && it.amount == 2.0 })
        assertEquals("Otros", slices.last().label)
        assertTrue(slices.none { it.label == "Orzuelo" })
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
    fun inventarioBuckets_skipsZeroAndMapsUnits() {
        val slices = ReportesChartsModel.inventarioBuckets(
            listOf(
                InventarioBucket("Metálico", 4),
                InventarioBucket("Pasta", 0),
                InventarioBucket("Sin dato", 1),
            ),
        )
        assertEquals(listOf("Metálico", "Sin dato"), slices.map { it.label })
        assertEquals(listOf(4.0, 1.0), slices.map { it.amount })
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
    fun ventasSerieComparativo_mapsBucketsToGroupedSeries() {
        val series = ReportesChartsModel.ventasSerieComparativo(
            PeriodSeriesComparison(
                granularity = PeriodSeriesComparison.Granularity.SEMANAL_DIAS,
                labels = listOf("Lun", "Mar"),
                currentValues = listOf(10.0, 20.0),
                previousValues = listOf(5.0, 8.0),
            ),
            actualLabel = "Act",
            anteriorLabel = "Ant",
        )
        assertEquals(2, series.size)
        assertEquals(listOf("Lun", "Mar"), series[0].points.map { it.label })
        assertEquals(listOf(10.0, 20.0), series[0].points.map { it.value })
        assertEquals(listOf(5.0, 8.0), series[1].points.map { it.value })
    }

    @Test
    fun ventasMix_bothChannels() {
        val slices = ReportesChartsModel.ventasMix(200.0, 50.0, "OT", "Serv")
        assertEquals(2, slices.size)
        assertTrue(slices.all { it.amount > 0 })
    }
}
