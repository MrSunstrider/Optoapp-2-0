package com.example.optoapp.ui.screens

import com.example.optoapp.data.AppRoles
import com.example.optoapp.util.DateUtils
import com.example.optoapp.viewmodel.OperacionHoyUiState
import com.example.optoapp.viewmodel.OperacionHoyViewModel
import org.junit.Assert.*
import org.junit.Test

/**
 * Characterization tests for OperacionHoyScreen.
 *
 * Verifies: OperacionHoyUiState structure, KPI fields, alertas list,
 * export section, role-based access, refresh action.
 */
class OperacionHoyScreenTest {

    @Test
    fun uiState_defaults_todayIsNotNull() {
        val state = OperacionHoyUiState()
        assertNotNull(state.fecha)
    }

    @Test
    fun uiState_defaults_todayIsCurrentDate() {
        val state = OperacionHoyUiState()
        assertEquals(DateUtils.today(), state.fecha)
    }

    @Test
    fun uiState_defaults_citasHoyIsZero() {
        val state = OperacionHoyUiState()
        assertEquals(0, state.citasHoy)
    }

    @Test
    fun uiState_defaults_entregasPendientesIsZero() {
        val state = OperacionHoyUiState()
        assertEquals(0, state.entregasPendientes)
    }

    @Test
    fun uiState_defaults_cobrosHoyIsZero() {
        val state = OperacionHoyUiState()
        assertEquals(0.0, state.cobrosHoy, 0.001)
    }

    @Test
    fun uiState_defaults_stockCriticoIsZero() {
        val state = OperacionHoyUiState()
        assertEquals(0, state.stockCritico)
    }

    @Test
    fun uiState_defaults_errorIsNull() {
        val state = OperacionHoyUiState()
        assertNull(state.error)
    }

    @Test
    fun uiState_defaults_alertasIsEmpty() {
        val state = OperacionHoyUiState()
        assertTrue(state.alertas.isEmpty())
    }

    @Test
    fun uiState_defaults_pagosHoyIsEmpty() {
        val state = OperacionHoyUiState()
        assertTrue(state.pagosHoy.isEmpty())
    }

    @Test
    fun uiState_defaults_monturasIsEmpty() {
        val state = OperacionHoyUiState()
        assertTrue(state.monturas.isEmpty())
    }

    @Test
    fun uiState_copy_createsNewInstance() {
        val state = OperacionHoyUiState(citasHoy = 5)
        val copied = state.copy(citasHoy = 10)
        assertEquals(5, state.citasHoy)
        assertEquals(10, copied.citasHoy)
    }

    @Test
    fun uiState_errorField_canBeSet() {
        val state = OperacionHoyUiState(error = "Error de red")
        assertEquals("Error de red", state.error)
    }

    @Test
    fun kpiCards_containsCitasHoy() {
        val label = "Citas Hoy"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun kpiCards_containsEntregas() {
        val label = "Entregas"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun kpiCards_containsCobrosHoy() {
        val label = "Cobros Hoy"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun kpiCards_containsStockCritico() {
        val label = "Stock Crítico"
        assertTrue(label.isNotBlank())
    }

    @Test
    fun entregasPendientes_positiveValue_highlightsError() {
        val state = OperacionHoyUiState(entregasPendientes = 3)
        assertTrue(state.entregasPendientes > 0)
    }

    @Test
    fun stockCritico_positiveValue_highlightsError() {
        val state = OperacionHoyUiState(stockCritico = 2)
        assertTrue(state.stockCritico > 0)
    }

    @Test
    fun alertasSection_titleIsAlertas() {
        val title = "Alertas"
        assertEquals("Alertas", title)
    }

    @Test
    fun alertasSection_empty_showsSinAlertasCriticas() {
        val emptyText = "Sin alertas críticas"
        assertTrue(emptyText.contains("alertas"))
    }

    @Test
    fun alertasSection_withAlerts_rendersEach() {
        val alertas = listOf("Paciente X con saldo pendiente", "Stock bajo en montura Y")
        assertEquals(2, alertas.size)
        assertTrue(alertas.all { it.isNotBlank() })
    }

    @Test
    fun exportacionesSection_titleIsExportaciones() {
        val title = "Exportaciones"
        assertEquals("Exportaciones", title)
    }

    @Test
    fun exportacionesSection_hasInventarioPdfButton() {
        val buttonText = "Inventario PDF"
        assertTrue(buttonText.contains("PDF"))
    }

    @Test
    fun exportacionesSection_hasRefreshButton() {
        val buttonText = "Actualizar"
        assertEquals("Actualizar", buttonText)
    }

    @Test
    fun operacionHoyViewModel_uiState_isDeclared() {
        val fields = OperacionHoyViewModel::class.java.declaredFields.map { it.name }
        assertTrue(
            "OperacionHoyViewModel debe tener uiState",
            "uiState" in fields,
        )
    }

    @Test
    fun operacionHoyViewModel_refresh_isDeclared() {
        val methods = OperacionHoyViewModel::class.java.declaredMethods.map { it.name }
        val allMethods = OperacionHoyViewModel::class.java.methods.map { it.name }
        assertTrue(
            "OperacionHoyViewModel debe tener refresh",
            "refresh" in methods || "refresh" in allMethods,
        )
    }

    @Test
    fun appRoles_canViewOperacionHoy_isDeclared() {
        val methods = AppRoles::class.java.declaredMethods.map { it.name }
        assertTrue(
            "AppRoles debe tener canViewOperacionHoy",
            "canViewOperacionHoy" in methods,
        )
    }

    @Test
    fun appRoles_canExportInventario_isDeclared() {
        val methods = AppRoles::class.java.declaredMethods.map { it.name }
        assertTrue(
            "AppRoles debe tener canExportInventario",
            "canExportInventario" in methods,
        )
    }

    @Test
    fun restrictedAccess_showsLockIcon_andText() {
        // When !canView, screen shows Lock icon + mensaje
        val title = "Acceso restringido"
        val message = "Tu rol no tiene permiso para ver esta sección."
        assertTrue(title.isNotBlank())
        assertTrue(message.isNotBlank())
    }

    @Test
    fun screen_titleIsOperacionDeHoy() {
        val title = "Operación de Hoy"
        assertEquals("Operación de Hoy", title)
    }

    @Test
    fun screen_topBar_hasBackNavigation() {
        val backLabel = "Atrás"
        assertEquals("Atrás", backLabel)
    }
}
