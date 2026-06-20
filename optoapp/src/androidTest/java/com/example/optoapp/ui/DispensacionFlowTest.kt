package com.example.optoapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.optoapp.testing.TestTags
import com.example.optoapp.ui.components.DropdownField
import com.example.optoapp.ui.components.OptoTextField
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

    // ── Form Fields (main dispensación form) ──────────────────────────────

    @Test
    fun montoTotalField_isDisplayed() {
        composeTestRule.setContent {
            OptoTextField(
                value = "350.00",
                onValueChange = {},
                label = "Monto Total",
                keyboardType = KeyboardType.Decimal
            )
        }
        composeTestRule.onNodeWithText("Monto Total").assertIsDisplayed()
    }

    @Test
    fun montoTotalField_acceptsValue() {
        var value = ""
        composeTestRule.setContent {
            OptoTextField(
                value = value,
                onValueChange = { value = it },
                label = "Monto Total",
                keyboardType = KeyboardType.Decimal
            )
        }
        composeTestRule.onNodeWithText("Monto Total").performTextInput("450.00")
        assertEquals("450.00", value)
    }

    @Test
    fun estadoEntregaDropdown_showsOptions() {
        composeTestRule.setContent {
            DropdownField(
                label = "Estado de Entrega",
                selected = "Pendiente",
                options = listOf("Pendiente", "Entregado")
            ) { }
        }
        composeTestRule.onNodeWithText("Estado de Entrega").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pendiente").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TipoLenteDropdownHarness(
        selected: String = "Lejos",
        onSelected: (String) -> Unit = {}
    ) {
        var expanded by remember { mutableStateOf(false) }
        val options = listOf("Lejos", "Cerca", "Bifocal", "Progresivo", "Ocupacional")
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tipo de Lente", fontWeight = FontWeight.Bold)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selected,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Lente") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("tipoLenteDropdown")
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun tipoLenteDropdown_showsDefaultOption() {
        composeTestRule.setContent { TipoLenteDropdownHarness() }
        composeTestRule.onNodeWithTag("tipoLenteDropdown").assertIsDisplayed()
    }

    @Test
    fun tipoLenteDropdown_showsOptions_whenExpanded() {
        composeTestRule.setContent { TipoLenteDropdownHarness() }
        // Click to expand
        composeTestRule.onNodeWithTag("tipoLenteDropdown").performClick()
        // Lejos is the default selected value, so it appears in both the field and the dropdown
        // Check for unique options that don't appear in the collapsed field
        composeTestRule.onNodeWithText("Cerca").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bifocal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Progresivo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ocupacional").assertIsDisplayed()
    }

    @Test
    fun materialLenteDropdown_renders() {
        composeTestRule.setContent {
            DropdownField(
                label = "Material del Lente",
                selected = "Orgánico",
                options = listOf("Orgánico", "Policarbonato", "Trivex", "Mineral")
            ) { }
        }
        composeTestRule.onNodeWithText("Material del Lente").assertIsDisplayed()
        composeTestRule.onNodeWithText("Orgánico").assertIsDisplayed()
    }

    @Test
    fun financieraInfoSection_showsAllLabels() {
        val uiState = DispensacionUiState(
            montoTotal = "500.00",
            estadoEntrega = "Pendiente"
        )
        composeTestRule.setContent {
            com.example.optoapp.ui.screens.FinancieraInfoSection(
                uiState = uiState,
                onUpdate = {},
                onAddPago = {},
                onUpdatePago = {},
                onRemovePago = {}
            )
        }
        composeTestRule.onNodeWithText("Información Financiera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monto Total").assertIsDisplayed()
        composeTestRule.onNodeWithText("Historial de Abonos").assertIsDisplayed()
        composeTestRule.onNodeWithText("SALDO RESTANTE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Estado de Entrega").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar Abono").assertIsDisplayed()
    }
}
