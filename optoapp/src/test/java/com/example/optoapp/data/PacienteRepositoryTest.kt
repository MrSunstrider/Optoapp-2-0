package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
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
import java.io.IOException
import java.time.LocalDate

/**
 * Tests de integración para [PacienteRepository] usando Room in-memory database.
 *
 * Verifica CRUD de pacientes y evaluaciones, sugerencia de historia optométrica,
 * y detección de duplicados.
 */
@RunWith(RobolectricTestRunner::class)
class PacienteRepositoryTest {

    private lateinit var db: OptoDatabase
    private lateinit var pacienteDao: PacienteDao
    private lateinit var evaluacionDao: EvaluacionDao
    private lateinit var repo: PacienteRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        pacienteDao = db.pacienteDao()
        evaluacionDao = db.evaluacionDao()
        repo = PacienteRepository(pacienteDao, evaluacionDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun pacientesFlowForOptica_returnsPatientsForOptica() = runBlocking {
        val p1 = Paciente(
            id = "p1", nombreCompleto = "Alice", edad = 30, telefono = "111",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "opticaA"
        )
        val p2 = Paciente(
            id = "p2", nombreCompleto = "Bob", edad = 25, telefono = "222",
            fechaCreacion = LocalDate.parse("2026-02-01"), opticaId = "opticaB"
        )
        pacienteDao.insertPaciente(p1)
        pacienteDao.insertPaciente(p2)

        val patients = repo.pacientesFlowForOptica("opticaA").first()

        assertEquals(1, patients.size)
        assertEquals("Alice", patients[0].nombreCompleto)
    }

    @Test
    fun countPacientesForOptica_returnsCorrectCount() = runBlocking {
        val p1 = Paciente(
            id = "p1", nombreCompleto = "Alice", edad = 30, telefono = "111",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "o1"
        )
        val p2 = Paciente(
            id = "p2", nombreCompleto = "Bob", edad = 25, telefono = "222",
            fechaCreacion = LocalDate.parse("2026-02-01"), opticaId = "o1"
        )
        pacienteDao.insertPaciente(p1)
        pacienteDao.insertPaciente(p2)

        val count = repo.countPacientesForOptica("o1").first()

        assertEquals(2, count)
    }

    @Test
    fun getPacienteById_withExistingId_returnsSuccess() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "p1", nombreCompleto = "Carlos", edad = 35, telefono = "333",
            fechaCreacion = LocalDate.parse("2026-03-01"), opticaId = "o1"
        ))

        val result = repo.getPacienteById("p1")

        assertTrue(result is Resource.Success)
        assertEquals("Carlos", (result as Resource.Success).data!!.nombreCompleto)
    }

    @Test
    fun getPacienteById_withUnknownId_returnsError() = runBlocking {
        val result = repo.getPacienteById("nonexistent")

        assertTrue(result is Resource.Error)
    }

    @Test
    fun insertPaciente_persistsRecord() = runBlocking {
        val paciente = Paciente(
            id = "p_new", nombreCompleto = "Diana", edad = 28, telefono = "444",
            fechaCreacion = LocalDate.parse("2026-04-01"), opticaId = "o1"
        )
        repo.insertPaciente(paciente)

        val retrieved = pacienteDao.getPacienteById("p_new")
        assertNotNull(retrieved)
        assertEquals("Diana", retrieved!!.nombreCompleto)
    }

    @Test
    fun updatePaciente_modifiesExistingRecord() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "p1", nombreCompleto = "Elena", edad = 32, telefono = "555",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        ))
        val updated = pacienteDao.getPacienteById("p1")!!.copy(nombreCompleto = "Elena Updated", edad = 33)

        repo.updatePaciente(updated)

        val retrieved = pacienteDao.getPacienteById("p1")
        assertEquals("Elena Updated", retrieved!!.nombreCompleto)
        assertEquals(33, retrieved.edad)
    }

    @Test
    fun deletePaciente_removesRecord() = runBlocking {
        val paciente = Paciente(
            id = "p_del", nombreCompleto = "ToDelete", edad = 40, telefono = "666",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        )
        pacienteDao.insertPaciente(paciente)

        repo.deletePaciente(paciente)

        val retrieved = pacienteDao.getPacienteById("p_del")
        assertTrue(retrieved == null)
    }

    @Test
    fun suggestNextHistoriaOptometrica_returnsNextSequence() = runBlocking {
        val year = LocalDate.now().year
        pacienteDao.insertPaciente(Paciente(
            id = "p1", nombreCompleto = "P1", edad = 20, telefono = "111",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
            historiaOptometrica = "HO-$year-0001"
        ))
        pacienteDao.insertPaciente(Paciente(
            id = "p2", nombreCompleto = "P2", edad = 22, telefono = "222",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
            historiaOptometrica = "HO-$year-0003"
        ))

        val next = repo.suggestNextHistoriaOptometrica("o1")

        assertEquals("HO-$year-0004", next)
    }

    @Test
    fun existsDuplicateHistoriaOptometrica_detectsDuplicate() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "p1", nombreCompleto = "Original", edad = 30, telefono = "111",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
            historiaOptometrica = "HO-2026-0001"
        ))

        val isDuplicate = repo.existsDuplicateHistoriaOptometrica("o1", "HO-2026-0001", null)

        assertTrue(isDuplicate)
    }

    // ─── Virtual Try-On: measurement persistence ──────────────────────────

    @Test
    fun updateEvaluacion_persistsDIPandDNPmeasurements() = runBlocking {
        val paciente = Paciente(
            id = "p_meas", nombreCompleto = "Paciente Medidas", edad = 35, telefono = "999",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        )
        pacienteDao.insertPaciente(paciente)

        val evaluacion = EvaluacionClinica(
            id = "eval_meas", pacienteId = "p_meas", fecha = LocalDate.parse("2026-05-29"),
            opticaId = "o1"
        )
        evaluacionDao.insertEvaluacion(evaluacion)

        val updated = evaluacion.copy(
            dipTotalMm = 63.5,
            dnpOdMm = 31.0,
            dnpOiMm = 32.5
        )
        repo.updateEvaluacion(updated)

        val eval = evaluacionDao.getEvaluacionById("eval_meas")!!
        assertEquals(63.5, eval.dipTotalMm!!, 0.001)
        assertEquals(31.0, eval.dnpOdMm!!, 0.001)
        assertEquals(32.5, eval.dnpOiMm!!, 0.001)
    }

    @Test
    fun updateEvaluacion_persistsNullMeasurements() = runBlocking {
        val paciente = Paciente(
            id = "p_null", nombreCompleto = "Paciente Null", edad = 40, telefono = "888",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        )
        pacienteDao.insertPaciente(paciente)

        val evaluacion = EvaluacionClinica(
            id = "eval_null", pacienteId = "p_null",
            fecha = LocalDate.parse("2026-05-29"), opticaId = "o1",
            dipTotalMm = 62.0, dnpOdMm = 30.5, dnpOiMm = 31.5
        )
        evaluacionDao.insertEvaluacion(evaluacion)

        val cleared = evaluacion.copy(dipTotalMm = null, dnpOdMm = null, dnpOiMm = null)
        repo.updateEvaluacion(cleared)

        val result = evaluacionDao.getEvaluacionById("eval_null")
        assertNotNull(result)
        assertNull("dipTotalMm debe poder ser null", result!!.dipTotalMm)
        assertNull("dnpOdMm debe poder ser null", result.dnpOdMm)
        assertNull("dnpOiMm debe poder ser null", result.dnpOiMm)
    }

    @Test
    fun getPacientesSnapshotForOptica_returnsAllForOptica() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "p1", nombreCompleto = "A", edad = 20, telefono = "1",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        ))
        pacienteDao.insertPaciente(Paciente(
            id = "p2", nombreCompleto = "B", edad = 22, telefono = "2",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1"
        ))

        val snapshot = repo.getPacientesSnapshotForOptica("o1")

        assertEquals(2, snapshot.size)
    }
}
