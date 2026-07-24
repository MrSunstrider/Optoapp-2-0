package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class SyncTelemetryLogDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: SyncTelemetryLogDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.syncTelemetryLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_observeByOpticaId_returns_rows_ordered_by_createdAt_desc() = runBlocking {
        val opticaId = "optica-1"
        val oldEntry = SyncTelemetryLogEntity(
            id = UUID.randomUUID().toString(),
            opticaId = opticaId,
            status = "error",
            stage = "finanzas",
            errorMessage = "timeout",
            createdAt = 1000L,
        )
        val newEntry = SyncTelemetryLogEntity(
            id = UUID.randomUUID().toString(),
            opticaId = opticaId,
            status = "ok",
            stage = "finalizado",
            errorMessage = "",
            createdAt = 2000L,
        )

        dao.insert(oldEntry)
        dao.insert(newEntry)

        val rows = dao.observeByOpticaId(opticaId).first()
        assertEquals("Should return 2 rows", 2, rows.size)
        assertEquals("Newest should be first", newEntry.id, rows[0].id)
        assertEquals("Oldest should be second", oldEntry.id, rows[1].id)
    }

    @Test
    fun observeByOpticaId_filters_by_optica() = runBlocking {
        val opticaA = "optica-a"
        val opticaB = "optica-b"
        val entryA = SyncTelemetryLogEntity(
            id = UUID.randomUUID().toString(),
            opticaId = opticaA,
            status = "ok",
            stage = "finalizado",
            errorMessage = "",
            createdAt = 1000L,
        )
        val entryB = SyncTelemetryLogEntity(
            id = UUID.randomUUID().toString(),
            opticaId = opticaB,
            status = "error",
            stage = "pacientes",
            errorMessage = "timeout",
            createdAt = 1000L,
        )

        dao.insert(entryA)
        dao.insert(entryB)

        val rowsA = dao.observeByOpticaId(opticaA).first()
        assertEquals("Should only return optica-a rows", 1, rowsA.size)
        assertEquals(entryA.id, rowsA[0].id)
    }

    @Test
    fun observeByOpticaId_returns_empty_for_unknown_optica() = runBlocking {
        val opticaId = "optica-1"
        val entry = SyncTelemetryLogEntity(
            id = UUID.randomUUID().toString(),
            opticaId = opticaId,
            status = "ok",
            stage = "finalizado",
            errorMessage = "",
            createdAt = 1000L,
        )
        dao.insert(entry)

        val rows = dao.observeByOpticaId("unknown-optica").first()
        assertEquals("Should be empty for unrelated optica", 0, rows.size)
    }
}
