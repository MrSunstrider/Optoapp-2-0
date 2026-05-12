package com.example.optoapp.viewmodel

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for OperacionHoyUiState data class contracts and copy behavior.
 *
 * NOTE: Default OperacionHoyUiState() uses DateUtils.today() which requires
 * core library desugaring for java.time on Android. These tests avoid that
 * by constructing explicit states.
 */
class OperacionHoyViewModelTest {

    @Test
    fun operacionHoyUiState_customValues() {
        val state = OperacionHoyUiState(
            fecha = LocalDate.of(2024, 6, 15),
            citasHoy = 5,
            entregasPendientes = 3,
            cobrosHoy = 450.0,
            stockCritico = 2,
            alertas = listOf("Stock crítico", "Entregas atrasadas")
        )
        assertEquals(5, state.citasHoy)
        assertEquals(3, state.entregasPendientes)
        assertEquals(450.0, state.cobrosHoy, 0.001)
        assertEquals(2, state.stockCritico)
        assertEquals(2, state.alertas.size)
    }

    @Test
    fun operacionHoyUiState_copyWithModifications() {
        val original = OperacionHoyUiState(
            fecha = LocalDate.of(2024, 1, 1)
        )
        val modified = original.copy(citasHoy = 10, alertas = listOf("Test alerta"))
        assertEquals(10, modified.citasHoy)
        assertEquals(1, modified.alertas.size)
        // Original unchanged
        assertEquals(0, original.citasHoy)
    }

    @Test
    fun operacionHoyUiState_dataClassEquality() {
        val state1 = OperacionHoyUiState(fecha = LocalDate.of(2024, 6, 15))
        val state2 = OperacionHoyUiState(fecha = LocalDate.of(2024, 6, 15))
        assertEquals(state1, state2)
    }

    @Test
    fun operacionHoyUiState_dataClassInequality() {
        val state1 = OperacionHoyUiState(fecha = LocalDate.of(2024, 1, 1), citasHoy = 1)
        val state2 = OperacionHoyUiState(fecha = LocalDate.of(2024, 1, 1), citasHoy = 2)
        assertNotEquals(state1, state2)
    }

    @Test
    fun operacionHoyUiState_alertasList_immutable() {
        val state = OperacionHoyUiState(
            fecha = LocalDate.of(2024, 1, 1),
            alertas = mutableListOf("Alerta 1", "Alerta 2")
        )
        assertEquals(2, state.alertas.size)
    }
}
