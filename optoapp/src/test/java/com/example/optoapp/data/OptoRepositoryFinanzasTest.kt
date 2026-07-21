package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.gastooperativo.GastoOperativoDao
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.github.jan.supabase.SupabaseClient
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.math.BigDecimal
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
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()

        gastoOperativoDao = db.gastoOperativoDao()

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
            pacienteDao,
            monturaDao,
            monturaMovimientoDao,
            pacienteRepo,
            dispensacionRepo,
            syncRepo,
            regaloDispensacionDao,
        )
        val backupCoordinator = com.example.optoapp.data.backup.BackupRestoreCoordinator(
            pacienteRepo,
            dispensacionRepo,
            evaluacionDao,
            pacienteDao,
            schedulerLazy,
            db,
        )
        val monturaCoordinator = com.example.optoapp.data.montura.MonturaInventoryCoordinator(
            monturaDao,
            monturaMovimientoDao,
            schedulerLazy,
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
            supabase = mockk<SupabaseClient>(relaxed = true),
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertGastoOperativo_stamps_timestamp_when_null() = runBlocking {
        val entity = GastoOperativoEntity(
            id = "g1",
            opticaId = opticaId,
            categoria = "alquiler",
            descripcion = "Local junio",
            monto = BigDecimal.valueOf(500.0),
            fecha = testDate,
            createdAt = null,
        )

        repo.insertGastoOperativo(entity)

        val inserted = gastoOperativoDao.getByOpticaId(opticaId).first().first()
        assertNotNull(inserted.createdAt) { "Expected createdAt to be stamped but was null" }
        assertEquals("g1", inserted.id)
        assertEquals("alquiler", inserted.categoria)
        assertEquals(BigDecimal.valueOf(500.0), inserted.monto)
    }

    @Test
    fun insertGastoOperativo_preserves_createdAt_when_already_set() = runBlocking {
        val originalTimestamp = "2026-01-15T12:30:00Z"
        val entity = GastoOperativoEntity(
            id = "g1b",
            opticaId = opticaId,
            categoria = "alquiler",
            descripcion = "Local julio",
            monto = BigDecimal.valueOf(500.0),
            fecha = testDate,
            createdAt = originalTimestamp,
        )

        repo.insertGastoOperativo(entity)

        val inserted = gastoOperativoDao.getByOpticaId(opticaId).first().first()
        assertEquals(originalTimestamp, inserted.createdAt)
    }

    @Test
    fun insertGastoOperativo_calls_scheduleFinanzasSync() = runBlocking {
        coEvery { scheduler.scheduleFinanzasSync(any()) } just Runs

        val entity = GastoOperativoEntity(
            id = "g2",
            opticaId = opticaId,
            categoria = "servicios",
            descripcion = "Electricidad",
            monto = BigDecimal.valueOf(200.0),
            fecha = testDate,
        )

        repo.insertGastoOperativo(entity)

        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
    }

    @Test
    fun upsertGastoOperativo_preserves_createdAt_on_existing_record() = runBlocking {
        val originalTimestamp = "2026-01-01T00:00:00Z"
        val entity = GastoOperativoEntity(
            id = "g3",
            opticaId = opticaId,
            categoria = "personal",
            descripcion = "Asistente",
            monto = BigDecimal.valueOf(1500.0),
            fecha = testDate,
            createdAt = originalTimestamp,
        )

        repo.upsertGastoOperativo(entity)

        val upserted = gastoOperativoDao.getByOpticaId(opticaId).first().first()
        assertEquals(originalTimestamp, upserted.createdAt)
    }

    @Test
    fun upsertGastoOperativo_calls_scheduleFinanzasSync() = runBlocking {
        coEvery { scheduler.scheduleFinanzasSync(any()) } just Runs

        val entity = GastoOperativoEntity(
            id = "g4",
            opticaId = opticaId,
            categoria = "marketing",
            descripcion = "Facebook Ads",
            monto = BigDecimal.valueOf(300.0),
            fecha = testDate,
        )

        repo.upsertGastoOperativo(entity)

        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
    }

    @Test
    fun deleteGastoOperativo_schedules_finanzas_sync() = runBlocking {
        coEvery { scheduler.scheduleFinanzasSync(any()) } just Runs
        val entity = GastoOperativoEntity(
            id = "g_del",
            opticaId = opticaId,
            categoria = "otro",
            descripcion = "A borrar",
            monto = BigDecimal.valueOf(50.0),
            fecha = testDate,
        )
        gastoOperativoDao.upsert(entity)

        repo.deleteGastoOperativo(entity)

        coVerify(exactly = 1) { scheduler.scheduleFinanzasSync(opticaId) }
    }

    @Test
    fun deleteGastoOperativo_calls_markDeleted() = runBlocking {
        coEvery { scheduler.scheduleFinanzasSync(any()) } just Runs
        val entity = GastoOperativoEntity(
            id = "g_md",
            opticaId = opticaId,
            categoria = "otro",
            descripcion = "A marcar como eliminado",
            monto = BigDecimal.valueOf(60.0),
            fecha = testDate,
        )
        gastoOperativoDao.upsert(entity)

        repo.deleteGastoOperativo(entity)

        coVerify(exactly = 1) { syncStateTracker.markDeleted(opticaId, "gasto_operativo", entity.id) }
    }

    @Test
    fun getGastosOperativos_delegates_to_dao() = runBlocking {
        val g1 = GastoOperativoEntity(
            id = "g_r1",
            opticaId = opticaId,
            categoria = "alquiler",
            descripcion = "Local",
            monto = BigDecimal.valueOf(500.0),
            fecha = testDate.plusDays(1),
        )
        val g2 = GastoOperativoEntity(
            id = "g_r2",
            opticaId = opticaId,
            categoria = "servicios",
            descripcion = "Fibra",
            monto = BigDecimal.valueOf(80.0),
            fecha = testDate,
        )
        gastoOperativoDao.upsert(g1)
        gastoOperativoDao.upsert(g2)

        val result = repo.getGastosOperativos(opticaId).first()

        assertEquals(2, result.size)
        // Ordenado por fecha DESC
        assertEquals("g_r1", result[0].id)
        assertEquals("g_r2", result[1].id)
    }
}
