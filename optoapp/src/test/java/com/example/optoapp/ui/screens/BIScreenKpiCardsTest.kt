package com.example.optoapp.ui.screens

import com.example.optoapp.viewmodel.BIUiState
import com.example.optoapp.viewmodel.Periodo
import org.junit.Assert.assertEquals
import org.junit.Test

class BIScreenKpiCardsTest {

    @Test
    fun biUiState_inventoryKpiFieldsDefaultToZero() {
        val state = BIUiState(periodo = Periodo.MES_ACTUAL)

        assertEquals(0, state.totalModelos)
        assertEquals(0, state.inventarioStockBajo)
    }

    @Test
    fun biUiState_inventoryKpiFields_retainAssignedValues() {
        val state = BIUiState(
            periodo = Periodo.MES_ACTUAL,
            totalModelos = 42,
            inventarioStockBajo = 5
        )

        assertEquals(42, state.totalModelos)
        assertEquals(5, state.inventarioStockBajo)
    }

    @Test
    fun biUiState_otherFieldsNotAffectedByKpiAddition() {
        val state = BIUiState(
            periodo = Periodo.TRIMESTRE,
            examenesActual = 25,
            examenesAnterior = 20,
            recaudacionProyectada = 5000.0,
            recaudacionCobrada = 3200.0,
            totalModelos = 100,
            inventarioStockBajo = 12
        )

        assertEquals(Periodo.TRIMESTRE, state.periodo)
        assertEquals(25, state.examenesActual)
        assertEquals(20, state.examenesAnterior)
        assertEquals(5000.0, state.recaudacionProyectada, 0.001)
        assertEquals(3200.0, state.recaudacionCobrada, 0.001)
        assertEquals(100, state.totalModelos)
        assertEquals(12, state.inventarioStockBajo)
    }
}
