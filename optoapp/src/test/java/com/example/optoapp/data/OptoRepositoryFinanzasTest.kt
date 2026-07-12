package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
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
import java.time.Instant
import java.time.LocalDate

/**
 * Tests de integracion para metodos de escritura local de finanzas en [OptoRepository].
 * Usa Room in-memory database para DAOs y MockK para el scheduler.
 *
 * Local-write contract (Pattern B from exploration):
 *  1. El DAO recibe la entidad con timestamp stamped (Instant.now()).
 *  2. PostSaveSyncScheduler.scheduleFinanzasSync() es llamado.
 *
 * These tests reference constructor parameters and methods that DO NOT EXIST YET
 * on OptoRepository — RED phase.
 */
@RunWith(RobolectricTestRunner::class)
class OptoRepositoryFinanzasTest {

    private lateinit var db: OptoDatabase
    private lateinit var gastoOperativoDao: GastoOperativoDao
    private lateinit var resumenDiarioDao: ResumenDiarioDao
    private lateinit var configuracionFinancieraDao: ConfiguracionFinancieraDao
    private lateinit var syncStateTracker: SyncStateTracker
    private lateinit var scheduler: PostSaveSyncScheduler
    private lateinit var schedulerLazy: Lazy<PostSaveSyncScheduler>
    private lateinit var repo: OptoRepository

    private val opticaId = "optica-test"
    private val testDate = LocalDate.of(2026, 6, 15)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()

        gastoOperativoDao = db.gastoOperativoDao()
        resumenDiarioDao = db.resumenDiarioDao()
        configuracionFinancieraDao = db.configuracionFinancieraDao()

        scheduler = mockk(relaxed = true)
        schedulerLazy = mockk()
        every { schedulerLazy.get() } returns scheduler

        syncStateTracker = mockk(relaxed = true)
        val pacienteDao = db.pacienteDao()
        val evaluacionDao = db.evaluacionDao()
        val dispensacionDao = db.dispensacionDao()
        val dispensacionItemDao = db.dispensacionItemDao()
        val pagoDao = db.pagoDao()
        val servicioExtraDao = db.servicioExtraDao()
        val monturaDao = db.monturaDao()
        val monturaMovimientoDao = db.monturaMovimientoDao()

        val pacienteRepo = PacienteRepository(pacienteDao, evaluacionDao)
        val dispensacionRepo = DispensacionRepository(dispensacionDao, dispensacionItemDao, pagoDao, servicioExtraDao)
        val syncRepo = SyncRepository(syncStateTracker, monturaDao, monturaMovimientoDao)

        val regaloDispensacionDao = db.regaloDispensacionDao()
        val snapshotCoordinator = com.example.optoapp.data.sync.SyncSnapshotCoordinator(
            pacienteDao, monturaDao, monturaMovimientoDao, pacienteRepo, dispensacionRepo, syncRepo, regaloDispensacionDao
        )
        val backupCoordinator = com.example.optoapp.data.backup.BackupRestoreCoordinator(
            pacienteRepo, dispensacionRepo, evaluacionDao, pacienteDao, schedulerLazy
        )
        val monturaCoordinator = com.example.optoapp.data.montura.MonturaInventoryCoordinator(
            monturaDao, monturaMovimientoDao, schedulerLazy
        )

