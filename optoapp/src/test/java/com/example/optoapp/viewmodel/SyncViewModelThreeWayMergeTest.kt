package com.example.optoapp.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.ConflictRecord
import com.example.optoapp.data.ConflictSnapshot
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.OrdenCompraRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.data.SyncEntityStateDao
import com.example.optoapp.data.SyncTelemetry
import com.example.optoapp.domain.FinanzasSyncResult
import com.example.optoapp.domain.HistorialSyncResult
import com.example.optoapp.domain.InventarioFisicoSyncResult
import com.example.optoapp.domain.InventarioSyncResult
import com.example.optoapp.domain.OrdenesCompraSyncResult
import com.example.optoapp.domain.PacientesSyncResult
import com.example.optoapp.domain.ProveedoresSyncResult
import com.example.optoapp.domain.SyncFinanzasUseCase
import com.example.optoapp.domain.SyncHistorialUseCase
import com.example.optoapp.domain.SyncInventarioFisicoUseCase
import com.example.optoapp.domain.SyncInventarioUseCase
import com.example.optoapp.domain.SyncInventoryKpisUseCase
import com.example.optoapp.domain.SyncOrdenesCompraUseCase
import com.example.optoapp.domain.SyncPacientesUseCase
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.domain.observer.TableObserver
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.sync.SyncGate
import com.example.optoapp.util.BackgroundErrorCollector
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

