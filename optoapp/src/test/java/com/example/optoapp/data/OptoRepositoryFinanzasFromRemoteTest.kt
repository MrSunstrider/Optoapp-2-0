package com.example.optoapp.data

import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Unit tests (MockK) verifying the remote-bypass write path for finanzas
 * entities on [OptoRepository].
 *
 * Remote-bypass contract (Pattern C from exploration):
 *  1. Entity is passed to DAO with the ORIGINAL remote timestamp unchanged.
 *  2. PostSaveSyncScheduler is NOT called (no upload re-schedule after download).
 *
 * These tests reference constructor parameters and methods that DO NOT EXIST YET
 * on OptoRepository — RED phase.
 */
@RunWith(RobolectricTestRunner::class)
class OptoRepositoryFinanzasFromRemoteTest {

    private val T_REMOTE = "2026-06-15T10:00:00Z"
    private val testDate = LocalDate.of(2026, 6, 15)
    private val opticaId = "optica-test"

    private lateinit var gastoOperativoDao: GastoOperativoDao
    private lateinit var resumenDiarioDao: ResumenDiarioDao
    private lateinit var configuracionFinancieraDao: ConfiguracionFinancieraDao
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var schedulerLazy: Lazy<PostSaveSyncScheduler>
    private lateinit var repo: OptoRepository

    @Before
    fun setUp() {
        gastoOperativoDao = mockk(relaxed = true)
        resumenDiarioDao = mockk(relaxed = true)
        configuracionFinancieraDao = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        schedulerLazy = mockk()
        every { schedulerLazy.get() } returns scheduler

        val dispensacionRepo = mockk<DispensacionRepository>(relaxed = true)
        val pacienteRepo = mockk<PacienteRepository>(relaxed = true)
        val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
        val syncRepo = mockk<SyncRepository>(relaxed = true)
        val snapshotCoordinator = mockk<SyncSnapshotCoordinator>(relaxed = true)
        val backupCoordinator = mockk<BackupRestoreCoordinator>(relaxed = true)
        val monturaCoordinator = mockk<MonturaInventoryCoordinator>(relaxed = true)
        val database = mockk<OptoDatabase>(relaxed = true)

        repo = OptoRepository(
            database = database,
            syncStateTracker = syncStateTracker,
            postSaveSyncScheduler = schedulerLazy,
            pacienteRepo = pacienteRepo,
            dispensacionRepo = dispensacionRepo,
            syncRepo = syncRepo,
            snapshotCoordinator = snapshotCoordinator,
            backupCoordinator = backupCoordinator,
            monturaCoordinator = monturaCoordinator,
            gastoOperativoDao = gastoOperativoDao,
            resumenDiarioDao = resumenDiarioDao,
            configuracionFinancieraDao = configuracionFinancieraDao,
            categoriaProductoDao = mockk(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── GastoOperativo remote bypass: timestamp preserved, scheduler NOT called ──

    @Test
    fun `upsertGastoOperativoFromRemote passes entity with original timestamp to dao`() = runTest {
        val entity = GastoOperativoEntity(
            id = "g1", opticaId = opticaId, categoria = "alquiler",
            descripcion = "Local junio", monto = 500.0, fecha = testDate,
            createdAt = T_REMOTE
        )

        repo.upsertGastoOperativoFromRemote(entity)

        coVerify(exactly = 1) { gastoOperativoDao.upsert(match { it.createdAt == T_REMOTE }) }
    }

    @Test
    fun `upsertGastoOperativoFromRemote does NOT call PostSaveSyncScheduler`() = runTest {
        val entity = GastoOperativoEntity(
            id = "g1", opticaId = opticaId, categoria = "alquiler",
            descripcion = "Local junio", monto = 500.0, fecha = testDate,
            createdAt = T_REMOTE
        )

        repo.upsertGastoOperativoFromRemote(entity)

        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
        coVerify(exactly = 0) { scheduler.scheduleHistorialSync(any()) }
        coVerify(exactly = 0) { scheduler.schedulePacientesSync(any()) }
    }

    // ── ResumenDiario remote bypass: download-only, no sync scheduling ──────

    @Test
    fun `upsertResumenDiarioFromRemote passes entity without scheduling sync`() = runTest {
        val entity = ResumenDiarioEntity(
            id = "rd1", opticaId = opticaId, fecha = "2026-06-15",
            ventasCantidad = 12, ventasMontoTotal = 3500.0,
            ventasCostoTotal = 1400.0, cobrosCantidad = 8,
            cobrosMontoTotal = 2800.0, saldoPendienteTotal = 700.0,
            saldoPendienteCantidad = 4, calculadoEn = T_REMOTE
        )

        repo.upsertResumenDiarioFromRemote(entity)

        coVerify(exactly = 1) { resumenDiarioDao.upsert(entity) }
    }

    @Test
    fun `upsertResumenDiarioFromRemote does NOT call PostSaveSyncScheduler`() = runTest {
        val entity = ResumenDiarioEntity(
            id = "rd1", opticaId = opticaId, fecha = "2026-06-15",
            ventasCantidad = 5, ventasMontoTotal = 1200.0, ventasCostoTotal = 500.0,
            cobrosCantidad = 3, cobrosMontoTotal = 900.0, saldoPendienteTotal = 300.0,
            saldoPendienteCantidad = 2, calculadoEn = T_REMOTE
        )

        repo.upsertResumenDiarioFromRemote(entity)

        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
        coVerify(exactly = 0) { scheduler.scheduleHistorialSync(any()) }
        coVerify(exactly = 0) { scheduler.schedulePacientesSync(any()) }
    }

    // ── ConfiguracionFinanciera remote bypass: download-only, no sync scheduling

    @Test
    fun `upsertConfiguracionFinancieraFromRemote passes entity without scheduling sync`() = runTest {
        val entity = ConfiguracionFinancieraEntity(
            opticaId = opticaId, margenNetoObjetivo = 20.0,
            ticketPromedioObjetivo = 150.0, caidaVentasAlertaPct = 15.0,
            deudaViejaAlertaDias = 45, deudaTotalAlertaMonto = 5000.0,
            stockEstancadoAlertaDias = 90, stockBajoAlertaUnidades = 5,
            minVentasParaRecomendar = 10, frecuenciaRecalculoDias = 3
        )

        repo.upsertConfiguracionFinancieraFromRemote(entity)

        coVerify(exactly = 1) { configuracionFinancieraDao.upsert(entity) }
    }

    @Test
    fun `upsertConfiguracionFinancieraFromRemote does NOT call PostSaveSyncScheduler`() = runTest {
        val entity = ConfiguracionFinancieraEntity(
            opticaId = opticaId, margenNetoObjetivo = 18.0,
            ticketPromedioObjetivo = 200.0, caidaVentasAlertaPct = 10.0,
            deudaViejaAlertaDias = 30, deudaTotalAlertaMonto = 3000.0,
            stockEstancadoAlertaDias = 180, stockBajoAlertaUnidades = 2,
            minVentasParaRecomendar = 5, frecuenciaRecalculoDias = 1
        )

        repo.upsertConfiguracionFinancieraFromRemote(entity)

        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
        coVerify(exactly = 0) { scheduler.scheduleHistorialSync(any()) }
        coVerify(exactly = 0) { scheduler.schedulePacientesSync(any()) }
    }
}
