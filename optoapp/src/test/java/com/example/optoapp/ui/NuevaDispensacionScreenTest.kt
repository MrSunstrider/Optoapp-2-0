package com.example.optoapp.ui.screens

import com.example.optoapp.R
import com.example.optoapp.viewmodel.DispensacionUiState
import com.example.optoapp.viewmodel.DispensacionViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class NuevaDispensacionScreenTest {

    @Test
    fun uiState_defaultValuesAreCorrect() {
        val state = DispensacionUiState(fecha = LocalDate.of(2024, 1, 1))
        assertEquals("", state.ot)
        assertEquals(1, state.items.size)
        assertEquals("", state.items[0].tipoLente)
        assertEquals("", state.items[0].distanciaLente)
        assertEquals("", state.items[0].altura)
        assertEquals("", state.items[0].materialLente)
        assertEquals(emptyList<String>(), state.items[0].tratamientos)
        assertEquals("", state.items[0].colorLente)
        assertEquals("", state.items[0].notasDiseno)
        assertEquals("", state.origenMontura)
        assertEquals("", state.monturaId)
        assertEquals("", state.tipoAro)
        assertEquals("", state.materialMontura)
        assertEquals("", state.descripcionMontura)
        assertEquals("", state.montoTotal)
        assertEquals("Pendiente", state.estadoEntrega)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.pagos.isEmpty())
    }

    @Test
    fun wizardSteps_alwaysFourSteps_includingCostosMargen() {
        val expected = listOf(
            R.string.disp_paso_orden,
            R.string.disp_productos,
            R.string.disp_paso_gestion,
            R.string.disp_paso_costos_margen,
        )
        assertEquals(expected, wizardStepsForMode(isEditMode = false))
        assertEquals(expected, wizardStepsForMode(isEditMode = true))
    }

    @Test
    fun dispensacionViewModel_exposesTheScreenContract() {
        val fields = DispensacionViewModel::class.java.declaredFields.map { it.name }
        val methods = DispensacionViewModel::class.java.methods.map { it.name }
        val declaredMethods = DispensacionViewModel::class.java.declaredMethods.map { it.name }
        assertTrue("uiState" in fields || "uiState" in methods)
        assertTrue("monturasActivas" in fields || "monturasActivas" in methods)
        listOf("updateUiState", "suggestOt", "saveDispensacion").forEach { name ->
            assertTrue("$name must be declared by DispensacionViewModel", name in declaredMethods)
        }
    }

    @Test
    fun uiState_generatedIdIsEmptyUntilTheViewModelAssignsIt() {
        assertTrue(DispensacionUiState().generatedId.isEmpty())
    }
}
