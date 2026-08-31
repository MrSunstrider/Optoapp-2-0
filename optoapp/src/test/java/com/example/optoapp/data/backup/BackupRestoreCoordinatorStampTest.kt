package com.example.optoapp.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.BackupData
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.DispensacionRepository
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class BackupRestoreCoordinatorStampTest {

    private lateinit var db: OptoDatabase
    private lateinit var coordinator: BackupRestoreCoordinator
    private val opticaId = "optica-restore"
    private val fecha = LocalDate.of(2026, 8, 29)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()

        val scheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
        val schedulerLazy = mockk<Lazy<PostSaveSyncScheduler>>()
        every { schedulerLazy.get() } returns scheduler

        val pacienteRepo = PacienteRepository(db.pacienteDao(), db.evaluacionDao())
        val dispensacionRepo = DispensacionRepository(
            db.dispensacionDao(),
            db.dispensacionItemDao(),
            db.pagoDao(),
            db.servicioExtraDao(),
        )
        coordinator = BackupRestoreCoordinator(
            pacienteRepo,
            dispensacionRepo,
            db.evaluacionDao(),
            db.pacienteDao(),
            schedulerLazy,
            db,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun restoreBackup_pacienteWithoutUpdatedAt_isStamped() = runBlocking {
        val paciente = Paciente(
            id = "p1",
            nombreCompleto = "Ana Test",
            edad = 30,
            telefono = "999",
            fechaCreacion = fecha,
            opticaId = "other",
            updatedAt = null,
        )
        assertNull(paciente.updatedAt)

        coordinator.restoreBackup(
            BackupData(pacientes = listOf(paciente)),
            opticaId,
        )

        val stored = db.pacienteDao().getPacienteByIdScoped("p1", opticaId)
        assertNotNull(stored)
        assertNotNull(stored!!.updatedAt)
        assertTrue(stored.updatedAt!!.isNotBlank())
    }

    @Test
    fun restoreBackup_preservesExistingUpdatedAt() = runBlocking {
        val preserved = "2026-01-15T08:30:00Z"
        coordinator.restoreBackup(
            BackupData(
                pacientes = listOf(
                    Paciente(
                        id = "p1",
                        nombreCompleto = "Ana Test",
                        edad = 30,
                        telefono = "999",
                        fechaCreacion = fecha,
                        opticaId = "other",
                        updatedAt = preserved,
                    ),
                ),
            ),
            opticaId,
        )

        assertEquals(preserved, db.pacienteDao().getPacienteByIdScoped("p1", opticaId)?.updatedAt)
    }

    @Test
    fun restoreBackup_blankUpdatedAt_isStamped() = runBlocking {
        coordinator.restoreBackup(
            BackupData(
                pacientes = listOf(
                    Paciente(
                        id = "p1",
                        nombreCompleto = "Ana Test",
                        edad = 30,
                        telefono = "999",
                        fechaCreacion = fecha,
                        opticaId = "other",
                        updatedAt = "   ",
                    ),
                ),
            ),
            opticaId,
        )

        val stored = db.pacienteDao().getPacienteByIdScoped("p1", opticaId)
        assertNotNull(stored?.updatedAt)
        assertTrue(stored!!.updatedAt!!.isNotBlank())
    }

    @Test
    fun restoreBackup_allSyncEntitiesWithoutUpdatedAt_areStamped() = runBlocking {
        val paciente = Paciente(
            id = "p1",
            nombreCompleto = "Ana Test",
            edad = 30,
            telefono = "999",
            fechaCreacion = fecha,
            opticaId = "other",
        )
        val evaluacion = EvaluacionClinica(
            id = "e1",
            pacienteId = "p1",
            fecha = fecha,
            opticaId = "other",
            updatedAt = null,
        )
        val dispensacion = DispensacionOptica(
            id = "d1",
            pacienteId = "p1",
            fecha = fecha,
            opticaId = "other",
            updatedAt = null,
        )
        val pago = Pago(
            id = "pay1",
            dispensacionId = "d1",
            fecha = fecha,
            tipo = "Abono",
            monto = 50.0,
            opticaId = "other",
            updatedAt = null,
        )
        val servicio = ServicioExtra(
            id = "s1",
            descripcion = "Limpieza",
            montoTotal = 20.0,
            estado = "Pendiente",
            fecha = fecha,
            pacienteId = "p1",
            opticaId = "other",
            updatedAt = null,
        )

        coordinator.restoreBackup(
            BackupData(
                pacientes = listOf(paciente),
                evaluaciones = listOf(evaluacion),
                dispensaciones = listOf(dispensacion),
                pagos = listOf(pago),
                serviciosExtra = listOf(servicio),
            ),
            opticaId,
        )

        assertNotNull(db.pacienteDao().getPacienteByIdScoped("p1", opticaId)?.updatedAt)
        assertNotNull(db.evaluacionDao().getEvaluacionById("e1", opticaId)?.updatedAt)
        assertNotNull(db.dispensacionDao().getDispensacionById("d1", opticaId)?.updatedAt)
        assertNotNull(db.pagoDao().getPagoByIdForOptica("pay1", opticaId)?.updatedAt)
        assertNotNull(db.servicioExtraDao().getServicioById("s1", opticaId)?.updatedAt)
    }
}
