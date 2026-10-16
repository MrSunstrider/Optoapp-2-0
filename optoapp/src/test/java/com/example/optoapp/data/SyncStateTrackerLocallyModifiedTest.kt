package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Real-Room DAO test: in-memory Room needs Robolectric for its Android context, the AGENTS.md exception.
@RunWith(RobolectricTestRunner::class)
class SyncStateTrackerLocallyModifiedTest {

    private val opticaId = "o1"
    private lateinit var db: OptoDatabase
    private lateinit var tracker: SyncStateTracker

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OptoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tracker = SyncStateTracker(db.syncEntityStateDao(), db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `markLocallyModified stores the row as pending upload`() = runTest {
        tracker.markSynced(opticaId, "costo_producto", "p1")

        tracker.markLocallyModified(opticaId, "costo_producto", "p1")

        assertEquals("pending_upload", db.syncEntityStateDao().getState(opticaId, "costo_producto", "p1")?.status)
        assertFalse(tracker.isSynced(opticaId, "costo_producto", "p1"))
    }

    @Test
    fun `syncedIds returns only synced rows of the requested entity type`() = runTest {
        tracker.markSynced(opticaId, "costo_producto", "synced")
        tracker.markLocallyModified(opticaId, "costo_producto", "pending")
        tracker.markError(opticaId, "costo_producto", "failed", "timeout")
        tracker.markSynced(opticaId, "costo_biselado", "other-type")
        tracker.markSynced("o2", "costo_producto", "other-optica")

        assertEquals(setOf("synced"), tracker.syncedIds(opticaId, "costo_producto"))
    }

    @Test
    fun `pending upload is not reported as error, conflict or awaiting remote`() = runTest {
        tracker.markLocallyModified(opticaId, "costo_producto", "p1")

        assertEquals(0, tracker.getErrorsCount(opticaId))
        assertEquals(0, tracker.getConflictedCount(opticaId))
        assertTrue(tracker.awaitingRemoteIds(opticaId, "costo_producto").isEmpty())
        assertTrue(tracker.quarantinedEntityIds(opticaId, "costo_producto").isEmpty())
    }
}
