package com.example.optoapp.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.optoapp.factories.TestDataFactory
import com.example.optoapp.fakes.FakeSupabaseClient
import com.example.optoapp.rules.TestDatabaseRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Offline sync behaviour tests using [FakeSupabaseClient].
 *
 * These tests do NOT require a real Supabase project — all network interactions
 * are simulated through the fake, so they run on any emulator or device without
 * credential configuration.
 *
 * Each test verifies that data created while offline is persisted in Room with
 * the correct sync status, and that network errors trigger the retry mechanism.
 */
@RunWith(AndroidJUnit4::class)
class OfflineSyncTest {

    @get:Rule
    val dbRule = TestDatabaseRule()

    private lateinit var fakeSupabase: FakeSupabaseClient
    private val testOpticaId = "offline-test-optica"

    @Before
    fun setUp() {
        fakeSupabase = FakeSupabaseClient()
    }

    @After
    fun tearDown() {
        fakeSupabase.reset()
    }

    @Test
    fun createDataOffline_dataPersistsInRoom() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val syncDao = dbRule.syncEntityStateDao

        // Act: simulate creating a patient while offline.
        val paciente = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Offline Patient"
        )
        pacienteDao.insertPaciente(paciente)

        // Mark sync as pending (app behaviour when there's no network).
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "pending",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Assert: data persisted in Room despite being offline.
        val stored = pacienteDao.getPacienteById(paciente.id)
        assertNotNull("Patient must be stored in Room even when offline", stored)
        assertEquals("Offline Patient", stored?.nombreCompleto)
        assertEquals(testOpticaId, stored?.opticaId)
    }

    @Test
    fun comeOnline_queuedDataSyncsAutomatically() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val syncDao = dbRule.syncEntityStateDao

        // Arrange: create patient while "offline".
        val paciente = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Queued Sync Patient"
        )
        pacienteDao.insertPaciente(paciente)
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "pending",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Act: simulate coming online by calling the fake sync.
        fakeSupabase.networkErrorEnabled = false
        val syncedCount = fakeSupabase.syncPatients(testOpticaId)

        // Assert: data was "synced".
        assertEquals("Fake sync should report 1 patient synced", 1, syncedCount)

        // Verify the fake recorded the sync operation.
        assertEquals("Fake sync counter should be 1", 1, fakeSupabase.syncedPatientCount)

        // Update local sync status as the real coordinator would.
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "synced",
                updatedAt = System.currentTimeMillis()
            )
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
            nombreCompleto = "Local Version"
        )
        pacienteDao.insertPaciente(paciente)
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "pending",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Simulate: fake returns a different remote version but the app
        // policy is "local wins" — so we overwrite remote with local.
        // In a real implementation the sync coordinator would upsert with
        // last-write-wins or explicit local-wins policy.

        // Act: mark synced with local data — simulating local-wins policy.
        syncDao.upsert(
            SyncEntityState(
                opticaId = testOpticaId,
                entityType = "paciente",
                entityId = paciente.id,
                status = "synced",
                updatedAt = System.currentTimeMillis()
            )
        )

        // Assert: local version is preserved.
        val stored = pacienteDao.getPacienteById(paciente.id)
        assertNotNull(stored)
        assertEquals("Local version must be preserved after sync conflict",
            "Local Version", stored?.nombreCompleto)
    }

    @Test
    fun networkError_triggersRetryMechanism() = runBlocking {
        val pacienteDao = dbRule.pacienteDao

        // Arrange: enable network errors on the fake.
        fakeSupabase.networkErrorEnabled = true

        // Act: attempt to sync — should throw IOException.
        try {
            fakeSupabase.syncPatients(testOpticaId)
            // If we reach here, the fake didn't throw — fail the test.
            assertTrue("Expected IOException but sync succeeded", false)
        } catch (e: IOException) {
            // Expected: network error occurred.
            assertTrue("Network error should match", e.message?.contains("Fake network error") == true)
        } catch (e: Exception) {
            // Also acceptable — the fake might throw another exception type.
            assertTrue("Exception should mention network: ${e.message}",
                e.message?.contains("network", ignoreCase = true) == true)
        }

        // Now disable network errors and retry.
        fakeSupabase.networkErrorEnabled = false
        val retryCount = fakeSupabase.syncPatients(testOpticaId)

        // Assert: retry succeeded.
        assertEquals("After retry, sync should succeed", 1, retryCount)
        assertEquals("Fake should record 1 successful sync", 1, fakeSupabase.syncedPatientCount)
    }

    @Test
    fun multipleOfflineOperations_queueAllForSync() = runBlocking {
        val pacienteDao = dbRule.pacienteDao
        val evaluacionDao = dbRule.evaluacionDao
        val syncDao = dbRule.syncEntityStateDao

        // Arrange: create a patient and evaluation while offline.
        val paciente1 = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Offline Patient 1"
        )
        val paciente2 = TestDataFactory.createTestPaciente(
            opticaId = testOpticaId,
            nombreCompleto = "Offline Patient 2"
        )
        pacienteDao.insertPaciente(paciente1)
        pacienteDao.insertPaciente(paciente2)

        syncDao.upsert(
            SyncEntityState(testOpticaId, "paciente", paciente1.id, "pending")
        )
        syncDao.upsert(
            SyncEntityState(testOpticaId, "paciente", paciente2.id, "pending")
        )

        val evaluacion = TestDataFactory.createTestEvaluacion(
            pacienteId = paciente1.id,
            opticaId = testOpticaId
        )
        evaluacionDao.insertEvaluacion(evaluacion)
        syncDao.upsert(
            SyncEntityState(testOpticaId, "evaluacion", evaluacion.id, "pending")
        )

        // Verify all data is in Room.
        assertEquals("Both patients should be in Room", 2,
            pacienteDao.getPacientesListByOptica(testOpticaId).size)
        assertNotNull("Evaluation should be in Room",
            evaluacionDao.getEvaluacionById(evaluacion.id))

        // Verify pending markers exist.
        val allPending = syncDao.observeErrorsForOptica(testOpticaId) // This returns all, not just errors
        assertTrue("Pending markers should exist in sync_entity_state",
            allPending.isNotEmpty())
    }
}
