package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.arqueo.ArqueoCaja
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
 * Verifies that uploadDispensacionItems and uploadArqueos
 * filter conflicted entities via conflictHelper.filterConflicts
 * before pushing to Supabase.
 */
class UploadSyncCoordinatorConflictFilterTest {

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
        io.mockk.mockkStatic("android.util.Log")
        io.mockk.every { android.util.Log.d(any(), any()) } returns 0
        io.mockk.every { android.util.Log.w(any(), any<String>()) } returns 0
        io.mockk.every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        io.mockk.every { android.util.Log.e(any(), any<String>()) } returns 0
        io.mockk.every { android.util.Log.e(any(), any<String>(), any()) } returns 0

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

    private fun dispensacionItem(id: String) = DispensacionItem(
        id = id,
        dispensacionId = "disp-001",
        opticaId = opticaId
    )

    private fun arqueoCaja(id: String) = ArqueoCaja(
        id = id,
        fecha = LocalDate.now(),
        opticaId = opticaId,
        fondoCaja = 0.0,
        efectivoContado = 0.0,
        tarjetaContado = 0.0,
        transferenciaContado = 0.0,
        movilContado = 0.0,
        efectivoCobrado = 0.0,
        tarjetaCobrado = 0.0,
        transferenciaCobrado = 0.0,
        movilCobrado = 0.0,
        diferenciaEfectivo = 0.0,
        diferenciaTarjeta = 0.0,
        diferenciaTransferencia = 0.0,
        diferenciaMovil = 0.0,
        diferenciaTotal = 0.0,
        cerradoPor = "user-001",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    // ─── 6.1 RED: Upload conflict filter tests ─────────────────────────────

    @Test
    fun uploadDispensacionItems_callsFilterConflicts() = runBlocking {
        val item1 = dispensacionItem("di-1")
        val item2 = dispensacionItem("di-2")
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns listOf(item1, item2)
        coEvery {
            conflictHelper.filterConflicts(
                tableName = any(),
                opticaId = opticaId,
                entityType = "dispensacion_item",
                localEntities = any()
            )
        } returns listOf(LocalEntity("di-1", ""), LocalEntity("di-2", ""))
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } just Runs

        coordinator.uploadDispensacionItems(opticaId)

        coVerify {
            conflictHelper.filterConflicts(
                tableName = "dispensacion_items",
                opticaId = opticaId,
                entityType = "dispensacion_item",
                localEntities = any()
            )
        }
    }

    @Test
    fun uploadDispensacionItems_whenAllConflicted_doesNotCallRetryNetwork() = runBlocking {
        val item1 = dispensacionItem("di-conflict")
        coEvery { repository.getDispensacionItemsSnapshotForOptica(opticaId) } returns listOf(item1)
        coEvery {
            conflictHelper.filterConflicts(any(), any(), any(), any())
        } returns emptyList()

        coordinator.uploadDispensacionItems(opticaId)

        coVerify(exactly = 0) { networkRetryHelper.retryNetwork(any(), any()) }
    }

    @Test
    fun uploadArqueos_callsFilterConflicts() = runBlocking {
        val arq1 = arqueoCaja("arq-1")
        val arq2 = arqueoCaja("arq-2")
        coEvery { repository.getArqueosByOpticaList(opticaId) } returns listOf(arq1, arq2)
        coEvery {
            conflictHelper.filterConflicts(
                tableName = any(),
                opticaId = opticaId,
                entityType = "arqueo_caja",
                localEntities = any()
            )
        } returns listOf(LocalEntity("arq-1", "2026-01-01T00:00:00Z"), LocalEntity("arq-2", "2026-01-01T00:00:00Z"))
        coEvery { networkRetryHelper.retryNetwork(any(), any()) } just Runs

        coordinator.uploadArqueos(opticaId)

        coVerify {
            conflictHelper.filterConflicts(
                tableName = "arqueo_caja",
                opticaId = opticaId,
                entityType = "arqueo_caja",
                localEntities = any()
            )
        }
    }

    @Test
    fun uploadArqueos_whenAllConflicted_doesNotUpload() = runBlocking {
        val arq1 = arqueoCaja("arq-conflict")
        coEvery { repository.getArqueosByOpticaList(opticaId) } returns listOf(arq1)
        coEvery {
            conflictHelper.filterConflicts(any(), any(), any(), any())
        } returns emptyList()

        val uploaded = coordinator.uploadArqueos(opticaId)

        // When all arqueos are conflicted, uploaded count should be 0
        assert(uploaded == 0) { "Expected 0 uploaded, got $uploaded" }
    }
}
