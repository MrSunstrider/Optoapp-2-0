package com.example.optoapp.domain

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NetworkRetryHelperTest {

    private lateinit var logger: SyncLogger
    private lateinit var supabase: SupabaseClient
    private lateinit var auth: Auth

    @Before
    fun setUp() {
        mockkStatic("io.github.jan.supabase.auth.AuthKt")
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0

        logger = object : SyncLogger {
            override fun d(tag: String, msg: String) { android.util.Log.d(tag, msg) }
            override fun w(tag: String, msg: String, e: Throwable?) {
                if (e != null) android.util.Log.w(tag, msg, e) else android.util.Log.w(tag, msg)
            }
            override fun e(tag: String, msg: String, e: Throwable?) {
                if (e != null) android.util.Log.e(tag, msg, e) else android.util.Log.e(tag, msg)
            }
        }

        auth = mockk()
        coEvery { auth.refreshCurrentSession() } returns Unit

        supabase = mockk()
        every { supabase.auth } returns auth
    }

    private fun createHelper() = NetworkRetryHelper(logger, supabase)

    private fun mockJwtExpiredException(): RestException {
        val ex = mockk<RestException>()
        every { ex.statusCode } returns 401
        every { ex.error } returns """{"message":"JWT expired"}"""
        every { ex.message } returns "JWT expired"
        return ex
    }

    private fun mockNonJwt401Exception(): RestException {
        val ex = mockk<RestException>()
        every { ex.statusCode } returns 401
        every { ex.error } returns """{"message":"Unauthorized"}"""
        every { ex.message } returns "Unauthorized"
        return ex
    }

    // REQ-JWT-003/004: 401 JWT-expired refreshes session and retries once
    @Test
    fun `401 JWT-expired refreshes session and retries once successfully`() = runTest {
        var attempts = 0
        val jwtEx = mockJwtExpiredException()
        val helper = createHelper()

        helper.retryNetwork("test") {
            attempts++
            if (attempts == 1) throw jwtEx
            // second attempt succeeds
        }

        assertEquals(2, attempts)
        coVerify(exactly = 1) { auth.refreshCurrentSession() }
    }

    // REQ-JWT-003: 401 non-JWT does NOT refresh and propagates
    @Test
    fun `401 non-JWT does not refresh and propagates`() = runTest {
        val nonJwtEx = mockNonJwt401Exception()
        val helper = createHelper()

        try {
            helper.retryNetwork("test") { throw nonJwtEx }
            fail("Should have thrown")
        } catch (e: RestException) {
            // expected
        }

        coVerify(exactly = 0) { auth.refreshCurrentSession() }
    }

    // REQ-JWT-003: refresh failure propagates original exception
    @Test
    fun `JWT retry refresh failure propagates original exception`() = runTest {
        val jwtEx = mockJwtExpiredException()
        coEvery { auth.refreshCurrentSession() } throws IOException("network error")
        val helper = createHelper()

        try {
            helper.retryNetwork("test") { throw jwtEx }
            fail("Should have thrown")
        } catch (e: RestException) {
            // original JWT expired exception should propagate
        }
    }

    // REQ-JWT-003: retry after refresh also fails — propagates
    @Test
    fun `JWT retry after refresh fails again propagates`() = runTest {
        val jwtEx = mockJwtExpiredException()
        val helper = createHelper()

        try {
            helper.retryNetwork("test") { throw jwtEx }
            fail("Should have thrown")
        } catch (e: RestException) {
            // expected — both attempts fail
        }

        coVerify(exactly = 1) { auth.refreshCurrentSession() }
    }

    // Existing: isRetryable returns true for IOException
    @Test
    fun `isRetryable returns true for IOException`() {
        val helper = createHelper()
        assertTrue(helper.isRetryable(IOException("connection timeout")))
    }

    // Existing: isRetryable returns true for HTTP 429
    @Test
    fun `isRetryable returns true for HTTP 429`() {
        val helper = createHelper()
        val ex = mockk<RestException>()
        every { ex.statusCode } returns 429
        assertTrue(helper.isRetryable(ex))
    }

    // Existing: retryNetwork retries transient IOException
    @Test
    fun `retryNetwork retries transient IOException`() = runTest {
        var attempts = 0
        val helper = createHelper()
        try {
            helper.retryNetwork("test") {
                attempts++
                throw IOException("connection timeout")
            }
        } catch (_: IOException) {
            // Expected
        }
        assertEquals(3, attempts)
    }

    // Existing: non-IOException is not retried
    @Test
    fun `retryNetwork does not retry non-IOException`() = runTest {
        var attempts = 0
        val helper = createHelper()
        try {
            helper.retryNetwork("test") {
                attempts++
                throw IllegalStateException("boom")
            }
        } catch (_: IllegalStateException) {
            // Expected
        }
        assertEquals(1, attempts)
    }
}
