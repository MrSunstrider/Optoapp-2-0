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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

/**
 * Tests de integración para [DispensacionRepository] usando Room in-memory database.
 *
 * Verifica CRUD de dispensaciones, pagos y servicios extra,
 * sugerencia de OT, detección de duplicados y anulación de pagos.
 */
@RunWith(RobolectricTestRunner::class)
class DispensacionRepositoryTest {

    private lateinit var db: OptoDatabase
    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var dispensacionItemDao: DispensacionItemDao
    private lateinit var pagoDao: PagoDao
    private lateinit var servicioExtraDao: ServicioExtraDao
    private lateinit var pacienteDao: PacienteDao
    private lateinit var repo: DispensacionRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dispensacionDao = db.dispensacionDao()
        dispensacionItemDao = db.dispensacionItemDao()
        pagoDao = db.pagoDao()
        servicioExtraDao = db.servicioExtraDao()
        pacienteDao = db.pacienteDao()
        repo = DispensacionRepository(dispensacionDao, dispensacionItemDao, pagoDao, servicioExtraDao)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    private suspend fun insertDummyPaciente(id: String = "p_dummy", opticaId: String = "o1") {
        pacienteDao.insertPaciente(Paciente(
            id = id, nombreCompleto = "Dummy", edad = 0, telefono = "000",
            fechaCreacion = LocalDate.parse("2026-01-15"), opticaId = opticaId
        ))
    }

    // ── Dispensaciones ──────────────────────────────────────────────────────

    @Test
    fun getDispensacionById_withExistingId_returnsSuccess() = runBlocking {
        insertDummyPaciente()
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d1", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1", montoTotal = 200.0
        ))

        val result = repo.getDispensacionById("d1")

        assertTrue(result is Resource.Success)
        assertEquals(200.0, (result as Resource.Success).data!!.montoTotal, 0.001)
    }

    @Test
    fun getDispensacionById_withUnknownId_returnsError() = runBlocking {
        val result = repo.getDispensacionById("nonexistent")

        assertTrue(result is Resource.Error)
    }

    @Test
    fun insertDispensacion_persistsRecord() = runBlocking {
        insertDummyPaciente()
        val disp = DispensacionOptica(
            id = "d_new", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-02-01"),
            opticaId = "o1", montoTotal = 350.0
        )

        repo.insertDispensacion(disp)

        val retrieved = dispensacionDao.getDispensacionById("d_new")
        assertNotNull(retrieved)
        assertEquals(350.0, retrieved!!.montoTotal, 0.001)
    }

    @Test
    fun updateDispensacion_modifiesExistingRecord() = runBlocking {
        insertDummyPaciente()
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d1", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1", montoTotal = 200.0, estadoEntrega = "Pendiente"
        ))
        val updated = DispensacionOptica(
            id = "d1", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1", montoTotal = 250.0, estadoEntrega = "Entregado"
        )

        repo.updateDispensacion(updated)

        val retrieved = dispensacionDao.getDispensacionById("d1")
        assertEquals(250.0, retrieved!!.montoTotal, 0.001)
        assertEquals("Entregado", retrieved.estadoEntrega)
    }

    @Test
    fun deleteDispensacionById_removesRecord() = runBlocking {
        insertDummyPaciente()
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d_del", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))

        val rows = repo.deleteDispensacionById("d_del")

        assertEquals(1, rows)
        assertNull(dispensacionDao.getDispensacionById("d_del"))
    }

    @Test
    fun suggestNextOt_returnsNextSequence() = runBlocking {
        insertDummyPaciente()
        val year = 2026
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d1", pacienteId = "p_dummy", fecha = LocalDate.parse("$year-01-15"),
            opticaId = "o1", ot = "OT-$year-0001"
        ))
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d2", pacienteId = "p_dummy", fecha = LocalDate.parse("$year-02-01"),
            opticaId = "o1", ot = "OT-$year-0003"
        ))

        val next = repo.suggestNextOt("o1", LocalDate.of(year, 6, 1))

        assertEquals("OT-$year-0004", next)
    }

    // ── Pagos ───────────────────────────────────────────────────────────────

    @Test
    fun insertPago_and_getByDispensacion_returnsPago() = runBlocking {
        insertDummyPaciente()
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d1", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))
        val pago = Pago(
            id = "p1", dispensacionId = "d1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 150.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )

        repo.insertPago(pago)

        val pagos = repo.getPagosByDispensacion("d1").first()
        assertEquals(1, pagos.size)
        assertEquals(150.0, pagos[0].monto, 0.001)
    }

    @Test
    fun deletePagoRegistrandoAnulacionEnCaja_createsReversal() = runBlocking {
        insertDummyPaciente()
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d1", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))
        val pago = Pago(
            id = "p1", dispensacionId = "d1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        pagoDao.insertPago(pago)

        repo.deletePagoRegistrandoAnulacionEnCaja(pago, "o1")

        // Original deleted
        assertNull(pagoDao.getPagoById("p1"))
        // Reversal created
        val allPagos = pagoDao.getAllPagos()
        assertEquals(1, allPagos.size)
        val reversal = allPagos[0]
        assertEquals(-100.0, reversal.monto, 0.001)
        assertEquals("Anulación", reversal.tipo)
        assertEquals(LocalDate.parse("2026-01-15"), reversal.fecha)
    }

    @Test
    fun deletePagoRegistrandoAnulacionEnCaja_zeroMontoSkipsReversal() = runBlocking {
        insertDummyPaciente()
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d_zero", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))
        val pago = Pago(
            id = "p_zero", dispensacionId = "d_zero", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 0.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        pagoDao.insertPago(pago)

        repo.deletePagoRegistrandoAnulacionEnCaja(pago, "o1")

        // Original deleted
        assertNull(pagoDao.getPagoById("p_zero"))
        // No reversal inserted
        val allPagos = pagoDao.getAllPagos()
        assertEquals(0, allPagos.size)
    }

    @Test
    fun deletePagoRegistrandoAnulacionEnCaja_nonExistentPagoDoesNotCrash() = runBlocking {
        val pago = Pago(
            id = "p_ghost", dispensacionId = "d1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "CONTADO", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"
        )
        // pago not inserted — should not throw and should not insert reversal
        repo.deletePagoRegistrandoAnulacionEnCaja(pago, "o1")

        val allPagos = pagoDao.getAllPagos()
        assertEquals(0, allPagos.size)
    }

    // ── Servicios Extra ─────────────────────────────────────────────────────

    @Test
    fun insertServicio_and_getAll_returnsServicio() = runBlocking {
        val servicio = ServicioExtra(
            id = "se1", descripcion = "Lentes de contacto", montoTotal = 200.0, aCuenta = 50.0,
            estado = "Pendiente", fecha = LocalDate.parse("2026-01-15"),
            metodoPago = "EFECTIVO", opticaId = "o1"
        )

        repo.insertServicio(servicio)

        val all = repo.getAllServicios().first()
        assertEquals(1, all.size)
        assertEquals("Lentes de contacto", all[0].descripcion)
    }

    @Test
    fun getDispensacionesSnapshotForOptica_returnsAllForOptica() = runBlocking {
        insertDummyPaciente()
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d1", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-01-15"),
            opticaId = "o1"
        ))
        dispensacionDao.insertDispensacion(DispensacionOptica(
            id = "d2", pacienteId = "p_dummy", fecha = LocalDate.parse("2026-02-01"),
            opticaId = "o1"
        ))

        val snapshot = repo.getDispensacionesSnapshotForOptica("o1")

        assertEquals(2, snapshot.size)
    }
}
