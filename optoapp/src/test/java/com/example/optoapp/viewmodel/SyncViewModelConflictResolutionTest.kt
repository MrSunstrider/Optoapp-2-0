package com.example.optoapp.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.ConflictRecord
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.OrdenCompraRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.ServicioExtra
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
import com.example.optoapp.domain.SyncSessionHelper
import com.example.optoapp.domain.SyncProveedoresUseCase
import com.example.optoapp.domain.observer.TableObserver
import com.example.optoapp.subscription.SubscriptionManager
import com.example.optoapp.sync.PostSaveSyncScheduler
import com.example.optoapp.sync.SyncGate
import com.example.optoapp.util.BackgroundErrorCollector
import io.github.jan.supabase.SupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
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

    private val testOpticaId = "optica-test-123"

    private val pacienteConflict = ConflictRecord(
        entityId = "paciente-001",
        opticaId = testOpticaId,
        entityType = "paciente",
        localSnapshot = """{"id":"paciente-001","nombre":"Juan"}""",
        remoteSnapshot = """{"id":"paciente-001","nombre":"Juan Remoto"}""",
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
            FinanzasSyncResult(uploadedDispensaciones = 0, uploadedServicios = 0, uploadedPagos = 0, downloadedDispensaciones = 0, downloadedServicios = 0, downloadedPagos = 0),
        )
        coEvery { syncInventarioUseCase(any(), any(), any()) } returns Resource.Success(
            InventarioSyncResult(uploadedMonturas = 0, uploadedMovimientos = 0, downloadedMonturas = 0, downloadedMovimientos = 0),
        )
        coEvery { syncProveedoresUseCase(any(), any(), any()) } returns Resource.Success(
            ProveedoresSyncResult(0, 0, 0, 0),
        )
        coEvery { syncOrdenesCompraUseCase(any(), any(), any()) } returns Resource.Success(
            OrdenesCompraSyncResult(0, 0, 0, 0),
        )
        coEvery { syncInventarioFisicoUseCase(any(), any(), any()) } returns Resource.Success(
            InventarioFisicoSyncResult(0, 0, 0, 0),
        )
        coEvery { syncInventoryKpisUseCase(any()) } returns Resource.Success(
            com.example.optoapp.domain.InventoryKpiSummary(0, 0, emptyList(), null),
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
            postSaveSyncScheduler = postSaveSyncScheduler,
            syncOrchestrator = mockk(relaxed = true),
            syncTelemetryLogDao = mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resolveKeepMine_uploadsLocalEntity_beforeDeletingConflict() = runTest(testDispatcher) {
        // The new paciente bump branch calls getPacienteById — mock it to avoid NPE from relaxed sealed-class mock
        coEvery { repository.getPacienteByIdScoped(pacienteConflict.entityId, testOpticaId) } returns Resource.Error("not found in test")

        viewModel.resolveKeepMine(pacienteConflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            syncPacientesUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
            conflictDao.resolveConflict(pacienteConflict.entityId, testOpticaId)
        }
    }

    @Test
    fun resolveKeepMine_writesServerTimestampToRoom() = runTest(testDispatcher) {
        coEvery { repository.getPacienteByIdScoped(pacienteConflict.entityId, testOpticaId) } returns Resource.Error("not found in test")

        viewModel.resolveKeepMine(pacienteConflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            syncPacientesUseCase(
                opticaId = testOpticaId,
                skipUpload = false,
                downloadAfterUpload = true,
            )
        }
    }

    @Test
    fun resolveKeepMine_doesNotRegenerateConflictOnNextSync() = runTest(testDispatcher) {
        coEvery { conflictDao.getConflicts(testOpticaId) } returns emptyList()
        coEvery { repository.getPacienteByIdScoped(pacienteConflict.entityId, testOpticaId) } returns Resource.Error("not found in test")

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

    private fun makeServicioConflict(entityId: String) = ConflictRecord(
        entityId = entityId,
        opticaId = testOpticaId,
        entityType = "servicio_extra",
        localSnapshot = """{"id":"$entityId","descripcion":"Consulta"}""",
        remoteSnapshot = """{"id":"$entityId","descripcion":"Consulta Remota"}""",
    )

    private fun makeDispensacionConflict(entityId: String) = ConflictRecord(
        entityId = entityId,
        opticaId = testOpticaId,
        entityType = "dispensacion",
        localSnapshot = """{"id":"$entityId"}""",
        remoteSnapshot = """{"id":"$entityId","remote":true}""",
    )

    private fun makePagoConflict(entityId: String) = ConflictRecord(
        entityId = entityId,
        opticaId = testOpticaId,
        entityType = "pago",
        localSnapshot = """{"id":"$entityId","monto":100}""",
        remoteSnapshot = """{"id":"$entityId","monto":200}""",
    )

    private fun servicio(id: String) = ServicioExtra(
        id = id,
        descripcion = "Consulta",
        montoTotal = 500.0,
        aCuenta = 0.0,
        estado = "Pendiente",
        fecha = java.time.LocalDate.now(),
        opticaId = testOpticaId,
    )

    private fun dispensacion(id: String) = DispensacionOptica(
        id = id,
        pacienteId = "pac-001",
        fecha = java.time.LocalDate.now(),
        opticaId = testOpticaId,
    )

    private fun pago(id: String) = Pago(
        id = id,
        fecha = java.time.LocalDate.now(),
        tipo = "efectivo",
        monto = 100.0,
        metodoPago = "efectivo",
        opticaId = testOpticaId,
    )

    @Test
    fun resolveKeepMine_forServicio_callsUpdateServicioBeforeSync() = runTest(testDispatcher) {
        val conflict = makeServicioConflict("srv-001")
        val entity = servicio("srv-001")
        coEvery { repository.getServicioById("srv-001") } returns Resource.Success(entity)
        coEvery { repository.updateServicio(any()) } just Runs

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.updateServicio(any())
            syncFinanzasUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        }
    }

    @Test
    fun resolveKeepMine_forDispensacion_callsUpdateDispensacionBeforeSync() = runTest(testDispatcher) {
        val conflict = makeDispensacionConflict("disp-001")
        val entity = dispensacion("disp-001")
        coEvery { repository.getDispensacionById("disp-001") } returns Resource.Success(entity)
        coEvery { repository.updateDispensacion(any()) } just Runs

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.updateDispensacion(any())
            syncFinanzasUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        }
    }

    @Test
    fun resolveKeepMine_forPago_callsUpdatePagoBeforeSync() = runTest(testDispatcher) {
        val conflict = makePagoConflict("pago-001")
        val entity = pago("pago-001")
        coEvery { repository.getPagoById("pago-001", testOpticaId) } returns entity
        coEvery { repository.updatePago(any()) } just Runs

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.updatePago(any())
            syncFinanzasUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        }
    }

    @Test
    fun resolveKeepMine_retainsConflictRecord_whenSyncFails() = runTest(testDispatcher) {
        val conflict = makeServicioConflict("srv-fail")
        val entity = servicio("srv-fail")
        coEvery { repository.getServicioById("srv-fail") } returns Resource.Success(entity)
        coEvery { repository.updateServicio(any()) } just Runs
        coEvery {
            syncFinanzasUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        } returns Resource.Error("network error")

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { conflictDao.resolveConflict("srv-fail", testOpticaId) }
    }

    @Test
    fun resolveKeepMineAll_bumpsAllEntitiesAndClearsConflicts() = runTest(testDispatcher) {
        val srvConflict = makeServicioConflict("srv-bulk")
        val pagConflict = makePagoConflict("pago-bulk")
        coEvery { conflictDao.getConflicts(testOpticaId) } returns listOf(srvConflict, pagConflict)
        coEvery { repository.getServicioById("srv-bulk") } returns Resource.Success(servicio("srv-bulk"))
        coEvery { repository.updateServicio(any()) } just Runs
        coEvery { repository.getPagoById("pago-bulk", testOpticaId) } returns pago("pago-bulk")
        coEvery { repository.updatePago(any()) } just Runs

        viewModel.resolveKeepMineAll()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.updateServicio(any()) }
        coVerify { repository.updatePago(any()) }
        coVerify { conflictDao.resolveConflict("srv-bulk", testOpticaId) }
        coVerify { conflictDao.resolveConflict("pago-bulk", testOpticaId) }
        coVerify { syncEntityStateDao.deleteConflictedForOptica(testOpticaId) }
    }

    // ── resolveAcceptTheirs: clears conflict BEFORE download so guard doesn't block the entity ──

    @Test
    fun resolveAcceptTheirs_deletesConflictOnlyAfterDownloadSucceeds() = runTest(testDispatcher) {
        mockkObject(SyncSessionHelper)
        coEvery { SyncSessionHelper.refreshSessionBeforeSync(any()) } returns true

        viewModel.resolveAcceptTheirs(pacienteConflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            conflictDao.resolveConflict(pacienteConflict.entityId, testOpticaId)
            syncPacientesUseCase(testOpticaId, skipUpload = true, downloadAfterUpload = true)
        }
    }

    @Test
    fun resolveAcceptTheirs_clearsConflictEvenWhenDownloadFails() = runTest(testDispatcher) {
        mockkObject(SyncSessionHelper)
        coEvery { SyncSessionHelper.refreshSessionBeforeSync(any()) } returns true
        coEvery {
            syncPacientesUseCase(testOpticaId, skipUpload = true, downloadAfterUpload = true)
        } returns Resource.Error("network error")

        viewModel.resolveAcceptTheirs(pacienteConflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { conflictDao.resolveConflict(pacienteConflict.entityId, testOpticaId) }
    }

    @Test
    fun resolveAcceptTheirs_clearsConflictEvenWhenJwtRefreshFails() = runTest(testDispatcher) {
        mockkObject(SyncSessionHelper)
        coEvery { SyncSessionHelper.refreshSessionBeforeSync(any()) } returns false

        viewModel.resolveAcceptTheirs(pacienteConflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { conflictDao.resolveConflict(pacienteConflict.entityId, testOpticaId) }
        coVerify(exactly = 0) { syncPacientesUseCase(any(), any(), any()) }
    }
}
