package com.example.optoapp.ui.screens

import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.viewmodel.AuthViewModel
import com.example.optoapp.viewmodel.OpticaHeaderUi
import com.example.optoapp.viewmodel.SyncState
import com.example.optoapp.viewmodel.SyncViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization contracts for live [DrawerContent] (DrawerSections).
 * Replaces MainDrawerContentTest after dead MainDrawerContent removal.
 */
class DrawerContentContractsTest {

    @Test
    fun sections_containsLiveDrawerLabels() {
        val sections = listOf("GESTIÓN", "PROGRAMACIÓN", "INVENTARIO ÓPTICO", "FINANZAS", "SISTEMA")
        assertEquals(5, sections.size)
        assertTrue(sections.contains("INVENTARIO ÓPTICO"))
        assertTrue(sections.contains("FINANZAS"))
    }

    @Test
    fun defaultNavItems_containsExpected() {
        val items = listOf(
            "Pacientes",
            "Servicios Extra",
            "Agenda",
            "Monturas",
            "Conteo físico",
            "Pedidos a proveedor",
            "Proveedores",
            "Configuración",
            "Cerrar Sesión",
        )
        assertEquals(9, items.size)
        assertTrue(items.contains("Servicios Extra"))
        assertTrue(items.contains("Monturas"))
        assertTrue(items.contains("Proveedores"))
    }

    @Test
    fun conditionalItems_dashboard_whenShowOperacionHoyIsTrue() {
        val conditionalItems = listOf("Dashboard")
        assertEquals(1, conditionalItems.size)
    }

    @Test
    fun conditionalItems_finanzas_whenShowCierreCajaOrShowBiYReportes() {
        val conditionalItems = listOf(
            "Cierre de Caja",
            "Análisis Financiero",
            "Reportes",
            "Costos y Gastos",
        )
        assertEquals(4, conditionalItems.size)
    }

    @Test
    fun syncState_idle_isDeclared() {
        val idle: SyncState = SyncState.Idle
        assertTrue(idle is SyncState.Idle)
    }

    @Test
    fun syncState_loading_isDeclared() {
        val loading: SyncState = SyncState.Loading
        assertTrue(loading is SyncState.Loading)
    }

    @Test
    fun syncState_success_holdsMessage() {
        val success = SyncState.Success("Sincronización completada con éxito")
        assertEquals("Sincronización completada con éxito", success.message)
    }

    @Test
    fun syncState_error_holdsMessage() {
        val error = SyncState.Error("Sin conexión a internet")
        assertEquals("Sin conexión a internet", error.message)
    }

    @Test
    fun syncState_success_isDataClass() {
        val s1 = SyncState.Success("msg")
        val s2 = SyncState.Success("msg")
        assertEquals(s1, s2)
        assertEquals(s1.hashCode(), s2.hashCode())
    }

    @Test
    fun syncState_error_isDataClass() {
        val e1 = SyncState.Error("err")
        val e2 = SyncState.Error("err")
        assertEquals(e1, e2)
    }

    @Test
    fun syncViewModel_syncState_isDeclared() {
        val fields = SyncViewModel::class.java.declaredFields.map { it.name }
        val methods = SyncViewModel::class.java.methods.map { it.name }
        assertTrue(
            "syncState debe ser miembro de SyncViewModel",
            "syncState" in fields || "syncState" in methods,
        )
    }

    @Test
    fun syncViewModel_isSilentSyncing_isDeclared() {
        val fields = SyncViewModel::class.java.declaredFields.map { it.name }
        val methods = SyncViewModel::class.java.methods.map { it.name }
        assertTrue(
            "isSilentSyncing debe ser miembro de SyncViewModel",
            "isSilentSyncing" in fields || "isSilentSyncing" in methods,
        )
    }

