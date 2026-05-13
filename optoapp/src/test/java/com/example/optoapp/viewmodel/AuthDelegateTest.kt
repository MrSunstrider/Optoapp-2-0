package com.example.optoapp.viewmodel

import com.example.optoapp.viewmodel.auth.AuthDelegate
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for AuthDelegate pure logic extracted to companion object methods.
 *
 * - [AuthDelegate.extractDisplayName] — pure function
 * - [AuthDelegate.isTimestampWithin24h] — pure function
 *
 * Supabase-dependent methods (login, register, logout) require integration tests.
 */
class AuthDelegateTest {

    // ─── isTimestampWithin24h ─────────────────────────────────────────────

    @Test
    fun `isTimestampWithin24h zero timestamp returns false`() {
        assertFalse(AuthDelegate.isTimestampWithin24h(0L))
    }

    @Test
    fun `isTimestampWithin24h within 24h returns true`() {
        val ts = System.currentTimeMillis() - 1000L * 60 * 60 * 2
        assertTrue(AuthDelegate.isTimestampWithin24h(ts))
    }

    @Test
    fun `isTimestampWithin24h just under 24h returns true`() {
        val ts = System.currentTimeMillis() - 1000L * 60 * 60 * 23
        assertTrue(AuthDelegate.isTimestampWithin24h(ts))
    }

    @Test
    fun `isTimestampWithin24h over 24h returns false`() {
        val ts = System.currentTimeMillis() - 1000L * 60 * 60 * 25
        assertFalse(AuthDelegate.isTimestampWithin24h(ts))
    }

    // ─── extractDisplayName ───────────────────────────────────────────────

    private fun userInfo(
        id: String = "u1",
        email: String? = null,
        metadata: JsonObject? = null
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
        val user = userInfo(metadata = buildJsonObject {
            put("nombre", "Primero")
            put("full_name", "Segundo")
        })
        assertEquals("Primero", AuthDelegate.extractDisplayName(user, null, null))
    }

    @Test
    fun `extractDisplayName nombre is null falls through`() {
        val user = userInfo(metadata = buildJsonObject {
            put("full_name", "Solo Apellido")
        })
        assertEquals("Solo Apellido", AuthDelegate.extractDisplayName(user, null, null))
    }
}
