package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

/**
 * Integration tests (Room in-memory) verifying that upsertXxxFromRemote methods store the
 * remote timestamp verbatim. These tests operate directly against the real Room DAOs to
 * confirm the end-to-end write path, not just mocked sub-repo calls.
 *
 * Invariant under test: stored.updatedAt == entity.updatedAt for all 4 entity types.
 * Idempotency under test: re-downloading the same record does not duplicate rows.
 */
@RunWith(RobolectricTestRunner::class)
class DownloadTimestampIntegrityTest {

    private val T_REMOTE = "2026-06-15T10:00:00Z"
    private val testDate = LocalDate.of(2026, 6, 15)
    private val opticaId = "optica-integration"

    private lateinit var db: OptoDatabase
    private lateinit var pagoDao: PagoDao
    private lateinit var servicioExtraDao: ServicioExtraDao
    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var evaluacionDao: EvaluacionDao
    private lateinit var pacienteDao: PacienteDao
    private lateinit var repo: OptoRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()

        pagoDao = db.pagoDao()
        servicioExtraDao = db.servicioExtraDao()
        dispensacionDao = db.dispensacionDao()
        evaluacionDao = db.evaluacionDao()
        pacienteDao = db.pacienteDao()

        val dispensacionItemDao = db.dispensacionItemDao()
        val monturaDao = db.monturaDao()
        val monturaMovimientoDao = db.monturaMovimientoDao()

        val regaloDispensacionDao = db.regaloDispensacionDao()

        val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
        val postSaveSyncScheduler = mockk<Lazy<PostSaveSyncScheduler>>(relaxed = true)

        val pacienteRepo = PacienteRepository(pacienteDao, evaluacionDao)
        val dispensacionRepo = DispensacionRepository(dispensacionDao, dispensacionItemDao, pagoDao, servicioExtraDao)
        val syncRepo = SyncRepository(syncStateTracker, monturaDao, monturaMovimientoDao)

        val snapshotCoordinator = SyncSnapshotCoordinator(
            pacienteDao,
            monturaDao,
            monturaMovimientoDao,
            pacienteRepo,
            dispensacionRepo,
            syncRepo,
            regaloDispensacionDao,
        )
        val backupCoordinator = BackupRestoreCoordinator(
            pacienteRepo,
            dispensacionRepo,
            evaluacionDao,
            pacienteDao,
            postSaveSyncScheduler,
            db,
        )
        val monturaCoordinator = MonturaInventoryCoordinator(
            monturaDao,
            monturaMovimientoDao,
            postSaveSyncScheduler,
        )

        repo = OptoRepository(
            database = db,
            syncStateTracker = syncStateTracker,
            postSaveSyncScheduler = postSaveSyncScheduler,
            pacienteRepo = pacienteRepo,
            dispensacionRepo = dispensacionRepo,
            syncRepo = syncRepo,
            snapshotCoordinator = snapshotCoordinator,
            backupCoordinator = backupCoordinator,
            monturaCoordinator = monturaCoordinator,
            gastoOperativoDao = db.gastoOperativoDao(),
            supabase = mockk<SupabaseClient>(relaxed = true),
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsertServicioFromRemote stores record with remote updatedAt`() = runBlocking {
        // ServicioExtra has no FK dependency on paciente, so no parent row needed.
        val entity = ServicioExtra(
            id = "s1",
            descripcion = "Control",
            montoTotal = 80.0,
            aCuenta = 0.0,
            estado = "Pendiente",
            fecha = testDate,
            opticaId = opticaId,
            updatedAt = T_REMOTE,
        )

        repo.upsertServicioFromRemote(entity)

        val stored = servicioExtraDao.getServicioById("s1")
        assertNotNull("Record should exist in DB", stored)
        assertEquals(
            "Stored updatedAt must equal the remote timestamp",
            T_REMOTE,
            stored!!.updatedAt,
        )
    }

    @Test
    fun `upsertDispensacionFromRemote stores record with remote updatedAt`() = runBlocking {
        // Insert parent paciente first to satisfy FK constraint.
        pacienteDao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "Test",
                edad = 30,
                telefono = "123",
                fechaCreacion = testDate,
                opticaId = opticaId,
            ),
        )
        val entity = DispensacionOptica(
            id = "d1",
            pacienteId = "p1",
            fecha = testDate,
            opticaId = opticaId,
            updatedAt = T_REMOTE,
        )

        repo.upsertDispensacionFromRemote(entity)

        val stored = dispensacionDao.getDispensacionById("d1")
        assertNotNull("Record should exist in DB", stored)
        assertEquals(
            "Stored updatedAt must equal the remote timestamp",
            T_REMOTE,
            stored!!.updatedAt,
        )
    }

    @Test
    fun `upsertPagoFromRemote stores record with remote updatedAt`() = runBlocking {
        // Pago has no strict FK to paciente/dispensacion when dispensacionId is null.
        val entity = Pago(
            id = "pg1",
            fecha = testDate,
            tipo = "Abono",
            monto = 150.0,
            metodoPago = "EFECTIVO",
            opticaId = opticaId,
            updatedAt = T_REMOTE,
        )

        repo.upsertPagoFromRemote(entity)

        val stored = pagoDao.getPagoById("pg1")
        assertNotNull("Record should exist in DB", stored)
        assertEquals(
            "Stored updatedAt must equal the remote timestamp",
            T_REMOTE,
            stored!!.updatedAt,
        )
    }

    @Test
    fun `upsertEvaluacionFromRemote stores record with remote updatedAt`() = runBlocking {
        // Insert parent paciente first to satisfy FK constraint.
        pacienteDao.insertPaciente(
            Paciente(
                id = "p2",
                nombreCompleto = "Eval Patient",
                edad = 25,
                telefono = "456",
                fechaCreacion = testDate,
                opticaId = opticaId,
            ),
        )
        val entity = EvaluacionClinica(
            id = "ev1",
            pacienteId = "p2",
            fecha = testDate,
            opticaId = opticaId,
            updatedAt = T_REMOTE,
        )

        repo.upsertEvaluacionFromRemote(entity)

        val stored = evaluacionDao.getEvaluacionById("ev1")
        assertNotNull("Record should exist in DB", stored)
        assertEquals(
            "Stored updatedAt must equal the remote timestamp",
            T_REMOTE,
            stored!!.updatedAt,
        )
    }

    @Test
    fun `upsertServicioFromRemote called twice with same entity does not duplicate row`() = runBlocking {
        val entity = ServicioExtra(
            id = "s2",
            descripcion = "Exam",
            montoTotal = 60.0,
            aCuenta = 0.0,
            estado = "Completado",
            fecha = testDate,
            opticaId = opticaId,
            updatedAt = T_REMOTE,
        )

        repo.upsertServicioFromRemote(entity)
        repo.upsertServicioFromRemote(entity)

        val all = servicioExtraDao.getServiciosListByOptica(opticaId)
        assertEquals("Only one row should exist after two identical downloads", 1, all.size)
        assertEquals(T_REMOTE, all.first().updatedAt)
    }
}
