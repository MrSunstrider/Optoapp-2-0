package com.example.optoapp.data

import com.example.optoapp.data.membership.MembershipDataSource
import com.example.optoapp.data.membership.OpticaQueryHelper
import com.example.optoapp.data.membership.OpticaSettingsDataSource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Approval + behavior tests for [MembershipRepository] catch-block refactoring.
 *
 * Verifies:
 * - Pre-condition checks (no session) still return early
 */
class MembershipRepositoryErrorTest {

    private lateinit var repo: MembershipRepository
    private val supabase: SupabaseClient = mockk(relaxed = true)

    @Before
    fun setUp() {
        // auth is an extension property on SupabaseClient — mockkStatic to intercept
        mockkStatic("io.github.jan.supabase.auth.AuthKt")
        val authMock = mockk<Auth>(relaxed = true)
        every { authMock.currentUserOrNull() } returns null
        every { any<SupabaseClient>().auth } returns authMock
        val opticaQueryHelper = OpticaQueryHelper(supabase)
        val membershipDataSource = MembershipDataSource(supabase, opticaQueryHelper)
        val opticaSettingsDataSource = OpticaSettingsDataSource(supabase)
        repo = MembershipRepository(membershipDataSource, opticaSettingsDataSource)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── Pre-condition: no authenticated user → early return ───────────

    @Test
    fun `createOpticaForCurrentUser no session returns failure`() = runTest {
        val result = repo.createOpticaForCurrentUser("Test Optica")

        assertTrue("Expected failure but got $result", result.isFailure)
        assertEquals("Sin sesión", (result.exceptionOrNull() as? IllegalStateException)?.message)
    }

    @Test
    fun `assignRoleByEmail no session returns failure`() = runTest {
        val result = repo.assignRoleByEmail("opt_abc", "test@test.com", "admin")

        assertTrue("Expected failure but got $result", result.isFailure)
        assertEquals("Sin sesión", (result.exceptionOrNull() as? IllegalStateException)?.message)
    }

    @Test
    fun `fetchPlanSettings no session returns failure`() = runTest {
        val result = repo.fetchPlanSettings("opt_abc")

        assertTrue("Expected failure but got $result", result.isFailure)
        assertEquals("Sin sesión", (result.exceptionOrNull() as? IllegalStateException)?.message)
    }

    @Test
    fun `updatePlanSettings no session returns failure`() = runTest {
        val result = repo.updatePlanSettings(
            "opt_abc", PlanSettings("free", 1, 20, 2, "active")
        )

        assertTrue("Expected failure but got $result", result.isFailure)
        assertEquals("Sin sesión", (result.exceptionOrNull() as? IllegalStateException)?.message)
    }

    @Test
    fun `updateOpticaFiscalSettings no session returns failure`() = runTest {
        val result = repo.updateOpticaFiscalSettings(
            "opt_abc", "Optica", "RUC", "123", "Razon", "Dir",
            "Distrito", "PEN", "PE", "123456789"
        )

        assertTrue("Expected failure but got $result", result.isFailure)
        assertEquals("Sin sesión", (result.exceptionOrNull() as? IllegalStateException)?.message)
    }

    @Test
    fun `updateOpticaLaboratorioSettings no session returns failure`() = runTest {
        val result = repo.updateOpticaLaboratorioSettings("opt_abc", "Lab", "Contact")

        assertTrue("Expected failure but got $result", result.isFailure)
        assertEquals("Sin sesión", (result.exceptionOrNull() as? IllegalStateException)?.message)
    }

    // ── fetch methods: no session returns empty default ──────────────

    @Test
    fun `fetchMembershipsForCurrentUser no session returns empty`() = runTest {
        val result = repo.fetchMembershipsForCurrentUser()

        assertTrue("Expected empty list but got $result", result.isEmpty())
    }

    @Test
    fun `fetchMembersForOptica no session returns empty`() = runTest {
        val result = repo.fetchMembersForOptica("opt_abc")

        assertTrue("Expected empty list but got $result", result.isEmpty())
    }
}
