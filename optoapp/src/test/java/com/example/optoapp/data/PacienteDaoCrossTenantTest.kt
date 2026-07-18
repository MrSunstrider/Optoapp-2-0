package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
 * Cross-tenant isolation tests for PacienteDao.
 * Verifies that data from one optica is not visible to another.
 */
@RunWith(RobolectricTestRunner::class)
class PacienteDaoCrossTenantTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: PacienteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.pacienteDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun getPacientesByOptica_returnsOnlyPacientesForThatOptica() = runBlocking {
        val p1 = Paciente(
            id = "p1", nombreCompleto = "Juan Pérez", edad = 30,
            telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "o1"
        )
        val p2 = Paciente(
            id = "p2", nombreCompleto = "María García", edad = 25,
            telefono = "222", fechaCreacion = LocalDate.parse("2026-01-01"),
            opticaId = "o2"
        )
        dao.insertPaciente(p1)
        dao.insertPaciente(p2)

        val o1Pacientes = dao.getPacientesByOptica("o1").first()
        assertEquals(1, o1Pacientes.size)
        assertEquals("p1", o1Pacientes[0].id)

        val o2Pacientes = dao.getPacientesByOptica("o2").first()
        assertEquals(1, o2Pacientes.size)
        assertEquals("p2", o2Pacientes[0].id)
    }

    @Test
    fun getPacientesByOptica_returnsEmpty_forOpticaWithNoPacientes() = runBlocking {
        dao.insertPaciente(
            Paciente(
                id = "p1", nombreCompleto = "Test", edad = 20,
                telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"),
                opticaId = "o1"
            )
        )

        val result = dao.getPacientesByOptica("o_other").first()
        assertTrue("Expected empty list for unrelated optica", result.isEmpty())
    }

    @Test
    fun countByOptica_countsOnlyScopedPacientes() = runBlocking {
        dao.insertPaciente(
            Paciente(id = "p1", nombreCompleto = "A", edad = 20,
                telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1")
        )
        dao.insertPaciente(
            Paciente(id = "p2", nombreCompleto = "B", edad = 30,
                telefono = "222", fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1")
        )
        dao.insertPaciente(
            Paciente(id = "p3", nombreCompleto = "C", edad = 25,
                telefono = "333", fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o2")
        )

        assertEquals(2, dao.countByOptica("o1").first())
        assertEquals(1, dao.countByOptica("o2").first())
    }

    @Test
    fun searchPacientesForOptica_doesNotLeakCrossTenant() = runBlocking {
        dao.insertPaciente(
            Paciente(id = "p1", nombreCompleto = "Juan Pérez", edad = 30,
                telefono = "111", fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o1")
        )
        dao.insertPaciente(
            Paciente(id = "p2", nombreCompleto = "Juan López", edad = 40,
                telefono = "222", fechaCreacion = LocalDate.parse("2026-01-01"), opticaId = "o2")
        )

        val result = dao.searchPacientesForOptica("o1", "Juan").first()
        assertEquals(1, result.size)
        assertEquals("p1", result[0].id)
    }
}
