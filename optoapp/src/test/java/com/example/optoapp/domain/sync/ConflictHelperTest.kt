package com.example.optoapp.domain.sync

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [ConflictHelper]. */
@RunWith(RobolectricTestRunner::class)
class ConflictHelperTest {

    // ── isLocalNewerOrEqual (static, no instance needed) ─────────────────────

    @Test
    fun `same instant with Z and +0000 are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "2026-06-15T04:33:25+00:00"
            )
        )
    }

    @Test
    fun `same instant with and without millis are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25.000Z",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `same instant with micros and Z are equal`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25.123456Z",
                "2026-06-15T04:33:25.123Z"
            )
        )
    }

    @Test
    fun `local newer returns true`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T05:00:00Z",
                "2026-06-15T04:00:00Z"
            )
        )
    }

    @Test
    fun `local older returns false`() {
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:00:00Z",
                "2026-06-15T05:00:00Z"
            )
        )
    }

    @Test
    fun `exact same string returns true`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `different days local newer`() {
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-16T00:00:00Z",
                "2026-06-15T23:59:59Z"
            )
        )
    }

    @Test
    fun `local with +0500 vs remote Z same instant`() {
        // 2026-06-15T09:33:25+05:00 = 2026-06-15T04:33:25Z
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T09:33:25+05:00",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `unparseable falls back to string comparison`() {
        // "invalid" > "2026-06-15T04:33:25Z" as strings (i > 2)
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "invalid",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `both unparseable uses string comparison`() {
        // "abc" >= "xyz" is false as strings (a < x)
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual("abc", "xyz")
        )
    }

    @Test
    fun `local nullish string vs remote valid`() {
        // "null" > "2026-06-15T04:33:25Z" as strings (n > 2)
        assertTrue(
            ConflictHelper.isLocalNewerOrEqual(
                "null",
                "2026-06-15T04:33:25Z"
            )
        )
    }

    @Test
    fun `remote nullish string local valid`() {
        // remote unparseable → falls back to string
        // "2026-06-15T04:33:25Z" >= "null" → false as strings (2 < n)
        assertFalse(
            ConflictHelper.isLocalNewerOrEqual(
                "2026-06-15T04:33:25Z",
                "null"
            )
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
                "2026-06-15T23:30:00Z"
            )
        )
    }

    // ── Instance test helpers ─────────────────────────────────────────────────

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
        private val rowsToReturn: List<RemoteTimestamp> = emptyList()
    ) : ConflictHelper(mockSupabase, mockTracker, mockConflictDao) {

        var capturedIds: List<String>? = null
        var selectRemoteRowsCalled = false

        internal override suspend fun selectRemoteRows(
            tableName: String,
            opticaId: String,
            ids: List<String>
        ): List<RemoteTimestamp> {
            selectRemoteRowsCalled = true
            capturedIds = ids
            return rowsToReturn
        }
    }

    // ── RC-1: fetchRemoteUpdatedAt ────────────────────────────────────────────

    @Test
    fun fetchRemoteUpdatedAt_returnsEmptyMap_whenIdsEmpty() = runTest {
        val helper = FakeConflictHelper()

        val result = helper.fetchRemoteUpdatedAt(TABLE, OPTICA_ID, emptyList())

        assertTrue("Expected empty map but got: $result", result.isEmpty())
        assertFalse(
            "selectRemoteRows must NOT be called when ids is empty",
            helper.selectRemoteRowsCalled
        )
    }

    @Test
    fun fetchRemoteUpdatedAt_usesInFilter_whenIdsNonEmpty() = runTest {
        val helper = FakeConflictHelper(
            rowsToReturn = listOf(
                RemoteTimestamp(ID1, T1),
                RemoteTimestamp(ID2, T2)
            )
        )

        helper.fetchRemoteUpdatedAt(TABLE, OPTICA_ID, listOf(ID1, ID2))

        assertTrue(
            "selectRemoteRows must be called when ids is non-empty",
            helper.selectRemoteRowsCalled
        )
        assertTrue(
            "Expected capturedIds=[id1, id2] but got: ${helper.capturedIds}",
            helper.capturedIds == listOf(ID1, ID2)
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
                RemoteTimestamp(ID2, T2)
                // id3 is absent — the server-side isIn filter would exclude it
            )
        )

        val result = helper.fetchRemoteUpdatedAt(TABLE, OPTICA_ID, listOf(ID1, ID2))

        assertTrue(
            "Expected keys={id1, id2} but got: ${result.keys}",
            result.keys == setOf(ID1, ID2)
        )
        assertFalse("id3 must not appear in result", ID3 in result)
    }

    // ── RC-3: filterConflicts auto-clear ──────────────────────────────────────

    @Test
    fun filterConflicts_callsResolveConflict_forSafeEntityWithRecord() = runTest {
        // e1: local T2 > remote T1 → safe
        val e1 = LocalEntity(ID1, T2)
        val helper = FakeConflictHelper(
            rowsToReturn = listOf(RemoteTimestamp(ID1, T1))
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
            rowsToReturn = listOf(RemoteTimestamp(ID2, T2))
        )
        coEvery { mockConflictDao.upsertConflict(any(), any(), any(), any(), any(), any()) } returns Unit

        helper.filterConflicts(TABLE, OPTICA_ID, "paciente", listOf(e2))

        coVerify(exactly = 0) { mockConflictDao.resolveConflict(ID2, any()) }
    }

    @Test
    fun filterConflicts_resolveConflict_isIdempotentWhenNoRecord() = runTest {
        // e3: local T2 > remote T1 → safe; no ConflictRecord exists in DB (relaxed mock = no-op)
        val e3 = LocalEntity(ID3, T2)
        val helper = FakeConflictHelper(
            rowsToReturn = listOf(RemoteTimestamp(ID3, T1))
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
}
