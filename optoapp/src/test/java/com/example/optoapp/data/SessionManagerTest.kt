package com.example.optoapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SessionManager "Recordar Cuenta" behavior.
 *
 * Since [SessionManager] creates [EncryptedSharedPreferences] at construction time
 * (requires real Android Keystore), we test the ISessionManager contract via
 * [EncryptedSessionManagerFake] — an in-memory fake that mirrors the correct
 * encrypted-storage behavior.
 *
 * Run on device to verify actual EncryptedSharedPreferences behavior:
 *   ./gradlew :optoapp:connectedAndroidTest
 */
class SessionManagerTest {

    // ─── Fake that mirrors correct encrypted-storage behavior ───────────────

    /**
     * In-memory fake implementing ISessionManager.
     */
    private class EncryptedSessionManagerFake : ISessionManager {
        // Simulates plain DataStore
        private val plainStore = mutableMapOf<String, String>()

        private val _isLoggedIn = MutableStateFlow(false)
        private val _opticaId = MutableStateFlow(SessionManager.LEGACY_OPTICA_ID)
        private val _opticaRol = MutableStateFlow("admin")
        private val _pinHasBeenSet = MutableStateFlow(false)
        private val _isPinRequired = MutableStateFlow(false)

        override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()
        override val opticaId: Flow<String> = _opticaId.asStateFlow()
        override val opticaRol: Flow<String> = _opticaRol.asStateFlow()
        override val userEmail: Flow<String> = MutableStateFlow("")
        override val userName: Flow<String> = MutableStateFlow("")
        override val userTimeZone: Flow<String?> = MutableStateFlow(null)
        override val lastLoginTimestamp: Flow<Long> = MutableStateFlow(0L)
        override val pinHasBeenSet: Flow<Boolean> = _pinHasBeenSet.asStateFlow()
        override val isPinRequired: Flow<Boolean> = _isPinRequired.asStateFlow()

        override suspend fun saveSession(opticaId: String, email: String, name: String, rol: String) {
            _opticaId.value = opticaId
            _opticaRol.value = rol
            _isLoggedIn.value = true
        }

        override suspend fun clearSession() {
            _isLoggedIn.value = false
            _opticaId.value = SessionManager.LEGACY_OPTICA_ID
            _opticaRol.value = "admin"
        }

        override suspend fun setPinRequired(required: Boolean) {
            _isPinRequired.value = required
        }

        // ── Remembered Email (plain store — email is less sensitive) ─────

        override suspend fun saveRememberedEmail(email: String) {
            plainStore["pref_remembered_email"] = email
        }

        override suspend fun getRememberedEmail(): String {
            return plainStore["pref_remembered_email"] ?: ""
        }

        override suspend fun clearRememberedEmail() {
            plainStore.remove("pref_remembered_email")
        }
    }

    // ─── Email Tests ────────────────────────────────────────────────────────

    @Test
    fun `saveRememberedEmail stores email that can be retrieved`() = runTest {
        val sm = EncryptedSessionManagerFake()
        sm.saveRememberedEmail("test@example.com")

        assertEquals("test@example.com", sm.getRememberedEmail())
    }

    @Test
    fun `clearRememberedEmail removes stored email`() = runTest {
        val sm = EncryptedSessionManagerFake()
        sm.saveRememberedEmail("to_clear@example.com")
        sm.clearRememberedEmail()

        assertEquals("", sm.getRememberedEmail())
    }

    // ─── Session Lifecycle Tests ────────────────────────────────────────────

    @Test
    fun `isLoggedIn is false by default`() = runTest {
        val sm = EncryptedSessionManagerFake()
        assertFalse(sm.isLoggedIn.first())
    }

    @Test
    fun `saveSession sets isLoggedIn to true`() = runTest {
        val sm = EncryptedSessionManagerFake()
        sm.saveSession("optica_1", "user@test.com", "Test User", "admin")

        assertTrue(sm.isLoggedIn.first())
        assertEquals("optica_1", sm.opticaId.first())
    }

    @Test
    fun `clearSession resets isLoggedIn to false`() = runTest {
        val sm = EncryptedSessionManagerFake()
        sm.saveSession("optica_1", "user@test.com", "Test User", "admin")
        sm.clearSession()

        assertFalse(sm.isLoggedIn.first())
        assertEquals(SessionManager.LEGACY_OPTICA_ID, sm.opticaId.first())
    }

    @Test
    fun `clearSession resets opticaRol to admin`() = runTest {
        val sm = EncryptedSessionManagerFake()
        sm.saveSession("optica_1", "user@test.com", "Test User", "gerente")
        sm.clearSession()

        assertEquals("admin", sm.opticaRol.first())
    }
}
