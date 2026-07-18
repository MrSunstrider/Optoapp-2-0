package com.example.optoapp.domain

import android.util.Log
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/** Minimal [SyncLogger] stub that delegates to android.util.Log for test assertions. */
private val testLogger = object : SyncLogger {
    override fun d(tag: String, msg: String) { Log.d(tag, msg) }
    override fun w(tag: String, msg: String, e: Throwable?) {
        if (e != null) Log.w(tag, msg, e) else Log.w(tag, msg)
    }
    override fun e(tag: String, msg: String, e: Throwable?) {
        if (e != null) Log.e(tag, msg, e) else Log.e(tag, msg)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NetworkRetryHelperTest {

    // ---- Bug 2: 429 rate-limit detection tests ----

    @Test
    fun `isRetryable returns true for HTTP 429 IOException`() {
        val helper = NetworkRetryHelper(testLogger)
        val ex = IOException("HTTP 429 Too Many Requests")
        assertTrue(helper.isRetryable(ex))
    }

    @Test
    fun `isRetryable returns true for transient message`() {
        val helper = NetworkRetryHelper(testLogger)
        val ex = IOException("too many requests")
        assertTrue(helper.isRetryable(ex))
    }

    @Test
    fun `isRetryable returns true for 429 in message`() {
        val helper = NetworkRetryHelper(testLogger)
        val ex = IOException("Request failed with status 429")
        assertTrue(helper.isRetryable(ex))
    }

    // ---- Bug 1: Missing delay in IOException branch ----

    @Test
    fun `retryNetwork delays before retrying transient IOException`() = runTest {
        var attempts = 0
        val helper = NetworkRetryHelper(testLogger)
        try {
            helper.retryNetwork("test") {
                attempts++
                throw IOException("connection timeout")
            }
        } catch (_: IOException) {
            // Expected after all retries exhausted
        }

        assertEquals("Should retry NETWORK_RETRY_ATTEMPTS times", 3, attempts)
        assertTrue(
            "Virtual time should reflect backoff delays (400 + 800 = 1200ms), got ${testScheduler.currentTime}ms",
            testScheduler.currentTime >= 1200L
        )
    }

    // ---- Bug 3: Generic Exception should NOT be retried ----

    @Test
    fun `retryNetwork does not retry non-IOException with transient-like message`() = runTest {
        var attempts = 0
        val helper = NetworkRetryHelper(testLogger)
        try {
            helper.retryNetwork("test") {
                attempts++
                throw RuntimeException("timeout occurred")
            }
        } catch (_: RuntimeException) {
            // Expected — should propagate immediately without retry
        }

        assertEquals(
            "Non-IOException should not be retried, even with transient-like message",
            1,
            attempts
        )
    }

    @Test
    fun `retryNetwork does not retry non-IOException exception at all`() = runTest {
        var attempts = 0
        val helper = NetworkRetryHelper(testLogger)
        try {
            helper.retryNetwork("test") {
                attempts++
                throw IllegalStateException("unexpected state")
            }
        } catch (_: IllegalStateException) {
            // Expected
        }

        assertEquals(1, attempts)
    }
}
