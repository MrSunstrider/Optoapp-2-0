package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.pago.PagoDao
import com.example.optoapp.data.servicio.ServicioExtraDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class PagoDaoSumTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: PagoDao
    private lateinit var pacienteDao: PacienteDao
    private lateinit var dispensacionDao: DispensacionDao
    private lateinit var servicioExtraDao: ServicioExtraDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.pagoDao()
        pacienteDao = db.pacienteDao()
        dispensacionDao = db.dispensacionDao()
        servicioExtraDao = db.servicioExtraDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    private suspend fun seedDisp(id: String) {
        pacienteDao.insertPaciente(
            Paciente(
                id = "p_dummy",
                nombreCompleto = "Dummy",
                edad = 0,
                telefono = "000",
                fechaCreacion = LocalDate.parse("2026-01-15"),
                opticaId = "o1",
            ),
        )
        dispensacionDao.insertDispensacion(
            DispensacionOptica(
                id = id,
                pacienteId = "p_dummy",
                fecha = LocalDate.parse("2026-01-15"),
                opticaId = "o1",
            ),
        )
    }

    @Test
    fun sumMontoByDispensacion_pagoEffectMatrix() = runBlocking {
        seedDisp("disp1")
        dao.insertPago(Pago(id = "p1", dispensacionId = "disp1", fecha = LocalDate.parse("2026-01-15"), tipo = "Abono", monto = 100.0, metodoPago = "EFECTIVO", opticaId = "o1"))
        dao.insertPago(Pago(id = "p2", dispensacionId = "disp1", fecha = LocalDate.parse("2026-01-20"), tipo = "Pago completo", monto = 50.0, metodoPago = "TARJETA", opticaId = "o1"))
        dao.insertPago(Pago(id = "p3", dispensacionId = "disp1", fecha = LocalDate.parse("2026-01-25"), tipo = "Reverso", monto = 40.0, metodoPago = "EFECTIVO", opticaId = "o1", reversaPagoId = "p1"))
        dao.insertPago(Pago(id = "p4", dispensacionId = "disp1", fecha = LocalDate.parse("2026-01-26"), tipo = "Anulación", monto = 999.0, metodoPago = "EFECTIVO", opticaId = "o1"))

        assertEquals(110.0, dao.sumMontoByDispensacion("disp1"), 0.001)
    }

    @Test
    fun sumMontoByDispensacion_noPagos_returnsZero() = runBlocking {
        seedDisp("dispEmpty")
        assertEquals(0.0, dao.sumMontoByDispensacion("dispEmpty"), 0.001)
    }

    @Test
    fun sumMontoByDispensacion_unknownDispensacion_returnsZero() = runBlocking {
        assertEquals(0.0, dao.sumMontoByDispensacion("nonexistent"), 0.001)
    }

    @Test
    fun sumMontoByServicioExtra_pagoEffectWithReembolso() = runBlocking {
        servicioExtraDao.insertServicio(
            ServicioExtra(
                id = "se1",
                descripcion = "Test",
                montoTotal = 0.0,
                aCuenta = 0.0,
                estado = "Pendiente",
                fecha = LocalDate.parse("2026-02-01"),
                opticaId = "o1",
            ),
        )
        dao.insertPago(Pago(id = "p1", servicioExtraId = "se1", fecha = LocalDate.parse("2026-02-01"), tipo = "Abono", monto = 80.0, metodoPago = "EFECTIVO", opticaId = "o1"))
        dao.insertPago(Pago(id = "p2", servicioExtraId = "se1", fecha = LocalDate.parse("2026-02-10"), tipo = "Reembolso", monto = 20.0, metodoPago = "TARJETA", opticaId = "o1"))

        assertEquals(60.0, dao.sumMontoByServicioExtra("se1"), 0.001)
    }

    @Test
    fun sumMontoByServicioExtra_noPagos_returnsZero() = runBlocking {
        assertEquals(0.0, dao.sumMontoByServicioExtra("nonexistent"), 0.001)
    }
}
