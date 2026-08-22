package com.example.optoapp.data.regalodispensacion

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.Paciente
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RegaloDispensacionDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: RegaloDispensacionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.regaloDispensacionDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_createsRegalo() = runBlocking {
        val paciente = createPaciente()
        db.pacienteDao().insertPaciente(paciente)
        val dispensacion = createDispensacion("d1", paciente.id)
        db.dispensacionDao().insertDispensacion(dispensacion)

        val regalo = createTestRegalo("r1", "d1")
        dao.insert(regalo)

        val result = dao.getByDispensacionId("d1", "optica1")
        assertEquals(1, result.size)
        assertEquals("r1", result[0].id)
        assertEquals("d1", result[0].dispensacionId)
        assertEquals("prod1", result[0].productoId)
        assertEquals(2, result[0].cantidad)
        assertEquals(50.0, result[0].costoUnitario, 0.001)
        assertEquals("Estuche de regalo", result[0].descripcion)
        assertEquals("optica1", result[0].opticaId)
    }

    @Test
    fun getByDispensacionId_returnsMultipleRegalos() = runBlocking {
        val paciente = createPaciente()
        db.pacienteDao().insertPaciente(paciente)
        db.dispensacionDao().insertDispensacion(createDispensacion("d1", paciente.id))
        db.dispensacionDao().insertDispensacion(createDispensacion("d2", paciente.id))

        dao.insert(createTestRegalo("r1", "d1"))
        dao.insert(createTestRegalo("r2", "d1", productoId = "prod2", cantidad = 1, costoUnitario = 30.0, descripcion = "Funda"))
        dao.insert(createTestRegalo("r3", "d2"))

        val result = dao.getByDispensacionId("d1", "optica1")
        assertEquals(2, result.size)

        val resultD2 = dao.getByDispensacionId("d2", "optica1")
        assertEquals(1, resultD2.size)
    }

    @Test
    fun getByDispensacionId_unknownId_returnsEmpty() = runBlocking {
        val result = dao.getByDispensacionId("nonexistent", "optica1")
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteByDispensacionId_removesAllForThatDispensacion() = runBlocking {
        val paciente = createPaciente()
        db.pacienteDao().insertPaciente(paciente)
        db.dispensacionDao().insertDispensacion(createDispensacion("d1", paciente.id))
        db.dispensacionDao().insertDispensacion(createDispensacion("d2", paciente.id))

        dao.insert(createTestRegalo("r1", "d1"))
        dao.insert(createTestRegalo("r2", "d1"))
        dao.insert(createTestRegalo("r3", "d2"))

        dao.deleteByDispensacionId("d1", "optica1")

        val result = dao.getByDispensacionId("d1", "optica1")
        assertTrue(result.isEmpty())

        // d2 regalos should still exist
        val resultD2 = dao.getByDispensacionId("d2", "optica1")
        assertEquals(1, resultD2.size)
    }

    @Test
    fun reassignRegalosDispensacion_onlyMovesRegalosForGivenOptica() = runBlocking {
        // Given: two tenants with regalos assigned to their dispensaciones
        val p1 = createPacienteWithOptica("optica1")
        db.pacienteDao().insertPaciente(p1)
        db.dispensacionDao().insertDispensacion(createDispensacionWithOptica("dsrc1", p1.id, "optica1"))
        db.dispensacionDao().insertDispensacion(createDispensacionWithOptica("dtgt1", p1.id, "optica1"))

        val p2 = createPacienteWithOptica("optica2")
        db.pacienteDao().insertPaciente(p2)
        db.dispensacionDao().insertDispensacion(createDispensacionWithOptica("dsrc2", p2.id, "optica2"))
        db.dispensacionDao().insertDispensacion(createDispensacionWithOptica("dtgt2", p2.id, "optica2"))

        dao.insert(createTestRegalo("r-o1", "dsrc1", opticaId = "optica1"))
        dao.insert(createTestRegalo("r-o2", "dsrc2", opticaId = "optica2"))

        // When: reassign regalos for optica1 only
        val moved = dao.reassignRegalosDispensacion("dsrc1", "dtgt1", "optica1")

        // Then: only optica1 regalo was reassigned
        assertEquals(1, moved)

        assertEquals(1, dao.getByDispensacionId("dtgt1", "optica1").size)
        assertTrue(dao.getByDispensacionId("dsrc1", "optica1").isEmpty())

        assertEquals(1, dao.getByDispensacionId("dsrc2", "optica2").size)
        assertEquals("r-o2", dao.getByDispensacionId("dsrc2", "optica2")[0].id)
    }

    @Test
    fun reassignRegalosDispensacion_crossTenant_doesNotMoveOtherTenant() = runBlocking {
        // Given: two tenants with regalos
        val p1 = createPacienteWithOptica("optica1")
        db.pacienteDao().insertPaciente(p1)
        db.dispensacionDao().insertDispensacion(createDispensacionWithOptica("s1", p1.id, "optica1"))
        db.dispensacionDao().insertDispensacion(createDispensacionWithOptica("t1", p1.id, "optica1"))

        val p2 = createPacienteWithOptica("optica2")
        db.pacienteDao().insertPaciente(p2)
        db.dispensacionDao().insertDispensacion(createDispensacionWithOptica("s2", p2.id, "optica2"))
        db.dispensacionDao().insertDispensacion(createDispensacionWithOptica("t2", p2.id, "optica2"))

        dao.insert(createTestRegalo("r1", "s1", opticaId = "optica1"))
        dao.insert(createTestRegalo("r2", "s2", opticaId = "optica2"))

        // When: try to reassign optica2 data using optica1 filter
        val moved = dao.reassignRegalosDispensacion("s2", "t2", "optica1")

        // Then: 0 rows affected
        assertEquals(0, moved)

        // optica2 regalo still in source
        assertEquals(1, dao.getByDispensacionId("s2", "optica2").size)
        assertEquals("r2", dao.getByDispensacionId("s2", "optica2")[0].id)
    }

    @Test
    fun cascadeDelete_whenDispensacionDeleted_regalosAutoDeleted() = runBlocking {
        // Given: a paciente, a dispensacion, and a regalo
        val paciente = createPaciente()
        db.pacienteDao().insertPaciente(paciente)
        db.dispensacionDao().insertDispensacion(createDispensacion("d1", paciente.id))

        val regalo = createTestRegalo("r1", "d1")
        dao.insert(regalo)

        // Verify regalo exists
        assertEquals(1, dao.getByDispensacionId("d1", "optica1").size)

        // When: dispensacion is deleted (CASCADE)
        db.dispensacionDao().deleteById("d1", "optica1")

        // Then: regalo should be auto-deleted
        val result = dao.getByDispensacionId("d1", "optica1")
        assertTrue("Regalos should be cascade-deleted when dispensacion is deleted", result.isEmpty())
    }

    private fun createPaciente() = Paciente(
        id = UUID.randomUUID().toString(),
        nombreCompleto = "Test Paciente",
        edad = 30,
        telefono = "123456789",
        fechaCreacion = LocalDate.parse("2026-07-01"),
        opticaId = "optica1",
    )

    private fun createPacienteWithOptica(opticaId: String) = Paciente(
        id = UUID.randomUUID().toString(),
        nombreCompleto = "Test Paciente",
        edad = 30,
        telefono = "123456789",
        fechaCreacion = LocalDate.parse("2026-07-01"),
        opticaId = opticaId,
    )

    private fun createDispensacion(id: String, pacienteId: String) = DispensacionOptica(
        id = id,
        pacienteId = pacienteId,
        fecha = LocalDate.parse("2026-07-01"),
        opticaId = "optica1",
    )

    private fun createDispensacionWithOptica(id: String, pacienteId: String, opticaId: String) = DispensacionOptica(
        id = id,
        pacienteId = pacienteId,
        fecha = LocalDate.parse("2026-07-01"),
        opticaId = opticaId,
    )

    private fun createTestRegalo(
        id: String = UUID.randomUUID().toString(),
        dispensacionId: String,
        productoId: String = "prod1",
        cantidad: Int = 2,
        costoUnitario: Double = 50.0,
        descripcion: String = "Estuche de regalo",
        motivo: String = "",
        opticaId: String = "optica1",
    ) = RegaloDispensacionEntity(
        id = id,
        dispensacionId = dispensacionId,
        productoId = productoId,
        cantidad = cantidad,
        costoUnitario = costoUnitario,
        descripcion = descripcion,
        motivo = motivo,
        opticaId = opticaId,
    )
}
