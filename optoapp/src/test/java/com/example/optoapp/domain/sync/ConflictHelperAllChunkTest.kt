package com.example.optoapp.domain.sync

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.SyncStateTracker
import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * Tests for REQ-SYNC-004: Graceful All-Chunk Failure.
 *
 * Verifies that [ConflictHelper.fetchRemoteUpdatedAt] returns an empty map
 * instead of throwing when all chunk queries fail.
 */
class ConflictHelperAllChunkTest {

    private val mockSupabase = mockk<SupabaseClient>(relaxed = true)
    private val mockTracker = mockk<SyncStateTracker>(relaxed = true)
    private val mockConflictDao = mockk<ConflictDao>(relaxed = true)

    /**
     * Fake that always throws on [selectRemoteRowsChunk], simulating complete
     * network failure for all chunks.
     */
    private inner class AllChunkFailingHelper : ConflictHelper(mockSupabase, mockTracker, mockConflictDao) {
        internal override suspend fun selectRemoteRowsChunk(
            tableName: String,
            opticaId: String,
            ids: List<String>,
        ): List<RemoteTimestamp> {
            throw RuntimeException("Simulated chunk failure")
        }
    }

    @Test
    fun `fetchRemoteUpdatedAt returns empty map when all chunks fail`() = runTest {
        val helper = AllChunkFailingHelper()

        val result = helper.fetchRemoteUpdatedAt(
            "test_table", "optica_1", listOf("id1", "id2"),
        )

        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun `fetchRemoteUpdatedAt returns empty map for empty ids without calling chunks`() = runTest {
        val helper = AllChunkFailingHelper()

        val result = helper.fetchRemoteUpdatedAt(
            "test_table", "optica_1", emptyList(),
        )

        Assert.assertTrue(result.isEmpty())
    }

    @Test
    fun `fetchRemoteUpdatedAt handles partial failure gracefully`() = runTest {
        val helper = object : ConflictHelper(mockSupabase, mockTracker, mockConflictDao) {
            private var callCount = 0
            internal override suspend fun selectRemoteRowsChunk(
                tableName: String,
                opticaId: String,
                ids: List<String>,
            ): List<RemoteTimestamp> {
                callCount++
                if (callCount == 1) {
                    throw RuntimeException("First chunk fails")
                }
                return ids.map { RemoteTimestamp(it, "2026-07-25T00:00:00Z") }
            }
        }

        val result = helper.fetchRemoteUpdatedAt(
            "test_table", "optica_1", (1..160).map { "id$it" },
        )

        Assert.assertTrue(result.isNotEmpty())
    }
}