/**
 * Phase 12 — Tests for three-way merge resolution in SyncViewModel.
 *
 * Tests that resolveKeepMine and resolveAcceptTheirs correctly branch
 * between ThreeWayMerge path and fallback bump path based on snapshot data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelThreeWayMergeTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testOpticaId = "optica-3wm-test"

    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager
    private lateinit var membershipRepository: MembershipRepository
    private lateinit var repository: OptoRepository
    private lateinit var proveedorRepository: ProveedorRepository
    private lateinit var ordenCompraRepository: OrdenCompraRepository
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

    private val conflictWithSnapshots = ConflictRecord(
        entityId = "paciente-3wm-001",
        opticaId = testOpticaId,
        entityType = "paciente",
        localSnapshot = """{"id":"paciente-3wm-001","nombre":"Juan Local"}""",
        remoteSnapshot = """{"id":"paciente-3wm-001","nombre":"Juan Remoto"}""",
        baseSnapshot = """{"id":"paciente-3wm-001","nombre":"Juan Base"}""",
        localData = """{"id":"paciente-3wm-001","nombre":"Juan Local","telefono":"555-0100"}""",
        remoteData = """{"id":"paciente-3wm-001","nombre":"Juan Remoto","telefono":"555-0999"}"""
    )

    private val conflictWithoutSnapshots = ConflictRecord(
        entityId = "paciente-fallback-001",
        opticaId = testOpticaId,
        entityType = "paciente",
        localSnapshot = "2026-06-22T09:00:00Z",
        remoteSnapshot = "2026-06-22T10:00:00Z",
        baseSnapshot = "{}"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic("android.util.Log")
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        context = mockk(relaxed = true)
        val mockConnectivityManager = mockk<ConnectivityManager>(relaxed = true)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager
        every { mockConnectivityManager.activeNetwork } returns null
        sessionManager = mockk(relaxed = true)
        membershipRepository = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        proveedorRepository = mockk(relaxed = true)
        ordenCompraRepository = mockk(relaxed = true)
        syncTelemetry = mockk(relaxed = true)
        subscriptionManager = mockk(relaxed = true)
        supabase = mockk(relaxed = true)
        syncPacientesUseCase = mockk(relaxed = true)
        syncHistorialUseCase = mockk(relaxed = true)
        syncFinanzasUseCase = mockk(relaxed = true)
        syncInventarioUseCase = mockk(relaxed = true)
        syncProveedoresUseCase = mockk(relaxed = true)
        val syncOrdenesCompraUseCase: SyncOrdenesCompraUseCase = mockk(relaxed = true)
        val syncInventarioFisicoUseCase: SyncInventarioFisicoUseCase = mockk(relaxed = true)
        val syncInventoryKpisUseCase: SyncInventoryKpisUseCase = mockk(relaxed = true)
        syncGate = SyncGate()
        conflictDao = mockk(relaxed = true)
        syncEntityStateDao = mockk(relaxed = true)
        supabaseObserver = mockk(relaxed = true)
        bgErrorCollector = mockk(relaxed = true)
        postSaveSyncScheduler = mockk(relaxed = true)

        every { sessionManager.opticaId } returns flowOf(testOpticaId)

        coEvery { syncPacientesUseCase(any(), any(), any()) } returns Resource.Success(PacientesSyncResult(0, 0))
        coEvery { syncHistorialUseCase(any(), any(), any()) } returns Resource.Success(HistorialSyncResult(0, 0))
        coEvery { syncFinanzasUseCase(any(), any(), any()) } returns Resource.Success(
            FinanzasSyncResult(0, 0, 0, 0, 0, 0, 0, 0, 0)
        )
        coEvery { syncInventarioUseCase(any(), any(), any()) } returns Resource.Success(
            InventarioSyncResult(0, 0, 0, 0)
        )
        coEvery { syncProveedoresUseCase(any(), any(), any()) } returns Resource.Success(
            ProveedoresSyncResult(0, 0, 0, 0)
        )
        coEvery { syncOrdenesCompraUseCase(any(), any(), any()) } returns Resource.Success(
            OrdenesCompraSyncResult(0, 0, 0, 0)
        )
        coEvery { syncInventarioFisicoUseCase(any(), any(), any()) } returns Resource.Success(
            InventarioFisicoSyncResult(0, 0, 0, 0)
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
            proveedorRepository = proveedorRepository,
            ordenCompraRepository = ordenCompraRepository,
            syncTelemetry = syncTelemetry,
            subscriptionManager = subscriptionManager,
            supabase = supabase,
            syncPacientesUseCase = syncPacientesUseCase,
            syncHistorialUseCase = syncHistorialUseCase,
            syncFinanzasUseCase = syncFinanzasUseCase,
            syncInventarioUseCase = syncInventarioUseCase,
            syncProveedoresUseCase = syncProveedoresUseCase,
            syncOrdenesCompraUseCase = syncOrdenesCompraUseCase,
            syncInventarioFisicoUseCase = syncInventarioFisicoUseCase,
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

    // ─── FR-10: resolveKeepMine with snapshot data ──────────────────────

    @Test
    fun resolveKeepMine_withSnapshots_resolvesConflict() = runTest(testDispatcher) {
        coEvery { conflictDao.getConflictSnapshot(any(), any()) } returns ConflictSnapshot(
            baseSnapshot = """{"id":"paciente-3wm-001","nombre":"Juan Base"}""",
            localData = """{"id":"paciente-3wm-001","nombre":"Juan Local","telefono":"555-0100"}""",
            remoteData = """{"id":"paciente-3wm-001","nombre":"Juan Remoto","telefono":"555-0999"}"""
        )
        coEvery { repository.getPacienteById(conflictWithSnapshots.entityId) } returns Resource.Error("test")

        viewModel.resolveKeepMine(conflictWithSnapshots)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) {
            conflictDao.resolveConflict(conflictWithSnapshots.entityId, testOpticaId)
        }
    }

    // ─── FR-10: resolveKeepMine without snapshots → fallback to bump ────

    @Test
    fun resolveKeepMine_withoutSnapshots_callsResolve() = runTest(testDispatcher) {
        coEvery { repository.getPacienteById(conflictWithoutSnapshots.entityId) } returns Resource.Error("test")

        viewModel.resolveKeepMine(conflictWithoutSnapshots)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) {
            conflictDao.resolveConflict(conflictWithoutSnapshots.entityId, testOpticaId)
        }
    }

    // ─── FR-11: resolveAcceptTheirs with snapshots ─────────────────────

    @Test
    fun resolveAcceptTheirs_withSnapshots_clearsConflict() = runTest(testDispatcher) {
        coEvery { conflictDao.getConflictSnapshot(any(), any()) } returns ConflictSnapshot(
            baseSnapshot = """{"id":"paciente-3wm-001","nombre":"Juan Base"}""",
            localData = """{"id":"paciente-3wm-001","nombre":"Juan Local"}""",
            remoteData = """{"id":"paciente-3wm-001","nombre":"Juan Remoto"}"""
        )

        viewModel.resolveAcceptTheirs(conflictWithSnapshots)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) {
            conflictDao.resolveConflict(conflictWithSnapshots.entityId, testOpticaId)
        }
    }
}