        repo = OptoRepository(
            database = db,
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
            categoriaProductoDao = db.categoriaProductoDao(),
            costoProductoDao = db.costoProductoDao(),
            costoBiseladoDao = db.costoBiseladoDao()
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    // ── GastoOperativo local writes ──────────────────────────────────────────

    @Test
    fun insertGastoOperativo_stamps_timestamp_when_null() = runBlocking {
        val entity = GastoOperativoEntity(
            id = "g1", opticaId = opticaId, categoria = "alquiler",
            descripcion = "Local junio", monto = 500.0, fecha = testDate,
            createdAt = null
        )

        repo.insertGastoOperativo(entity)

        val inserted = gastoOperativoDao.getAll().first().first()
        assertNotNull(inserted.createdAt) { "Expected createdAt to be stamped but was null" }
        assertEquals("g1", inserted.id)
        assertEquals("alquiler", inserted.categoria)
        assertEquals(500.0, inserted.monto, 0.001)
    }

    @Test
    fun insertGastoOperativo_preserves_createdAt_when_already_set() = runBlocking {
        val originalTimestamp = "2026-01-15T12:30:00Z"
        val entity = GastoOperativoEntity(
            id = "g1b", opticaId = opticaId, categoria = "alquiler",
            descripcion = "Local julio", monto = 500.0, fecha = testDate,
            createdAt = originalTimestamp
        )

        repo.insertGastoOperativo(entity)

        val inserted = gastoOperativoDao.getAll().first().first()
        assertEquals(originalTimestamp, inserted.createdAt)
    }

    @Test
    fun insertGastoOperativo_calls_scheduleFinanzasSync() = runBlocking {
        coEvery { scheduler.scheduleFinanzasSync(any()) } just Runs

        val entity = GastoOperativoEntity(
            id = "g2", opticaId = opticaId, categoria = "servicios",
            descripcion = "Electricidad", monto = 200.0, fecha = testDate
        )

        repo.insertGastoOperativo(entity)

        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
    }

    @Test
    fun upsertGastoOperativo_preserves_createdAt_on_existing_record() = runBlocking {
        val originalTimestamp = "2026-01-01T00:00:00Z"
        val entity = GastoOperativoEntity(
            id = "g3", opticaId = opticaId, categoria = "personal",
            descripcion = "Asistente", monto = 1500.0, fecha = testDate,
            createdAt = originalTimestamp
        )

        repo.upsertGastoOperativo(entity)

        val upserted = gastoOperativoDao.getAll().first().first()
        assertEquals(originalTimestamp, upserted.createdAt)
    }

    @Test
    fun upsertGastoOperativo_calls_scheduleFinanzasSync() = runBlocking {
        coEvery { scheduler.scheduleFinanzasSync(any()) } just Runs

        val entity = GastoOperativoEntity(
            id = "g4", opticaId = opticaId, categoria = "marketing",
            descripcion = "Facebook Ads", monto = 300.0, fecha = testDate
        )

        repo.upsertGastoOperativo(entity)

        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
    }

    @Test
    fun deleteGastoOperativo_schedules_finanzas_sync() = runBlocking {
        coEvery { scheduler.scheduleFinanzasSync(any()) } just Runs
        val entity = GastoOperativoEntity(
            id = "g_del", opticaId = opticaId, categoria = "otro",
            descripcion = "A borrar", monto = 50.0, fecha = testDate
        )
        gastoOperativoDao.upsert(entity)

        repo.deleteGastoOperativo(entity)

        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
    }

    @Test
    fun deleteGastoOperativo_calls_markDeleted() = runBlocking {
        coEvery { scheduler.scheduleFinanzasSync(any()) } just Runs
        val entity = GastoOperativoEntity(
            id = "g_md", opticaId = opticaId, categoria = "otro",
            descripcion = "A marcar como eliminado", monto = 60.0, fecha = testDate
        )
        gastoOperativoDao.upsert(entity)

        repo.deleteGastoOperativo(entity)

        coVerify(exactly = 1) { syncStateTracker.markDeleted(opticaId, "gasto_operativo", entity.id) }
    }

    // ── GastoOperativo reads ─────────────────────────────────────────────────

    @Test
    fun getGastosOperativos_delegates_to_dao() = runBlocking {
        val g1 = GastoOperativoEntity(
            id = "g_r1", opticaId = opticaId, categoria = "alquiler",
            descripcion = "Local", monto = 500.0, fecha = testDate.plusDays(1)
        )
        val g2 = GastoOperativoEntity(
            id = "g_r2", opticaId = opticaId, categoria = "servicios",
            descripcion = "Fibra", monto = 80.0, fecha = testDate
        )
        gastoOperativoDao.upsert(g1)
        gastoOperativoDao.upsert(g2)

        val result = repo.getGastosOperativos(opticaId).first()

        assertEquals(2, result.size)
        // Ordenado por fecha DESC
        assertEquals("g_r1", result[0].id)
        assertEquals("g_r2", result[1].id)
    }

    // ── ResumenDiario reads ──────────────────────────────────────────────────

    @Test
    fun getResumenDiario_delegates_to_dao() = runBlocking {
        val rd1 = ResumenDiarioEntity(
            id = "rd_a", opticaId = opticaId, fecha = "2026-06-16",
            ventasCantidad = 3, ventasMontoTotal = 900.0, ventasCostoTotal = 360.0,
            cobrosCantidad = 2, cobrosMontoTotal = 600.0, saldoPendienteTotal = 300.0,
            saldoPendienteCantidad = 1
        )
        val rd2 = ResumenDiarioEntity(
            id = "rd_b", opticaId = opticaId, fecha = "2026-06-15",
            ventasCantidad = 5, ventasMontoTotal = 1500.0, ventasCostoTotal = 600.0,
            cobrosCantidad = 4, cobrosMontoTotal = 1200.0, saldoPendienteTotal = 300.0,
            saldoPendienteCantidad = 1
        )
        resumenDiarioDao.upsert(rd1)
        resumenDiarioDao.upsert(rd2)

        val result = repo.getResumenDiario(opticaId).first()

        assertEquals(2, result.size)
        // Ordenado por fecha DESC
        assertEquals("rd_a", result[0].id)
        assertEquals("rd_b", result[1].id)
    }

    // ── ConfiguracionFinanciera reads ────────────────────────────────────────

    @Test
    fun getConfiguracionFinanciera_delegates_to_dao() = runBlocking {
        val config = ConfiguracionFinancieraEntity(
            opticaId = opticaId, margenNetoObjetivo = 25.0,
            ticketPromedioObjetivo = 180.0, caidaVentasAlertaPct = 12.0,
            deudaViejaAlertaDias = 60, deudaTotalAlertaMonto = 4000.0,
            stockEstancadoAlertaDias = 120, stockBajoAlertaUnidades = 3,
            minVentasParaRecomendar = 8, frecuenciaRecalculoDias = 2
        )
        configuracionFinancieraDao.upsert(config)

        val result = repo.getConfiguracionFinanciera(opticaId).first()

        assertNotNull(result)
        assertEquals(25.0, result!!.margenNetoObjetivo, 0.001)
        assertEquals(180.0, result.ticketPromedioObjetivo!!, 0.001)
        assertEquals(60, result.deudaViejaAlertaDias)
    }

}
