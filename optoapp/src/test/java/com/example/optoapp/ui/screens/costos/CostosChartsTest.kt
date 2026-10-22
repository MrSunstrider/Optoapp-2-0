package com.example.optoapp.ui.screens.costos

import com.example.optoapp.domain.CoberturaCosto
import com.example.optoapp.domain.CostosPorLinea
import com.example.optoapp.domain.FuenteDatos
import com.example.optoapp.domain.GastoCategoria
import com.example.optoapp.domain.MarginFacts
import com.example.optoapp.ui.components.charts.ChartEmptyStateModel
import com.example.optoapp.ui.components.charts.ChartSummaryBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CostosChartsTest {

    private val copy = ComponentesCostoCopy(
        serie = "Costo",
        lentes = "Lentes",
        montura = "Montura",
        lc = "LC",
        biselado = "Biselado",
        servicios = "Servicios",
        regalos = "Regalos",
    )

    @Test
    fun threeGastoCategoriesBecomeThreeLabelledSlicesWithSummary() {
        val facts = MarginFacts(
            gastosPorCategoria = listOf(
                GastoCategoria("alquiler", 2000.0, "fijo"),
                GastoCategoria("marketing", 500.0, "variable"),
                GastoCategoria("personal", 1500.0, "fijo"),
            ),
        )

        val slices = CostosChartsModel.gastosPorCategoria(facts)

        assertEquals(listOf("Alquiler", "Marketing", "Personal"), slices.map { it.label })
        assertEquals(listOf(2000.0, 500.0, 1500.0), slices.map { it.amount })
        assertFalse(ChartEmptyStateModel.forSlices("Gastos", slices).isEmpty)
        val resumen = ChartSummaryBuilder.donut("Gastos", slices)
        slices.forEach { assertTrue(resumen.contains(it.label)) }
    }

    @Test
    fun offlineComponentsChartIsAnEmptyStateNotZeroBars() {
        val facts = MarginFacts(fuente = FuenteDatos.OFFLINE)

        assertEquals(ComponentesCostoChart.SinConexion, CostosChartsModel.componentes(facts, copy))
    }

    @Test
    fun onlineComponentsChartGroupsServiciosAndKeepsEveryComponent() {
        val facts = MarginFacts(
            costosPorLinea = CostosPorLinea(
                lentes = 100.0,
                montura = 80.0,
                lc = 20.0,
                biselado = 10.0,
                serviciosPropios = 5.0,
                serviciosTercerizados = 7.0,
                serviciosSinClasificar = 3.0,
                regalos = 4.0,
            ),
        )

        val chart = CostosChartsModel.componentes(facts, copy) as ComponentesCostoChart.Serie

        val puntos = chart.series.single().points
        assertEquals(listOf("Lentes", "Montura", "LC", "Biselado", "Servicios", "Regalos"), puntos.map { it.label })
        assertEquals(listOf(100.0, 80.0, 20.0, 10.0, 15.0, 4.0), puntos.map { it.value })
    }

    @Test
    fun onlineComponentsChartOmitsZeroBarsSoLabelsStayAligned() {
        val facts = MarginFacts(
            costosPorLinea = CostosPorLinea(
                lentes = 100.0,
                montura = 0.0,
                lc = 20.0,
                biselado = 0.0,
                serviciosPropios = 15.0,
                regalos = 0.0,
            ),
        )

        val chart = CostosChartsModel.componentes(facts, copy) as ComponentesCostoChart.Serie

        assertEquals(listOf("Lentes", "LC", "Servicios"), chart.series.single().points.map { it.label })
    }

    @Test
    fun coverageGaugeReusesTheCostCoverageBadgeModel() {
        val online = CostosChartsModel.cobertura(
            MarginFacts(cobertura = CoberturaCosto(lineasTotal = 10, lineasReales = 6, pctReal = 60.0, pctFaltante = 40.0)),
        )
        val offline = CostosChartsModel.cobertura(MarginFacts(fuente = FuenteDatos.OFFLINE))

        assertEquals("60 %", online.shares?.real)
        assertTrue(offline.offline)
        assertNull(offline.shares)
    }

    @Test
    fun otsSinCostoPluralUsesSingularAndPluralForms() {
        val formas = plural("ots_sin_costo")

        assertEquals("1 OT sin costo", formas.getValue("one").format(1))
        assertEquals("5 OTs sin costo", formas.getValue("other").format(5))
    }

    private fun plural(nombre: String): Map<String, String> {
        // Gradle runs unit tests from the module directory, an IDE run may start at the repository root.
        val strings = listOf("src/main/res/values/strings.xml", "optoapp/src/main/res/values/strings.xml")
            .map(::File)
            .first { it.exists() }
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(strings)
        val nodos = doc.getElementsByTagName("plurals")
        val plural = (0 until nodos.length).map { nodos.item(it) as org.w3c.dom.Element }
            .single { it.getAttribute("name") == nombre }
        val items = plural.getElementsByTagName("item")
        return (0 until items.length).associate {
            val item = items.item(it) as org.w3c.dom.Element
            item.getAttribute("quantity") to item.textContent
        }
    }
}
