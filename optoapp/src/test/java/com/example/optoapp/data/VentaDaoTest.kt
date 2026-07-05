package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.venta.Venta
import com.example.optoapp.data.venta.VentaDao
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

@RunWith(RobolectricTestRunner::class)
class VentaDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: VentaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.ventaDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertVenta_and_getById_returnsCorrectVenta() = runBlocking {
        val venta = Venta(
            id = "v_disp_test1",
            opticaId = "optica1",
            origen = "dispensacion",
            origenId = "test1",
            pacienteId = "p1",
            fecha = LocalDate.parse("2026-07-01"),
            montoTotal = 150.0,
            estado = "Pendiente"
        )
        dao.upsertVenta(venta)

        val retrieved = dao.getVentaById("v_disp_test1")
        assertNotNull(retrieved)
        assertEquals("v_disp_test1", retrieved!!.id)
        assertEquals(150.0, retrieved.montoTotal, 0.001)
        assertEquals("dispensacion", retrieved.origen)
        assertEquals("optica1", retrieved.opticaId)
    }

    @Test
    fun getVentaById_withUnknownId_returnsNull() = runBlocking {
        val retrieved = dao.getVentaById("nonexistent")
        assertNull(retrieved)
    }

    @Test
    fun upsertVenta_overwritesExistingRow() = runBlocking {
        val original = Venta(
            id = "v_disp_dup", opticaId = "optica1", origen = "dispensacion",
            origenId = "dup1", pacienteId = "p1",
            fecha = LocalDate.parse("2026-07-01"), montoTotal = 100.0, estado = "Pendiente"
        )
        dao.upsertVenta(original)

        val updated = original.copy(montoTotal = 200.0, estado = "Entregado")
        dao.upsertVenta(updated)

        val retrieved = dao.getVentaById("v_disp_dup")
        assertEquals(200.0, retrieved!!.montoTotal, 0.001)
        assertEquals("Entregado", retrieved.estado)
    }

    @Test
    fun getVentasByOpticaAndDateRange_returnsVentasInRange() = runBlocking {
        val v1 = Venta(id = "v_disp_a", opticaId = "optica1", origen = "dispensacion",
            origenId = "a", pacienteId = "p1", fecha = LocalDate.parse("2026-07-01"),
            montoTotal = 100.0, estado = "Pendiente")
        val v2 = Venta(id = "v_disp_b", opticaId = "optica1", origen = "dispensacion",
            origenId = "b", pacienteId = "p1", fecha = LocalDate.parse("2026-07-15"),
            montoTotal = 200.0, estado = "Pendiente")
        val v3 = Venta(id = "v_disp_c", opticaId = "optica1", origen = "dispensacion",
            origenId = "c", pacienteId = "p1", fecha = LocalDate.parse("2026-08-01"),
            montoTotal = 300.0, estado = "Pendiente")

        dao.upsertVenta(v1)
        dao.upsertVenta(v2)
        dao.upsertVenta(v3)

        val julyVentas = dao.getVentasByOpticaAndDateRange(
            "optica1",
            LocalDate.parse("2026-07-01"),
            LocalDate.parse("2026-07-31")
        ).first()

        assertEquals(2, julyVentas.size)
    }

    @Test
    fun getVentasByOpticaAndDateRange_outsideRange_returnsEmpty() = runBlocking {
        val venta = Venta(id = "v_disp_x", opticaId = "optica1", origen = "dispensacion",
            origenId = "x", pacienteId = "p1", fecha = LocalDate.parse("2026-06-01"),
            montoTotal = 100.0, estado = "Pendiente")
        dao.upsertVenta(venta)

        val empty = dao.getVentasByOpticaAndDateRange(
            "optica1",
            LocalDate.parse("2026-07-01"),
            LocalDate.parse("2026-07-31")
        ).first()

        assertEquals(0, empty.size)
    }

    @Test
    fun getAllVentasByOptica_returnsAllForOptica() = runBlocking {
        val v1 = Venta(id = "v_disp_a", opticaId = "opticaA", origen = "dispensacion",
            origenId = "a", pacienteId = "p1", fecha = LocalDate.parse("2026-07-01"),
            montoTotal = 100.0, estado = "Pendiente")
        val v2 = Venta(id = "v_disp_b", opticaId = "opticaB", origen = "dispensacion",
            origenId = "b", pacienteId = "p1", fecha = LocalDate.parse("2026-07-02"),
            montoTotal = 200.0, estado = "Pendiente")
        val v3 = Venta(id = "v_serv_c", opticaId = "opticaA", origen = "servicio_extra",
            origenId = "c", pacienteId = "p1", fecha = LocalDate.parse("2026-07-03"),
            montoTotal = 50.0, estado = "Pendiente")

        dao.upsertVenta(v1)
        dao.upsertVenta(v2)
        dao.upsertVenta(v3)

        val opticaAVentas = dao.getAllVentasByOptica("opticaA")
        assertEquals(2, opticaAVentas.size)
    }

    @Test
    fun deleteAll_removesAllRecords() = runBlocking {
        val v1 = Venta(id = "v1", opticaId = "o1", origen = "dispensacion",
            origenId = "1", pacienteId = "p1", fecha = LocalDate.parse("2026-07-01"),
            montoTotal = 100.0, estado = "Pendiente")
        val v2 = Venta(id = "v2", opticaId = "o1", origen = "servicio_extra",
            origenId = "2", pacienteId = "p1", fecha = LocalDate.parse("2026-07-02"),
            montoTotal = 50.0, estado = "Pendiente")
        dao.upsertVenta(v1)
        dao.upsertVenta(v2)
        dao.deleteAll()

        val all = dao.getAllVentasByOptica("o1")
        assertEquals(0, all.size)
    }

    @Test
    fun venta_anulado_state_is_persisted() = runBlocking {
        val venta = Venta(id = "v_disp_anul", opticaId = "optica1", origen = "dispensacion",
            origenId = "anul", pacienteId = "p1",
            fecha = LocalDate.parse("2026-07-01"), montoTotal = 150.0, estado = "Anulado")
        dao.upsertVenta(venta)

        val retrieved = dao.getVentaById("v_disp_anul")
        assertNotNull(retrieved)
        assertEquals("Anulado", retrieved!!.estado)
    }
}
