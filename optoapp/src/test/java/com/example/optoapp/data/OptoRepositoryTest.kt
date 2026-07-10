package com.example.optoapp.data

import com.example.optoapp.data.backup.BackupRestoreCoordinator
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import com.example.optoapp.data.sync.SyncSnapshotCoordinator
import com.example.optoapp.sync.PostSaveSyncScheduler
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dagger.Lazy
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

/**
 * Tests de integración para [OptoRepository] usando Room in-memory database
 * para DAOs y MockK para dependencias de infraestructura (sync).
 *
 * Verifica getMonturaById y restoreBackup.
 */
@RunWith(RobolectricTestRunner::class)
class OptoRepositoryTest {

    private lateinit var db: OptoDatabase
    private lateinit var monturaDao: MonturaDao
    private lateinit var pacienteDao: PacienteDao
    private lateinit var evaluacionDao: EvaluacionDao
    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var pagoDao: PagoDao
    private lateinit var servicioExtraDao: ServicioExtraDao
    private lateinit var monturaMovimientoDao: MonturaMovimientoDao
    private lateinit var repo: OptoRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()

        monturaDao = db.monturaDao()
        pacienteDao = db.pacienteDao()
        evaluacionDao = db.evaluacionDao()
        dispensacionDao = db.dispensacionDao()
        pagoDao = db.pagoDao()
        servicioExtraDao = db.servicioExtraDao()
        monturaMovimientoDao = db.monturaMovimientoDao()
        val dispensacionItemDao = db.dispensacionItemDao()

        val syncStateTracker = mockk<SyncStateTracker>(relaxed = true)
        val postSaveSyncScheduler = mockk<Lazy<PostSaveSyncScheduler>>(relaxed = true)

        val pacienteRepo = PacienteRepository(pacienteDao, evaluacionDao)
        val dispensacionRepo = DispensacionRepository(dispensacionDao, dispensacionItemDao, pagoDao, servicioExtraDao)
        val syncRepo = SyncRepository(syncStateTracker, monturaDao, monturaMovimientoDao)

        val snapshotCoordinator = SyncSnapshotCoordinator(
            pacienteDao, monturaDao, monturaMovimientoDao, pacienteRepo, dispensacionRepo, syncRepo
        )
        val backupCoordinator = BackupRestoreCoordinator(
            pacienteRepo, dispensacionRepo, evaluacionDao, pacienteDao, postSaveSyncScheduler
        )
        val monturaCoordinator = MonturaInventoryCoordinator(
            monturaDao, monturaMovimientoDao, postSaveSyncScheduler
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
            resumenDiarioDao = db.resumenDiarioDao(),
            configuracionFinancieraDao = db.configuracionFinancieraDao(),
            categoriaProductoDao = db.categoriaProductoDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    // ── getMonturaById ──────────────────────────────────────────────────────

    @Test
    fun getMonturaById_withExistingMontura_returnsSuccess() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "SKU001", marca = "Ray-Ban", modelo = "Wayfarer",
            color = "Negro", talla = "M", costo = 80.0, precio = 200.0,
            stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1"
        )
        monturaDao.insertMontura(montura)

