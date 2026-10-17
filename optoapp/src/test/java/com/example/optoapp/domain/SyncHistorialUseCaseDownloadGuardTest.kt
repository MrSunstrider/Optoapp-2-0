package com.example.optoapp.domain

import com.example.optoapp.data.FakeConflictDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import io.github.jan.supabase.createSupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SyncHistorialUseCaseDownloadGuardTest {

    private val opticaId = "optica-historial-guard"

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val database = mockk<OptoDatabase>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = FakeConflictDao()

    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://placeholder.supabase.co",
        supabaseKey = "placeholder-key",
    ) {}

    private lateinit var useCase: SyncHistorialUseCase

    @Before
    fun setUp() {
        conflictDao.returnEntityIds = emptyList()
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
        coEvery { syncStateTracker.markError(any(), any(), any(), any()) } just Runs
        coEvery { syncStateTracker.dao.getPendingDeletions(any()) } returns emptyList()
        coEvery { repository.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            firstArg<suspend () -> Any?>().invoke()
        }

        useCase = SyncHistorialUseCase(
            repository = repository,
            supabase = fakeSupabase,
            database = database,
            syncStateTracker = syncStateTracker,
            conflictHelper = conflictHelper,
            conflictDao = conflictDao,
        )
    }

    @Test
    fun constructor_takesSixDependencies() {
        val constructors = SyncHistorialUseCase::class.java.declaredConstructors
        assertEquals(1, constructors.size)
        val params = constructors[0].parameterTypes
        assertEquals(
            "SyncHistorialUseCase should accept exactly 6 constructor params after database injection",
            6,
            params.size,
        )
    }

    @Test
    fun conflictDao_isAcceptedAsConstructorParam() {
        val constructors = SyncHistorialUseCase::class.java.declaredConstructors
        val params = constructors[0].parameterTypes
        val hasConflictDao = params.any { it.simpleName == "ConflictDao" }
        assertTrue(
            "ConflictDao must be a constructor parameter of SyncHistorialUseCase",
            hasConflictDao,
        )
    }

    @Test
    fun downloadEvaluaciones_queriesConflictEntityIds() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertTrue(
            "downloadEvaluaciones() should call getConflictEntityIds",
            conflictDao.getConflictEntityIdsCalled.get(),
        )
    }

    @Test
    fun downloadEvaluaciones_usesCorrectEntityType() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertEquals("evaluacion", conflictDao.lastEntityType)
    }

    @Test
    fun downloadEvaluaciones_withNoConflicts_callsGetConflictEntityIdsOnce() = runBlocking {
        conflictDao.returnEntityIds = emptyList()
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertTrue(
            "getConflictEntityIds should have been called",
            conflictDao.getConflictEntityIdsCalled.get(),
        )
    }

    @Test
    fun downloadEvaluaciones_queriesPendingDeletions() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        io.mockk.coVerify(atLeast = 1) { syncStateTracker.dao.getPendingDeletions(opticaId) }
    }

    @Test
    fun downloadEvaluaciones_pendingDeletionsGuardFailure_isFailClosed() = runBlocking {
        val testUseCase = TestableDownloadUseCase(
            repository = repository,
            supabase = fakeSupabase,
            database = database,
            syncStateTracker = syncStateTracker,
            conflictHelper = conflictHelper,
            conflictDao = conflictDao,
        )
        // pushPendingEvaluacionDeletions consumes the first call; download guard retries twice.
        var pendingCalls = 0
        coEvery { syncStateTracker.dao.getPendingDeletions(opticaId) } answers {
            pendingCalls++
            if (pendingCalls == 1) {
                emptyList()
            } else {
                throw RuntimeException("pending deletions failed")
            }
        }
        testUseCase.remoteRows = listOf(
            EvaluacionRemota(
                id = "E1",
                pacienteId = "P1",
                fecha = "2026-01-01",
                opticaId = opticaId,
            ),
        )

        val result = testUseCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true)

        assertTrue(result is Resource.Success)
        assertEquals(0, (result as Resource.Success).data!!.downloaded)
        assertFalse("fetch must not run when deletion guard fails", testUseCase.fetchCalled)
        coVerify(exactly = 0) { repository.upsertEvaluacionFromRemote(any()) }
        coVerify {
            syncStateTracker.markError(opticaId, "download_evaluaciones", "batch", any())
        }
    }

    @Test
    fun downloadEvaluaciones_pendingDeletionsCancellationException_propagates() = runBlocking {
        var pendingCalls = 0
        coEvery { syncStateTracker.dao.getPendingDeletions(opticaId) } answers {
            pendingCalls++
            if (pendingCalls == 1) {
                emptyList()
            } else {
                throw CancellationException("cancelled during deletion guard")
            }
        }

        try {
            useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true)
            fail("Expected CancellationException from deletion guard query")
        } catch (_: CancellationException) {
            // Expected — must not be swallowed by fail-closed handling
        }

        coVerify(inverse = true) {
            syncStateTracker.markError(opticaId, "download_evaluaciones", "batch", any())
        }
        coVerify(exactly = 0) { repository.upsertEvaluacionFromRemote(any()) }
    }

    @Test
    fun downloadEvaluaciones_skipsPendingEvaluacionTombstoneIds() = runBlocking {
        val testUseCase = TestableDownloadUseCase(
            repository = repository,
            supabase = fakeSupabase,
            database = database,
            syncStateTracker = syncStateTracker,
            conflictHelper = conflictHelper,
            conflictDao = conflictDao,
        )
        coEvery { syncStateTracker.dao.getPendingDeletions(opticaId) } returns listOf(
            SyncEntityState(
                opticaId = opticaId,
                entityType = "evaluacion",
                entityId = "E-tombstone",
                status = "deleted",
            ),
        )
        testUseCase.remoteRows = listOf(
            EvaluacionRemota(
                id = "E-tombstone",
                pacienteId = "P1",
                fecha = "2026-01-01",
                opticaId = opticaId,
            ),
            EvaluacionRemota(
                id = "E-keep",
                pacienteId = "P2",
                fecha = "2026-01-02",
                opticaId = opticaId,
            ),
        )

        val result = testUseCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true)

        assertTrue(result is Resource.Success)
        assertTrue(testUseCase.fetchCalled)
        coVerify(exactly = 0) {
            repository.upsertEvaluacionFromRemote(match { it.id == "E-tombstone" })
        }
        coVerify(exactly = 1) {
            repository.upsertEvaluacionFromRemote(match { it.id == "E-keep" })
        }
    }

    private class TestableDownloadUseCase(
        repository: OptoRepository,
        supabase: io.github.jan.supabase.SupabaseClient,
        database: OptoDatabase,
        syncStateTracker: SyncStateTracker,
        conflictHelper: ConflictHelper,
        conflictDao: com.example.optoapp.data.ConflictDao,
    ) : SyncHistorialUseCase(
        repository,
        supabase,
        database,
        syncStateTracker,
        conflictHelper,
        conflictDao,
    ) {
        var remoteRows: List<EvaluacionRemota> = emptyList()
        var fetchCalled = false

        override suspend fun fetchRemoteEvaluacionesForDownload(opticaId: String): List<EvaluacionRemota> {
            fetchCalled = true
            return remoteRows
        }
    }
}
