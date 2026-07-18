package com.example.optoapp.data

import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import java.math.BigDecimal
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
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var schedulerLazy: Lazy<PostSaveSyncScheduler>
    private lateinit var repo: OptoRepository

    @Before
    fun setUp() {
        gastoOperativoDao = mockk(relaxed = true)
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
            gastoOperativoDao = gastoOperativoDao
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
            descripcion = "Local junio", monto = BigDecimal.valueOf(500.0), fecha = testDate,
            createdAt = T_REMOTE
        )

        repo.upsertGastoOperativoFromRemote(entity)

        coVerify(exactly = 1) { gastoOperativoDao.upsert(match { it.createdAt == T_REMOTE }) }
    }

    @Test
    fun `upsertGastoOperativoFromRemote does NOT call PostSaveSyncScheduler`() = runTest {
        val entity = GastoOperativoEntity(
            id = "g1", opticaId = opticaId, categoria = "alquiler",
            descripcion = "Local junio", monto = BigDecimal.valueOf(500.0), fecha = testDate,
            createdAt = T_REMOTE
        )

        repo.upsertGastoOperativoFromRemote(entity)

        coVerify(exactly = 0) { scheduler.scheduleFinanzasSync(any()) }
        coVerify(exactly = 0) { scheduler.scheduleHistorialSync(any()) }
        coVerify(exactly = 0) { scheduler.schedulePacientesSync(any()) }
    }

    // ── ConfiguracionFinanciera + ResumenDiario remote bypass tests were removed
    //     because those passthrough methods were eliminated from OptoRepository.
    //     Direct DAO behavior is covered by CostoProductoDaoTest, etc.
}
