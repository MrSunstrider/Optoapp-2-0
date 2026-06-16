package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.Pago
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.domain.sync.LocalEntity
import io.github.jan.supabase.createSupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Verifies that uploadPagos filters conflicted pagos before pushing to Supabase.
 *
 * Without filterConflicts, a pago with an active ConflictRecord silently overwrites
 * the remote version, losing the pending resolution and potentially corrupting financial
 * data (RC-6).
 */
class UploadSyncCoordinatorTest {

    private val opticaId = "test-optica"

    private val repository = mockk<OptoRepository>()
    private val syncStateTracker = mockk<SyncStateTracker>()
    private val mergeHandler = mockk<DispensacionMergeHandler>()
    private val networkRetryHelper = mockk<NetworkRetryHelper>()
    private val conflictHelper = mockk<ConflictHelper>()

    private val fakeSupabase = createSupabaseClient(
        supabaseUrl = "https://test.supabase.co",
        supabaseKey = "test-key"
    ) {}

    private lateinit var coordinator: UploadSyncCoordinator

    @Before
    fun setUp() {
        coordinator = UploadSyncCoordinator(
            repository = repository,
            supabase = fakeSupabase,
            syncStateTracker = syncStateTracker,
            mergeHandler = mergeHandler,
            networkRetryHelper = networkRetryHelper,
            conflictHelper = conflictHelper
        )
        coEvery { syncStateTracker.markSynced(any(), any(), any()) } just Runs
    }

    private fun pago(id: String, updatedAt: String? = "2026-01-01T00:00:00Z") = Pago(
        id = id,
        fecha = LocalDate.of(2026, 1, 1),
        tipo = "efectivo",
        monto = 100.0,
        metodoPago = "efectivo",
        opticaId = opticaId,
        updatedAt = updatedAt
    )

    private fun localEntity(id: String) =
        LocalEntity(id = id, updatedAt = "2026-01-01T00:00:00Z")

    @Test
    fun uploadPagos_callsFilterConflicts_beforePush() = runBlocking {
        val pago1 = pago("p1")
        val pago2 = pago("p2")
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(pago1, pago2)
        coEvery {
            conflictHelper.filterConflicts(
                tableName = "pagos",
                opticaId = opticaId,
                entityType = "pago",
                localEntities = any()
            )
        } returns listOf(localEntity("p1"), localEntity("p2"))
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } just Runs

        coordinator.uploadPagos(opticaId)

        coVerify {
            conflictHelper.filterConflicts(
                tableName = "pagos",
                opticaId = opticaId,
                entityType = "pago",
                localEntities = any()
            )
        }
    }

    @Test
    fun uploadPagos_doesNotCallRetryNetwork_whenAllPagosConflicted() = runBlocking {
        val conflictedPago = pago("p-conflict")
        coEvery { repository.getPagosSnapshotForOptica(opticaId) } returns listOf(conflictedPago)
        coEvery {
            conflictHelper.filterConflicts(any(), any(), any(), any())
        } returns emptyList()

        coordinator.uploadPagos(opticaId)

        coVerify(exactly = 0) { networkRetryHelper.retryNetwork(any(), any()) }
    }
}
