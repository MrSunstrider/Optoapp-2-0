package com.example.optoapp.fakes

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.FilterConfiguration
import io.github.jan.supabase.auth.SignUpResult
import io.github.jan.supabase.auth.SignInResult
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PostgrestApi
import io.github.jan.supabase.postgrest.PostgrestConfig
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake [SupabaseClient] for offline / unit tests.
 *
 * Simulates auth responses (success / failure), sync operations, and network
 * errors so that tests can verify offline behaviour without hitting a real
 * Supabase project.
 *
 * Configuration:
 * - [networkErrorEnabled]: when `true` every operation throws [IOException].
 * - [syncEnabled]: when `false` [syncPatients] / [syncEvaluations] return zero.
 * - [authShouldFail]: when `true` [fakeSignUp] / [fakeSignIn] throw.
 */
class FakeSupabaseClient(
    var networkErrorEnabled: Boolean = false,
    var syncEnabled: Boolean = true,
    var authShouldFail: Boolean = false
) {
    // ── Auth state ──────────────────────────────────────────────────────────

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession

    /** Users "registered" in this fake. */
    private val registeredUsers = mutableMapOf<String, String>() // email → password

    data class FakeSession(
        override val accessToken: String,
        override val refreshToken: String,
        override val user: UserInfo
    ) : UserSession

    data class FakeUserInfo(
        override val id: String,
        override val email: String?,
        override val userMetadata: Map<String, Any>? = null
    ) : UserInfo

    // ── Auth operations ─────────────────────────────────────────────────────

    suspend fun fakeSignUp(email: String, password: String): SignUpResult {
        maybeThrowNetworkError("signUp")
        registeredUsers[email] = password
        val user = FakeUserInfo(
            id = "fake-${email.hashCode().toLong().ushr(1)}",
            email = email
        )
        val session = FakeSession(
            accessToken = "fake-at-${System.currentTimeMillis()}",
            refreshToken = "fake-rt-${System.currentTimeMillis()}",
            user = user
        )
        _currentSession.value = session
        return SignUpResult(session = session, user = user)
    }

    suspend fun fakeSignIn(email: String, password: String): SignInResult {
        maybeThrowNetworkError("signIn")
        val storedPassword = registeredUsers[email]
        if (storedPassword == null || storedPassword != password) {
            throw FakeAuthException("Invalid login credentials")
        }
        val user = FakeUserInfo(
            id = "fake-${email.hashCode().toLong().ushr(1)}",
            email = email
        )
        val session = FakeSession(
            accessToken = "fake-at-${System.currentTimeMillis()}",
            refreshToken = "fake-rt-${System.currentTimeMillis()}",
            user = user
        )
        _currentSession.value = session
        return SignInResult(session = session, user = user)
    }

    suspend fun fakeSignOut() {
        _currentSession.value = null
    }

    /** Returns null when not logged in. */
    suspend fun fakeCurrentSessionOrNull(): UserSession? = _currentSession.value

    // ── Sync operations ─────────────────────────────────────────────────────

    /** Fake number of patients "synced" to remote. */
    var syncedPatientCount: Int = 0
        private set

    /** Fake number of evaluations "synced" to remote. */
    var syncedEvaluationCount: Int = 0
        private set

    suspend fun syncPatients(opticaId: String): Int {
        maybeThrowNetworkError("syncPatients")
        if (!syncEnabled) return 0
        syncedPatientCount++
        return 1
    }

    suspend fun syncEvaluations(opticaId: String): Int {
        maybeThrowNetworkError("syncEvaluations")
        if (!syncEnabled) return 0
        syncedEvaluationCount++
        return 1
    }

    /** Resets all counters and registered users. */
    fun reset() {
        _currentSession.value = null
        registeredUsers.clear()
        syncedPatientCount = 0
        syncedEvaluationCount = 0
        networkErrorEnabled = false
        syncEnabled = true
        authShouldFail = false
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private suspend fun maybeThrowNetworkError(op: String) {
        if (networkErrorEnabled) {
            throw java.io.IOException("Fake network error during $op")
        }
        if (authShouldFail && op in listOf("signUp", "signIn")) {
            throw FakeAuthException("Fake auth failure")
        }
    }
}

/** Exception thrown by [FakeSupabaseClient] to simulate auth errors. */
class FakeAuthException(message: String) : Exception(message)

// ── Placeholder types matching supabase-kt signatures ──────────────────────
// These are used by test code that expects real supabase-kt return types.
// The fake does NOT extend SupabaseClient — it's a standalone fake.
// Tests that need the real SupabaseClient should use the credential-guarded
// integration tests instead.
