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
class SyncInventarioUseCaseDownloadGuardTest {

    private val opticaId = "optica-inventario-guard"

    private val repository = mockk<OptoRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = FakeConflictDao()

    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://placeholder.supabase.co",
        supabaseKey = "placeholder-key"
    ) {}

    private lateinit var useCase: SyncInventarioUseCase

    @Before
    fun setUp() {
        conflictDao.returnEntityIds = emptyList()
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
        coEvery { syncStateTracker.markError(any(), any(), any(), any()) } just Runs

        useCase = SyncInventarioUseCase(
            repository = repository,
            supabase = fakeSupabase,
            syncStateTracker = syncStateTracker,
            conflictHelper = conflictHelper,
            conflictDao = conflictDao
        )
    }

    // ─── Constructor contract ────────────────────────────────────────────

    @Test
    fun constructor_takesFiveDependencies() {
        val constructors = SyncInventarioUseCase::class.java.declaredConstructors
        assertEquals(1, constructors.size)
        val params = constructors[0].parameterTypes
        assertEquals(
            "SyncInventarioUseCase should accept exactly 5 constructor params after ConflictDao injection",
            5,
            params.size
        )
    }

    @Test
    fun conflictDao_isAcceptedAsConstructorParam() {
        val constructors = SyncInventarioUseCase::class.java.declaredConstructors
        val params = constructors[0].parameterTypes
        val hasConflictDao = params.any { it.simpleName == "ConflictDao" }
        assertTrue(
            "ConflictDao must be a constructor parameter of SyncInventarioUseCase",
            hasConflictDao
        )
    }

    // ─── Guard: downloadMonturas fires with "montura" ─────────────────────

    @Test
    fun downloadMonturas_queriesConflictEntityIds() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertTrue(
            "downloadMonturas() should call getConflictEntityIds",
            conflictDao.getConflictEntityIdsCalled.get()
        )
    }

    @Test
    fun downloadMonturas_usesCorrectEntityType() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertEquals("montura", conflictDao.lastEntityType)
    }
}
