package com.example.optoapp.data.montura

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LowStockThresholdTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: MonturaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.monturaDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun lowStockCount_usesPerMonturaMinimo_overDefault() = runBlocking {
        dao.insertMontura(montura("m1", stockActual = 3, stockMinimo = 5))
        dao.insertMontura(montura("m2", stockActual = 4, stockMinimo = 0))

        val count = dao.getLowStockCount("o1", defaultThreshold = 2).first()

        assertEquals(1, count)
    }

    @Test
    fun lowStockList_returnsOnlyActiveLowStockMonturas() = runBlocking {
        dao.insertMontura(montura("m1", stockActual = 0, stockMinimo = 3, activo = true))
        dao.insertMontura(montura("m2", stockActual = 10, stockMinimo = 2, activo = true))
        dao.insertMontura(montura("m3", stockActual = 0, stockMinimo = 3, activo = false))
        dao.insertMontura(montura("m4", stockActual = 1, stockMinimo = 0, activo = true))

        val list = dao.getLowStockList("o1", defaultThreshold = 2)

        assertEquals(2, list.size)
        assertEquals("m1", list[0].id)
    }

    @Test
    fun lowStockQuery_withHighThreshold_includesMoreMonturas() = runBlocking {
        dao.insertMontura(montura("m1", stockActual = 8, stockMinimo = 0))
        dao.insertMontura(montura("m2", stockActual = 3, stockMinimo = 0))

        val withThreshold2 = dao.getLowStockCount("o1", defaultThreshold = 2).first()
        val withThreshold10 = dao.getLowStockCount("o1", defaultThreshold = 10).first()

        assertEquals(0, withThreshold2)
        assertEquals(2, withThreshold10)
    }

    @Test
    fun lowStockQuery_respectsOpticaId() = runBlocking {
        dao.insertMontura(montura("m1", stockActual = 0, stockMinimo = 3, opticaId = "o1"))
        dao.insertMontura(montura("m2", stockActual = 0, stockMinimo = 3, opticaId = "o2"))

        val count = dao.getLowStockCount("o1", defaultThreshold = 2).first()

        assertEquals(1, count)
    }

    private fun montura(
        id: String,
        stockActual: Int = 10,
        stockMinimo: Int = 2,
        activo: Boolean = true,
        opticaId: String = "o1"
    ) = Montura(
        id = id, sku = "SKU-$id", marca = "M", modelo = "X",
        color = "N", talla = "M", costo = 50.0, precio = 100.0,
        stockActual = stockActual, stockMinimo = stockMinimo,
        activo = activo, opticaId = opticaId
    )
}
