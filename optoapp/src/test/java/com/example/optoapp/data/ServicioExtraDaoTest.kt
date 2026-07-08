package com.example.optoapp.data

import com.example.optoapp.data.servicio.ServicioExtraDao
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

/**
 * Tests de integración para [ServicioExtraDao] usando Room in-memory database.
 *
 * Verifica que @Query, @Insert, @Update, @Delete y @Upsert funcionan
 * correctamente después de la extracción del DAO a archivo separado.
 */
@RunWith(RobolectricTestRunner::class)
class ServicioExtraDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: ServicioExtraDao
    private lateinit var pacienteDao: PacienteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.servicioExtraDao()
        pacienteDao = db.pacienteDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertServicio_and_getById_returnsCorrectServicio() = runBlocking {
        val servicio = ServicioExtra(
            id = "se1",
            ot = "OT-2026-001",
            descripcion = "Lentes de contacto",
            montoTotal = 250.0,
            aCuenta = 100.0,
            estado = "Pendiente",
            fecha = LocalDate.parse("2026-01-15"),
            metodoPago = "EFECTIVO",
            opticaId = "optica1"
        )
        dao.insertServicio(servicio)

        val retrieved = dao.getServicioById("se1")
        assertNotNull(retrieved)
        assertEquals("se1", retrieved!!.id)
        assertEquals("Lentes de contacto", retrieved.descripcion)
        assertEquals(250.0, retrieved.montoTotal, 0.001)
        assertEquals("optica1", retrieved.opticaId)
    }

    @Test
    fun getServicioById_withUnknownId_returnsNull() = runBlocking {
        val retrieved = dao.getServicioById("nonexistent")

        assertNull(retrieved)
    }

    @Test
    fun getAllServiciosForOptica_returnsServiciosForOpticaOnly() = runBlocking {
        val s1 = ServicioExtra(
            id = "s1", descripcion = "Servicio A", montoTotal = 100.0, aCuenta = 50.0,
            estado = "Entregado", fecha = LocalDate.parse("2026-01-15"),
            metodoPago = "EFECTIVO", opticaId = "opticaA"
        )
        val s2 = ServicioExtra(
            id = "s2", descripcion = "Servicio B", montoTotal = 200.0, aCuenta = 100.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-02-01"),
            metodoPago = "TARJETA", opticaId = "opticaB"
        )
        dao.insertServicio(s1)
        dao.insertServicio(s2)

        val opticaAServicios = dao.getAllServiciosForOptica("opticaA").first()

        assertEquals(1, opticaAServicios.size)
        assertEquals("Servicio A", opticaAServicios[0].descripcion)
    }

    @Test
    fun getServiciosByPaciente_returnsServiciosForPaciente() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "pac1", nombreCompleto = "Juan Perez", edad = 30, telefono = "999",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "o1"
        ))
        pacienteDao.insertPaciente(Paciente(
            id = "pac2", nombreCompleto = "Maria Lopez", edad = 25, telefono = "888",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "o1"
        ))
        val s1 = ServicioExtra(
            id = "s1", descripcion = "Para Juan", montoTotal = 100.0, aCuenta = 50.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-01-15"),
            pacienteId = "pac1", metodoPago = "EFECTIVO", opticaId = "o1"
        )
        val s2 = ServicioExtra(
            id = "s2", descripcion = "Para Maria", montoTotal = 200.0, aCuenta = 100.0,
            estado = "Entregado", fecha = LocalDate.parse("2026-02-01"),
            pacienteId = "pac2", metodoPago = "TARJETA", opticaId = "o1"
        )
        dao.insertServicio(s1)
        dao.insertServicio(s2)

        val pac1Servicios = dao.getServiciosByPaciente("pac1").first()

        assertEquals(1, pac1Servicios.size)
        assertEquals("Para Juan", pac1Servicios[0].descripcion)
    }

    @Test
    fun updateServicio_modifiesExistingRecord() = runBlocking {
        val servicio = ServicioExtra(
            id = "s1", descripcion = "Original", montoTotal = 100.0, aCuenta = 50.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-01-15"),
            metodoPago = "EFECTIVO", opticaId = "o1"
        )
        dao.insertServicio(servicio)

        val updated = servicio.copy(descripcion = "Modificado", estado = "Entregado")
        dao.updateServicio(updated)

        val retrieved = dao.getServicioById("s1")
        assertEquals("Modificado", retrieved!!.descripcion)
        assertEquals("Entregado", retrieved.estado)
    }

    @Test
    fun deleteServicio_removesRecord() = runBlocking {
        val servicio = ServicioExtra(
            id = "s1", descripcion = "Temp", montoTotal = 100.0, aCuenta = 50.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-01-15"),
            metodoPago = "EFECTIVO", opticaId = "o1"
        )
        dao.insertServicio(servicio)
        dao.deleteServicio(servicio)

        val retrieved = dao.getServicioById("s1")
        assertNull(retrieved)
    }

    @Test
    fun deleteAll_removesAllServicios() = runBlocking {
        val s1 = ServicioExtra(
            id = "s1", descripcion = "A", montoTotal = 50.0, aCuenta = 25.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-01-15"),
            metodoPago = "EFECTIVO", opticaId = "o1"
        )
        val s2 = ServicioExtra(
            id = "s2", descripcion = "B", montoTotal = 75.0, aCuenta = 0.0,
            estado = "Entregado", fecha = LocalDate.parse("2026-02-01"),
            metodoPago = "TARJETA", opticaId = "o1"
        )
        dao.insertServicio(s1)
        dao.insertServicio(s2)
        dao.deleteAll("o1")

        val all = dao.getAllServicios().first()
        assertEquals(0, all.size)
    }

    @Test
    fun getServiciosListByOptica_returnsServiciosForOptica() = runBlocking {
        val s1 = ServicioExtra(
            id = "s1", descripcion = "A", montoTotal = 100.0, aCuenta = 50.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-01-15"),
            metodoPago = "EFECTIVO", opticaId = "opticaX"
        )
        dao.insertServicio(s1)

        val list = dao.getServiciosListByOptica("opticaX")

        assertEquals(1, list.size)
        assertEquals("s1", list[0].id)
    }

    @Test
    fun reassignFromLegacyMiOpticaBase_updatesOpticaId() = runBlocking {
        val servicio = ServicioExtra(
            id = "s1", descripcion = "Legacy", montoTotal = 100.0, aCuenta = 50.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-01-15"),
            metodoPago = "EFECTIVO", opticaId = "mi_optica_base"
        )
        dao.insertServicio(servicio)

        val updatedCount = dao.reassignFromLegacyMiOpticaBase("newOpticaId")
        assertEquals(1, updatedCount)

        val retrieved = dao.getServicioById("s1")
        assertEquals("newOpticaId", retrieved!!.opticaId)
    }
}
