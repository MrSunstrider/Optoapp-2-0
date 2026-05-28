package com.example.optoapp.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.dispensacion.PagosSection
import com.example.optoapp.viewmodel.DispensacionUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Compose UI tests for the Dispensación (Optical Dispensing) form.
 *
 * Tests [PagosSection] (payment information, abonos, totals) and verifies
 * key form fields via [TestTags]. [DispensacionUiState] is a simple data
 * class instantiated directly — no ViewModel required.
 *
 * @see com.example.optoapp.ui.screens.NuevaDispensacionScreen
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DispensacionFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── PagosSection (Información Financiera) ────────────────────────────────

    @Test
    fun pagosSection_showsTitle() {
        composeTestRule.setContent {
            PagosSection(
                uiState = DispensacionUiState(),
                onUpdate = {},
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }

        composeTestRule.onNodeWithText("Información Financiera").assertIsDisplayed()
    }

    @Test
    fun pagosSection_montoTotalField_isDisplayed() {
        composeTestRule.setContent {
            PagosSection(
                uiState = DispensacionUiState(),
                onUpdate = {},
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.DISPENSACION_MONTO_TOTAL).assertIsDisplayed()
    }

    @Test
    fun pagosSection_montoTotalField_acceptsDecimalInput() {
        var state = DispensacionUiState()
        composeTestRule.setContent {
            PagosSection(
                uiState = state,
                onUpdate = { state = it },
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }

        composeTestRule.onNodeWithTag(TestTags.DISPENSACION_MONTO_TOTAL)
            .performTextInput("350.00")

        assert(state.montoTotal == "350.00") { "Expected montoTotal '350.00' but got '${state.montoTotal}'" }
    }

    @Test
    fun pagosSection_showsHistorialDeAbonos() {
        composeTestRule.setContent {
            PagosSection(
                uiState = DispensacionUiState(),
                onUpdate = {},
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }

        composeTestRule.onNodeWithText("Historial de Abonos").assertIsDisplayed()
    }

    @Test
    fun pagosSection_showsAgregarAbonoButton() {
        composeTestRule.setContent {
            PagosSection(
                uiState = DispensacionUiState(),
                onUpdate = {},
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }

        composeTestRule.onNodeWithText("Agregar Abono").assertIsDisplayed()
    }

    @Test
    fun pagosSection_showsSaldoRestante() {
        composeTestRule.setContent {
            PagosSection(
                uiState = DispensacionUiState(),
                onUpdate = {},
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }

        composeTestRule.onNodeWithText("SALDO RESTANTE").assertIsDisplayed()
    }

    @Test
    fun pagosSection_showsEstadoEntregaDropdown() {
        composeTestRule.setContent {
            PagosSection(
                uiState = DispensacionUiState(),
                onUpdate = {},
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }

        composeTestRule.onNodeWithText("Estado de Entrega").assertIsDisplayed()
    }

    @Test
    fun pagosSection_saldoCalculatesCorrectly() {
        val uiState = DispensacionUiState(
            montoTotal = "500.00",
            pagos = listOf(
                com.example.optoapp.data.Pago(
                    id = "pago-1",
                    dispensacionId = "disp-1",
                    fecha = LocalDate.now(),
                    tipo = "Contado",
                    monto = 200.0,
                    metodoPago = "Efectivo",
                    opticaId = "test-optica",
                    nota = ""
                )
            )
        )

        composeTestRule.setContent {
            PagosSection(
                uiState = uiState,
                onUpdate = {},
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }

        // Saldo should be 500 - 200 = 300
        composeTestRule.onNodeWithText("s/. 300.00").assertIsDisplayed()
    }

    // ── OT Field ──────────────────────────────────────────────────────────────

    @Test
    fun otField_isDisplayed() {
        // The OT field uses OptoTextField with DISPENSACION_OT_FIELD tag.
        // We test it independently using the OptoTextField component.
        composeTestRule.setContent {
            com.example.optoapp.ui.components.OptoTextField(
                value = "",
                onValueChange = {},
                label = "N° OT (OT-AAAA-####)",
                modifier = Modifier.testTag(TestTags.DISPENSACION_OT_FIELD)
            )
        }

        composeTestRule.onNodeWithTag(TestTags.DISPENSACION_OT_FIELD).assertIsDisplayed()
    }

    @Test
    fun otField_acceptsInput() {
        var otValue = ""
        composeTestRule.setContent {
            com.example.optoapp.ui.components.OptoTextField(
                value = otValue,
                onValueChange = { otValue = it },
                label = "N° OT (OT-AAAA-####)",
                modifier = Modifier.testTag(TestTags.DISPENSACION_OT_FIELD)
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TestTags.DISPENSACION_OT_FIELD)
            .performTextInput("OT-2026-0001")

        assertEquals("OT-2026-0001", otValue)
    }
}
