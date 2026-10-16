package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

// Robolectric only supplies the Context that Room.inMemoryDatabaseBuilder needs, so clearPendingRecalc's
// withTransaction runs against real Room (the AGENTS.md real-Room exception).
@RunWith(RobolectricTestRunner::class)
class SyncStateTrackerPendingRecalcTest {

    private val opticaId = "o1"
    private val fecha = LocalDate.of(2026, 7, 10)
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
    fun `markPendingRecalc stores a marker with its own status`() = runTest {
        tracker.markPendingRecalc(opticaId, fecha, "dispensacion", "d1")

        val markers = tracker.pendingRecalcMarkers(opticaId)

        assertEquals(1, markers.size)
        assertEquals(fecha, markers[0].fecha)
        assertEquals("dispensacion", markers[0].causeType)
        assertEquals("d1", markers[0].causeId)
        val state = tracker.stateOf(opticaId, SyncStateTracker.ENTITY_RESUMEN_RECALC, "2026-07-10|dispensacion|d1")
        assertEquals(SyncStateTracker.STATUS_PENDING_RECALC, state?.status)
        assertEquals(state?.updatedAt, markers[0].markedAt)
    }

    @Test
    fun `pending recalc is not reported as error, conflict or awaiting remote`() = runTest {
        tracker.markPendingRecalc(opticaId, fecha, "servicio_extra", "s1")

        assertEquals(0, tracker.getErrorsCount(opticaId))
        assertEquals(0, tracker.getConflictedCount(opticaId))
        assertTrue(tracker.awaitingRemoteIds(opticaId, SyncStateTracker.ENTITY_RESUMEN_RECALC).isEmpty())
        assertTrue(tracker.awaitingRemoteIds(opticaId, "servicio_extra").isEmpty())
    }

    @Test
    fun `re-marking the same record refreshes markedAt`() = runTest {
        db.syncEntityStateDao().upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = SyncStateTracker.ENTITY_RESUMEN_RECALC,
                entityId = "2026-07-10|dispensacion|d1",
                status = SyncStateTracker.STATUS_PENDING_RECALC,
                updatedAt = 1_000L,
            ),
        )

        tracker.markPendingRecalc(opticaId, fecha, "dispensacion", "d1")

        val markers = tracker.pendingRecalcMarkers(opticaId)
        assertEquals(1, markers.size)
        assertTrue(markers[0].markedAt > 1_000L)
    }

    @Test
    fun `malformed marker ids are skipped`() = runTest {
        listOf("not-a-marker", "2026-13-40|dispensacion|d1", "2026-07-10||d1").forEach { id ->
            db.syncEntityStateDao().upsert(
                SyncEntityState(opticaId, SyncStateTracker.ENTITY_RESUMEN_RECALC, id, SyncStateTracker.STATUS_PENDING_RECALC),
            )
        }
        tracker.markPendingRecalc(opticaId, fecha, "dispensacion", "d1")

        assertEquals(listOf("d1"), tracker.pendingRecalcMarkers(opticaId).map { it.causeId })
    }

    @Test
    fun `clearPendingRecalc removes the flushed marker`() = runTest {
        tracker.markPendingRecalc(opticaId, fecha, "dispensacion", "d1")
        tracker.markPendingRecalc(opticaId, fecha.plusDays(1), "dispensacion", "d1")
        val flushed = tracker.pendingRecalcMarkers(opticaId).first { it.fecha == fecha }

        tracker.clearPendingRecalc(opticaId, flushed)

        assertEquals(listOf(fecha.plusDays(1)), tracker.pendingRecalcMarkers(opticaId).map { it.fecha })
    }

    @Test
    fun `clearPendingRecalc keeps a marker refreshed after it was read`() = runTest {
        tracker.markPendingRecalc(opticaId, fecha, "dispensacion", "d1")
        val leido = tracker.pendingRecalcMarkers(opticaId).single()
        db.syncEntityStateDao().upsert(
            SyncEntityState(
                opticaId = opticaId,
                entityType = SyncStateTracker.ENTITY_RESUMEN_RECALC,
                entityId = "2026-07-10|dispensacion|d1",
                status = SyncStateTracker.STATUS_PENDING_RECALC,
                updatedAt = leido.markedAt + 5_000L,
            ),
        )

        tracker.clearPendingRecalc(opticaId, leido)

        assertEquals(leido.markedAt + 5_000L, tracker.pendingRecalcMarkers(opticaId).single().markedAt)
    }

    @Test
    fun `stateOf returns null for an unknown entity`() = runTest {
        assertNull(tracker.stateOf(opticaId, "dispensacion", "missing"))
    }
}
