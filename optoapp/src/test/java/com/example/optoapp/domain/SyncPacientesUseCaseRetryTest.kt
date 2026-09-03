package com.example.optoapp.domain

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import io.github.jan.supabase.createSupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * Pacientes network I/O must go through [NetworkRetryHelper] so transient
 * "prematurely closed the connection" failures are retried like finanzas.
 */
class SyncPacientesUseCaseRetryTest {

    private val opticaId = "optica-retry-pacientes"
    private val repository = mockk<OptoRepository>(relaxed = true)
    private val database = mockk<OptoDatabase>(relaxed = true)
    private val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
    private val conflictHelper = mockk<ConflictHelper>(relaxed = true)
    private val conflictDao = mockk<ConflictDao>(relaxed = true)
    private val networkRetryHelper = mockk<NetworkRetryHelper>(relaxed = true)

    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://placeholder.supabase.co",
        supabaseKey = "placeholder-key",
    ) {}

    @Before
    fun setUp() {
        every { syncStateTracker.dao } returns mockk(relaxed = true)
        coEvery { conflictDao.getConflictEntityIds(any(), any()) } returns emptyList()
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } coAnswers { }
    }

    @Test
    fun `download wraps select in retryNetwork`() = runBlocking {
        val useCase = SyncPacientesUseCase(
            repository,
            fakeSupabase,
            database,
            syncStateTracker,
            conflictHelper,
            conflictDao,
            networkRetryHelper,
        )

        useCase.invoke(opticaId, downloadAfterUpload = true, skipUpload = true)

        coVerify(atLeast = 1) {
            networkRetryHelper.retryNetwork(match { it.startsWith("download:") }, any())
        }
    }
}
