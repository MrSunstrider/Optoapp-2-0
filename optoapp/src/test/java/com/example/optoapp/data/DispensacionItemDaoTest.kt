package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class DispensacionItemDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: DispensacionItemDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.dispensacionItemDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun reassignItemsDispensacion_onlyMovesItemsForGivenOptica() = runBlocking {
        // Given: two pacientes and dispensaciones for two opticas
        val paciente1 = createPaciente("optica1")
        db.pacienteDao().insertPaciente(paciente1)
        db.dispensacionDao().insertDispensacion(createDispensacion("source-o1", paciente1.id, "optica1"))
        db.dispensacionDao().insertDispensacion(createDispensacion("target-o1", paciente1.id, "optica1"))

        val paciente2 = createPaciente("optica2")
        db.pacienteDao().insertPaciente(paciente2)
        db.dispensacionDao().insertDispensacion(createDispensacion("source-o2", paciente2.id, "optica2"))
        db.dispensacionDao().insertDispensacion(createDispensacion("target-o2", paciente2.id, "optica2"))

        // Insert items for both opticas
        dao.insertItem(createItem("item-o1", "source-o1", "optica1"))
        dao.insertItem(createItem("item-o2", "source-o2", "optica2"))

        // When: reassign items for optica1 only
        val moved = dao.reassignItemsDispensacion("source-o1", "target-o1", "optica1")

        // Then: only optica1 item was reassigned
        assertEquals(1, moved)

        val target1Items = dao.getItemsListByDispensacion("target-o1", "optica1")
        assertEquals(1, target1Items.size)
        assertEquals("item-o1", target1Items[0].id)

        val source1Items = dao.getItemsListByDispensacion("source-o1", "optica1")
        assertTrue(source1Items.isEmpty())

        val source2Items = dao.getItemsListByDispensacion("source-o2", "optica2")
        assertEquals(1, source2Items.size)
        assertEquals("item-o2", source2Items[0].id)
    }

    @Test
    fun reassignItemsDispensacion_crossTenant_doesNotMoveOtherTenant() = runBlocking {
        // Given: two tenants with items assigned to their own dispensaciones
        val paciente1 = createPaciente("optica1")
        db.pacienteDao().insertPaciente(paciente1)
        db.dispensacionDao().insertDispensacion(createDispensacion("s1", paciente1.id, "optica1"))
        db.dispensacionDao().insertDispensacion(createDispensacion("t1", paciente1.id, "optica1"))

        val paciente2 = createPaciente("optica2")
        db.pacienteDao().insertPaciente(paciente2)
        db.dispensacionDao().insertDispensacion(createDispensacion("s2", paciente2.id, "optica2"))
        db.dispensacionDao().insertDispensacion(createDispensacion("t2", paciente2.id, "optica2"))

        dao.insertItem(createItem("i1", "s1", "optica1"))
        dao.insertItem(createItem("i2", "s2", "optica2"))

        // When: try to reassign optica2 items using optica1 filter
        val moved = dao.reassignItemsDispensacion("s2", "t2", "optica1")

        // Then: 0 rows affected because s2 items belong to optica2
        assertEquals(0, moved)

        // optica2 items are untouched
        val s2Items = dao.getItemsListByDispensacion("s2", "optica2")
        assertEquals(1, s2Items.size)
        assertEquals("i2", s2Items[0].id)

        // optica1 items are also untouched
        val s1Items = dao.getItemsListByDispensacion("s1", "optica1")
        assertEquals(1, s1Items.size)
        assertEquals("i1", s1Items[0].id)
    }

    private fun createPaciente(opticaId: String) = Paciente(
        id = UUID.randomUUID().toString(),
        nombreCompleto = "Test Paciente",
        edad = 30,
        telefono = "123456789",
        fechaCreacion = LocalDate.parse("2026-07-01"),
        opticaId = opticaId,
    )

    private fun createDispensacion(id: String, pacienteId: String, opticaId: String) = DispensacionOptica(
        id = id,
        pacienteId = pacienteId,
        fecha = LocalDate.parse("2026-07-01"),
        opticaId = opticaId,
    )

    private fun createItem(
        id: String,
        dispensacionId: String,
        opticaId: String,
    ) = DispensacionItem(
        id = id,
        dispensacionId = dispensacionId,
        opticaId = opticaId,
    )
}
