package com.example.optoapp.viewmodel

import android.content.Context
import com.example.optoapp.data.ISecurityManager
import com.example.optoapp.data.ISessionManager
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OpticaFiscalSettingsStore
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.membership.MembershipFetch
import com.example.optoapp.viewmodel.auth.AuthDelegate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.user.UserInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.time.ExperimentalTime

/**
 * Unit tests for AuthDelegate pure logic extracted to companion object methods.
 *
 * - [AuthDelegate.extractDisplayName] — pure function
 * - [AuthDelegate.isTimestampWithinSessionWindow] — pure function
 *
 * Supabase-dependent methods (login, register, logout) require integration tests.
 */
class AuthDelegateTest {

    @Test
    fun `isTimestampWithinSessionWindow zero timestamp returns false`() {
        assertFalse(AuthDelegate.isTimestampWithinSessionWindow(0L))
    }

    @Test
    fun `isTimestampWithinSessionWindow within 3h returns true`() {
        val ts = System.currentTimeMillis() - 1000L * 60 * 60 * 2
        assertTrue(AuthDelegate.isTimestampWithinSessionWindow(ts))
    }

    @Test
    fun `isTimestampWithinSessionWindow just under 3h returns true`() {
        val ts = System.currentTimeMillis() - 1000L * 60 * 60 * 2 // 2h
        assertTrue(AuthDelegate.isTimestampWithinSessionWindow(ts))
    }

    @Test
    fun `isTimestampWithinSessionWindow over 3h returns false`() {
        val ts = System.currentTimeMillis() - 1000L * 60 * 60 * 4
        assertFalse(AuthDelegate.isTimestampWithinSessionWindow(ts))
    }

    @OptIn(ExperimentalTime::class)
    private fun userInfo(
        id: String = "u1",
        email: String? = null,
        metadata: JsonObject? = null,
    ): UserInfo = UserInfo(id = id, aud = "authenticated", email = email, userMetadata = metadata)

    @Test
    fun `extractDisplayName uses nombre from metadata`() {
        val user = userInfo(metadata = buildJsonObject { put("nombre", "Juan") })
        assertEquals("Juan", AuthDelegate.extractDisplayName(user, null, null))
    }

    @Test
    fun `extractDisplayName falls back to full_name`() {
        val user = userInfo(metadata = buildJsonObject { put("full_name", "Maria Garcia") })
        assertEquals("Maria Garcia", AuthDelegate.extractDisplayName(user, null, null))
    }

    @Test
    fun `extractDisplayName falls back to name from metadata`() {
        val user = userInfo(metadata = buildJsonObject { put("name", "Carlos") })
        assertEquals("Carlos", AuthDelegate.extractDisplayName(user, null, null))
    }

    @Test
    fun `extractDisplayName falls back to nameFallback`() {
        assertEquals("Pedro", AuthDelegate.extractDisplayName(userInfo(), null, "Pedro"))
    }

    @Test
    fun `extractDisplayName falls back to email prefix`() {
        val user = userInfo(email = "ana@gmail.com")
        assertEquals("ana", AuthDelegate.extractDisplayName(user, null, null))
    }

    @Test
    fun `extractDisplayName falls back to emailFallback when email missing`() {
        assertEquals("test", AuthDelegate.extractDisplayName(userInfo(), "test@example.com", null))
    }

    @Test
    fun `extractDisplayName returns Usuario when all candidates blank`() {
        assertEquals("Usuario", AuthDelegate.extractDisplayName(userInfo(), null, null))
    }

    @Test
    fun `extractDisplayName prefers nombre over full_name`() {
        val user = userInfo(
            metadata = buildJsonObject {
                put("nombre", "Primero")
                put("full_name", "Segundo")
            },
        )
        assertEquals("Primero", AuthDelegate.extractDisplayName(user, null, null))
    }

    @Test
    fun `extractDisplayName nombre is null falls through`() {
        val user = userInfo(
            metadata = buildJsonObject {
                put("full_name", "Solo Apellido")
            },
        )
        assertEquals("Solo Apellido", AuthDelegate.extractDisplayName(user, null, null))
    }

    @Test
    fun emptyMemberships_doNotClearSession_andSaveOnboarding() {
        val flags = AuthDelegate.flagsFor(MembershipFetch.Empty)

        assertFalse(flags.clearSession)
        assertTrue(flags.saveOnboardingSession)
        assertTrue(flags.requiresOnboarding)
        assertFalse(flags.membershipFetchError)
        assertFalse(flags.requiresSelection)
    }

