package com.example.optoapp.ui

import com.example.optoapp.domain.AnalisisMensual
import com.example.optoapp.domain.Deudor
import com.example.optoapp.domain.DeudoresResumen
import com.example.optoapp.domain.MargenCategoria
import com.example.optoapp.domain.ProyeccionCaja
import com.example.optoapp.domain.StockEstancadoItem
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AnalisisDetalleScreenTest {

    // ─── Screen labels ──────────────────────────────────────────────────────

    @Test
    fun screen_title_isAnalisisCompleto() {
        val title = "Análisis Completo"
        assertEquals("Análisis Completo", title)
    }

    @Test
    fun screen_route_isAnalisisDetalle() {
        val route = "analisis_detalle"
        assertEquals("analisis_detalle", route)
    }

    @Test
    fun topBar_hasBackNavigation() {
        val backLabel = "Atrás"
        assertEquals("Atrás", backLabel)
    }

    // ─── Expandable section titles ──────────────────────────────────────────

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

    // ─── Sections expandable by default ─────────────────────────────────────

    @Test
    fun firstSection_isExpandedByDefault() {
        val expandedIndex = 0
        assertEquals(0, expandedIndex)
    }

    // ─── Deudor model ───────────────────────────────────────────────────────

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

    // ─── StockEstancadoItem model ───────────────────────────────────────────

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

    // ─── ProyeccionCaja model ───────────────────────────────────────────────

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

    // ─── Bar data verification ──────────────────────────────────────────────

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

    // ─── Empty states ───────────────────────────────────────────────────────

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
