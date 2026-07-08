package com.example.optoapp.data

import com.example.optoapp.data.pago.PagoDao
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
 * Tests de integración para [PagoDao] usando Room in-memory database.
 *
 * Verifica que @Query, @Insert, @Update, @Delete y @Upsert funcionan
 * correctamente después de la extracción del DAO a archivo separado.
 */
@RunWith(RobolectricTestRunner::class)
class PagoDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: PagoDao
    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var servicioExtraDao: ServicioExtraDao
    private lateinit var pacienteDao: PacienteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.pagoDao()
        dispensacionDao = db.dispensacionDao()
        servicioExtraDao = db.servicioExtraDao()
        pacienteDao = db.pacienteDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertPago_and_getById_returnsCorrectPago() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "p_dummy", nombreCompleto = "Dummy", edad = 0, telefono = "000",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "optica1"
        ))
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "disp1", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "optica1"
        ))
        val pago = Pago(
            id = "pago1",
            dispensacionId = "disp1",
            fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO",
            monto = 150.0,
            metodoPago = "EFECTIVO",
            opticaId = "optica1"
        )
        dao.insertPago(pago)

        val retrieved = dao.getPagoById("pago1")
        assertNotNull(retrieved)
        assertEquals("pago1", retrieved!!.id)
        assertEquals(150.0, retrieved.monto, 0.001)
        assertEquals("EFECTIVO", retrieved.metodoPago)
        assertEquals("optica1", retrieved.opticaId)
    }

    @Test
    fun getPagoById_withUnknownId_returnsNull() = runBlocking {
        val retrieved = dao.getPagoById("nonexistent")
        assertNull(retrieved)
    }

    @Test
    fun getPagosByDispensacion_returnsPaymentsForDispensacion() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "p_dummy", nombreCompleto = "Dummy", edad = 0, telefono = "000",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "o1"
        ))
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "dispX", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "dispY", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-25"),
            opticaId = "o1"
        ))
        val pago1 = Pago(
            id = "p1", dispensacionId = "dispX", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        val pago2 = Pago(
            id = "p2", dispensacionId = "dispX", fecha = LocalDate.parse("2026-01-20"),
            tipo = "CREDITO", monto = 50.0, metodoPago = "TARJETA", opticaId = "o1"
        )
        val pago3 = Pago(
            id = "p3", dispensacionId = "dispY", fecha = LocalDate.parse("2026-01-25"),
            tipo = "CONTADO", monto = 200.0, metodoPago = "TRANSFERENCIA", opticaId = "o1"
        )
        dao.insertPago(pago1)
        dao.insertPago(pago2)
        dao.insertPago(pago3)

        val pagosDispX = dao.getPagosByDispensacion("dispX").first()

        assertEquals(2, pagosDispX.size)
        assertEquals("p1", pagosDispX[1].id)
        assertEquals("p2", pagosDispX[0].id)
    }

    @Test
    fun getPagosByServicioExtra_returnsPaymentsForServicio() = runBlocking {
        servicioExtraDao.insertServicio(ServicioExtra(
            id = "se1", descripcion = "Test", montoTotal = 0.0, aCuenta = 0.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-02-01"),
            opticaId = "o1"
        ))
        val pago1 = Pago(
            id = "p1", servicioExtraId = "se1", fecha = LocalDate.parse("2026-02-01"),
            tipo = "CONTADO", monto = 80.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        val pago2 = Pago(
            id = "p2", servicioExtraId = "se1", fecha = LocalDate.parse("2026-02-10"),
            tipo = "CONTADO", monto = 20.0, metodoPago = "TARJETA", opticaId = "o1"
        )
        dao.insertPago(pago1)
        dao.insertPago(pago2)

        val pagosSe1 = dao.getPagosByServicioExtra("se1").first()

        assertEquals(2, pagosSe1.size)
        assertEquals(80.0, pagosSe1[1].monto, 0.001)
        assertEquals(20.0, pagosSe1[0].monto, 0.001)
    }

    @Test
    fun getPagosByDateRange_returnsPaymentsInRange() = runBlocking {
        val pago1 = Pago(
            id = "p1", fecha = LocalDate.parse("2026-03-01"), tipo = "CONTADO",
            monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        val pago2 = Pago(
            id = "p2", fecha = LocalDate.parse("2026-03-15"), tipo = "CONTADO",
            monto = 200.0, metodoPago = "TARJETA", opticaId = "o1"
        )
        val pago3 = Pago(
            id = "p3", fecha = LocalDate.parse("2026-04-01"), tipo = "CONTADO",
            monto = 300.0, metodoPago = "TRANSFERENCIA", opticaId = "o1"
        )
        dao.insertPago(pago1)
        dao.insertPago(pago2)
        dao.insertPago(pago3)

        val range = dao.getPagosByDateRange(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31")
        ).first()

        assertEquals(2, range.size)
    }

    @Test
    fun getPagosByDateRange_outsideRange_returnsEmpty() = runBlocking {
        val pago = Pago(
            id = "p1", fecha = LocalDate.parse("2026-01-01"), tipo = "CONTADO",
            monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        dao.insertPago(pago)

        val range = dao.getPagosByDateRange(
            LocalDate.parse("2026-06-01"),
            LocalDate.parse("2026-06-30")
        ).first()

        assertEquals(0, range.size)
    }

    @Test
    fun updatePago_modifiesExistingRecord() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "p_dummy", nombreCompleto = "Dummy", edad = 0, telefono = "000",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "o1"
        ))
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d1", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))
        val pago = Pago(
            id = "p1", dispensacionId = "d1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        dao.insertPago(pago)

        val updated = pago.copy(monto = 150.0, metodoPago = "TARJETA")
        dao.updatePago(updated)

        val retrieved = dao.getPagoById("p1")
        assertEquals(150.0, retrieved!!.monto, 0.001)
        assertEquals("TARJETA", retrieved.metodoPago)
    }

    @Test
    fun deletePago_removesRecord() = runBlocking {
        val pago = Pago(
            id = "p1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        dao.insertPago(pago)
        dao.deletePago(pago)

        val retrieved = dao.getPagoById("p1")
        assertNull(retrieved)
    }

    @Test
    fun deleteAll_removesAllRecords() = runBlocking {
        val p1 = Pago(
            id = "p1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        val p2 = Pago(
            id = "p2", fecha = LocalDate.parse("2026-02-15"),
            tipo = "CREDITO", monto = 50.0, metodoPago = "TARJETA", opticaId = "o1"
        )
        dao.insertPago(p1)
        dao.insertPago(p2)
        dao.deleteAll("o1")

        val all = dao.getAllPagos()
        assertEquals(0, all.size)
    }

    @Test
    fun getPagosListByOptica_filtersByOptica() = runBlocking {
        val pago1 = Pago(
            id = "p1", fecha = LocalDate.parse("2026-01-15"), tipo = "CONTADO",
            monto = 100.0, metodoPago = "EFECTIVO", opticaId = "opticaA"
        )
        val pago2 = Pago(
            id = "p2", fecha = LocalDate.parse("2026-01-20"), tipo = "CONTADO",
            monto = 200.0, metodoPago = "TARJETA", opticaId = "opticaB"
        )
        dao.insertPago(pago1)
        dao.insertPago(pago2)

        val opticaAPagos = dao.getPagosListByOptica("opticaA")

        assertEquals(1, opticaAPagos.size)
        assertEquals("p1", opticaAPagos[0].id)
    }

    @Test
    fun reassignFromLegacyMiOpticaBase_updatesOpticaId() = runBlocking {
        val pago = Pago(
            id = "p1", fecha = LocalDate.parse("2026-01-15"), tipo = "CONTADO",
            monto = 100.0, metodoPago = "EFECTIVO", opticaId = "mi_optica_base"
        )
        dao.insertPago(pago)

        val updatedCount = dao.reassignFromLegacyMiOpticaBase("newOpticaId")
        assertEquals(1, updatedCount)

        val retrieved = dao.getPagoById("p1")
        assertEquals("newOpticaId", retrieved!!.opticaId)
    }

    @Test
    fun reassignDispensacionId_updatesDispensacionReference() = runBlocking {
        pacienteDao.insertPaciente(Paciente(
            id = "p_dummy", nombreCompleto = "Dummy", edad = 0, telefono = "000",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = "o1"
        ))
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "oldDisp", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "newDisp", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))
        val pago = Pago(
            id = "p1", dispensacionId = "oldDisp", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        dao.insertPago(pago)

        val updatedCount = dao.reassignDispensacionId("oldDisp", "newDisp")
        assertEquals(1, updatedCount)

        val retrieved = dao.getPagoById("p1")
        assertEquals("newDisp", retrieved!!.dispensacionId)
    }
}
