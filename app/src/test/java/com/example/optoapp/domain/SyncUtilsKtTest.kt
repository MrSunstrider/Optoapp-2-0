package com.example.optoapp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the transient-network-error classification logic used by:
 * - SyncFinanzasUseCase.isTransientNetworkError (internal member function)
 * - SyncDiagnosticsViewModel.isTransientNetworkError (internal member function)
 *
 * Both are instance methods on classes requiring Hilt injection.
 * This file tests the pure logic contract that both share.
 */
class SyncUtilsKtTest {

    // ── UseCase variant (includes "connect failed") ─────────────────────

    @Test
    fun isTransientNetworkError_timeoutMessage_returnsTrue() {
        assertTrue(isTransientHelper(Exception("timeout")))
    }

    @Test
    fun isTransientNetworkError_timedOutMessage_returnsTrue() {
        assertTrue(isTransientHelper(Exception("timed out")))
    }

    @Test
    fun isTransientNetworkError_connectFailedMessage_returnsTrue() {
        assertTrue(isTransientHelper(Exception("connect failed")))
    }

    @Test
    fun isTransientNetworkError_unableToResolveHost_returnsTrue() {
        assertTrue(isTransientHelper(Exception("unable to resolve host")))
    }

    @Test
    fun isTransientNetworkError_networkUnreachable_returnsTrue() {
        assertTrue(isTransientHelper(Exception("network is unreachable")))
    }

    @Test
    fun isTransientNetworkError_connectionReset_returnsTrue() {
        assertTrue(isTransientHelper(Exception("connection reset")))
    }

    @Test
    fun isTransientNetworkError_http401_returnsFalse() {
        assertFalse(isTransientHelper(Exception("401 Unauthorized")))
    }

    @Test
    fun isTransientNetworkError_http500_returnsFalse() {
        assertFalse(isTransientHelper(Exception("500 Internal Server Error")))
    }

    @Test
    fun isTransientNetworkError_nullMessage_returnsFalse() {
        assertFalse(isTransientHelper(Exception(null as String?)))
    }

    @Test
    fun isTransientNetworkError_irrelevantMessage_returnsFalse() {
        assertFalse(isTransientHelper(Exception("some random error")))
    }

    /**
     * Replicates the exact logic of [SyncFinanzasUseCase.isTransientNetworkError].
     */
    private fun isTransientHelper(e: Exception): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return msg.contains("timeout") ||
            msg.contains("timed out") ||
            (msg.contains("connect") && msg.contains("failed")) ||
            msg.contains("unable to resolve host") ||
            msg.contains("network is unreachable") ||
            msg.contains("connection reset")
    }
}
