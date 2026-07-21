package com.example.optoapp.viewmodel

import com.example.optoapp.util.DispensacionStockHelper
import org.junit.Assert.*
import org.junit.Test

/**
 * Characterization tests for DispensacionViewModel.
 *
 * Verifies injection contract: DispensacionViewModel must receive
 * DispensacionStockHelper via constructor injection (6.3.5).
 *
 * Uses Java reflection to avoid Hilt/Android dependency in unit tests,
 * following the same pattern as SettingsViewModelCharacterizationTest.
 */
class DispensacionViewModelCharacterizationTest {

    @Test
    fun `viewModel constructor receives DispensacionStockHelper`() {
        val stockHelperClass = DispensacionStockHelper::class.java
        val hasStockHelper = DispensacionViewModel::class.java.constructors.any { ctor ->
            ctor.parameterTypes.contains(stockHelperClass)
        }
        assertTrue(
            "DispensacionViewModel debe recibir DispensacionStockHelper como parámetro de constructor",
            hasStockHelper,
        )
    }
}
