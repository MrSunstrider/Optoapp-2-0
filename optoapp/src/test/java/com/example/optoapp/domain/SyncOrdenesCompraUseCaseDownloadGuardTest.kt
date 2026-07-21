package com.example.optoapp.domain

import com.example.optoapp.data.FakeConflictDao
import com.example.optoapp.data.OrdenCompraRepository
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
class SyncOrdenesCompraUseCaseDownloadGuardTest {

    private val opticaId = "optica-oc-guard"

    private val repository = mockk<OrdenCompraRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = FakeConflictDao()

    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://placeholder.supabase.co",
        supabaseKey = "placeholder-key",
    ) {}

    private lateinit var useCase: SyncOrdenesCompraUseCase

    @Before
    fun setUp() {
        conflictDao.returnEntityIds = emptyList()
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
        coEvery { syncStateTracker.markError(any(), any(), any(), any()) } just Runs

        useCase = SyncOrdenesCompraUseCase(
            repository = repository,
            supabase = fakeSupabase,
            syncStateTracker = syncStateTracker,
            conflictHelper = conflictHelper,
            conflictDao = conflictDao,
        )
    }

    @Test
    fun constructor_takesFiveDependencies() {
        val constructors = SyncOrdenesCompraUseCase::class.java.declaredConstructors
        assertEquals(1, constructors.size)
        val params = constructors[0].parameterTypes
        assertEquals(
            "SyncOrdenesCompraUseCase should accept exactly 5 constructor params after ConflictDao injection",
            5,
            params.size,
        )
    }

    @Test
    fun conflictDao_isAcceptedAsConstructorParam() {
        val constructors = SyncOrdenesCompraUseCase::class.java.declaredConstructors
        val params = constructors[0].parameterTypes
        val hasConflictDao = params.any { it.simpleName == "ConflictDao" }
        assertTrue(
            "ConflictDao must be a constructor parameter of SyncOrdenesCompraUseCase",
            hasConflictDao,
        )
    }

    @Test
    fun downloadOrdenesCompra_queriesConflictEntityIds() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertTrue(
            "downloadOrdenesCompra() should call getConflictEntityIds",
            conflictDao.getConflictEntityIdsCalled.get(),
        )
    }

    @Test
    fun downloadOrdenesCompra_usesCorrectEntityType() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertEquals("orden_compra", conflictDao.lastEntityType)
    }
}
