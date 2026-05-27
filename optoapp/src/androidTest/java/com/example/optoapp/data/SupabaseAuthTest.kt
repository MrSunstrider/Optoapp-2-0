package com.example.optoapp.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.optoapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for Supabase Auth flows.
 *
 * These tests require a dedicated test Supabase project. Credentials are read
 * from [BuildConfig.SUPABASE_TEST_URL] and [BuildConfig.SUPABASE_TEST_ANON_KEY],
 * which are populated from `local.properties` (gitignored).
 *
 * If no credentials are available, all tests are skipped via
 * [org.junit.Assume] (reported as "ignored", not "failed").
 *
 * Each test run uses a unique email (timestamp-suffixed) to avoid collisions
 * with concurrent or previous runs. Cleanup happens in [tearDown] via the
 * Admin API — requires `SUPABASE_TEST_SERVICE_KEY` for user deletion.
 */
@RunWith(AndroidJUnit4::class)
class SupabaseAuthTest {

    private var supabase: SupabaseClient? = null
    private var testUserId: String? = null

    // Unique identifier for this test run — used in emails and cleanup.
    private val runId = "auth-test-${System.currentTimeMillis()}"
    private val testEmail = "$runId@optoapp-test.local"
    private val testPassword = "TestPass123!"

    @Before
    fun setUp() {
        // Guard: skip all tests if no test credentials are configured.
        val url = BuildConfig.SUPABASE_TEST_URL
        val anonKey = BuildConfig.SUPABASE_TEST_ANON_KEY
        assumeNotNull("SUPABASE_TEST_URL is not set — skipping Supabase integration tests", url)
        assumeNotNull("SUPABASE_TEST_ANON_KEY is not set — skipping Supabase integration tests", anonKey)
        assumeTrue("SUPABASE_TEST_URL is blank — skipping Supabase integration tests", url.isNotBlank())
        assumeTrue("SUPABASE_TEST_ANON_KEY is blank — skipping Supabase integration tests", anonKey.isNotBlank())

        supabase = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            install(Postgrest)
            install(Auth)
        }
    }

    @After
    fun tearDown() = runBlocking {
        // Attempt to clean up the test user via the Admin API.
        // This requires SUPABASE_TEST_SERVICE_KEY to be set in local.properties.
        val userId = testUserId ?: return@runBlocking
        val serviceKey = System.getProperty("SUPABASE_TEST_SERVICE_KEY", "")
        if (serviceKey.isNotBlank()) {
            try {
                val adminClient = createSupabaseClient(
                    supabaseUrl = BuildConfig.SUPABASE_TEST_URL,
                    supabaseKey = serviceKey
                ) {
                    install(Auth)
                }
                // Attempt admin user deletion (may fail if service key is wrong)
                runCatching { adminClient.auth.admin.deleteUser(userId) }
            } catch (_: Exception) {
                // Cleanup failure is non-fatal — the test already ran.
            }
        }
        supabase = null
    }

    @Test
    fun register_new_user_returns_session() = runBlocking {
        val client = supabase!!
        val result = client.auth.signUpWith(Email) {
            email = testEmail
            password = testPassword
        }
        assertNotNull("Session must be non-null after signup", result.session)
        assertNotNull("User must be non-null after signup", result.user)
        testUserId = result.user!!.id
    }

    @Test
    fun login_with_existing_user_restores_session() = runBlocking {
        val client = supabase!!

        // Arrange: register a user first
        val signUpResult = client.auth.signUpWith(Email) {
            email = testEmail
            password = testPassword
        }
        assertNotNull("Signup must succeed", signUpResult.session)
        testUserId = signUpResult.user!!.id

        // Act: login with the same credentials
        val signInResult = client.auth.signInWith(Email) {
            email = testEmail
            password = testPassword
        }

        // Assert
        assertNotNull("Session must be non-null after login", signInResult.session)
        assertNotNull("User must be non-null after login", signInResult.user)
        assertEquals("User ID must match registered user", testUserId, signInResult.user!!.id)
    }

    @Test
    fun login_with_wrong_password_throws_error() = runBlocking {
        val client = supabase!!

        // Register a user
        val signUpResult = client.auth.signUpWith(Email) {
            email = testEmail
            password = testPassword
        }
        assertNotNull("Signup must succeed", signUpResult.session)
        testUserId = signUpResult.user!!.id

        // Act & Assert: login with wrong password must throw
        val exception = org.junit.Assert.assertThrows(Exception::class.java) {
            runBlocking {
                client.auth.signInWith(Email) {
                    email = testEmail
                    password = "WrongPassword999!"
                }
            }
        }
        assertTrue("Error message should mention invalid credentials",
            exception.message?.contains("Invalid login credentials", ignoreCase = true) == true
        )
    }

    @Test
    fun logout_clears_session() = runBlocking {
        val client = supabase!!

        // Arrange: register and login
        client.auth.signUpWith(Email) {
            email = testEmail
            password = testPassword
        }
        val user = client.auth.currentUserOrNull()
        assertNotNull("User should be logged in after signup", user)
        testUserId = user!!.id

        // Act: logout
        client.auth.signOut()

        // Assert
        val sessionAfterLogout = client.auth.currentSessionOrNull()
        assertNull("Session should be null after logout", sessionAfterLogout)
    }
}
