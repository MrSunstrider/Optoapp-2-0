package com.example.optoapp.util

import com.example.optoapp.data.BackgroundError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDiagnosticsBackgroundErrorStoreTest {

    private class FakeStore(initial: List<BackgroundError> = emptyList()) : BackgroundErrorStore {
        var saved: List<BackgroundError> = initial
        override fun load(): List<BackgroundError> = saved
        override fun save(errors: List<BackgroundError>) {
            saved = errors
        }
    }

    @Test
    fun `codec round trip preserves source message and timestamp`() {
        val errors = listOf(
            BackgroundError("sync:finanzas", "HTTP 400 code=23514", 1_700_000_000_000L),
            BackgroundError("auth", "JWT refresh failed", 1_700_000_001_000L),
        )
        val decoded = BackgroundErrorCodec.decode(BackgroundErrorCodec.encode(errors))
        assertEquals(errors, decoded)
    }

    @Test
    fun `codec survives multiline messages`() {
        val errors = listOf(BackgroundError("sync:pagos", "line1\nline2\tline3", 42L))
        val decoded = BackgroundErrorCodec.decode(BackgroundErrorCodec.encode(errors))
        assertEquals(1, decoded.size)
        assertEquals("line1\nline2\tline3", decoded[0].message)
        assertEquals(42L, decoded[0].timestampMs)
    }

    @Test
    fun `codec decodes blank or corrupt payload as empty`() {
        assertTrue(BackgroundErrorCodec.decode(null).isEmpty())
        assertTrue(BackgroundErrorCodec.decode("").isEmpty())
        assertTrue(BackgroundErrorCodec.decode("garbage-without-separators").isEmpty())
    }

    @Test
    fun `collector restores persisted errors on construction`() {
        val persisted = listOf(BackgroundError("sync:finanzas", "23514 pagos_monto_chk", 7L))
        val collector = BackgroundErrorCollector(FakeStore(persisted))
        assertEquals(persisted, collector.errors.value)
    }

    @Test
    fun `clear also wipes persisted errors`() {
        val store = FakeStore(listOf(BackgroundError("sync", "old", 1L)))
        val collector = BackgroundErrorCollector(store)
        collector.clear()
        assertTrue(collector.errors.value.isEmpty())
        assertTrue(store.saved.isEmpty())
    }
}
