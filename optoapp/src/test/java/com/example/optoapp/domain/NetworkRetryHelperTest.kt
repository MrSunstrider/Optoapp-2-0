package com.example.optoapp.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NetworkRetryHelperTest {

    // ---- Bug 2: 429 rate-limit detection tests ----

    @Test
    fun `isTransientNetworkError returns true for HTTP 429 status code`() {
        val helper = NetworkRetryHelper()
        val ex = IOException("HTTP 429 Too Many Requests")
        assertTrue(helper.isTransientNetworkError(ex))
    }

    @Test
    fun `isTransientNetworkError returns true for too many requests message`() {
        val helper = NetworkRetryHelper()
        val ex = IOException("too many requests")
        assertTrue(helper.isTransientNetworkError(ex))
    }

    @Test
    fun `isTransientNetworkError returns true for 429 in message`() {
        val helper = NetworkRetryHelper()
        val ex = IOException("Request failed with status 429")
        assertTrue(helper.isTransientNetworkError(ex))
    }

    // ---- Bug 1: Missing delay in IOException branch ----

    @Test
    fun `retryNetwork delays before retrying transient IOException`() = runTest {
        var attempts = 0
        val helper = NetworkRetryHelper()
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
        val helper = NetworkRetryHelper()
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
        val helper = NetworkRetryHelper()
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
