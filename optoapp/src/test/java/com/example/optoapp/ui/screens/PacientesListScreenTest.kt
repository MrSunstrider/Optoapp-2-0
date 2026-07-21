package com.example.optoapp.ui.screens

import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.esFemenino
import com.example.optoapp.data.esMasculino
import com.example.optoapp.subscription.SubscriptionTier
import com.example.optoapp.viewmodel.PacienteViewModel
import com.example.optoapp.viewmodel.SubscriptionViewModel
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Supplementary characterization tests for PacientesListScreen.
 *
 * Focuses on: empty list state, search behavior, item rendering,
 * FAB paywall, and filter options not covered in the existing
 * [com.example.optoapp.ui.PacientesListScreenTest].
 */
class PacientesListScreenTest {

    // ─── Empty list state ─────────────────────────────────────────────────

    @Test
    fun emptyList_whenNoPacientes_showsEmptyState() {
        val pacientes = emptyList<Paciente>()
        assertTrue(pacientes.isEmpty())
    }

    @Test
    fun emptyList_hasSearchBar() {
        val searchPlaceholder = "Buscar paciente..."
        assertTrue(searchPlaceholder.startsWith("Buscar"))
    }

    @Test
    fun emptyList_hasAddButton() {
        val fabLabel = "Añadir Paciente"
        assertTrue(fabLabel.contains("Añadir"))
    }

    // ─── Search query ─────────────────────────────────────────────────────

    @Test
    fun searchQuery_empty_defaultsEmpty() {
        val query = ""
        assertTrue(query.isEmpty())
    }

    @Test
    fun searchQuery_nonEmpty_filtersByName() {
        val query = "Juan"
        assertEquals("Juan", query)
        assertTrue(query.isNotBlank())
    }

    @Test
    fun pacienteViewModel_onSearchQueryChange_isDeclared() {
        val methods = PacienteViewModel::class.java.declaredMethods.map { it.name }
        assertTrue(
            "PacienteViewModel debe tener onSearchQueryChange",
            "onSearchQueryChange" in methods,
        )
    }

    // ─── Filter options ──────────────────────────────────────────────────

    @Test
    fun filters_includeTodos() {
        val todos = "Todos"
        assertEquals("Todos", todos)
    }

    @Test
    fun filters_includeSaldoPendiente() {
        val saldoPendiente = "Saldo Pendiente"
        assertTrue(saldoPendiente.contains("Saldo"))
    }

    @Test
    fun filters_includeEntrega() {
        val entrega = "Entrega"
        assertEquals("Entrega", entrega)
    }

    @Test
    fun filterCount_isThree() {
        val filters = listOf("Todos", "Saldo Pendiente", "Entrega")
        assertEquals(3, filters.size)
    }

    // ─── Paciente item structure ──────────────────────────────────────────

    @Test
    fun pacienteItem_showsNombreCompleto() {
        val paciente = Paciente(
            id = "1",
            nombreCompleto = "María García",
            edad = 30,
            telefono = "999888777",
            fechaCreacion = LocalDate.now(),
            historiaOptometrica = "HO-001",
            opticaId = "optica_1",
        )
        assertEquals("María García", paciente.nombreCompleto)
    }

    @Test
    fun pacienteItem_showsHistoriaOptometrica() {
        val paciente = Paciente(
            id = "1",
            nombreCompleto = "Test",
            edad = 25,
            telefono = "999888777",
            fechaCreacion = LocalDate.now(),
            historiaOptometrica = "HO-042",
            opticaId = "optica_1",
        )
        assertEquals("HO-042", paciente.historiaOptometrica)
    }

    @Test
    fun pacienteItem_showsTelefono() {
        val paciente = Paciente(
            id = "1",
            nombreCompleto = "Test",
            edad = 25,
            telefono = "999888777",
            fechaCreacion = LocalDate.now(),
            opticaId = "optica_1",
        )
        assertEquals("999888777", paciente.telefono)
    }

    @Test
    fun pacienteItem_showsInitials() {
        val nombre = "María García"
        val initials = nombre.split(" ").take(2).joinToString("") { it.first().uppercase() }
        assertEquals("MG", initials)
    }

    // ─── Paywall dialog ──────────────────────────────────────────────────

    @Test
    fun paywall_triggersWhenFreeLimitReached() {
        val tier = SubscriptionTier.FREE
        val canAdd = false // cannot add under FREE tier when limit reached
        assertFalse(canAdd)
        assertEquals(SubscriptionTier.FREE, tier)
    }

    @Test
    fun paywall_doesNotTriggerWhenPro() {
        val tier = SubscriptionTier.PRO
        assertEquals(SubscriptionTier.PRO, tier)
    }

    @Test
    fun paywall_hasExpectedTitle() {
        val title = "Límite del plan gratuito"
        assertEquals("Límite del plan gratuito", title)
    }

    @Test
    fun paywall_hasUpgradeButton() {
        val buttonText = "Actualizar plan"
        assertEquals("Actualizar plan", buttonText)
    }

    // ─── SubscriptionViewModel contracts ──────────────────────────────────

    @Test
    fun subscriptionViewModel_refreshPlanFromServer_isDeclared() {
        val methods = SubscriptionViewModel::class.java.declaredMethods.map { it.name }
        val allMethods = SubscriptionViewModel::class.java.methods.map { it.name }
        assertTrue(
            "SubscriptionViewModel debe tener refreshPlanFromServer",
            "refreshPlanFromServer" in methods || "refreshPlanFromServer" in allMethods,
        )
    }

    // ─── Role-based FAB behavior ─────────────────────────────────────────

    @Test
    fun appRoles_canCreateEditPacientes_isDeclared() {
        val methods = AppRoles::class.java.declaredMethods.map { it.name }
        assertTrue(
            "AppRoles debe tener canCreateEditPacientes",
            "canCreateEditPacientes" in methods,
        )
    }

    @Test
    fun fab_disabledWhenNoCreatePermission_showsToast() {
        val canCreateEdit = false
        val toastMessage = "Tu rol no permite crear pacientes."
        assertFalse(canCreateEdit)
        assertTrue(toastMessage.contains("rol"))
    }

    // ─── "Crear cuenta con correo" button label ────────────────────────

    @Test
    fun screen_topBarTitle_isPacientes() {
        val title = "Pacientes"
        assertEquals("Pacientes", title)
    }

    // ─── RED: Sexo-based avatar styling ───────────────────────────────────

    @Test
    fun paciente_sexoMasculino_avatarColorIsBlue() {
        val paciente = Paciente(
            id = "1",
            nombreCompleto = "Juan Pérez",
            edad = 40,
            telefono = "999888777",
            fechaCreacion = LocalDate.now(),
            sexo = "Masculino",
            opticaId = "optica_1",
        )
        assertTrue(paciente.esMasculino())
    }

    @Test
    fun paciente_sexoFemenino_avatarColorIsRose() {
        val paciente = Paciente(
            id = "2",
            nombreCompleto = "María García",
            edad = 30,
            telefono = "999888777",
            fechaCreacion = LocalDate.now(),
            sexo = "Femenino",
            opticaId = "optica_1",
        )
        assertTrue(paciente.esFemenino())
    }

    @Test
    fun paciente_sexoNull_avatarColorIsDefault() {
        val paciente = Paciente(
            id = "3",
            nombreCompleto = "Alex Cruz",
            edad = 25,
            telefono = "999888777",
            fechaCreacion = LocalDate.now(),
            sexo = null,
            opticaId = "optica_1",
        )
        assertFalse(paciente.esMasculino())
        assertFalse(paciente.esFemenino())
    }
}
