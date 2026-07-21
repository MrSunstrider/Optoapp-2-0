package com.example.optoapp.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.ConflictRecord
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.MembershipRepository
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.OrdenCompra
import com.example.optoapp.data.OrdenCompraRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Proveedor
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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelBumpCoverageTest {

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
    private lateinit var syncOrdenesCompraUseCase: SyncOrdenesCompraUseCase
    private lateinit var syncInventarioFisicoUseCase: SyncInventarioFisicoUseCase
    private lateinit var syncInventoryKpisUseCase: SyncInventoryKpisUseCase
    private lateinit var syncGate: SyncGate
    private lateinit var conflictDao: ConflictDao
    private lateinit var syncEntityStateDao: SyncEntityStateDao
    private lateinit var supabaseObserver: TableObserver
    private lateinit var bgErrorCollector: BackgroundErrorCollector
    private lateinit var postSaveSyncScheduler: PostSaveSyncScheduler

    private lateinit var viewModel: SyncViewModel

    private val testOpticaId = "optica-bump-test"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        io.mockk.mockkStatic("android.util.Log")
        io.mockk.every { android.util.Log.d(any(), any()) } returns 0
        io.mockk.every { android.util.Log.w(any(), any<String>()) } returns 0
        io.mockk.every { android.util.Log.e(any(), any<String>(), any()) } returns 0

        context = mockk(relaxed = true)
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
        syncOrdenesCompraUseCase = mockk(relaxed = true)
        syncInventarioFisicoUseCase = mockk(relaxed = true)
        syncInventoryKpisUseCase = mockk(relaxed = true)
        syncGate = SyncGate()
        conflictDao = mockk(relaxed = true)
        syncEntityStateDao = mockk(relaxed = true)
        supabaseObserver = mockk(relaxed = true)
        bgErrorCollector = mockk(relaxed = true)
        postSaveSyncScheduler = mockk(relaxed = true)

        io.mockk.every { sessionManager.opticaId } returns flowOf(testOpticaId)

        coEvery { syncPacientesUseCase(any(), any(), any()) } returns Resource.Success(PacientesSyncResult(0, 0))
        coEvery { syncHistorialUseCase(any(), any(), any()) } returns Resource.Success(HistorialSyncResult(0, 0))
        coEvery { syncFinanzasUseCase(any(), any(), any()) } returns Resource.Success(
            FinanzasSyncResult(
                uploadedDispensaciones = 0,
                uploadedServicios = 0,
                uploadedPagos = 0,
                downloadedDispensaciones = 0,
                downloadedServicios = 0,
                downloadedPagos = 0,
            ),
        )
        coEvery { syncInventarioUseCase(any(), any(), any()) } returns Resource.Success(
            InventarioSyncResult(0, 0, 0, 0),
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
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private fun makeConflict(entityId: String, entityType: String) = ConflictRecord(
        entityId = entityId,
        opticaId = testOpticaId,
        entityType = entityType,
        localSnapshot = """{"id":"$entityId"}""",
        remoteSnapshot = """{"id":"$entityId","remote":true}""",
    )

    private fun paciente(id: String) = Paciente(
        id = id,
        nombreCompleto = "Test Patient",
        edad = 30,
        telefono = "555-0001",
        fechaCreacion = LocalDate.of(2026, 1, 1),
        opticaId = testOpticaId,
    )

    private fun evaluacion(id: String) = EvaluacionClinica(
        id = id,
        pacienteId = "pac-001",
        fecha = LocalDate.now(),
        opticaId = testOpticaId,
    )

    private fun montura(id: String) = Montura(
        id = id,
        modelo = "Test",
        stockActual = 5,
        opticaId = testOpticaId,
    )

    private fun proveedor(id: String) = Proveedor(
        id = id,
        nombre = "Proveedor Test",
        ruc = "12345678",
        opticaId = testOpticaId,
    )

    private fun ordenCompra(id: String) = OrdenCompra(
        id = id,
        numero = "OC-001",
        fecha = LocalDate.now(),
        proveedorId = "prov-001",
        estado = "PENDIENTE",
        total = 100.0,
        opticaId = testOpticaId,
    )

    // ─── 4.1 RED: Bump coverage tests ──────────────────────────────────────

    @Test
    fun bumpPaciente_callsUpdatePaciente() = runTest(testDispatcher) {
        val conflict = makeConflict("pac-bump", "paciente")
        val entity = paciente("pac-bump")
        coEvery { repository.getPacienteById("pac-bump") } returns Resource.Success(entity)
        coEvery { repository.updatePaciente(any()) } just Runs

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.updatePaciente(any())
            syncPacientesUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        }
    }

    @Test
    fun bumpEvaluacion_callsUpdateEvaluacion() = runTest(testDispatcher) {
        val conflict = makeConflict("eval-bump", "evaluacion")
        val entity = evaluacion("eval-bump")
        coEvery { repository.getEvaluacionById("eval-bump") } returns Resource.Success(entity)
        coEvery { repository.updateEvaluacion(any()) } just Runs

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.updateEvaluacion(any())
            syncHistorialUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        }
    }

    @Test
    fun bumpMontura_callsUpdateMontura() = runTest(testDispatcher) {
        val conflict = makeConflict("mont-bump", "montura")
        val entity = montura("mont-bump")
        coEvery { repository.getMonturaById("mont-bump", any()) } returns Resource.Success(entity)
        coEvery { repository.updateMontura(any()) } just Runs

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            repository.updateMontura(any())
            syncInventarioUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        }
    }

    @Test
    fun bumpProveedor_callsProveedorRepositoryUpdate() = runTest(testDispatcher) {
        val conflict = makeConflict("prov-bump", "proveedor")
        val entity = proveedor("prov-bump")
        coEvery { proveedorRepository.getById("prov-bump") } returns entity
        coEvery { proveedorRepository.update(any()) } just Runs

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            proveedorRepository.update(any())
            syncProveedoresUseCase(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        }
    }

    @Test
    fun bumpOrdenCompra_callsOrdenCompraRepositoryUpdate() = runTest(testDispatcher) {
        val conflict = makeConflict("oc-bump", "orden_compra")
        val entity = ordenCompra("oc-bump")
        coEvery { ordenCompraRepository.getById("oc-bump") } returns entity
        coEvery { ordenCompraRepository.update(any()) } just Runs

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            ordenCompraRepository.update(any())
            syncOrdenesCompraUseCase.invoke(testOpticaId, skipUpload = false, downloadAfterUpload = true)
        }
    }

    @Test
    fun bumpPaciente_whenNotFound_logsWarningAndDoesNotCrash() = runTest(testDispatcher) {
        val conflict = makeConflict("pac-missing", "paciente")
        coEvery { repository.getPacienteById("pac-missing") } returns Resource.Error("Not found")

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.updatePaciente(any()) }
    }

    @Test
    fun bumpMontura_whenNotFound_logsWarningAndDoesNotCrash() = runTest(testDispatcher) {
        val conflict = makeConflict("mont-missing", "montura")
        coEvery { repository.getMonturaById("mont-missing", any()) } returns Resource.Error("Not found")

        viewModel.resolveKeepMine(conflict)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.updateMontura(any()) }
    }
}
