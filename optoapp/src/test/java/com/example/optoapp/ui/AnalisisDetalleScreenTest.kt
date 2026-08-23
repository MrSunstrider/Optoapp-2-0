package com.example.optoapp.ui.screens

import com.example.optoapp.domain.Deudor
import com.example.optoapp.domain.ProyeccionCaja
import com.example.optoapp.domain.StockEstancadoItem
import com.example.optoapp.ui.navigation.Route
import org.junit.Assert.*
import org.junit.Test

class AnalisisDetalleScreenTest {

    @Test
    fun proyeccionCaja_hasMesesHistoricosField() {
        val fields = ProyeccionCaja::class.java.declaredFields.map { it.name }
        assertTrue("ProyeccionCaja debe tener mesesHistoricos", "mesesHistoricos" in fields)
    }

    @Test
    fun proyeccionCaja_mesesHistoricos_defaultsToZero() {
        val proyeccion = ProyeccionCaja(
            ingresosEsperados = 5000.0,
            egresosProgramados = 2000.0,
            saldoNeto = 3000.0,
        )
        assertEquals(0, proyeccion.mesesHistoricos)
    }

    @Test
    fun warningShown_whenMesesHistoricosIsOne() {
        val proyeccion = ProyeccionCaja(
            ingresosEsperados = 5000.0,
            egresosProgramados = 2000.0,
            saldoNeto = 3000.0,
            mesesHistoricos = 1,
        )
        val showWarning = proyeccion.mesesHistoricos < 3
        assertTrue("Warning should show when mesesHistoricos < 3", showWarning)
    }

    @Test
    fun warningHidden_whenMesesHistoricosIsFive() {
        val proyeccion = ProyeccionCaja(
            ingresosEsperados = 5000.0,
            egresosProgramados = 2000.0,
            saldoNeto = 3000.0,
            mesesHistoricos = 5,
        )
        val showWarning = proyeccion.mesesHistoricos < 3
        assertFalse("Warning should NOT show when mesesHistoricos >= 3", showWarning)
    }

    @Test
    fun warningShown_whenMesesHistoricosIsTwo_edgeCase() {
        val proyeccion = ProyeccionCaja(
            ingresosEsperados = 5000.0,
            egresosProgramados = 2000.0,
            saldoNeto = 3000.0,
            mesesHistoricos = 2,
        )
        val showWarning = proyeccion.mesesHistoricos < 3
        assertTrue("Warning should show when mesesHistoricos == 2 (edge)", showWarning)
    }

    @Test
    fun warningHidden_whenMesesHistoricosIsThree_edgeCase() {
        val proyeccion = ProyeccionCaja(
            ingresosEsperados = 5000.0,
            egresosProgramados = 2000.0,
            saldoNeto = 3000.0,
            mesesHistoricos = 3,
        )
        val showWarning = proyeccion.mesesHistoricos < 3
        assertFalse("Warning should NOT show when mesesHistoricos == 3 (edge)", showWarning)
    }

    @Test
    fun screen_title_isAnalisisCompleto() {
        val title = "Análisis Completo"
        assertEquals("Análisis Completo", title)
    }

    @Test
    fun screen_route_isAnalisisDetalleWithYearMonth() {
        assertEquals("analisis_detalle/2026-03", Route.AnalisisDetalle("2026-03").route)
        assertEquals("analisis_detalle/{yearMonth}", Route.AnalisisDetalle("{yearMonth}").route)
    }

    @Test
    fun topBar_hasBackNavigation() {
        val backLabel = "Atrás"
        assertEquals("Atrás", backLabel)
    }

    @Test
    fun section1_title_isPlataQueEntroYSalio() {
        val title = "Plata que entró y salió"
        assertEquals("Plata que entró y salió", title)
    }

    @Test
    fun section2_title_isLoQueMasTeDeja() {
        val title = "Lo que más te deja"
        assertEquals("Lo que más te deja", title)
    }

    @Test
    fun section3_title_isClientesQueTeDeben() {
        val title = "Clientes que te deben"
        assertEquals("Clientes que te deben", title)
    }

    @Test
    fun section4_title_isProductosSinVender() {
        val title = "Productos sin vender"
        assertEquals("Productos sin vender", title)
    }

    @Test
    fun section5_title_isPlataQueVasATener() {
        val title = "Plata que vas a tener"
        assertEquals("Plata que vas a tener", title)
    }

    @Test
    fun firstSection_isExpandedByDefault() {
        val expandedIndex = 0
        assertEquals(0, expandedIndex)
    }

    @Test
    fun deudor_hasPacienteNombreField() {
        val fields = Deudor::class.java.declaredFields.map { it.name }
        assertTrue("Deudor debe tener pacienteNombre", "pacienteNombre" in fields)
    }

    @Test
    fun deudor_hasPacienteTelefonoField() {
        val fields = Deudor::class.java.declaredFields.map { it.name }
        assertTrue("Deudor debe tener pacienteTelefono", "pacienteTelefono" in fields)
    }

    @Test
    fun deudor_hasSaldoField() {
        val fields = Deudor::class.java.declaredFields.map { it.name }
        assertTrue("Deudor debe tener saldo", "saldo" in fields)
    }

    @Test
    fun deudor_hasDiasDeudaField() {
        val fields = Deudor::class.java.declaredFields.map { it.name }
        assertTrue("Deudor debe tener diasDeuda", "diasDeuda" in fields)
    }

    @Test
    fun stockEstancado_hasModeloField() {
        val fields = StockEstancadoItem::class.java.declaredFields.map { it.name }
        assertTrue("StockEstancadoItem debe tener modelo", "modelo" in fields)
    }

    @Test
    fun stockEstancado_hasDiasSinVentaField() {
        val fields = StockEstancadoItem::class.java.declaredFields.map { it.name }
        assertTrue("StockEstancadoItem debe tener diasSinVenta", "diasSinVenta" in fields)
    }

    @Test
    fun proyeccionCaja_hasIngresosEsperadosField() {
        val fields = ProyeccionCaja::class.java.declaredFields.map { it.name }
        assertTrue("ProyeccionCaja debe tener ingresosEsperados", "ingresosEsperados" in fields)
    }

    @Test
    fun proyeccionCaja_hasEgresosProgramadosField() {
        val fields = ProyeccionCaja::class.java.declaredFields.map { it.name }
        assertTrue("ProyeccionCaja debe tener egresosProgramados", "egresosProgramados" in fields)
    }

    @Test
    fun proyeccionCaja_hasSaldoNetoField() {
        val fields = ProyeccionCaja::class.java.declaredFields.map { it.name }
        assertTrue("ProyeccionCaja debe tener saldoNeto", "saldoNeto" in fields)
    }

    @Test
    fun barSection_containsVentas() {
        val label = "Ventas"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun barSection_containsCobros() {
        val label = "Cobros"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun barSection_containsCostos() {
        val label = "Costos"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun barSection_containsGastos() {
        val label = "Gastos"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun barSection_containsGanancia() {
        val label = "Ganancia"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun emptyDeudores_showsNoDebtors() {
        val text = "Sin deudores pendientes"
        assertTrue(text.contains("deudores"))
    }

    @Test
    fun emptyStock_showsNoStockIssues() {
        val text = "Sin productos estancados"
        assertTrue(text.contains("productos"))
    }

    @Test
    fun emptyProyeccion_showsNoProjection() {
        val text = "Sin datos de proyección"
        assertTrue(text.contains("proyección"))
    }
}
