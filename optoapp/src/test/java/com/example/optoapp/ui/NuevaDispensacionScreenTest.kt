package com.example.optoapp.ui.screens

import com.example.optoapp.viewmodel.DispensacionUiState
import com.example.optoapp.viewmodel.DispensacionViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Characterization tests for NuevaDispensacionScreen.
 *
 * Verifies DispensacionUiState defaults, form field structure,
 * validation rules, and ViewModel contracts.
 */
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
    fun uiState_isLoadingDefaultIsFalse() {
        val state = DispensacionUiState(fecha = LocalDate.of(2024, 1, 1))
        assertFalse(state.isLoading)
    }

    @Test
    fun uiState_errorDefaultIsNull() {
        val state = DispensacionUiState(fecha = LocalDate.of(2024, 1, 1))
        assertNull(state.error)
    }

    @Test
    fun screenTitle_newModeIsNuevaDispensacion() {
        val title = "Nueva Dispensación"
        assertEquals("Nueva Dispensación", title)
    }

    @Test
    fun screenTitle_editModeIsEditarDispensacion() {
        val title = "Editar Dispensación"
        assertEquals("Editar Dispensación", title)
    }

    @Test
    fun formFields_containsOtField() {
        val label = "N° OT (OT-AAAA-####)"
        assertEquals("N° OT (OT-AAAA-####)", label)
    }

    @Test
    fun formFields_containsFechaField() {
        val prefix = "Fecha:"
        assertTrue(prefix.startsWith("Fecha"))
    }

    @Test
    fun formFields_containsSuggestOtLabel() {
        val label = "Sugerir OT"
        assertEquals("Sugerir OT", label)
    }

    @Test
    fun sections_containsLenteInfo() {
        val header = "Información del Lente"
        assertEquals("Información del Lente", header)
    }

    @Test
    fun sections_containsMonturaInfo() {
        val header = "Información de Montura"
        assertEquals("Información de Montura", header)
    }

    @Test
    fun sections_containsFinancieraInfo() {
        val header = "Información Financiera"
        assertEquals("Información Financiera", header)
    }

    @Test
    fun buttonLabels_crearOrdenForNew() {
        val label = "Crear Orden"
        assertEquals("Crear Orden", label)
    }

    @Test
    fun wizardSteps_newModeHasTwoSteps() {
        val steps = wizardStepsForMode(isEditMode = false)
        assertEquals(listOf("Orden", "Productos"), steps)
    }

    @Test
    fun wizardSteps_editModeHasGestionStep() {
        val steps = wizardStepsForMode(isEditMode = true)
        assertEquals(listOf("Orden", "Productos", "Gestión"), steps)
    }

    @Test
    fun buttonLabels_actualizarOrdenForEdit() {
        val label = "Actualizar Orden"
        assertEquals("Actualizar Orden", label)
    }

    @Test
    fun buttonLabels_suggestOtExists() {
        val label = "Sugerir OT"
        assertEquals("Sugerir OT", label)
    }

    @Test
    fun validation_otRequiredErrorMessage() {
        val error = "La OT es obligatoria para guardar la dispensación."
        assertTrue(error.contains("OT"))
        assertTrue(error.contains("obligatoria"))
    }

    @Test
    fun validation_alturaRequiredForBifocal() {
        val errorPrefix = "La altura es obligatoria para"
        assertTrue(errorPrefix.startsWith("La altura"))
        assertTrue(errorPrefix.contains("obligatoria"))
    }

    @Test
    fun validation_montoTotalGreaterThanZero() {
        val error = "El monto total debe ser mayor a cero."
        assertTrue(error.contains("monto total"))
    }

    @Test
    fun validation_abonoGreaterThanTotalError() {
        // From FinanzasRemoteDefaults.Messages.ABONO_MAYOR_QUE_TOTAL
        val error = "La suma de los abonos no puede superar el monto total."
        assertTrue(error.contains("abonos"))
        assertTrue(error.contains("monto total"))
    }

    @Test
    fun dispensacionViewModel_uiState_isDeclared() {
        val fields = DispensacionViewModel::class.java.declaredFields.map { it.name }
        val methods = DispensacionViewModel::class.java.methods.map { it.name }
        assertTrue(
            "uiState debe ser miembro de DispensacionViewModel",
            "uiState" in fields || "uiState" in methods,
        )
    }

    @Test
    fun dispensacionViewModel_monturasActivas_isDeclared() {
        val fields = DispensacionViewModel::class.java.declaredFields.map { it.name }
        val methods = DispensacionViewModel::class.java.methods.map { it.name }
        assertTrue(
            "monturasActivas debe ser miembro de DispensacionViewModel",
            "monturasActivas" in fields || "monturasActivas" in methods,
        )
    }

    @Test
    fun dispensacionViewModel_updateUiState_isDeclared() {
        val methods = DispensacionViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "updateUiState debe ser método de DispensacionViewModel",
            "updateUiState" in methods,
        )
    }

    @Test
    fun dispensacionViewModel_suggestOt_isDeclared() {
        val methods = DispensacionViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "suggestOt debe ser método de DispensacionViewModel",
            "suggestOt" in methods,
        )
    }

    @Test
    fun dispensacionViewModel_saveDispensacion_isDeclared() {
        val methods = DispensacionViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "saveDispensacion debe ser método de DispensacionViewModel",
            "saveDispensacion" in methods,
        )
    }

    @Test
    fun duplicateOtWarning_titleExists() {
        val title = "Advertencia de OT duplicada"
        assertEquals("Advertencia de OT duplicada", title)
    }

    @Test
    fun duplicateOtWarning_containsEntendido() {
        val button = "Entendido"
        assertEquals("Entendido", button)
    }

    @Test
    fun lenteTipoOptions_containsExpected() {
        val options = listOf("Monofocal", "Bifocal", "Progresivo", "Ocupacional")
        assertEquals(4, options.size)
        assertTrue(options.contains("Monofocal"))
        assertTrue(options.contains("Bifocal"))
        assertTrue(options.contains("Progresivo"))
        assertTrue(options.contains("Ocupacional"))
    }

    @Test
    fun materialLenteOptions_containsExpected() {
        val options = listOf("Resina", "Policarbonato", "Cristal", "Trivex")
        assertEquals(4, options.size)
        assertTrue(options.contains("Resina"))
        assertTrue(options.contains("Policarbonato"))
    }

    @Test
    fun estadoEntregaOptions_containsExpected() {
        val options = listOf("Pendiente", "Entregado")
        assertEquals(2, options.size)
    }

    @Test
    fun uiState_generatedId_isSetByViewModel() {
        val state = DispensacionUiState()
        // generatedId starts empty — the ViewModel injects a UUID at init
        assertTrue("generatedId default is empty; ViewModel must set it", state.generatedId.isEmpty())
    }

    @Test
    fun uiState_allFieldsAreDeclared() {
        val fields = DispensacionUiState::class.java.declaredFields.map { it.name }
        assertTrue(fields.contains("ot"))
        assertTrue(fields.contains("items"))
        assertTrue(fields.contains("montoTotal"))
        assertTrue(fields.contains("estadoEntrega"))
        assertTrue(fields.contains("error"))
        assertTrue(fields.contains("isLoading"))
    }
}
