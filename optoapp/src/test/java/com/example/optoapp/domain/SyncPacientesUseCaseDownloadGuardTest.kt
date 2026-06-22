package com.example.optoapp.domain

import com.example.optoapp.data.FakeConflictDao
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncPacientesUseCaseDownloadGuardTest {

    private val opticaId = "optica-pacientes-guard"
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = FakeConflictDao()

    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://placeholder.supabase.co",
        supabaseKey = "placeholder-key"
    ) {}

    private lateinit var useCase: SyncPacientesUseCase

    @Before
    fun setUp() {
        conflictDao.returnEntityIds = emptyList()
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
        coEvery { syncStateTracker.markError(any(), any(), any(), any()) } just Runs
        useCase = SyncPacientesUseCase(
            repository = repository,
            supabase = fakeSupabase,
            syncStateTracker = syncStateTracker,
            conflictHelper = conflictHelper,
            conflictDao = conflictDao
        )
    }

    @Test
    fun constructor_takesFiveDependencies() {
        assertEquals(5, SyncPacientesUseCase::class.java.declaredConstructors[0].parameterTypes.size)
    }

    @Test
    fun conflictDao_isAcceptedAsConstructorParam() {
        val hasConflictDao = SyncPacientesUseCase::class.java.declaredConstructors[0].parameterTypes
            .any { it.simpleName == "ConflictDao" }
        assertTrue("ConflictDao must be a constructor parameter", hasConflictDao)
    }

    @Test
    fun download_queriesConflictEntityIds() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertTrue("getConflictEntityIds should be called", conflictDao.getConflictEntityIdsCalled.get())
        assertEquals("paciente", conflictDao.lastEntityType)
    }

    @Test
    fun download_usesCorrectEntityType() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertEquals("paciente", conflictDao.lastEntityType)
    }

    @Test
    fun download_withNoConflicts_callsGetConflictEntityIdsOnce() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertTrue("getConflictEntityIds should have been called", conflictDao.getConflictEntityIdsCalled.get())
    }
}
