package com.example.optoapp.ui.screens.costos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costolc.CostoLcEntity
import com.example.optoapp.util.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MatricesTabsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val biselado = CostoBiseladoEntity(
        id = "b1", opticaId = "o1", material = "Resina", tipoAro = "semi_aire",
        stockOFabricacion = "fabricacion", serie = 2, altoIndice = "1.67", costoPorPar = 12.5,
        vigenteDesde = "2026-03-01",
    )

    private val lc = CostoLcEntity(
        id = "l1", opticaId = "o1", tipoLc = "graduado", materialLc = "Hydrogel",
        modalidad = "mensual", costoUnitario = 45.0, vigenteDesde = "2026-03-01",
    )

    @Test
    fun biseladoRowShowsEveryMatrixColumn() {
        composeTestRule.setContent {
            BiseladoTab(filas = listOf(biselado), puedeEditar = true, onEditar = {}, onAgregar = {})
        }

        listOf("Resina", "Semi al aire", "Fabricación", "Serie 2", "Índice 1.67", MoneyFormatter.format(12.5)).forEach {
            composeTestRule.onNodeWithText(it, substring = true, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun lcRowShowsEveryMatrixColumn() {
        composeTestRule.setContent {
            LcTab(filas = listOf(lc), puedeEditar = true, onEditar = {}, onAgregar = {})
        }

        listOf("Graduado", "Hydrogel", "Mensual", MoneyFormatter.format(45.0)).forEach {
            composeTestRule.onNodeWithText(it, substring = true, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun tappingThePriceSideOfABiseladoRowOpensItsEdit() {
        var editada: CostoBiseladoEntity? = null
        composeTestRule.setContent {
            BiseladoTab(filas = listOf(biselado), puedeEditar = true, onEditar = { editada = it }, onAgregar = {})
        }

        composeTestRule.onNodeWithText(MoneyFormatter.format(12.5), useUnmergedTree = true).performClick()

        assertEquals(biselado, editada)
    }

    @Test
    fun tappingTheDescriptionOfAnLcRowOpensItsEdit() {
        var editada: CostoLcEntity? = null
        composeTestRule.setContent {
            LcTab(filas = listOf(lc), puedeEditar = true, onEditar = { editada = it }, onAgregar = {})
        }

        composeTestRule.onNodeWithText("Hydrogel", substring = true, useUnmergedTree = true).performClick()

        assertEquals(lc, editada)
    }

    @Test
    fun functionalTabsNeverShowTheComingSoonPlaceholder() {
        composeTestRule.setContent {
            BiseladoTab(filas = listOf(biselado), puedeEditar = true, onEditar = {}, onAgregar = {})
            LcTab(filas = emptyList(), puedeEditar = true, onEditar = {}, onAgregar = {})
        }

        composeTestRule.onNodeWithText("próximamente", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun anEmptyMatrixShowsTheEmptyStateWithTheAddPriceAction() {
        var agregar = 0
        composeTestRule.setContent {
            LcTab(filas = emptyList(), puedeEditar = true, onEditar = {}, onAgregar = { agregar++ })
        }

        composeTestRule.onNodeWithText("Aún no hay precios cargados", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar precio").performClick()

        assertEquals(1, agregar)
    }

    @Test
    fun readOnlyRolesGetNoAddOrEditAffordances() {
        var editada = false
        composeTestRule.setContent {
            BiseladoTab(filas = listOf(biselado), puedeEditar = false, onEditar = { editada = true }, onAgregar = {})
        }

        composeTestRule.onNodeWithText("Agregar precio").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Agregar precio").assertDoesNotExist()
        composeTestRule.onNodeWithText("Solo administradores y gerentes", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(MoneyFormatter.format(12.5), useUnmergedTree = true).performClick()

        assertFalse(editada)
    }

    @Test
    fun readOnlyEmptyMatrixHidesTheAddPriceAction() {
        composeTestRule.setContent {
            LcTab(filas = emptyList(), puedeEditar = false, onEditar = {}, onAgregar = {})
        }

        composeTestRule.onNodeWithText("Aún no hay precios cargados", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar precio").assertDoesNotExist()
    }
}
