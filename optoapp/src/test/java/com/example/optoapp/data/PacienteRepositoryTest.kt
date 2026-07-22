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
import java.time.Instant
import java.time.LocalDate

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
            OptoDatabase::class.java,
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
            id = "p1",
            nombreCompleto = "Alice",
            edad = 30,
            telefono = "111",
            fechaCreacion = LocalDate.parse("2026-01-15"),
            opticaId = "opticaA",
        )
        val p2 = Paciente(
            id = "p2",
            nombreCompleto = "Bob",
            edad = 25,
            telefono = "222",
            fechaCreacion = LocalDate.parse("2026-02-01"),
            opticaId = "opticaB",
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
            id = "p1",
            nombreCompleto = "Alice",
            edad = 30,
            telefono = "111",
            fechaCreacion = LocalDate.parse("2026-01-15"),
            opticaId = "o1",
        )
        val p2 = Paciente(
            id = "p2",
            nombreCompleto = "Bob",
            edad = 25,
            telefono = "222",
            fechaCreacion = LocalDate.parse("2026-02-01"),
            opticaId = "o1",
        )
        pacienteDao.insertPaciente(p1)
        pacienteDao.insertPaciente(p2)

        val count = repo.countPacientesForOptica("o1").first()

        assertEquals(2, count)
    }

    @Test
    fun getPacienteById_withExistingId_returnsSuccess() = runBlocking {
        pacienteDao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "Carlos",
                edad = 35,
                telefono = "333",
                fechaCreacion = LocalDate.parse("2026-03-01"),
                opticaId = "o1",
            ),
        )

        val result = repo.getPacienteById("p1", "o1")

        assertTrue(result is Resource.Success)
        assertEquals("Carlos", (result as Resource.Success).data!!.nombreCompleto)
    }

    @Test
    fun getPacienteById_withUnknownId_returnsError() = runBlocking {
        val result = repo.getPacienteById("nonexistent", "o1")

        assertTrue(result is Resource.Error)
    }

    @Test
    fun insertPaciente_persistsRecord() = runBlocking {
        val paciente = Paciente(
            id = "p_new",
            nombreCompleto = "Diana",
            edad = 28,
            telefono = "444",
            fechaCreacion = LocalDate.parse("2026-04-01"),
            opticaId = "o1",
        )
        repo.insertPaciente(paciente)

        val retrieved = pacienteDao.getPacienteById("p_new")
        assertNotNull(retrieved)
        assertEquals("Diana", retrieved!!.nombreCompleto)
    }

    @Test
    fun upsertPaciente_modifiesExistingRecord() = runBlocking {
        pacienteDao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "Elena",
                edad = 32,
                telefono = "555",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
            ),
        )
        val updated = pacienteDao.getPacienteById("p1")!!.copy(nombreCompleto = "Elena Updated", edad = 33)

        repo.upsertPaciente(updated)

        val retrieved = pacienteDao.getPacienteById("p1")
        assertEquals("Elena Updated", retrieved!!.nombreCompleto)
        assertEquals(33, retrieved.edad)
    }

    @Test
    fun upsertPaciente_insertsNewRecord_whenNoExistingRecord() = runBlocking {
        val paciente = Paciente(
            id = "p_insert_upsert",
            nombreCompleto = "New Patient",
            edad = 25,
            telefono = "777",
            fechaCreacion = LocalDate.parse("2026-06-01"),
            opticaId = "o1",
        )

        repo.upsertPaciente(paciente)

        val retrieved = pacienteDao.getPacienteById("p_insert_upsert")
        assertNotNull(retrieved)
        assertEquals("New Patient", retrieved!!.nombreCompleto)
    }

    @Test
    fun deletePaciente_removesRecord() = runBlocking {
        val paciente = Paciente(
            id = "p_del",
            nombreCompleto = "ToDelete",
            edad = 40,
            telefono = "666",
            fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "o1",
        )
        pacienteDao.insertPaciente(paciente)

        repo.deletePaciente(paciente)

        val retrieved = pacienteDao.getPacienteById("p_del")
        assertTrue(retrieved == null)
    }

    @Test
    fun suggestNextHistoriaOptometrica_returnsNextSequence() = runBlocking {
        val year = LocalDate.now().year
        pacienteDao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "P1",
                edad = 20,
                telefono = "111",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
                historiaOptometrica = "HO-$year-0001",
            ),
        )
        pacienteDao.insertPaciente(
            Paciente(
                id = "p2",
                nombreCompleto = "P2",
                edad = 22,
                telefono = "222",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
                historiaOptometrica = "HO-$year-0003",
            ),
        )

        val next = repo.suggestNextHistoriaOptometrica("o1")

        assertEquals("HO-$year-0004", next)
    }

    @Test
    fun getMaxHistoriaNum_returnsMaxForCurrentYear() = runBlocking {
        val year = LocalDate.now().year.toString()
        pacienteDao.insertPaciente(
            Paciente(
                id = "max1", nombreCompleto = "A", edad = 20, telefono = "1",
                fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
                historiaOptometrica = "HO-$year-0005",
            ),
        )
        pacienteDao.insertPaciente(
            Paciente(
                id = "max2", nombreCompleto = "B", edad = 22, telefono = "2",
                fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
                historiaOptometrica = "HO-$year-0042",
            ),
        )
        pacienteDao.insertPaciente(
            Paciente(
                id = "max3", nombreCompleto = "C", edad = 24, telefono = "3",
                fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
                historiaOptometrica = "HO-2025-9999",  // different year, should be excluded
            ),
        )

        val max = pacienteDao.getMaxHistoriaNum("o1", year)

        assertEquals(42, max)
    }

    @Test
    fun getMaxHistoriaNum_returnsNull_whenNoHistoriaForYear() = runBlocking {
        val max = pacienteDao.getMaxHistoriaNum("o1", "2030")

        assertEquals(null, max)
    }

    @Test
    fun getMaxHistoriaNum_ignoresOtherOpticas() = runBlocking {
        val year = LocalDate.now().year.toString()
        pacienteDao.insertPaciente(
            Paciente(
                id = "mx_a", nombreCompleto = "A", edad = 20, telefono = "1",
                fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
                historiaOptometrica = "HO-$year-0010",
            ),
        )
        pacienteDao.insertPaciente(
            Paciente(
                id = "mx_b", nombreCompleto = "B", edad = 22, telefono = "2",
                fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o2",
                historiaOptometrica = "HO-$year-9999",
            ),
        )

        val max = pacienteDao.getMaxHistoriaNum("o1", year)

        assertEquals(10, max)
    }

    @Test
    fun existsDuplicateHistoriaOptometrica_detectsDuplicate() = runBlocking {
        pacienteDao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "Original",
                edad = 30,
                telefono = "111",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
                historiaOptometrica = "HO-2026-0001",
            ),
        )

        val isDuplicate = repo.existsDuplicateHistoriaOptometrica("o1", "HO-2026-0001", null)

        assertTrue(isDuplicate)
    }

    @Test
    fun updateEvaluacion_persistsDIPandDNPmeasurements() = runBlocking {
        val paciente = Paciente(
            id = "p_meas",
            nombreCompleto = "Paciente Medidas",
            edad = 35,
            telefono = "999",
            fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "o1",
        )
        pacienteDao.insertPaciente(paciente)

        val evaluacion = EvaluacionClinica(
            id = "eval_meas",
            pacienteId = "p_meas",
            fecha = LocalDate.parse("2026-05-29"),
            opticaId = "o1",
        )
        evaluacionDao.insertEvaluacion(evaluacion)

        val updated = evaluacion.copy(
            dipTotalMm = 63.5,
            dnpOdMm = 31.0,
            dnpOiMm = 32.5,
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
            id = "p_null",
            nombreCompleto = "Paciente Null",
            edad = 40,
            telefono = "888",
            fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "o1",
        )
        pacienteDao.insertPaciente(paciente)

        val evaluacion = EvaluacionClinica(
            id = "eval_null",
            pacienteId = "p_null",
            fecha = LocalDate.parse("2026-05-29"),
            opticaId = "o1",
            dipTotalMm = 62.0,
            dnpOdMm = 30.5,
            dnpOiMm = 31.5,
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
    fun deletePaciente_returnsOneForExistingRecord() = runBlocking {
        pacienteDao.insertPaciente(
            Paciente(
                id = "b4-del", nombreCompleto = "ToDelete", edad = 30,
                telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
            ),
        )

        val rowsDeleted = pacienteDao.deletePaciente("b4-del", "o1")

        assertEquals(1, rowsDeleted)
    }

    @Test
    fun deletePaciente_returnsZeroForNonExistentRecord() = runBlocking {
        val rowsDeleted = pacienteDao.deletePaciente("nonexistent", "o1")

        assertEquals(0, rowsDeleted)
    }

    @Test
    fun resolveDuplicatePacientesByHistoria_threeDuplicates_accumulatesAllFields() = runBlocking {
        // Three pacientes with same HO — each has unique non-blank fields
        val pA = Paciente(
            id = "dup-A", nombreCompleto = "Alice", edad = 30, telefono = "",
            email = "a@test.com", direccion = "", historiaOptometrica = "HO-2026-0001",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
        )
        val pB = Paciente(
            id = "dup-B", nombreCompleto = "Bob", edad = 35, telefono = "111",
            email = "", direccion = "", historiaOptometrica = "HO-2026-0001",
            fechaCreacion = LocalDate.parse("2026-01-03"), opticaId = "o1",
        )
        val pC = Paciente(
            id = "dup-C", nombreCompleto = "Carol", edad = 40, telefono = "",
            email = "", direccion = "Calle 1", historiaOptometrica = "HO-2026-0001",
            fechaCreacion = LocalDate.parse("2026-01-05"), opticaId = "o1",
        )
        pacienteDao.insertPaciente(pA)
        pacienteDao.insertPaciente(pB)
        pacienteDao.insertPaciente(pC)

        val result = repo.resolveDuplicatePacientesByHistoria("o1", db)

        assertTrue("Should have merged pacientes", result.mergedPacientes > 0)
        // Oldest paciente (pA) should survive
        val merged = pacienteDao.getPacienteById("dup-A")!!
        assertEquals("email from A", "a@test.com", merged.email)
        assertEquals("telefono from B", "111", merged.telefono)
        assertEquals("direccion from C", "Calle 1", merged.direccion)
        // Duplicates B and C should be deleted
        assertNull("dup-B deleted", pacienteDao.getPacienteById("dup-B"))
        assertNull("dup-C deleted", pacienteDao.getPacienteById("dup-C"))
    }

    @Test
    fun resolveDuplicatePacientesByHistoria_twoDuplicates_mergesCorrectly() = runBlocking {
        val pA = Paciente(
            id = "dup-2-A", nombreCompleto = "Alice", edad = 30, telefono = "111",
            email = "", direccion = "Calle A", historiaOptometrica = "HO-2026-0002",
            fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1",
        )
        val pB = Paciente(
            id = "dup-2-B", nombreCompleto = "Alice", edad = 30, telefono = "",
            email = "a@test.com", direccion = "", historiaOptometrica = "HO-2026-0002",
            fechaCreacion = LocalDate.parse("2026-01-03"), opticaId = "o1",
        )
        pacienteDao.insertPaciente(pA)
        pacienteDao.insertPaciente(pB)

        val result = repo.resolveDuplicatePacientesByHistoria("o1", db)

        assertTrue("Should have merged 1 duplicate", result.mergedPacientes > 0)
        val merged = pacienteDao.getPacienteById("dup-2-A")!!
        assertEquals("telefono from A preserved", "111", merged.telefono)
        assertEquals("email from B preserved", "a@test.com", merged.email)
        assertEquals("direccion from A preserved", "Calle A", merged.direccion)
        assertNull("dup-2-B deleted", pacienteDao.getPacienteById("dup-2-B"))
    }

    @Test
    fun insertPaciente_preservesUpdatedAtAtRepoLevel() = runBlocking {
        val paciente = Paciente(
            id = "f3-ts", nombreCompleto = "TimeStamp", edad = 25,
            telefono = "555", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "o1", updatedAt = null,
        )
        repo.insertPaciente(paciente)
        val saved = pacienteDao.getPacienteById("f3-ts")!!
        assertNull("PacienteRepository does not stamp updatedAt", saved.updatedAt)
    }

    @Test
    fun getPacientesSnapshotForOptica_returnsAllForOptica() = runBlocking {
        pacienteDao.insertPaciente(
            Paciente(
                id = "p1",
                nombreCompleto = "A",
                edad = 20,
                telefono = "1",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
            ),
        )
        pacienteDao.insertPaciente(
            Paciente(
                id = "p2",
                nombreCompleto = "B",
                edad = 22,
                telefono = "2",
                fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1",
            ),
        )

        val snapshot = repo.getPacientesSnapshotForOptica("o1")

        assertEquals(2, snapshot.size)
    }
}
