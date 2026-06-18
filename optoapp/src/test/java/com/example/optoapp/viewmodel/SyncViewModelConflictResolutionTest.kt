package com.example.optoapp.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.ConflictRecord
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.FinanzasSyncResult
import com.example.optoapp.domain.HistorialSyncResult
import com.example.optoapp.domain.InventarioSyncResult
import com.example.optoapp.domain.PacientesSyncResult
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SyncTelemetry
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.domain.SyncOrdenesCompraUseCase
import com.example.optoapp.domain.SyncInventoryKpisUseCase
import com.example.optoapp.domain.ProveedoresSyncResult
import com.example.optoapp.domain.OrdenesCompraSyncResult
import com.example.optoapp.domain.observer.TableObserver
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.sync.SyncGate
import com.example.optoapp.util.BackgroundErrorCollector
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelConflictResolutionTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var membershipRepository: MembershipRepository
    private lateinit var repository: OptoRepository
    private lateinit var syncTelemetry: SyncTelemetry
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var supabase: SupabaseClient
    private lateinit var syncPacientesUseCase: SyncPacientesUseCase
    private lateinit var syncHistorialUseCase: SyncHistorialUseCase
    private lateinit var syncFinanzasUseCase: SyncFinanzasUseCase
    private lateinit var syncInventarioUseCase: SyncInventarioUseCase
    private lateinit var syncProveedoresUseCase: SyncProveedoresUseCase
    private lateinit var syncGate: SyncGate
    private lateinit var conflictDao: ConflictDao
    private lateinit var syncEntityStateDao: SyncEntityStateDao
    private lateinit var supabaseObserver: TableObserver
    private lateinit var bgErrorCollector: BackgroundErrorCollector
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler

    private lateinit var viewModel: SyncViewModel

    private val testOpticaId = "optica-test-123"

    private val pacienteConflict = ConflictRecord(
        entityId = "paciente-001",
        opticaId = testOpticaId,
        entityType = "paciente",
        localSnapshot = """{"id":"paciente-001","nombre":"Juan"}""",
        remoteSnapshot = """{"id":"paciente-001","nombre":"Juan Remoto"}"""
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic("android.util.Log")
        io.mockk.every { android.util.Log.d(any(), any()) } returns 0
        io.mockk.every { android.util.Log.w(any(), any<String>()) } returns 0
        io.mockk.every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        context = mockk(relaxed = true)
        // Make isNetworkAvailable() return false so performFullDownload() short-circuits safely
        val mockConnectivityManager = mockk<ConnectivityManager>(relaxed = true)
        io.mockk.every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        io.mockk.every { mockConnectivityManager.activeNetwork } returns null
        sessionManager = mockk(relaxed = true)
        membershipRepository = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        syncTelemetry = mockk(relaxed = true)
        subscriptionManager = mockk(relaxed = true)
        supabase = mockk(relaxed = true)
        syncPacientesUseCase = mockk(relaxed = true)
        syncHistorialUseCase = mockk(relaxed = true)
        syncFinanzasUseCase = mockk(relaxed = true)
        syncInventarioUseCase = mockk(relaxed = true)
        syncProveedoresUseCase = mockk(relaxed = true)
        val syncOrdenesCompraUseCase: SyncOrdenesCompraUseCase = mockk(relaxed = true)
        val syncInventoryKpisUseCase: SyncInventoryKpisUseCase = mockk(relaxed = true)
        syncGate = SyncGate() // real SyncGate; its mutex is unlocked by default
        conflictDao = mockk(relaxed = true)
        syncEntityStateDao = mockk(relaxed = true)
        supabaseObserver = mockk(relaxed = true)
        bgErrorCollector = mockk(relaxed = true)
        postSaveSyncScheduler = mockk(relaxed = true)

        io.mockk.every { sessionManager.opticaId } returns flowOf(testOpticaId)

        coEvery { syncPacientesUseCase(any(), any(), any()) } returns Resource.Success(PacientesSyncResult(0, 0))
        coEvery { syncHistorialUseCase(any(), any(), any()) } returns Resource.Success(HistorialSyncResult(0, 0))
        coEvery { syncFinanzasUseCase(any(), any(), any()) } returns Resource.Success(
            FinanzasSyncResult(uploadedDispensaciones = 0, uploadedServicios = 0, uploadedPagos = 0, downloadedDispensaciones = 0, downloadedServicios = 0, downloadedPagos = 0)
        )
        coEvery { syncInventarioUseCase(any(), any(), any()) } returns Resource.Success(
            InventarioSyncResult(uploadedMonturas = 0, uploadedMovimientos = 0, downloadedMonturas = 0, downloadedMovimientos = 0)
        )
        coEvery { syncProveedoresUseCase(any(), any(), any()) } returns Resource.Success(
            ProveedoresSyncResult(0, 0, 0, 0)
        )
        coEvery { syncOrdenesCompraUseCase(any(), any(), any()) } returns Resource.Success(
            OrdenesCompraSyncResult(0, 0, 0, 0)
        )
        coEvery { syncInventoryKpisUseCase(any()) } returns Resource.Success(
            com.example.optoapp.domain.InventoryKpiSummary(0, 0, emptyList(), null)
        )

        coEvery { conflictDao.resolveConflict(any(), any()) } just Runs
        coEvery { conflictDao.clearConflicts(any()) } just Runs
        coEvery { syncEntityStateDao.deleteConflictedForOptica(any()) } just Runs

        viewModel = SyncViewModel(
            context = context,
            sessionManager = sessionManager,
            membershipRepository = membershipRepository,
            repository = repository,
            syncTelemetry = syncTelemetry,
            subscriptionManager = subscriptionManager,
            supabase = supabase,
            syncPacientesUseCase = syncPacientesUseCase,
            syncHistorialUseCase = syncHistorialUseCase,
            syncFinanzasUseCase = syncFinanzasUseCase,
            syncInventarioUseCase = syncInventarioUseCase,
            syncProveedoresUseCase = syncProveedoresUseCase,
            syncOrdenesCompraUseCase = syncOrdenesCompraUseCase,
            syncInventoryKpisUseCase = syncInventoryKpisUseCase,
            syncGate = syncGate,
            conflictDao = conflictDao,
            syncEntityStateDao = syncEntityStateDao,
            supabaseObserver = supabaseObserver,
            bgErrorCollector = bgErrorCollector,
            postSaveSyncScheduler = postSaveSyncScheduler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resolveKeepMine_uploadsLocalEntity_beforeDeletingConflict() = runTest(testDispatcher) {
        viewModel.resolveKeepMine(pacienteConflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            syncPacientesUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
            conflictDao.resolveConflict(pacienteConflict.entityId, testOpticaId)
        }
    }

    @Test
    fun resolveKeepMine_writesServerTimestampToRoom() = runTest(testDispatcher) {
        viewModel.resolveKeepMine(pacienteConflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            syncPacientesUseCase(
                opticaId = testOpticaId,
                skipUpload = false,
                downloadAfterUpload = true
            )
        }
    }

    @Test
    fun resolveKeepMine_doesNotRegenerateConflictOnNextSync() = runTest(testDispatcher) {
        coEvery { conflictDao.getConflicts(testOpticaId) } returns emptyList()

        viewModel.resolveKeepMine(pacienteConflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            conflictDao.resolveConflict(pacienteConflict.entityId, testOpticaId)
        }

        viewModel.refreshConflicts()
        testDispatcher.scheduler.advanceUntilIdle()

        assert(viewModel.conflicts.value.isEmpty()) {
            "After resolveKeepMine + refreshConflicts, conflicts list should be empty"
        }
    }

    @Test
    fun acceptAllCloud_clearsBothConflictRecordsAndSyncEntityState() = runTest(testDispatcher) {
        viewModel.acceptAllCloud()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { conflictDao.clearConflicts(testOpticaId) }
        coVerify { syncEntityStateDao.deleteConflictedForOptica(testOpticaId) }
    }
}
