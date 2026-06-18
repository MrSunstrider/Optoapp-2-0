package com.example.optoapp.data

import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
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

/**
 * Tests de integración para [MonturaMovimientoDao] usando Room in-memory database.
 *
 * Verifica que @Query, @Insert, @Upsert funcionan correctamente después
 * de la extracción del DAO a archivo separado.
 */
@RunWith(RobolectricTestRunner::class)
class MonturaMovimientoDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: MonturaMovimientoDao
    private lateinit var monturaDao: MonturaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.monturaMovimientoDao()
        monturaDao = db.monturaDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertMovimiento_and_retrieveViaGetMovimientosByOptica() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "Marca", modelo = "Mod1",
            color = "Negro", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1"
        )
        monturaDao.insertMontura(montura)

        val movimiento = MonturaMovimiento(
            id = "mov1",
            monturaId = "m1",
            fecha = LocalDate.parse("2026-01-15"),
            tipo = "ENTRADA",
            cantidad = 5,
            stockPrevio = 5,
            stockNuevo = 10,
            referenciaId = "REF001",
            nota = "Compra inicial",
            opticaId = "o1"
        )
        dao.insertMovimiento(movimiento)

        val movimientos = dao.getMovimientosByOptica("o1").first()

        assertEquals(1, movimientos.size)
        assertEquals("mov1", movimientos[0].id)
        assertEquals("ENTRADA", movimientos[0].tipo)
        assertEquals(5, movimientos[0].cantidad)
    }

    @Test
    fun getMovimientosByOptica_filtersByOptica() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "M", modelo = "X",
            color = "N", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1"
        )
        monturaDao.insertMontura(montura)

        val mov1 = MonturaMovimiento(
            id = "mov1", monturaId = "m1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "ENTRADA", cantidad = 5, stockPrevio = 0, stockNuevo = 5,
            opticaId = "opticaA"
        )
        val mov2 = MonturaMovimiento(
            id = "mov2", monturaId = "m1", fecha = LocalDate.parse("2026-02-01"),
            tipo = "SALIDA_VENTA", cantidad = 2, stockPrevio = 5, stockNuevo = 3,
            opticaId = "opticaB"
        )
        dao.insertMovimiento(mov1)
        dao.insertMovimiento(mov2)

        val opticaAMovimientos = dao.getMovimientosByOptica("opticaA").first()

        assertEquals(1, opticaAMovimientos.size)
        assertEquals("mov1", opticaAMovimientos[0].id)
    }

    @Test
    fun getMovimientosByMontura_returnsMovimientosForMontura() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "M", modelo = "X",
            color = "N", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1"
        )
        monturaDao.insertMontura(montura)
        val montura2 = Montura(
            id = "m2", sku = "S002", marca = "M", modelo = "Y",
            color = "N", talla = "M", costo = 60.0, precio = 120.0,
            stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1"
        )
        monturaDao.insertMontura(montura2)

        val mov1 = MonturaMovimiento(
            id = "mov1", monturaId = "m1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "ENTRADA", cantidad = 5, stockPrevio = 0, stockNuevo = 5,
            opticaId = "o1"
        )
        val mov2 = MonturaMovimiento(
            id = "mov2", monturaId = "m1", fecha = LocalDate.parse("2026-02-01"),
            tipo = "SALIDA_VENTA", cantidad = 2, stockPrevio = 5, stockNuevo = 3,
            opticaId = "o1"
        )
        val mov3 = MonturaMovimiento(
            id = "mov3", monturaId = "m2", fecha = LocalDate.parse("2026-03-01"),
            tipo = "ENTRADA", cantidad = 10, stockPrevio = 0, stockNuevo = 10,
            opticaId = "o1"
        )
        dao.insertMovimiento(mov1)
        dao.insertMovimiento(mov2)
        dao.insertMovimiento(mov3)

        val m1Movimientos = dao.getMovimientosByMontura("m1").first()

        assertEquals(2, m1Movimientos.size)
        assertTrue(m1Movimientos.all { it.monturaId == "m1" })
    }

    @Test
    fun getMovimientosListByOptica_returnsAllMovimientosForOptica() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "M", modelo = "X",
            color = "N", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1"
        )
        monturaDao.insertMontura(montura)

        val mov1 = MonturaMovimiento(
            id = "mov1", monturaId = "m1", fecha = LocalDate.parse("2026-01-15"),
            tipo = "ENTRADA", cantidad = 5, stockPrevio = 0, stockNuevo = 5,
            opticaId = "opticaX"
        )
        val mov2 = MonturaMovimiento(
            id = "mov2", monturaId = "m1", fecha = LocalDate.parse("2026-02-01"),
            tipo = "AJUSTE", cantidad = -1, stockPrevio = 5, stockNuevo = 4,
            opticaId = "opticaX"
        )
        dao.insertMovimiento(mov1)
        dao.insertMovimiento(mov2)

        val list = dao.getMovimientosListByOptica("opticaX")

        assertEquals(2, list.size)
    }

    @Test
    fun getMovimientosByOptica_whenNoMovimientos_returnsEmpty() = runBlocking {
        val movimientos = dao.getMovimientosByOptica("nonexistent").first()

        assertTrue(movimientos.isEmpty())
    }

    @Test
    fun getMovimientosByMontura_whenNoMovimientos_returnsEmpty() = runBlocking {
        val movimientos = dao.getMovimientosByMontura("nonexistent").first()

        assertTrue(movimientos.isEmpty())
    }

    /**
     * Regression: Room 2.6+ @Upsert generates `ON CONFLICT(id) DO UPDATE` which only resolves
     * PK conflicts. A sync download with a different id but same (referenciaId, tipo, monturaId)
     * crashed with SQLITE_CONSTRAINT_UNIQUE (2067) at login. Fixed by @Insert(REPLACE).
     */
    @Test
    fun insertMovimiento_duplicateSecondaryUniqueKey_replacesRowWithoutThrowing() = runBlocking {
        val montura = Montura(
            id = "m1", sku = "S001", marca = "M", modelo = "X",
            color = "N", talla = "M", costo = 50.0, precio = 100.0,
            stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1"
        )
        monturaDao.insertMontura(montura)

        val localRow = MonturaMovimiento(
            id = "mov-local",
            monturaId = "m1",
            fecha = LocalDate.parse("2026-06-18"),
            tipo = "SALIDA_VENTA",
            cantidad = 1,
            stockPrevio = 10,
            stockNuevo = 9,
            referenciaId = "disp-001",
            opticaId = "o1"
        )
        dao.insertMovimiento(localRow)

        val serverRow = MonturaMovimiento(
            id = "mov-server",
            monturaId = "m1",
            fecha = LocalDate.parse("2026-06-18"),
            tipo = "SALIDA_VENTA",
            cantidad = 1,
            stockPrevio = 10,
            stockNuevo = 9,
            referenciaId = "disp-001",
            opticaId = "o1"
        )
        dao.insertMovimiento(serverRow)

        val result = dao.getMovimientosListByOptica("o1")
        assertEquals(1, result.size)
        assertEquals("mov-server", result[0].id)
    }
}
