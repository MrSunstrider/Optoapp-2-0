package com.example.optoapp.domain

import com.example.optoapp.data.FakeConflictDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import io.github.jan.supabase.SupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SyncProveedoresUseCaseDownloadGuardTest {

    private val opticaId = "optica-proveedores-guard"

    private val repository = mockk<ProveedorRepository>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val database = mockk<OptoDatabase>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = FakeConflictDao()
    private val networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true)

    private lateinit var useCase: SyncProveedoresUseCase

    @Before
    fun setUp() {
        conflictDao.returnEntityIds = emptyList()
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
        coEvery { syncStateTracker.markError(any(), any(), any(), any()) } just Runs

        useCase = SyncProveedoresUseCase(
            repository = repository,
            supabase = supabase,
            database = database,
            syncStateTracker = syncStateTracker,
            conflictHelper = conflictHelper,
            conflictDao = conflictDao,
            networkRetryHelper = networkRetryHelper,
        )
    }

    @Test
    fun constructor_takesSevenDependencies() {
        val constructors = SyncProveedoresUseCase::class.java.declaredConstructors
        assertEquals(1, constructors.size)
        val params = constructors[0].parameterTypes
        assertEquals(
            "SyncProveedoresUseCase should accept exactly 7 constructor params",
            7,
            params.size,
        )
    }

    @Test
    fun conflictDao_isAcceptedAsConstructorParam() {
        val constructors = SyncProveedoresUseCase::class.java.declaredConstructors
        val params = constructors[0].parameterTypes
        val hasConflictDao = params.any { it.simpleName == "ConflictDao" }
        assertTrue(
            "ConflictDao must be a constructor parameter of SyncProveedoresUseCase",
            hasConflictDao,
        )
    }

    @Test
    fun downloadProveedores_queriesConflictEntityIds() = runTest {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertTrue(
            "downloadProveedores() should call getConflictEntityIds",
            conflictDao.getConflictEntityIdsCalled.get(),
        )
    }

    @Test
    fun downloadProveedores_usesCorrectEntityType() = runTest {
        runCatching { useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true) }

        assertEquals("proveedor", conflictDao.lastEntityType)
    }
}