        val result = repo.getMonturaById("m1", "o1")

        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertNotNull(data)
        assertEquals("m1", data!!.id)
        assertEquals("Ray-Ban", data.marca)
        assertEquals(200.0, data.precio, 0.001)
        assertEquals(10, data.stockActual)
    }

    @Test
    fun getMonturaById_withUnknownId_returnsError() = runBlocking {
        val result = repo.getMonturaById("nonexistent", "o1")

        assertTrue(result is Resource.Error)
    }

    @Test
    fun getMonturaById_returnsCorrectMonturaFromMultiple() = runBlocking {
        monturaDao.insertMontura(Montura(
            id = "m1", sku = "S001", marca = "MarcaA", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "o1"
        ))
        monturaDao.insertMontura(Montura(
            id = "m2", sku = "S002", marca = "MarcaB", modelo = "Mod2",
            color = "Rojo", talla = "L", costo = 60.0, precio = 120.0,
            stockActual = 3, stockMinimo = 1, activo = true, opticaId = "o1"
        ))

        val result = repo.getMonturaById("m2", "o1")

        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data!!
        assertEquals("MarcaB", data.marca)
        assertEquals("Mod2", data.modelo)
    }

    // ── restoreBackup ───────────────────────────────────────────────────────

    @Test
    fun restoreBackup_clearsDataAndInsertsPacientes() = runBlocking {
        // Insert some initial data that should be cleared (same optica as restore target)
        pacienteDao.insertPaciente(Paciente(
            id = "old_p", nombreCompleto = "Old", edad = 50, telefono = "999",
            fechaCreacion = LocalDate.parse("2025-01-01"), opticaId = "current_o"
        ))

        val backup = BackupData(
            pacientes = listOf(
                Paciente(
                    id = "p1", nombreCompleto = "Restored", edad = 30, telefono = "111",
                    fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "source_o"
                )
            )
        )

        repo.restoreBackup(backup, "current_o")

        // Old data cleared
        assertTrue(pacienteDao.getPacienteById("old_p") == null)
        // New data inserted with currentOpticaId
        val inserted = pacienteDao.getPacienteById("p1")
        assertNotNull(inserted)
        assertEquals("Restored", inserted!!.nombreCompleto)
        assertEquals("current_o", inserted.opticaId)
    }

    @Test
    fun restoreBackup_insertsAllEntityTypes() = runBlocking {
        val backup = BackupData(
            pacientes = listOf(Paciente(
                id = "p1", nombreCompleto = "P", edad = 20, telefono = "1",
                fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "src"
            )),
            evaluaciones = listOf(EvaluacionClinica(
                id = "e1", pacienteId = "p1", fecha = LocalDate.parse("2026-01-15"),
                opticaId = "src"
            )),
            dispensaciones = listOf(DispensacionOptica(
                id = "d1", pacienteId = "p1", fecha = LocalDate.parse("2026-01-15"),
                opticaId = "src"
            )),
            pagos = listOf(Pago(
                id = "pg1", fecha = LocalDate.parse("2026-01-15"),
                tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "src"
            )),
            serviciosExtra = listOf(ServicioExtra(
                id = "s1", descripcion = "Serv", montoTotal = 50.0, aCuenta = 25.0,
                estado = "Pendiente", fecha = LocalDate.parse("2026-01-15"), opticaId = "src"
            ))
        )

        repo.restoreBackup(backup, "target_o")

        // Verify all entity types were inserted with currentOpticaId
        assertEquals("target_o", pacienteDao.getPacienteById("p1")!!.opticaId)
        assertEquals("target_o", evaluacionDao.getEvaluacionById("e1")!!.opticaId)
        assertEquals("target_o", dispensacionDao.getDispensacionById("d1")!!.opticaId)
        assertEquals("target_o", pagoDao.getPagoById("pg1")!!.opticaId)
        assertEquals("target_o", servicioExtraDao.getServicioById("s1")!!.opticaId)
    }

    @Test
    fun restoreBackup_withNullLists_doesNotCrash() = runBlocking {
        val backup = BackupData(
            pacientes = null,
            evaluaciones = null,
            dispensaciones = null,
            pagos = null,
            serviciosExtra = null
        )

        // Should not throw despite all null lists
        repo.restoreBackup(backup, "current_o")
        // Verify nothing was inserted
        assertTrue(pacienteDao.getAllPacientes().first().isEmpty())
    }

    @Test
    fun restoreBackup_withPartialData_resilientToErrors() = runBlocking {
        val backup = BackupData(
            pacientes = listOf(Paciente(
                id = "p_good", nombreCompleto = "Good", edad = 25, telefono = "123",
                fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "src"
            )),
            evaluaciones = listOf(EvaluacionClinica(
                id = "e_good", pacienteId = "p_good", fecha = LocalDate.parse("2026-01-15"),
                opticaId = "src"
            )),
            // Dispensacion without a valid pacienteId would normally FK-fail,
            // but with try-catch it should be skipped silently
            dispensaciones = listOf(DispensacionOptica(
                id = "d_bad", pacienteId = "nonexistent", fecha = LocalDate.parse("2026-01-15"),
                opticaId = "src"
            )),
            pagos = listOf(Pago(
                id = "pg_good", fecha = LocalDate.parse("2026-01-15"),
                tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "src"
            ))
        )

        repo.restoreBackup(backup, "target_o")

        // Good data still inserted despite bad dispensacion
        assertNotNull(pacienteDao.getPacienteById("p_good"))
        assertNotNull(evaluacionDao.getEvaluacionById("e_good"))
        assertNotNull(pagoDao.getPagoById("pg_good"))
    }
}
