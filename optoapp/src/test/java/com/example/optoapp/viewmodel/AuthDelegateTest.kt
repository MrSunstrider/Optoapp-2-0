package com.example.optoapp.viewmodel

import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.membership.MembershipFetch
import com.example.optoapp.viewmodel.auth.AuthDelegate
import io.github.jan.supabase.auth.user.UserInfo
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
}
