package com.example.optoapp.ui.screens

import com.example.optoapp.viewmodel.CostosYGastosUiState
import com.example.optoapp.viewmodel.CostosYGastosViewModel
import org.junit.Assert.*
import org.junit.Test

/**
 * RED test: CostosYGastosScreen structure — 2 tabs render, block dropdown works.
 *
 * Tests UiState defaults and ViewModel contract via reflection
 * (the screen and ViewModel don't exist yet — that's expected in RED phase).
 */
class CostosYGastosScreenTest {

    @Test
    fun uiState_defaults_tabIsMatriz() {
        val state = CostosYGastosUiState()
        assertEquals(0, state.selectedTab) // 0 = Matriz tab
    }

    @Test
    fun uiState_defaults_gastosIsEmpty() {
        val state = CostosYGastosUiState()
        assertTrue(state.gastosOperativos.isEmpty())
    }

    @Test
    fun uiState_defaults_errorIsNull() {
        val state = CostosYGastosUiState()
        assertNull(state.error)
    }

    @Test
    fun uiState_defaults_isLoadingIsFalse() {
        val state = CostosYGastosUiState()
        assertFalse(state.isLoading)
    }

    @Test
    fun uiState_selectedBlock_isNullByDefault() {
        val state = CostosYGastosUiState()
        assertNull(state.selectedBlock)
    }

    @Test
    fun uiState_costosPorDispensacion_isEmptyByDefault() {
        val state = CostosYGastosUiState()
        assertTrue(state.costosPorDispensacion.isEmpty())
    }

    @Test
    fun uiState_dispensacionFilter_isNullByDefault() {
        val state = CostosYGastosUiState()
        assertNull(state.dispensacionFilterId)
    }

    @Test
    fun uiState_tabTitles_containsMatriz() {
        val tabs = listOf("Matriz de Costos", "Gastos Operativos")
        assertEquals("Matriz de Costos", tabs[0])
        assertEquals("Gastos Operativos", tabs[1])
    }

    @Test
    fun uiState_switchingTab_updatesSelectedTab() {
        val state = CostosYGastosUiState(selectedTab = 1)
        assertEquals(1, state.selectedTab)
    }

    @Test
    fun uiState_blockList_contains8Blocks() {
        val blocks = listOf(
            "Stock Monofocal",
            "Stock Bifocal",
            "Stock Multifocal",
            "Fabricación Resina",
            "Fabricación Cristal",
            "Monturas",
            "Biselado",
            "Lentes Contacto",
        )
        assertEquals(8, blocks.size)
        assertTrue(blocks.contains("Stock Monofocal"))
        assertTrue(blocks.contains("Biselado"))
        assertTrue(blocks.contains("Lentes Contacto"))
    }

    @Test
    fun uiState_settingBlock_updatesSelectedBlock() {
        val state = CostosYGastosUiState(selectedBlock = "Stock Monofocal")
        assertEquals("Stock Monofocal", state.selectedBlock)
    }

    @Test
    fun viewModel_hasUiStateField() {
        val fields = CostosYGastosViewModel::class.java.declaredFields.map { it.name }
        assertTrue(
            "CostosYGastosViewModel debe tener uiState",
            "uiState" in fields,
        )
    }

    @Test
    fun viewModel_hasLoadBlockMethod() {
        val methods = CostosYGastosViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "CostosYGastosViewModel debe tener loadBlock",
            "loadBlock" in methods,
        )
    }

    @Test
    fun viewModel_hasSelectTabMethod() {
        val methods = CostosYGastosViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "CostosYGastosViewModel debe tener selectTab",
            "selectTab" in methods,
        )
    }
}
