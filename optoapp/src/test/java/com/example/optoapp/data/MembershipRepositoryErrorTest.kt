package com.example.optoapp.data

import android.util.Log
import com.example.optoapp.data.membership.MembershipDataSource
import com.example.optoapp.data.membership.OpticaQueryHelper
import com.example.optoapp.data.membership.OpticaSettingsDataSource
import com.example.optoapp.data.opticasettings.OpticaSettingsDao
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
        mockkStatic("android.util.Log")
        every { Log.w(any<String>(), any<String>()) } returns 0
        val authMock = mockk<Auth>(relaxed = true)
        every { authMock.currentUserOrNull() } returns null
        every { any<SupabaseClient>().auth } returns authMock
        val opticaQueryHelper = OpticaQueryHelper(supabase)
        val membershipDataSource = MembershipDataSource(supabase, opticaQueryHelper)
        val opticaSettingsDataSource = OpticaSettingsDataSource(supabase)
        repo = MembershipRepository(membershipDataSource, opticaSettingsDataSource, mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

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
    fun `updateOpticaFiscalSettings no session returns failure`() = runTest {
        val result = repo.updateOpticaFiscalSettings(
            "opt_abc",
            "Optica",
            "RUC",
            "123",
            "Razon",
            "Dir",
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

    @Test
    fun `fetchMembershipsForCurrentUser no session returns empty`() = runTest {
        val result = repo.fetchMembershipsForCurrentUser()

        assertTrue("Expected empty fetch but got $result", result is com.example.optoapp.data.membership.MembershipFetch.Empty)
    }

    @Test
    fun `fetchMembersForOptica no session returns empty`() = runTest {
        val result = repo.fetchMembersForOptica("opt_abc")

        assertTrue("Expected empty list but got $result", result.isEmpty())
    }
}
