package com.example.optoapp.domain.sync

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Tests for [ConflictHelper]. */
class ConflictHelperTest {

    // ===== normalizeTimestamp tests (1.1–1.4) =====

    @Test
    fun `normalizeTimestamp truncates microseconds with colon offset`() {
        assertEquals(
            "2026-07-25T02:32:19.469Z",
            ConflictHelper.normalizeTimestamp("2026-07-25T02:32:19.469712+00:00"),
        )
    }

    @Test
    fun `normalizeTimestamp truncates microseconds without colon offset`() {
        assertEquals(
            "2026-07-25T02:32:19.469Z",
            ConflictHelper.normalizeTimestamp("2026-07-25T02:32:19.469712+0000"),
        )
    }

    @Test
    fun `normalizeTimestamp is idempotent for millisecond Z timestamps`() {
        assertEquals(
            "2026-07-25T02:32:19.469Z",
            ConflictHelper.normalizeTimestamp("2026-07-25T02:32:19.469Z"),
        )
    }

    @Test
    fun `normalizeTimestamp passes through timestamps without fractional seconds`() {
        assertEquals(
            "2026-07-25T02:32:19Z",
            ConflictHelper.normalizeTimestamp("2026-07-25T02:32:19Z"),
        )
    }

    @Test
    fun `same instant with Z and +0000 are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "2026-06-15T04:33:25+00:00",
            ),
        )
    }

    @Test
    fun `same instant with and without millis are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25.000Z",
                "2026-06-15T04:33:25Z",
            ),
        )
    }

    @Test
    fun `same instant with micros and Z are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25.123456Z",
                "2026-06-15T04:33:25.123Z",
            ),
        )
    }

    @Test
    fun `local newer returns true`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T05:00:00Z",
                "2026-06-15T04:00:00Z",
            ),
        )
    }

    @Test
    fun `local older returns false`() {
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:00:00Z",
                "2026-06-15T05:00:00Z",
            ),
        )
    }

    @Test
    fun `exact same string returns true`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "2026-06-15T04:33:25Z",
            ),
        )
    }

    @Test
    fun `different days local newer`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-16T00:00:00Z",
                "2026-06-15T23:59:59Z",
            ),
        )
    }

    @Test
    fun `local with +0500 vs remote Z same instant`() {
        // 2026-06-15T09:33:25+05:00 = 2026-06-15T04:33:25Z
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T09:33:25+05:00",
                "2026-06-15T04:33:25Z",
            ),
        )
    }

    @Test
    fun `unparseable falls back to string comparison`() {
        // "invalid" > "2026-06-15T04:33:25Z" as strings (i > 2)
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "invalid",
                "2026-06-15T04:33:25Z",
            ),
        )
    }

    @Test
    fun `both unparseable uses string comparison`() {
        // "abc" >= "xyz" is false as strings (a < x)
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual("abc", "xyz"),
        )
    }

    @Test
    fun `local nullish string vs remote valid`() {
        // "null" > "2026-06-15T04:33:25Z" as strings (n > 2)
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "null",
                "2026-06-15T04:33:25Z",
            ),
        )
    }

    @Test
    fun `remote nullish string local valid`() {
        // remote unparseable → falls back to string
        // "2026-06-15T04:33:25Z" >= "null" → false as strings (2 < n)
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "null",
            ),
        )
    }

    @Test
    fun `local one day later with positive offset is earlier in UTC`() {
        // 2026-06-16T00:00:00+05:00 = 2026-06-15T19:00:00Z
        // remote = 2026-06-15T23:30:00Z
        // local is BEFORE remote → should be false
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-16T00:00:00+05:00",
                "2026-06-15T23:30:00Z",
            ),
        )
    }

    private val mockSupabase: SupabaseClient = mockk(relaxed = true)
    private val mockTracker: SyncStateTracker = mockk(relaxed = true)
    private val mockConflictDao: ConflictDao = mockk(relaxed = true)

    private val TABLE = "pacientes"
    private val OPTICA_ID = "optica-test-1"

    private val ID1 = "id-aaa"
    private val ID2 = "id-bbb"
    private val ID3 = "id-ccc"

    private val T1 = "2026-06-15T04:00:00Z"
    private val T2 = "2026-06-15T05:00:00Z"

    /**
     * Fake subclass that overrides [ConflictHelper.selectRemoteRows] as the testability seam.
     * Captures the ids argument and returns the configured rows.
     *
     * NOTE: [RemoteTimestamp] is the internal data class in [ConflictHelper]; once the seam is
     * added with internal visibility this class will compile. Until then this file is RED.
     */
    private inner class FakeConflictHelper(
        private val rowsToReturn: List<RemoteTimestamp> = emptyList(),
    ) : ConflictHelper(mockSupabase, mockTracker, mockConflictDao) {

        var capturedIds: List<String>? = null
        var selectRemoteRowsCalled = false

        internal override suspend fun selectRemoteRows(
            tableName: String,
            opticaId: String,
            ids: List<String>,
        ): List<RemoteTimestamp> {
            selectRemoteRowsCalled = true
            capturedIds = ids
            return rowsToReturn
        }
    }

    /**
     * Fake that overrides [selectRemoteRowsChunk] (the per-chunk seam) instead
     * of [selectRemoteRows], allowing the chunking logic in [selectRemoteRows]
     * to execute while still controlling per-chunk behavior.
     */
    private inner class ChunkCapturingHelper(
        private val alwaysEmpty: Boolean = false,
    ) : ConflictHelper(mockSupabase, mockTracker, mockConflictDao) {
        val chunkCalls = mutableListOf<List<String>>()

        internal override suspend fun selectRemoteRowsChunk(
            tableName: String,
            opticaId: String,
            ids: List<String>,
        ): List<RemoteTimestamp> {
            chunkCalls.add(ids)
            if (alwaysEmpty) return emptyList()
            return ids.map { RemoteTimestamp(it, "2026-07-25T00:00:00Z") }
        }
    }

    /**
     * Fake that fails on a specific chunk index to test partial failure recovery.
     */
    private inner class SingleChunkFailingHelper(
        private val failOnChunkIndex: Int,
    ) : ConflictHelper(mockSupabase, mockTracker, mockConflictDao) {
        val chunkCalls = mutableListOf<List<String>>()
        private var callCount = 0

        internal override suspend fun selectRemoteRowsChunk(
            tableName: String,
            opticaId: String,
            ids: List<String>,
        ): List<RemoteTimestamp> {
            chunkCalls.add(ids)
            val currentIndex = callCount++
            if (currentIndex == failOnChunkIndex) {
                throw RuntimeException("Simulated chunk failure")
            }
            return ids.map { RemoteTimestamp(it, "2026-07-25T00:00:00Z") }
        }
    }

    // ===== Chunking tests (1.6–1.8) =====

    @Test
    fun `selectRemoteRows with 50 IDs issues 1 query`() = runTest {
        val ids = (1..50).map { "id-$it" }
        val helper = ChunkCapturingHelper()

        helper.selectRemoteRows(TABLE, OPTICA_ID, ids)

        assertEquals(1, helper.chunkCalls.size)
    }

    @Test
    fun `selectRemoteRows with 80 IDs issues 1 query`() = runTest {
        val ids = (1..80).map { "id-$it" }
        val helper = ChunkCapturingHelper()

        helper.selectRemoteRows(TABLE, OPTICA_ID, ids)

        assertEquals(1, helper.chunkCalls.size)
    }

    @Test
    fun `selectRemoteRows with 200 IDs issues 3 queries`() = runTest {
        val ids = (1..200).map { "id-$it" }
        val helper = ChunkCapturingHelper()

        helper.selectRemoteRows(TABLE, OPTICA_ID, ids)

        assertEquals(3, helper.chunkCalls.size)
        assertEquals(80, helper.chunkCalls[0].size)
        assertEquals(80, helper.chunkCalls[1].size)
        assertEquals(40, helper.chunkCalls[2].size)
    }

    @Test
    fun `selectRemoteRows single chunk failure returns partial results`() = runTest {
        val ids = (1..200).map { "id-$it" }
        val helper = SingleChunkFailingHelper(failOnChunkIndex = 1) // second chunk (80–159) fails

        val result = helper.selectRemoteRows(TABLE, OPTICA_ID, ids)

        assertEquals(3, helper.chunkCalls.size)
        assertEquals(120, result.size) // 80 + 40 = 120
    }

    @Test
    fun `fetchRemoteUpdatedAt returns empty map when all chunks return empty for non-empty ids`() = runTest {
        val helper = ChunkCapturingHelper(alwaysEmpty = true)

        val result = helper.fetchRemoteUpdatedAt(TABLE, OPTICA_ID, listOf("id-1", "id-2"))
        assertTrue("Expected empty map when all chunks fail, got $result", result.isEmpty())
    }

    @Test
    fun fetchRemoteUpdatedAt_returnsEmptyMap_whenIdsEmpty() = runTest {
        val helper = FakeConflictHelper()

        val result = helper.fetchRemoteUpdatedAt(TABLE, OPTICA_ID, emptyList())

        assertTrue("Expected empty map but got: $result", result.isEmpty())
        assertFalse(
            "selectRemoteRows must NOT be called when ids is empty",
            helper.selectRemoteRowsCalled,
        )
    }

    @Test
    fun fetchRemoteUpdatedAt_usesInFilter_whenIdsNonEmpty() = runTest {
        val helper = FakeConflictHelper(
            rowsToReturn = listOf(
                RemoteTimestamp(ID1, T1),
                RemoteTimestamp(ID2, T2),
            ),
        )

        helper.fetchRemoteUpdatedAt(TABLE, OPTICA_ID, listOf(ID1, ID2))

        assertTrue(
            "selectRemoteRows must be called when ids is non-empty",
            helper.selectRemoteRowsCalled,
        )
        assertTrue(
            "Expected capturedIds=[id1, id2] but got: ${helper.capturedIds}",
            helper.capturedIds == listOf(ID1, ID2),
        )
    }

    @Test
    fun fetchRemoteUpdatedAt_returnsOnlyRequestedIds() = runTest {
        // selectRemoteRows (the isIn filter) returns exactly the rows for id1 and id2.
        // id3 is NOT returned by the server because the isIn("id", [id1, id2]) filter
        // excludes it. fetchRemoteUpdatedAt must build the result from the rows returned
        // by selectRemoteRows — the resulting map must have exactly those two entries.
        val helper = FakeConflictHelper(
            rowsToReturn = listOf(
                RemoteTimestamp(ID1, T1),
                RemoteTimestamp(ID2, T2),
                // id3 is absent — the server-side isIn filter would exclude it
            ),
        )

        val result = helper.fetchRemoteUpdatedAt(TABLE, OPTICA_ID, listOf(ID1, ID2))

        assertTrue(
            "Expected keys={id1, id2} but got: ${result.keys}",
            result.keys == setOf(ID1, ID2),
        )
        assertFalse("id3 must not appear in result", ID3 in result)
    }

    @Test
    fun filterConflicts_callsResolveConflict_forSafeEntityWithRecord() = runTest {
        // e1: local T2 > remote T1 → safe
        val e1 = LocalEntity(ID1, T2)
        val helper = FakeConflictHelper(
            rowsToReturn = listOf(RemoteTimestamp(ID1, T1)),
        )
        coEvery { mockConflictDao.resolveConflict(any(), any()) } returns Unit

        helper.filterConflicts(TABLE, OPTICA_ID, "paciente", listOf(e1))

        coVerify(exactly = 1) { mockConflictDao.resolveConflict(ID1, OPTICA_ID) }
    }

    @Test
    fun filterConflicts_doesNotCallResolveConflict_forConflictedEntity() = runTest {
        // e2: local T1 < remote T2 → conflict
        val e2 = LocalEntity(ID2, T1)
        val helper = FakeConflictHelper(
            rowsToReturn = listOf(RemoteTimestamp(ID2, T2)),
        )
        coEvery { mockConflictDao.upsertConflict(any(), any(), any(), any(), any(), any()) } returns Unit

        helper.filterConflicts(TABLE, OPTICA_ID, "paciente", listOf(e2))

        coVerify(exactly = 0) { mockConflictDao.resolveConflict(ID2, any()) }
    }

    @Test
    fun filterConflicts_clearsStaleConflicts_whenAllEntitiesHaveNullUpdatedAt() = runTest {
        // Regression test: all 78 servicios_extra had null updatedAt (pre-migration data).
        // The early return for checkable.isEmpty() was skipping resolveConflict(), leaving
        // conflict_records intact. The download guard then blocked all downloads,
        // so Room never received the server timestamps — infinite conflict loop.
        val e1 = LocalEntity(ID1, null)
        val e2 = LocalEntity(ID2, null)
        val helper = FakeConflictHelper()
        coEvery { mockConflictDao.resolveConflict(any(), any()) } returns Unit

        val result = helper.filterConflicts(TABLE, OPTICA_ID, "servicio_extra", listOf(e1, e2))

        assertEquals("All null-updatedAt entities must be returned as safe", 2, result.size)
        coVerify(exactly = 1) { mockConflictDao.resolveConflict(ID1, OPTICA_ID) }
        coVerify(exactly = 1) { mockConflictDao.resolveConflict(ID2, OPTICA_ID) }
        assertFalse("selectRemoteRows must NOT fire when nothing is checkable", helper.selectRemoteRowsCalled)
    }

    @Test
    fun filterConflicts_resolveConflict_isIdempotentWhenNoRecord() = runTest {
        // e3: local T2 > remote T1 → safe; no ConflictRecord exists in DB (relaxed mock = no-op)
        val e3 = LocalEntity(ID3, T2)
        val helper = FakeConflictHelper(
            rowsToReturn = listOf(RemoteTimestamp(ID3, T1)),
        )
        // relaxed mock: resolveConflict is a no-op even if no row exists in the table

        var exceptionThrown: Throwable? = null
        try {
            helper.filterConflicts(TABLE, OPTICA_ID, "paciente", listOf(e3))
        } catch (t: Throwable) {
            exceptionThrown = t
        }

        assertTrue("Expected no exception but got: $exceptionThrown", exceptionThrown == null)
        coVerify(exactly = 1) { mockConflictDao.resolveConflict(ID3, OPTICA_ID) }
    }

    /**
     * BUG-F3: distinctBy { it.id } allows duplicate uploads when two movements
     * share the same composite key (referenciaId, tipo, monturaId) but have
     * different UUID ids. Supabase unique index idx_movimientos_conflict on
     * (referencia_id, tipo, montura_id) rejects the duplicate → error 23505.
     */
    @Test
    fun `detectConflictMovimientos with different IDs but same composite key`() {
        val mov1 = MonturaMovimiento(
            id = "uuid-aaa",
            monturaId = "m1",
            tipo = "SALIDA_VENTA",
            cantidad = 1,
            stockPrevio = 5,
            stockNuevo = 4,
            referenciaId = "disp-1",
            opticaId = "o1",
        )
        val mov2 = MonturaMovimiento(
            id = "uuid-bbb",
            monturaId = "m1",
            tipo = "SALIDA_VENTA",
            cantidad = 1,
            stockPrevio = 5,
            stockNuevo = 4,
            referenciaId = "disp-1",
            opticaId = "o1",
        )

        // Current bug: distinctBy { it.id } keeps BOTH → duplicate FK violation
        val pkDedup = listOf(mov1, mov2).distinctBy { it.id }
        assertEquals("PK-based dedup keeps both (WRONG — causes error 23505)", 2, pkDedup.size)

        // Correct fix: dedup by composite key keeps only one
        val compositeDedup = listOf(mov1, mov2).distinctBy { Triple(it.referenciaId, it.tipo, it.monturaId) }
        assertEquals("Composite-key dedup keeps only one (CORRECT)", 1, compositeDedup.size)

        // detectConflictMovimientos: both movements same stock → both safe
        val (safeIds, conflictedIds) = ConflictHelper.detectConflictMovimientos(
            local = listOf(mov1),
            remote = listOf(mov2),
        )
        assertTrue("mov1 should be safe (same stock as remote)", mov1.id in safeIds)
        assertTrue("no conflicts when stock matches", conflictedIds.isEmpty())
    }

    @Test
    fun `detectConflictMovimientos flags conflict when stock differs`() {
        val local = MonturaMovimiento(
            id = "uuid-local",
            monturaId = "m1",
            tipo = "SALIDA_VENTA",
            cantidad = 1,
            stockPrevio = 10,
            stockNuevo = 9,
            referenciaId = "disp-1",
            opticaId = "o1",
        )
        val remote = MonturaMovimiento(
            id = "uuid-remote",
            monturaId = "m1",
            tipo = "SALIDA_VENTA",
            cantidad = 1,
            stockPrevio = 5,
            stockNuevo = 4,
            referenciaId = "disp-1",
            opticaId = "o1",
        )

        val (safeIds, conflictedIds) = ConflictHelper.detectConflictMovimientos(
            local = listOf(local),
            remote = listOf(remote),
        )
        assertTrue("local should be conflicted when stockNuevo differs", local.id in conflictedIds)
        assertTrue("no safe IDs when stock differs", safeIds.isEmpty())
    }
}
