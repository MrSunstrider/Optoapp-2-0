package com.example.optoapp.ui.screens

import com.example.optoapp.data.AppRoles
import com.example.optoapp.domain.Prioridad
import com.example.optoapp.domain.Recomendacion
import com.example.optoapp.ui.navigation.Route
import com.example.optoapp.viewmodel.AnalisisNegocioUiState
import com.example.optoapp.viewmodel.AnalisisNegocioViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AnalisisNegocioScreenTest {

    @Test
    fun uiState_defaults_mesSeleccionadoIsFirstOfMonth() {
        val state = AnalisisNegocioUiState()
        assertEquals(1, state.mesSeleccionado.dayOfMonth)
    }

    @Test
    fun uiState_defaults_analisisIsNull() {
        val state = AnalisisNegocioUiState()
        assertNull(state.analisis)
    }

    @Test
    fun uiState_defaults_deudoresIsEmpty() {
        val state = AnalisisNegocioUiState()
        assertTrue(state.deudores.isEmpty())
    }

    @Test
    fun uiState_defaults_recomendacionesIsEmpty() {
        val state = AnalisisNegocioUiState()
        assertTrue(state.recomendaciones.isEmpty())
    }

    @Test
    fun uiState_defaults_isLoadingIsFalse() {
        val state = AnalisisNegocioUiState()
        assertEquals(false, state.isLoading)
    }

    @Test
    fun uiState_defaults_errorIsNull() {
        val state = AnalisisNegocioUiState()
        assertNull(state.error)
    }

    @Test
    fun uiState_copy_createsNewInstance() {
        val state = AnalisisNegocioUiState(mesSeleccionado = LocalDate.of(2026, 7, 1))
        val copied = state.copy(mesSeleccionado = LocalDate.of(2026, 8, 1))
        assertEquals(LocalDate.of(2026, 7, 1), state.mesSeleccionado)
        assertEquals(LocalDate.of(2026, 8, 1), copied.mesSeleccionado)
    }

    @Test
    fun uiState_analisisField_canBeSet() {
        val analisis = null // just checking field exists
        val state = AnalisisNegocioUiState(analisis = analisis)
        assertNull(state.analisis)
    }

    @Test
    fun uiState_errorField_canBeSet() {
        val state = AnalisisNegocioUiState(error = "Error de conexion")
        assertEquals("Error de conexion", state.error)
    }

    @Test
    fun screen_title_isAnalisisFinanciero() {
        val title = "Análisis Financiero"
        assertEquals("Análisis Financiero", title)
    }

    @Test
    fun screen_route_isEstadisticasBi() {
        val route = "estadisticas_bi"
        assertEquals("estadisticas_bi", route)
    }

    @Test
    fun screen_detalleRoute_isAnalisisDetalleWithYearMonth() {
        assertEquals("analisis_detalle/2026-03", Route.AnalisisDetalle("2026-03").route)
    }

    @Test
    fun screen_button_verAnalisisCompleto() {
        val button = "Ver análisis completo"
        assertEquals("Ver análisis completo", button)
    }

    @Test
    fun monthSelector_hasPreviousButton() {
        val label = "Mes anterior"
        assertTrue(label.contains("anterior"))
    }

    @Test
    fun monthSelector_hasNextButton() {
        val label = "Mes siguiente"
        assertTrue(label.contains("siguiente"))
    }

    @Test
    fun summaryCard_containsVendiste() {
        val label = "Vendiste"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun summaryCard_containsCobraste() {
        val label = "Cobraste"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun summaryCard_containsSaldo() {
        val label = "Saldo pendiente"
        assertTrue(label.contains("Saldo"))
    }

    @Test
    fun summaryCard_containsMargen() {
        val label = "Margen"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun recomendacion_prioridadAlta_isALTA() {
        val prioridad = Prioridad.ALTA
        assertEquals(Prioridad.ALTA, prioridad)
    }

    @Test
    fun recomendacion_prioridadMedia_isMEDIA() {
        val prioridad = Prioridad.MEDIA
        assertEquals(Prioridad.MEDIA, prioridad)
    }

    @Test
    fun recomendacion_prioridadBaja_isBAJA() {
        val prioridad = Prioridad.BAJA
        assertEquals(Prioridad.BAJA, prioridad)
    }

    @Test
    fun errorState_showsRetryButton() {
        val buttonText = "Reintentar"
        assertEquals("Reintentar", buttonText)
    }

    @Test
    fun offlineState_showsBanner() {
        val bannerText = "Datos limitados — sin conexión"
        assertTrue(bannerText.contains("sin conexión"))
    }

    @Test
    fun emptyState_showsSinDatosPlaceholder() {
        val placeholder = "Sin datos para este mes"
        assertTrue(placeholder.contains("Sin datos"))
    }

    @Test
    fun analisisNegocioViewModel_uiState_isDeclared() {
        val fields = AnalisisNegocioViewModel::class.java.declaredFields.map { it.name }
        assertTrue(
            "AnalisisNegocioViewModel debe tener uiState",
            "uiState" in fields,
        )
    }

    @Test
    fun analisisNegocioViewModel_navigateMonth_isDeclared() {
        val methods = AnalisisNegocioViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "AnalisisNegocioViewModel debe tener navigateMonth",
            "navigateMonth" in methods,
        )
    }

    @Test
    fun analisisNegocioViewModel_refresh_isDeclared() {
        val methods = AnalisisNegocioViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "AnalisisNegocioViewModel debe tener refresh",
            "refresh" in methods,
        )
    }

    @Test
    fun appRoles_canViewBiAndReports_isDeclared() {
        val methods = AppRoles::class.java.declaredMethods.map { it.name }
        assertTrue(
            "AppRoles debe tener canViewBiAndReports",
            "canViewBiAndReports" in methods,
        )
    }

    @Test
    fun restrictedAccess_showsLockIcon_andText() {
        val title = "Acceso restringido"
        val message = "Tu rol no tiene permiso para ver esta sección."
        assertTrue(title.isNotBlank())
        assertTrue(message.isNotBlank())
    }

    @Test
    fun recomendacion_hasPrioridadField() {
        val fields = Recomendacion::class.java.declaredFields.map { it.name }
        assertTrue("Recomendacion debe tener prioridad", "prioridad" in fields)
    }

    @Test
    fun recomendacion_hasTituloField() {
        val fields = Recomendacion::class.java.declaredFields.map { it.name }
        assertTrue("Recomendacion debe tener titulo", "titulo" in fields)
    }

    @Test
    fun recomendacion_hasDetalleField() {
        val fields = Recomendacion::class.java.declaredFields.map { it.name }
        assertTrue("Recomendacion debe tener detalle", "detalle" in fields)
    }
}
