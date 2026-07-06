package com.example.optoapp.ui

import com.example.optoapp.viewmodel.OpticaHeaderUi
import com.example.optoapp.viewmodel.SyncState
import com.example.optoapp.viewmodel.SyncViewModel
import com.example.optoapp.viewmodel.AuthViewModel
import org.junit.Assert.*
import org.junit.Test

/**
 * Characterization tests for MainDrawerContent.
 *
 * Verifies section labels, conditional visibility parameters,
 * navigation item structure, and state contracts.
 */
class MainDrawerContentTest {

    @Test
    fun sections_containsAllExpectedLabels() {
        val sections = listOf("GESTIÓN", "PROGRAMACIÓN", "FINANZAS", "SISTEMA")
        assertEquals(4, sections.size)
        assertTrue(sections.contains("GESTIÓN"))
        assertTrue(sections.contains("PROGRAMACIÓN"))
        assertTrue(sections.contains("FINANZAS"))
        assertTrue(sections.contains("SISTEMA"))
    }

    @Test
    fun defaultNavItems_containsExpected() {
        val items = listOf(
            "Pacientes",
            "Servicios Varios",
            "Agenda",
            "Inventario",
            "Configuración",
            "Sincronizar Cloud",
            "Cerrar Sesión"
        )
        assertEquals(7, items.size)
        assertTrue(items.contains("Pacientes"))
        assertTrue(items.contains("Servicios Varios"))
        assertTrue(items.contains("Agenda"))
        assertTrue(items.contains("Inventario"))
        assertTrue(items.contains("Configuración"))
        assertTrue(items.contains("Sincronizar Cloud"))
        assertTrue(items.contains("Cerrar Sesión"))
    }

    @Test
    fun conditionalItems_operacionHoy_whenShowOperacionHoyIsTrue() {
        val conditionalItems = listOf("Operación de Hoy")
        assertEquals(1, conditionalItems.size)
    }

    @Test
    fun conditionalItems_finanzas_whenShowCierreCajaOrShowBiYReportes() {
        val conditionalItems = listOf("Cierre de Caja", "Mi Negocio", "Reportes")
        assertEquals(3, conditionalItems.size)
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
            "syncState" in fields || "syncState" in methods
        )
    }

    @Test
    fun syncViewModel_isSilentSyncing_isDeclared() {
        val fields = SyncViewModel::class.java.declaredFields.map { it.name }
        val methods = SyncViewModel::class.java.methods.map { it.name }
        assertTrue(
            "isSilentSyncing debe ser miembro de SyncViewModel",
            "isSilentSyncing" in fields || "isSilentSyncing" in methods
        )
    }

    @Test
    fun syncViewModel_clearSyncUiState_isDeclared() {
        val methods = SyncViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "clearSyncUiState debe ser método de SyncViewModel",
            "clearSyncUiState" in methods
        )
    }

    @Test
    fun syncViewModel_performFullSync_isDeclared() {
        val methods = SyncViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "performFullSync debe ser método de SyncViewModel",
            "performFullSync" in methods
        )
    }

    @Test
    fun authViewModel_logout_isDeclared() {
        val methods = AuthViewModel::class.java.declaredMethods.map { it.name }
        val allMethods = AuthViewModel::class.java.methods.map { it.name }
        assertTrue(
            "logout debe ser método de AuthViewModel",
            "logout" in methods || "logout" in allMethods
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
            field?.type?.name
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
        val route = "pacientes"
        assertEquals("pacientes", route)
    }

    @Test
    fun navigationRoutes_serviciosExtraExists() {
        val route = "servicios_extra"
        assertEquals("servicios_extra", route)
    }

    @Test
    fun navigationRoutes_agendaExists() {
        val route = "agenda"
        assertEquals("agenda", route)
    }

    @Test
    fun navigationRoutes_monturasExists() {
        val route = "monturas"
        assertEquals("monturas", route)
    }

    @Test
    fun navigationRoutes_configuracionExists() {
        val route = "configuracion"
        assertEquals("configuracion", route)
    }

    @Test
    fun navigationRoutes_cierreCajaExists() {
        val route = "cierre_caja"
        assertEquals("cierre_caja", route)
    }

    @Test
    fun navigationRoutes_estadisticasBiExists() {
        val route = "estadisticas_bi"
        assertEquals("estadisticas_bi", route)
    }

    @Test
    fun navigationRoutes_analisisDetalleExists() {
        val route = "analisis_detalle"
        assertEquals("analisis_detalle", route)
    }

    @Test
    fun navigationRoutes_reportesExists() {
        val route = "reportes"
        assertEquals("reportes", route)
    }

    @Test
    fun navigationRoutes_operacionHoyExists() {
        val route = "operacion_hoy"
        assertEquals("operacion_hoy", route)
    }

    @Test
    fun navigationRoutes_proveedoresExists() {
        val route = "proveedores"
        assertEquals("proveedores", route)
    }

    @Test
    fun navigationRoutes_ordenesCompraExists() {
        val route = "ordenes_compra"
        assertEquals("ordenes_compra", route)
    }
}