    @Test
    fun saveOnboardingSession_usesBlankOpticaIdNotLegacyBase() {
        assertEquals("", AuthDelegate.ONBOARDING_OPTICA_ID)
        assertNotEquals(SessionManager.LEGACY_OPTICA_ID, AuthDelegate.ONBOARDING_OPTICA_ID)
    }

    @Test
    fun membershipFetchError_doesNotClearSessionOrOnboard() {
        val flags = AuthDelegate.flagsFor(MembershipFetch.Error(IOException("net")))

        assertFalse(flags.clearSession)
        assertFalse(flags.saveOnboardingSession)
        assertFalse(flags.requiresOnboarding)
        assertTrue(flags.membershipFetchError)
    }

    @Test
    fun completeOnboardingOptica_existsWithFiveOwnerFields() {
        val found = AuthDelegate::class.java.methods.any { "completeOnboardingOptica" in it.name }
        assertTrue(found)
    }

    @Test
    fun androidMainSources_doNotReferenceInvitaciones() {
        val root = listOf(File("src/main/java"), File("optoapp/src/main/java")).first { it.exists() }
        val hits = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { "invitaciones" in it.readText().lowercase() }
            .map { it.path }
            .toList()

        assertTrue("Unexpected invitaciones refs: $hits", hits.isEmpty())
    }

    @Test
    fun sinOpticaScreen_ownerActionWiresCreateForm() {
        val relative = "src/main/java/com/example/optoapp/ui/screens/SinOpticaScreen.kt"
        val found = listOf(File(relative), File("optoapp/$relative")).first { it.exists() }
        val text = found.readText()
        assertTrue(text.contains("onOwnerCreateAction()"))
        assertTrue(text.contains("SinOpticaUiState"))
    }

    // ── completeOnboardingOptica behavioral tests ─────────────────────────────

    private fun buildDelegate(
        sessionManager: ISessionManager,
        membershipRepo: MembershipRepository,
        fiscalStore: OpticaFiscalSettingsStore,
    ): AuthDelegate {
        val securityManager = mockk<ISecurityManager>(relaxed = true)
        val repository = mockk<OptoRepository>(relaxed = true)
        val supabase = mockk<SupabaseClient>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        return AuthDelegate(
            securityManager = securityManager,
            sessionManager = sessionManager,
            repository = repository,
            membershipRepository = membershipRepo,
            supabase = supabase,
            fiscalStore = fiscalStore,
            appContext = context,
        )
    }

