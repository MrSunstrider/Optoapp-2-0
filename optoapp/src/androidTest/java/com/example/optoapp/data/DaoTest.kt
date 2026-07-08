package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var pacienteDao: PacienteDao
    private lateinit var evaluacionDao: EvaluacionDao
    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var syncDao: SyncEntityStateDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()

        pacienteDao = db.pacienteDao()
        evaluacionDao = db.evaluacionDao()
        dispensacionDao = db.dispensacionDao()
        syncDao = db.syncEntityStateDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun pacienteInsertAndRetrieve() = runBlocking {
        val paciente = Paciente(
            id = "p1",
            nombreCompleto = "Juan Perez",
            edad = 30,
            telefono = "123456789",
            fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "optica1"
        )
        pacienteDao.insertPaciente(paciente)

        val retrieved = pacienteDao.getPacienteById("p1")
        assertNotNull(retrieved)
        assertEquals("Juan Perez", retrieved?.nombreCompleto)
        assertEquals("optica1", retrieved?.opticaId)
    }

    @Test
    fun pacienteUpdateChangesData() = runBlocking {
        val paciente = Paciente(
            id = "p1",
            nombreCompleto = "Juan Perez",
            edad = 30,
            telefono = "123456789",
            fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "optica1"
        )
        pacienteDao.insertPaciente(paciente)

        val updated = paciente.copy(nombreCompleto = "Juan Carlos Perez", telefono = "987654321")
        pacienteDao.updatePaciente(updated)

        val retrieved = pacienteDao.getPacienteById("p1")
        assertEquals("Juan Carlos Perez", retrieved?.nombreCompleto)
        assertEquals("987654321", retrieved?.telefono)
    }

    @Test
    fun pacienteDeleteRemovesData() = runBlocking {
        val paciente = Paciente(
            id = "p1",
            nombreCompleto = "Juan Perez",
            edad = 30,
            telefono = "123456789",
            fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "optica1"
        )
        pacienteDao.insertPaciente(paciente)
        pacienteDao.deletePaciente(paciente.id, paciente.opticaId)

        val retrieved = pacienteDao.getPacienteById("p1")
        assertNull(retrieved)
    }

    @Test
    fun pacienteSearchByOptica() = runBlocking {
        val p1 = Paciente(
            id = "p1", nombreCompleto = "Juan", edad = 30, telefono = "111",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "optica1"
        )
        val p2 = Paciente(
            id = "p2", nombreCompleto = "Maria", edad = 25, telefono = "222",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "optica2"
        )
        pacienteDao.insertPaciente(p1)
        pacienteDao.insertPaciente(p2)

        val optica1Pacientes = pacienteDao.getPacientesListByOptica("optica1")
        assertEquals(1, optica1Pacientes.size)
        assertEquals("Juan", optica1Pacientes[0].nombreCompleto)
    }

    @Test
    fun evaluacionInsertAndRetrieve() = runBlocking {
        val paciente = Paciente(
            id = "p1", nombreCompleto = "Test", edad = 20, telefono = "123",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        )
        pacienteDao.insertPaciente(paciente)

        val evaluacion = EvaluacionClinica(
            id = "e1",
            pacienteId = "p1",
            fecha = LocalDate.parse("2026-05-09"),
            opticaId = "o1",
            motivoConsulta = "Control",
            citaEstado = "programada"
        )
        evaluacionDao.insertEvaluacion(evaluacion)

        val retrieved = evaluacionDao.getEvaluacionById("e1")
        assertNotNull(retrieved)
        assertEquals("Control", retrieved?.motivoConsulta)
        assertEquals("programada", retrieved?.citaEstado)
    }

    @Test
    fun syncEntityStateInsertAndVerify() = runBlocking {
        val state = SyncEntityState(
            opticaId = "optica1",
            entityType = "paciente",
            entityId = "p1",
            status = "error",
            lastError = "test error",
            updatedAt = System.currentTimeMillis()
        )
        syncDao.upsert(state)

        val errors = syncDao.observeErrorsForOptica("optica1").first()
        assertEquals(1, errors.size)
        assertEquals("error", errors[0].status)
    }

    @Test
    fun syncEntityStateUpdateStatus() = runBlocking {
        val state = SyncEntityState(
            opticaId = "optica1",
            entityType = "paciente",
            entityId = "p1",
            status = "error",
            lastError = "test error",
            updatedAt = System.currentTimeMillis()
        )
        syncDao.upsert(state)

        syncDao.deleteErrorsForOptica("optica1")
        val errorsAfterDelete = syncDao.observeErrorsForOptica("optica1").first()
        assertEquals(0, errorsAfterDelete.size)
    }

    @Test
    fun syncEntityStatePendingDeletions() = runBlocking {
        val state = SyncEntityState(
            opticaId = "optica1",
            entityType = "paciente",
            entityId = "p1",
            status = "deleted",
            updatedAt = System.currentTimeMillis()
        )
        syncDao.upsert(state)

        val deletions = syncDao.getPendingDeletions("optica1")
        assertEquals(1, deletions.size)
        assertEquals("deleted", deletions[0].status)
    }

    @Test
    fun dispensacionWithMontura() = runBlocking {
        val paciente = Paciente(
            id = "p1", nombreCompleto = "Test", edad = 20, telefono = "123",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        )
        pacienteDao.insertPaciente(paciente)

        val montura = Montura(
            id = "m1", sku = "SKU001", marca = "Marca", modelo = "Modelo",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 5, stockMinimo = 2, activo = true, opticaId = "o1"
        )
        db.monturaDao().insertMontura(montura)

        val dispensacion = DispensacionOptica(
            id = "d1", pacienteId = "p1", fecha = LocalDate.parse("2026-05-09"),
            opticaId = "o1",
            montoTotal = 100.0, montoPagado = 50.0, estadoEntrega = "Pendiente",
            monturaId = "m1", ot = "OT-2026-001",
            tratamientos = emptyList()
        )
        dispensacionDao.insertDispensacion(dispensacion)

        val retrieved = dispensacionDao.getDispensacionById("d1")
        assertNotNull(retrieved)
        assertEquals("m1", retrieved?.monturaId)
        assertEquals(100.0, retrieved!!.montoTotal, 0.01)
    }

    @Test
    fun pacienteDeleteCascadingPreservesOtherTables() = runBlocking {
        val paciente = Paciente(
            id = "p1", nombreCompleto = "Test", edad = 20, telefono = "123",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        )
        pacienteDao.insertPaciente(paciente)

        val ev = EvaluacionClinica(
            id = "e1", pacienteId = "p1", fecha = LocalDate.parse("2026-01-01"),
            opticaId = "o1", motivoConsulta = "", citaEstado = "programada"
        )
        evaluacionDao.insertEvaluacion(ev)

        pacienteDao.deletePaciente(paciente.id, paciente.opticaId)

        assertNull(pacienteDao.getPacienteById("p1"))
        assertNull(evaluacionDao.getEvaluacionById("e1"))
    }
}
