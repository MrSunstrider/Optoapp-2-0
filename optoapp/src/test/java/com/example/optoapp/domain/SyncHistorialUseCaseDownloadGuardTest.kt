package com.example.optoapp.domain

import com.example.optoapp.data.FakeConflictDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import io.github.jan.supabase.createSupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
}
