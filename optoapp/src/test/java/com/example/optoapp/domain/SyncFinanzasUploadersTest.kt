package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization tests for [SyncFinanzasUploaders] extracted from
 * SyncFinanzasUseCase.
 *
 * Currently tests the pure helper functions that moved with the uploaders.
 * The upload methods themselves require full dependency injection (Supabase,
 * Room, SyncStateTracker) and are tested at integration level.
 */
class SyncFinanzasUploadersTest {

    @Test
    fun uploaders_classConstants_accessible() {
        assertEquals("dispensaciones", SyncFinanzasUploaders.TABLE_DISPENSACIONES)
        assertEquals("pagos", SyncFinanzasUploaders.TABLE_PAGOS)
        assertEquals("servicios_extra", SyncFinanzasUploaders.TABLE_SERVICIOS)
        assertEquals(80, SyncFinanzasUploaders.UPSERT_BATCH_SIZE)
        assertEquals(3, SyncFinanzasUploaders.NETWORK_RETRY_ATTEMPTS)
    }

    // ── isTransientNetworkError ─────────────────────────────────────────────

    @Test
    fun isTransientNetworkError_timeoutMessage_returnsTrue() {
        assertTrue(SyncFinanzasUploaders.isTransientNetworkError(Exception("timeout")))
    }

    @Test
    fun isTransientNetworkError_timedOutMessage_returnsTrue() {
        assertTrue(SyncFinanzasUploaders.isTransientNetworkError(Exception("timed out")))
    }

    @Test
    fun isTransientNetworkError_connectFailed_returnsTrue() {
        assertTrue(SyncFinanzasUploaders.isTransientNetworkError(Exception("connect failed")))
    }

    @Test
    fun isTransientNetworkError_unableToResolveHost_returnsTrue() {
        assertTrue(SyncFinanzasUploaders.isTransientNetworkError(Exception("unable to resolve host")))
    }

    @Test
    fun isTransientNetworkError_networkUnreachable_returnsTrue() {
        assertTrue(SyncFinanzasUploaders.isTransientNetworkError(Exception("network is unreachable")))
    }

    @Test
    fun isTransientNetworkError_connectionReset_returnsTrue() {
        assertTrue(SyncFinanzasUploaders.isTransientNetworkError(Exception("connection reset")))
    }

    @Test
    fun isTransientNetworkError_http401_returnsFalse() {
        assertFalse(SyncFinanzasUploaders.isTransientNetworkError(Exception("401 Unauthorized")))
    }

    @Test
    fun isTransientNetworkError_http500_returnsFalse() {
        assertFalse(SyncFinanzasUploaders.isTransientNetworkError(Exception("500 Internal Server Error")))
    }

    @Test
    fun isTransientNetworkError_nullMessage_returnsFalse() {
        assertFalse(SyncFinanzasUploaders.isTransientNetworkError(Exception() as Exception))
    }

    @Test
    fun isTransientNetworkError_irrelevantMessage_returnsFalse() {
        assertFalse(SyncFinanzasUploaders.isTransientNetworkError(Exception("some random error")))
    }
}
