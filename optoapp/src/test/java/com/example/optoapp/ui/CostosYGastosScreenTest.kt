package com.example.optoapp.ui.screens

import com.example.optoapp.ui.navigation.CostosTab
import com.example.optoapp.viewmodel.COST_BLOCKS
import com.example.optoapp.viewmodel.CostosYGastosUiState
import com.example.optoapp.viewmodel.CostosYGastosViewModel
import com.example.optoapp.viewmodel.blockFilter
import com.example.optoapp.viewmodel.blockToFilter
import org.junit.Assert.*
import org.junit.Test

class CostosYGastosScreenTest {

    @Test
    fun uiState_defaults_tabIsGastosFirst() {
        val state = CostosYGastosUiState()
        assertEquals(CostosTab.GASTOS.ordinal, state.selectedTab)
        assertEquals(0, CostosTab.GASTOS.ordinal)
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
    fun uiState_switchingTab_updatesSelectedTab() {
        val state = CostosYGastosUiState(selectedTab = 1)
        assertEquals(1, state.selectedTab)
    }

    @Test
    fun costBlocks_mapToTheirStockOrFabricationFilter() {
        assertEquals(listOf("stock", "stock", "stock", "fabricacion", "fabricacion"), COST_BLOCKS.map(::blockToFilter))
        assertEquals("Monofocal", blockFilter("Stock Monofocal").tipoLente)
        assertEquals("Bifocal", blockFilter("Stock Bifocal").tipoLente)
        assertEquals("Resina", blockFilter("Fabricación Resina").materialContains)
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
