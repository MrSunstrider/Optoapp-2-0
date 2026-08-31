package com.example.optoapp.data.montura

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonturaDaoAdjustStockStampTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: MonturaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.monturaDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun adjustStock_setsUpdatedAt() = runBlocking {
        dao.insertMontura(
            Montura(
                id = "m1", sku = "S1", marca = "A", modelo = "X",
                color = "N", talla = "M", costo = 50.0, precio = 100.0,
                stockActual = 5, stockMinimo = 2, activo = true, opticaId = "o1",
            ),
        )
        assertNull(dao.getMonturaByIdForOptica("m1", "o1")?.updatedAt)

        val stamp = "2026-08-31T10:00:00Z"
        val changed = dao.adjustStock("m1", "o1", -1, stamp)
        assertEquals(1, changed)

        assertEquals(stamp, dao.getMonturaByIdForOptica("m1", "o1")?.updatedAt)
    }

    @Test
    fun adjustStock_belowZero_doesNotStamp() = runBlocking {
        dao.insertMontura(
            Montura(
                id = "m1", sku = "S1", marca = "A", modelo = "X",
                color = "N", talla = "M", costo = 50.0, precio = 100.0,
                stockActual = 1, stockMinimo = 0, activo = true, opticaId = "o1",
            ),
        )
        assertNull(dao.getMonturaByIdForOptica("m1", "o1")?.updatedAt)

        val changed = dao.adjustStock("m1", "o1", -5, "2026-08-31T10:00:00Z")
        assertEquals(0, changed)

        val retrieved = dao.getMonturaByIdForOptica("m1", "o1")
        assertEquals(1, retrieved!!.stockActual)
        assertNull(retrieved.updatedAt)
    }
}
