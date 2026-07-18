package com.example.optoapp.data

import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
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
 * Unit tests (MockK) verifying the remote-bypass write path on [OptoRepository].
 *
 * Remote-bypass contract:
 *  1. Sub-repo receives the entity with the ORIGINAL remote updatedAt unchanged.
 *  2. PostSaveSyncScheduler is NOT called (no upload re-schedule after download).
 *
 * Regression contract (local-action path):
 *  3. insertServicio  stamps updatedAt to a non-null value and calls scheduleFinanzasSync.
 *  4. updateServicio  refreshes updatedAt and calls scheduleFinanzasSync.
 */
@RunWith(RobolectricTestRunner::class)
class OptoRepositoryFromRemoteTest {

    private val T_REMOTE = "2026-06-15T10:00:00Z"
    private val testDate = LocalDate.of(2026, 6, 15)
    private val opticaId = "optica-test"

    private lateinit var dispensacionRepo: DispensacionRepository
    private lateinit var pacienteRepo: PacienteRepository
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var schedulerLazy: Lazy<PostSaveSyncScheduler>
    private lateinit var repo: OptoRepository

    @Before
    fun setUp() {
        dispensacionRepo = mockk(relaxed = true)
        pacienteRepo = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        schedulerLazy = mockk()
        every { schedulerLazy.get() } returns scheduler

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
            gastoOperativoDao = mockk(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── Remote bypass: timestamp preserved, scheduler NOT called ─────────────

    @Test
    fun `upsertServicioFromRemote passes entity with original updatedAt to sub-repo`() = runTest {
        val entity = ServicioExtra(
            id = "s1", descripcion = "Lente", montoTotal = 100.0, aCuenta = 50.0,
            estado = "Pendiente", fecha = testDate, opticaId = opticaId, updatedAt = T_REMOTE
        )

        repo.upsertServicioFromRemote(entity)

        coVerify(exactly = 1) { dispensacionRepo.insertServicio(match { it.updatedAt == T_REMOTE }) }
    }

    @Test
    fun `upsertServicioFromRemote does NOT call PostSaveSyncScheduler`() = runTest {
        val entity = ServicioExtra(
            id = "s1", descripcion = "Lente", montoTotal = 100.0, aCuenta = 50.0,
            estado = "Pendiente", fecha = testDate, opticaId = opticaId, updatedAt = T_REMOTE
        )

        repo.upsertServicioFromRemote(entity)

        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
        coVerify(exactly = 0) { scheduler.scheduleHistorialSync(any()) }
        coVerify(exactly = 0) { scheduler.schedulePacientesSync(any()) }
    }

    @Test
    fun `upsertDispensacionFromRemote passes entity with original updatedAt to sub-repo`() = runTest {
        val entity = DispensacionOptica(
            id = "d1", pacienteId = "p1", fecha = testDate, opticaId = opticaId, updatedAt = T_REMOTE
        )

        repo.upsertDispensacionFromRemote(entity)

        coVerify(exactly = 1) { dispensacionRepo.insertDispensacion(match { it.updatedAt == T_REMOTE }) }
    }

    @Test
    fun `upsertDispensacionFromRemote does NOT call PostSaveSyncScheduler`() = runTest {
        val entity = DispensacionOptica(
            id = "d1", pacienteId = "p1", fecha = testDate, opticaId = opticaId, updatedAt = T_REMOTE
        )

        repo.upsertDispensacionFromRemote(entity)

        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
        coVerify(exactly = 0) { scheduler.scheduleHistorialSync(any()) }
        coVerify(exactly = 0) { scheduler.schedulePacientesSync(any()) }
    }

    @Test
    fun `upsertPagoFromRemote passes entity with original updatedAt to sub-repo`() = runTest {
        val entity = Pago(
            id = "pg1", fecha = testDate, tipo = "Abono", monto = 200.0,
            metodoPago = "EFECTIVO", opticaId = opticaId, updatedAt = T_REMOTE
        )

        repo.upsertPagoFromRemote(entity)

        coVerify(exactly = 1) { dispensacionRepo.insertPago(match { it.updatedAt == T_REMOTE }) }
    }

    @Test
    fun `upsertPagoFromRemote does NOT call PostSaveSyncScheduler`() = runTest {
        val entity = Pago(
            id = "pg1", fecha = testDate, tipo = "Abono", monto = 200.0,
            metodoPago = "EFECTIVO", opticaId = opticaId, updatedAt = T_REMOTE
        )

        repo.upsertPagoFromRemote(entity)

        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
        coVerify(exactly = 0) { scheduler.scheduleHistorialSync(any()) }
        coVerify(exactly = 0) { scheduler.schedulePacientesSync(any()) }
    }

    @Test
    fun `upsertEvaluacionFromRemote passes entity with original updatedAt to sub-repo`() = runTest {
        val entity = EvaluacionClinica(
            id = "ev1", pacienteId = "p1", fecha = testDate, opticaId = opticaId, updatedAt = T_REMOTE
        )

        repo.upsertEvaluacionFromRemote(entity)

        coVerify(exactly = 1) { pacienteRepo.insertEvaluacion(match { it.updatedAt == T_REMOTE }) }
    }

    @Test
    fun `upsertEvaluacionFromRemote does NOT call PostSaveSyncScheduler`() = runTest {
        val entity = EvaluacionClinica(
            id = "ev1", pacienteId = "p1", fecha = testDate, opticaId = opticaId, updatedAt = T_REMOTE
        )

        repo.upsertEvaluacionFromRemote(entity)

        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
        coVerify(exactly = 0) { scheduler.scheduleHistorialSync(any()) }
        coVerify(exactly = 0) { scheduler.schedulePacientesSync(any()) }
    }

    // ── Regression: local-action path still stamps + schedules ───────────────

    @Test
    fun `insertServicio stamps updatedAt to non-null and calls scheduleFinanzasSync`() = runTest {
        val entity = ServicioExtra(
            id = "s2", descripcion = "Montura", montoTotal = 50.0, aCuenta = 0.0,
            estado = "Pendiente", fecha = testDate, opticaId = opticaId, updatedAt = null
        )

        repo.insertServicio(entity)

        coVerify(exactly = 1) { dispensacionRepo.insertServicio(match { it.updatedAt != null }) }
        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
    }

    @Test
    fun `updateServicio refreshes updatedAt and calls scheduleFinanzasSync`() = runTest {
        val existingUpdatedAt = "2026-01-01T00:00:00Z"
        val entity = ServicioExtra(
            id = "s3", descripcion = "Ajuste", montoTotal = 30.0, aCuenta = 0.0,
            estado = "Completado", fecha = testDate, opticaId = opticaId, updatedAt = existingUpdatedAt
        )

        repo.updateServicio(entity)

        coVerify(exactly = 1) { dispensacionRepo.updateServicio(match { it.updatedAt != null }) }
        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
    }

}
