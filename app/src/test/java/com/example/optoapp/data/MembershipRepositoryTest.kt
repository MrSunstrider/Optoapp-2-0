package com.example.optoapp.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests de unidad para [MembershipRepository] usando MockK.
 *
 * Verifica early-returns cuando no hay sesión y flujos exitosos
 * con mock del cliente Supabase.
 */
class MembershipRepositoryTest {

    private lateinit var supabase: SupabaseClient
    private lateinit var auth: Auth
    private lateinit var repo: MembershipRepository

    @Before
    fun setUp() {
        mockkStatic("io.github.jan.supabase.auth.AuthKt")
        supabase = mockk()
        auth = mockk()
        every { supabase.auth } returns auth
        repo = MembershipRepository(supabase)
    }

    // ── Sin sesión — early returns ──────────────────────────────────────────

    @Test
    fun fetchMembershipsForCurrentUser_noSession_returnsEmptyList() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.fetchMembershipsForCurrentUser()

        assertTrue(result.isEmpty())
    }

    @Test
    fun fetchMembersForOptica_noSession_returnsEmptyList() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.fetchMembersForOptica("optica1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun assignRoleByEmail_noSession_returnsFailure() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.assignRoleByEmail("optica1", "test@test.com", "admin")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun createOpticaForCurrentUser_noSession_returnsFailure() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.createOpticaForCurrentUser("Mi Optica")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun fetchPlanSettings_noSession_returnsFailure() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.fetchPlanSettings("optica1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun updatePlanSettings_noSession_returnsFailure() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.updatePlanSettings(
            "optica1",
            PlanSettings(planCode = "free", maxOpticas = 1, maxPacientesPorOptica = 20, maxUsuariosPorOptica = 2, planStatus = "active")
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun fetchOpticaPlan_noSession_returnsNull() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.fetchOpticaPlan("optica1")

        assertNull(result)
    }

    @Test
    fun fetchOpticaLaboratorioSettings_noSession_returnsNull() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.fetchOpticaLaboratorioSettings("optica1")

        assertNull(result)
    }

    @Test
    fun fetchOpticaFiscalSettings_noSession_returnsNull() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.fetchOpticaFiscalSettings("optica1")

        assertNull(result)
    }

    @Test
    fun fetchOpticaHeaderSummary_noSession_returnsNull() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.fetchOpticaHeaderSummary("optica1")

        assertNull(result)
    }

    @Test
    fun updateOpticaFiscalSettings_noSession_returnsFailure() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.updateOpticaFiscalSettings(
            opticaId = "optica1",
            nombreComercial = "Optica Test",
            docTipo = "RUC",
            docNumero = "12345678901",
            razonSocial = "Test SRL",
            direccionFiscal = "Av Test 123",
            distritoCiudadDepartamento = "Lima",
            moneda = "PEN",
            pais = "PE",
            contactoWhatsappTelefono = "999888777"
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun updateOpticaLaboratorioSettings_noSession_returnsFailure() = runBlocking {
        every { auth.currentUserOrNull() } returns null

        val result = repo.updateOpticaLaboratorioSettings("optica1", "Lab Test", "contacto@lab.com")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