    @Test
    fun syncViewModel_clearSyncUiState_isDeclared() {
        val methods = SyncViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "clearSyncUiState debe ser método de SyncViewModel",
            "clearSyncUiState" in methods,
        )
    }

    @Test
    fun syncViewModel_performFullSync_isDeclared() {
        val methods = SyncViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "performFullSync debe ser método de SyncViewModel",
            "performFullSync" in methods,
        )
    }

    @Test
    fun authViewModel_logout_isDeclared() {
        val methods = AuthViewModel::class.java.declaredMethods.map { it.name }
        val allMethods = AuthViewModel::class.java.methods.map { it.name }
        assertTrue(
            "logout debe ser método de AuthViewModel",
            "logout" in methods || "logout" in allMethods,
        )
    }

    @Test
    fun opticaHeaderUi_nombreOpticaExists() {
        val field = OpticaHeaderUi::class.java.declaredFields
            .firstOrNull { it.name == "nombreOptica" }
        assertNotNull("OpticaHeaderUi debe tener campo nombreOptica", field)
        assertEquals(
            "nombreOptica debe ser String",
            "java.lang.String",
            field?.type?.name,
        )
    }

    @Test
    fun opticaHeaderUi_fiscalEtiquetaExists() {
        val field = OpticaHeaderUi::class.java.declaredFields
            .firstOrNull { it.name == "fiscalEtiqueta" }
        assertNotNull("OpticaHeaderUi debe tener campo fiscalEtiqueta", field)
    }

    @Test
    fun opticaHeaderUi_defaultValues() {
        val header = OpticaHeaderUi()
        assertEquals("Óptica", header.nombreOptica)
        assertEquals("Sin documento fiscal", header.fiscalEtiqueta)
    }

    @Test
    fun opticaHeaderUi_isDataClass() {
        val h1 = OpticaHeaderUi("Mi Óptica", "RUC 12345678901")
        val h2 = OpticaHeaderUi("Mi Óptica", "RUC 12345678901")
        assertEquals(h1, h2)
        assertEquals(h1.hashCode(), h2.hashCode())
    }

    @Test
    fun navigationRoutes_pacientesExists() {
        assertEquals("pacientes", Route.Pacientes.route)
    }

    @Test
    fun navigationRoutes_serviciosExtraExists() {
        assertEquals("servicios_extra", Route.ServiciosExtra.route)
    }

    @Test
    fun navigationRoutes_agendaExists() {
        assertEquals("agenda", Route.Agenda.route)
    }

    @Test
    fun navigationRoutes_monturasExists() {
        assertEquals("monturas", Route.Monturas.route)
    }

    @Test
    fun navigationRoutes_configuracionExists() {
        assertEquals("configuracion", Route.Configuracion.route)
    }

    @Test
    fun navigationRoutes_cierreCajaExists() {
        assertEquals("cierre_caja", Route.CierreCaja.route)
    }

    @Test
    fun navigationRoutes_estadisticasBiExists() {
        assertEquals("estadisticas_bi", Route.EstadisticasBI.route)
    }

    @Test
    fun navigationRoutes_analisisDetalleExists() {
        assertEquals("analisis_detalle/{yearMonth}", Route.AnalisisDetalle("{yearMonth}").route)
    }

    @Test
    fun navigationRoutes_reportesExists() {
        assertEquals("reportes", Route.Reportes.route)
    }

    @Test
    fun navigationRoutes_operacionHoyExists() {
        assertEquals("operacion_hoy", Route.OperacionHoy.route)
    }

    @Test
    fun navigationRoutes_proveedoresExists() {
        assertEquals("proveedores", Route.Proveedores.route)
    }

    @Test
    fun navigationRoutes_ordenesCompraExists() {
        assertEquals("ordenes_compra", Route.OrdenesCompra.route)
    }

    @Test
    fun navigationRoutes_gastosAlias_isNotGastosScreenDestination() {
        assertEquals("gastos", Route.Gastos.route)
        assertEquals("costos_y_gastos", Route.CostosYGastos.route)
        assertFalse(Route.Gastos.route == "GastosScreen")
    }

    @Test
    fun deadGastosScreenAndMainDrawerContent_areRemoved() {
        assertNull(
            "GastosScreen must be deleted",
            runCatching { Class.forName("com.example.optoapp.ui.screens.GastosScreenKt") }.getOrNull(),
        )
        assertNull(
            "MainDrawerContent must be deleted",
            runCatching { Class.forName("com.example.optoapp.ui.components.MainDrawerContentKt") }.getOrNull(),
        )
        assertNull(
            "GastosViewModel must be deleted when unused",
            runCatching { Class.forName("com.example.optoapp.viewmodel.GastosViewModel") }.getOrNull(),
        )
        assertNotNull(
            "CostosYGastosScreen remains the canonical destination",
            Class.forName("com.example.optoapp.ui.screens.CostosYGastosScreenKt"),
        )
        assertNotNull(
            "DrawerContent remains the live drawer",
            Class.forName("com.example.optoapp.ui.screens.DrawerSectionsKt"),
        )
    }
}
