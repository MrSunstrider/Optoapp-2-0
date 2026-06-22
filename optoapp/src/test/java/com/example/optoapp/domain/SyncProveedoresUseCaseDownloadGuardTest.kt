package com.example.optoapp.domain

import com.example.optoapp.data.FakeConflictDao
import com.example.optoapp.data.ProveedorRepository
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
class SyncProveedoresUseCaseDownloadGuardTest {

    private val opticaId = "optica-proveedores-guard"

    private val repository = mockk<ProveedorRepository>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = FakeConflictDao()

    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://placeholder.supabase.co",
        supabaseKey = "placeholder-key"
    ) {}

    private lateinit var useCase: SyncProveedoresUseCase

    @Before
    fun setUp() {
        conflictDao.returnEntityIds = emptyList()
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
        coEvery { syncStateTracker.markError(any(), any(), any(), any()) } just Runs

        useCase = SyncProveedoresUseCase(
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
        val constructors = SyncProveedoresUseCase::class.java.declaredConstructors
        assertEquals(1, constructors.size)
        val params = constructors[0].parameterTypes
        assertEquals(
            "SyncProveedoresUseCase should accept exactly 5 constructor params after ConflictDao injection",
            5,
            params.size
        )
    }

    @Test
    fun conflictDao_isAcceptedAsConstructorParam() {
        val constructors = SyncProveedoresUseCase::class.java.declaredConstructors
        val params = constructors[0].parameterTypes
        val hasConflictDao = params.any { it.simpleName == "ConflictDao" }
        assertTrue(
            "ConflictDao must be a constructor parameter of SyncProveedoresUseCase",
            hasConflictDao
        )
    }

    // ─── Guard: downloadProveedores fires with "proveedor" ───────────────

    @Test
    fun downloadProveedores_queriesConflictEntityIds() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertTrue(
            "downloadProveedores() should call getConflictEntityIds",
            conflictDao.getConflictEntityIdsCalled.get()
        )
    }

    @Test
    fun downloadProveedores_usesCorrectEntityType() = runBlocking {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertEquals("proveedor", conflictDao.lastEntityType)
    }
}