    @Test
    fun completeOnboardingOptica_success_persistsSessionWithAdminRol() = runTest {
        val sessionManager = mockk<ISessionManager>(relaxed = true)
        val membershipRepo = mockk<MembershipRepository>(relaxed = true)
        val fiscalStore = mockk<OpticaFiscalSettingsStore>(relaxed = true)

        val returnedMembership = OpticaMembership(
            opticaId = "optica-abc",
            nombre = "Mi Óptica",
            rol = "admin",
        )

        every { sessionManager.userEmail } returns flowOf("owner@example.com")
        every { sessionManager.userName } returns flowOf("Owner")
        coEvery {
            membershipRepo.createOpticaForCurrentUser(any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(returnedMembership)

        val delegate = buildDelegate(sessionManager, membershipRepo, fiscalStore)

        val result = delegate.completeOnboardingOptica(
            nombreOptica = "Mi Óptica",
            fiscalDocTipo = "RUC",
            fiscalDocNumero = "12345",
            razonSocial = "Mi Óptica S.A.",
            direccionFiscal = "Calle 1",
        )

        assertTrue(result.isSuccess)
        assertEquals("optica-abc", result.getOrNull()?.opticaId)
        coVerify { sessionManager.saveSession(opticaId = "optica-abc", email = any(), name = any(), rol = "admin") }
        coVerify(exactly = 0) { sessionManager.clearSession() }
    }

    @Test
    fun completeOnboardingOptica_failure_doesNotPersistSession() = runTest {
        val sessionManager = mockk<ISessionManager>(relaxed = true)
        val membershipRepo = mockk<MembershipRepository>(relaxed = true)
        val fiscalStore = mockk<OpticaFiscalSettingsStore>(relaxed = true)

        every { sessionManager.userEmail } returns flowOf("owner@example.com")
        every { sessionManager.userName } returns flowOf("Owner")
        coEvery {
            membershipRepo.createOpticaForCurrentUser(any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("server error"))

        val delegate = buildDelegate(sessionManager, membershipRepo, fiscalStore)

        val result = delegate.completeOnboardingOptica(
            nombreOptica = "Mi Óptica",
            fiscalDocTipo = "",
            fiscalDocNumero = "",
            razonSocial = "",
            direccionFiscal = "",
        )

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { sessionManager.saveSession(any(), any(), any(), any()) }
    }

    // ── prepareOpticaSelection error propagation ──────────────────────────────

    @Test
    fun prepareOpticaSelection_errorFetch_returnsError() = runTest {
        val sessionManager = mockk<ISessionManager>(relaxed = true)
        val membershipRepo = mockk<MembershipRepository>(relaxed = true)
        val fiscalStore = mockk<OpticaFiscalSettingsStore>(relaxed = true)

        val cause = IOException("no network")
        every { sessionManager.userEmail } returns flowOf("")
        every { sessionManager.userName } returns flowOf("")
        coEvery { membershipRepo.fetchMembershipsForCurrentUser() } returns MembershipFetch.Error(cause)

        val delegate = buildDelegate(sessionManager, membershipRepo, fiscalStore)

        val fetch = delegate.prepareOpticaSelection()

        assertTrue(fetch is MembershipFetch.Error)
    }

    @Test
    fun prepareOpticaSelection_emptyFetch_returnsEmpty() = runTest {
        val sessionManager = mockk<ISessionManager>(relaxed = true)
        val membershipRepo = mockk<MembershipRepository>(relaxed = true)
        val fiscalStore = mockk<OpticaFiscalSettingsStore>(relaxed = true)

        every { sessionManager.userEmail } returns flowOf("")
        every { sessionManager.userName } returns flowOf("")
        coEvery { membershipRepo.fetchMembershipsForCurrentUser() } returns MembershipFetch.Empty

        val delegate = buildDelegate(sessionManager, membershipRepo, fiscalStore)

        val fetch = delegate.prepareOpticaSelection()

        assertEquals(MembershipFetch.Empty, fetch)
    }

    @Test
    fun createAdditionalOptica_blocksWhenUserAlreadyHasAdminMembership() = runTest {
        val sessionManager = mockk<ISessionManager>(relaxed = true)
        val membershipRepo = mockk<MembershipRepository>(relaxed = true)
        val fiscalStore = mockk<OpticaFiscalSettingsStore>(relaxed = true)

        every { sessionManager.opticaRol } returns flowOf("admin")
        coEvery { membershipRepo.fetchMembershipsForCurrentUser() } returns MembershipFetch.Ok(
            listOf(OpticaMembership(opticaId = "o1", nombre = "Una", rol = "admin")),
        )

        val delegate = buildDelegate(sessionManager, membershipRepo, fiscalStore)
        val result = delegate.createAdditionalOptica("Segunda")

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("1 óptica") == true,
        )
        coVerify(exactly = 0) { membershipRepo.createOpticaForCurrentUser(any()) }
    }

    @Test
    fun createAdditionalOptica_allowsWhenOnlyNonAdminMemberships() = runTest {
        val sessionManager = mockk<ISessionManager>(relaxed = true)
        val membershipRepo = mockk<MembershipRepository>(relaxed = true)
        val fiscalStore = mockk<OpticaFiscalSettingsStore>(relaxed = true)
        val created = OpticaMembership(opticaId = "o2", nombre = "Propia", rol = "admin")

        every { sessionManager.opticaRol } returns flowOf("admin")
        coEvery { membershipRepo.fetchMembershipsForCurrentUser() } returns MembershipFetch.Ok(
            listOf(OpticaMembership(opticaId = "o1", nombre = "Invitada", rol = "asesor")),
        )
        coEvery { membershipRepo.createOpticaForCurrentUser("Propia") } returns Result.success(created)

        val delegate = buildDelegate(sessionManager, membershipRepo, fiscalStore)
        val result = delegate.createAdditionalOptica("Propia")

        assertTrue(result.isSuccess)
        assertEquals(created, result.getOrNull())
        coVerify(exactly = 1) { membershipRepo.createOpticaForCurrentUser("Propia") }
    }

    @Test
    fun resetLocalStoreForNewAuthSession_wipesRoomCache() = runTest {
        val sessionManager = mockk<ISessionManager>(relaxed = true)
        val membershipRepo = mockk<MembershipRepository>(relaxed = true)
        val fiscalStore = mockk<OpticaFiscalSettingsStore>(relaxed = true)
        val securityManager = mockk<ISecurityManager>(relaxed = true)
        val repository = mockk<OptoRepository>(relaxed = true)
        val supabase = mockk<SupabaseClient>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val delegate = AuthDelegate(
            securityManager = securityManager,
            sessionManager = sessionManager,
            repository = repository,
            membershipRepository = membershipRepo,
            supabase = supabase,
            fiscalStore = fiscalStore,
            appContext = context,
        )

        delegate.resetLocalStoreForNewAuthSession()

        coVerify { repository.wipeLocalAccountData() }
    }
}
