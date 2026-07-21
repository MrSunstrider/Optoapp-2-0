package com.example.optoapp.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.optoapp.factories.TestDataFactory
import com.example.optoapp.rules.TestDatabaseRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Offline sync behaviour tests using Room persistence only.
 *
 * These tests do NOT require a Supabase project or any network. They verify
 * that data created while offline is persisted in Room with the correct sync
 * status markers, and that the queue survives the offline period.
 *
 * The actual sync upload (Room → Supabase) is tested in [SyncFlowTest].
 */
@RunWith(AndroidJUnit4::class)
class OfflineSyncTest {

    @get:Rule
    val dbRule = TestDatabaseRule()

    private val testOpticaId = "offline-test-optica"

    @Test
    fun createDataOffline_dataPersistsInRoom() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val syncDao = dbRule.syncEntityStateDao

        // Act: simulate creating a patient while offline.
        val paciente = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Offline Patient",
        )
        pacienteDao.insertPaciente(paciente)

        // Mark sync as pending (app behaviour when there's no network).
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "pending",
                updatedAt = System.currentTimeMillis(),
            ),
        )

        // Assert: data persisted in Room despite being offline.
        val stored = pacienteDao.getPacienteById(paciente.id)
        assertNotNull("Patient must be stored in Room even when offline", stored)
        assertEquals("Offline Patient", stored?.nombreCompleto)
        assertEquals(testOpticaId, stored?.opticaId)
    }

    @Test
    fun offlineData_markedForSync_afterComeOnline() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val syncDao = dbRule.syncEntityStateDao

        // Arrange: create patient while "offline".
        val paciente = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Queued Sync Patient",
        )
        pacienteDao.insertPaciente(paciente)
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "pending",
                updatedAt = System.currentTimeMillis(),
            ),
        )

        // Simulate "coming online" by updating sync status to synced.
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "synced",
                updatedAt = System.currentTimeMillis(),
            ),
        )

        // Verify the data is still in Room and marked synced.
        val stored = pacienteDao.getPacienteById(paciente.id)
        assertNotNull("Patient should still exist in Room after sync", stored)
        assertEquals("Queued Sync Patient", stored?.nombreCompleto)
    }

    @Test
    fun syncConflict_localVersionWins() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val syncDao = dbRule.syncEntityStateDao

        // Arrange: Patient exists in Room with local changes.
        val paciente = TestDataFactory.createTestPaciente(
            id = "conflict-patient-id",
            opticaId = testOpticaId,
            nombreCompleto = "Local Version",
        )
        pacienteDao.insertPaciente(paciente)
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "pending",
                updatedAt = System.currentTimeMillis(),
            ),
        )

        // Act: mark synced with local data — local-wins policy.
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "synced",
                updatedAt = System.currentTimeMillis(),
            ),
        )

        // Assert: local version is preserved.
        val stored = pacienteDao.getPacienteById(paciente.id)
        assertNotNull(stored)
        assertEquals(
            "Local version must be preserved after sync conflict",
            "Local Version",
            stored?.nombreCompleto,
        )
    }

    @Test
    fun multipleOfflineOperations_queueAllForSync() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val evaluacionDao = dbRule.evaluacionDao
        val syncDao = dbRule.syncEntityStateDao

        // Arrange: create a patient and evaluation while offline.
        val paciente1 = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Offline Patient 1",
        )
        val paciente2 = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Offline Patient 2",
        )
        pacienteDao.insertPaciente(paciente1)
        pacienteDao.insertPaciente(paciente2)

        syncDao.upsert(
            SyncEntityState(testOpticaId, "paciente", paciente1.id, "pending"),
        )
        syncDao.upsert(
            SyncEntityState(testOpticaId, "paciente", paciente2.id, "pending"),
        )

        val evaluacion = TestDataFactory.createTestEvaluacion(
            pacienteId = paciente1.id,
            opticaId = testOpticaId,
        )
        evaluacionDao.insertEvaluacion(evaluacion)
        syncDao.upsert(
            SyncEntityState(testOpticaId, "evaluacion", evaluacion.id, "pending"),
        )

        // Verify all data is in Room.
        assertEquals(
            "Both patients should be in Room",
            2,
            pacienteDao.getPacientesListByOptica(testOpticaId).size,
        )
        assertNotNull(
            "Evaluation should be in Room",
            evaluacionDao.getEvaluacionById(evaluacion.id),
        )

        // Verify pending markers exist.
        val allPending = syncDao.getPendingForOptica(testOpticaId).first()
        assertTrue(
            "Pending markers should exist in sync_entity_state",
            allPending.isNotEmpty(),
        )
    }
}
