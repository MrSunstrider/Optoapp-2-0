package com.example.optoapp.data

import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)

/**
 * Approval + behavior tests for [OptoRepository] catch-block refactoring.
 *
 * Verifies:
 * - [getMonturaById] DAO throws IOException → [Resource.Error]
 * - [getMonturaById] DAO throws CancellationException → rethrown
 * - [getMonturaById] DAO throws generic Exception → [Resource.Error]
 * - [restoreBackup] handles empty backup without error
 */
class OptoRepositoryErrorTest {

    private lateinit var database: OptoDatabase
    private lateinit var pacienteDao: PacienteDao
    private lateinit var evaluacionDao: EvaluacionDao
    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var pagoDao: PagoDao
    private lateinit var servicioExtraDao: ServicioExtraDao
    private lateinit var monturaDao: MonturaDao
    private lateinit var monturaMovimientoDao: MonturaMovimientoDao
    private lateinit var syncStateTracker: SyncStateTracker
    private lateinit var scheduler: Lazy<com.example.optoapp.sync.PostSaveSyncScheduler>
    private lateinit var pacienteRepo: PacienteRepository
    private lateinit var dispensacionRepo: DispensacionRepository
    private lateinit var syncRepo: SyncRepository
    private lateinit var repo: OptoRepository

    @Before
    fun setUp() {
        database = mockk(relaxed = true)
        pacienteDao = mockk(relaxed = true)
        evaluacionDao = mockk(relaxed = true)
        dispensacionDao = mockk(relaxed = true)
        pagoDao = mockk(relaxed = true)
        servicioExtraDao = mockk(relaxed = true)
        monturaDao = mockk()
        monturaMovimientoDao = mockk(relaxed = true)
        syncStateTracker = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)
        pacienteRepo = mockk(relaxed = true)
        dispensacionRepo = mockk(relaxed = true)
        syncRepo = mockk(relaxed = true)

        val snapshotCoordinator = SyncSnapshotCoordinator(
            pacienteDao, monturaDao, monturaMovimientoDao, pacienteRepo, dispensacionRepo, syncRepo
        )
        val backupCoordinator = BackupRestoreCoordinator(
            pacienteRepo, dispensacionRepo, evaluacionDao, pacienteDao, scheduler
        )
        val monturaCoordinator = MonturaInventoryCoordinator(
            monturaDao, monturaMovimientoDao, scheduler
        )

        repo = OptoRepository(
            database = database,
            syncStateTracker = syncStateTracker,
            postSaveSyncScheduler = scheduler,
            pacienteRepo = pacienteRepo,
            dispensacionRepo = dispensacionRepo,
            syncRepo = syncRepo,
            snapshotCoordinator = snapshotCoordinator,
            backupCoordinator = backupCoordinator,
            monturaCoordinator = monturaCoordinator,
            gastoOperativoDao = mockk(relaxed = true),
            resumenDiarioDao = mockk(relaxed = true),
            configuracionFinancieraDao = mockk(relaxed = true),
            categoriaProductoDao = mockk(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ── MonturaDao.getMonturaById ────────────────────────────────────

    @Test
    fun `getMonturaById dao throws IOException returns Error`() = runTest {
        coEvery { monturaDao.getMonturaByIdForOptica("m1", any()) } throws IOException("Network error")

        val result = repo.getMonturaById("m1", "o1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertNotNull((result as Resource.Error).message)
    }

    @Test
    fun `getMonturaById dao throws CancellationException rethrows`() = runTest {
        coEvery { monturaDao.getMonturaByIdForOptica("m1", any()) } throws CancellationException("Cancelled")

        var caught = false
        try {
            repo.getMonturaById("m1", "o1")
        } catch (e: CancellationException) {
            caught = true
        }
        assertTrue("CancellationException must be rethrown, not caught", caught)
    }

    @Test
    fun `getMonturaById dao throws generic Exception returns Error`() = runTest {
        coEvery { monturaDao.getMonturaByIdForOptica("m1", any()) } throws RuntimeException("DB corrupt")

        val result = repo.getMonturaById("m1", "o1")

        assertTrue("Expected Resource.Error but got $result", result is Resource.Error)
        assertEquals("DB corrupt", (result as Resource.Error).message)
    }

    // ── restoreBackup resilience ────────────────────────────────────

    @Test
    fun `restoreBackup with empty data completes without error`() = runTest {
        // Should not throw — restoreBackup is fire-and-forget
        repo.restoreBackup(EMPTY_BACKUP, "opt_abc")
    }

    @Test
    fun `restoreBackup with paciente data completes without error`() = runTest {
        // Should not throw — individual item failures are caught
        repo.restoreBackup(BACKUP_WITH_PACIENTE, "opt_abc")
    }

    companion object {
        private val EMPTY_BACKUP = BackupData(
            pacientes = emptyList(),
            evaluaciones = emptyList(),
            dispensaciones = emptyList(),
            pagos = emptyList(),
            serviciosExtra = emptyList()
        )

        private val BACKUP_WITH_PACIENTE = BackupData(
            pacientes = listOf(
                Paciente(
                    id = "p1", nombreCompleto = "Test", edad = 30, telefono = "123",
                    fechaCreacion = LocalDate.of(2024, 1, 15), opticaId = "opt_abc"
                )
            ),
            evaluaciones = emptyList(),
            dispensaciones = emptyList(),
            pagos = emptyList(),
            serviciosExtra = emptyList()
        )
    }
}
