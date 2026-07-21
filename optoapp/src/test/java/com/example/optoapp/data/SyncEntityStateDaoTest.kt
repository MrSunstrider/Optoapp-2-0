package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class SyncEntityStateDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: SyncEntityStateDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.syncEntityStateDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun deleteConflictedForOptica_deletesOnlyConflictedRows() = runBlocking {
        val opticaId = "optica-001"
        val otherOpticaId = "optica-002"

        dao.upsert(SyncEntityState(opticaId, "paciente", "p-1", "conflicted"))
        dao.upsert(SyncEntityState(opticaId, "paciente", "p-2", "conflicted"))

        dao.upsert(SyncEntityState(opticaId, "paciente", "p-3", "error"))
        dao.upsert(SyncEntityState(opticaId, "evaluacion", "e-1", "pending"))
        dao.upsert(SyncEntityState(opticaId, "montura", "m-1", "synced"))

        dao.upsert(SyncEntityState(otherOpticaId, "paciente", "p-4", "conflicted"))

        dao.deleteConflictedForOptica(opticaId)

        val remaining = dao.getByStatus(opticaId, "conflicted")
        assertEquals("conflicted rows for opticaId should be deleted", 0, remaining.size)

        val errors = dao.getByStatus(opticaId, "error")
        assertEquals("error rows should survive", 1, errors.size)

        val pending = dao.getByStatus(opticaId, "pending")
        assertEquals("pending rows should survive", 1, pending.size)

        val synced = dao.getByStatus(opticaId, "synced")
        assertEquals("synced rows should survive", 1, synced.size)

        val otherConflicted = dao.getByStatus(otherOpticaId, "conflicted")
        assertEquals("conflicted rows for other optica should survive", 1, otherConflicted.size)
    }

    @Test
    fun deleteConflictedForOptica_whenNoConflictedRows_doesNothing() = runBlocking {
        val opticaId = "optica-001"
        dao.upsert(SyncEntityState(opticaId, "paciente", "p-1", "error"))

        dao.deleteConflictedForOptica(opticaId)

        val errors = dao.getByStatus(opticaId, "error")
        assertEquals("error row should survive when no conflicted rows exist", 1, errors.size)
    }
}
