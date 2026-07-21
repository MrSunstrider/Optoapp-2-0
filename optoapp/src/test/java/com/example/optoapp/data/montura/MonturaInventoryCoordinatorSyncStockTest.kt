package com.example.optoapp.data.montura

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.Resource
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.Lazy
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class MonturaInventoryCoordinatorSyncStockTest {

    private lateinit var db: OptoDatabase
    private lateinit var monturaDao: MonturaDao
    private lateinit var movimientoDao: MonturaMovimientoDao
    private lateinit var coordinator: MonturaInventoryCoordinator

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        monturaDao = db.monturaDao()
        movimientoDao = db.monturaMovimientoDao()
        val mockScheduler = mockk<PostSaveSyncScheduler>(relaxed = true)
        coordinator = MonturaInventoryCoordinator(monturaDao, movimientoDao, Lazy { mockScheduler })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun syncStockFromMovimientos_rebuildsCorrectStock() = runBlocking {
        monturaDao.insertMontura(
            Montura(
                id = "m1", sku = "S1", marca = "A", modelo = "X",
                color = "N", talla = "M", costo = 50.0, precio = 100.0,
                stockActual = 0, stockMinimo = 2, activo = true, opticaId = "o1",
            ),
        )
        monturaDao.insertMontura(
            Montura(
                id = "m2", sku = "S2", marca = "B", modelo = "Y",
                color = "N", talla = "M", costo = 60.0, precio = 120.0,
                stockActual = 0, stockMinimo = 2, activo = true, opticaId = "o1",
            ),
        )

        movimientoDao.insertMovimiento(
            MonturaMovimiento(
                id = "mov1",
                monturaId = "m1",
                tipo = "ENTRADA",
                cantidad = 10,
                stockPrevio = 0,
                stockNuevo = 10,
                opticaId = "o1",
                fecha = LocalDate.of(2026, 6, 1),
            ),
        )
        movimientoDao.insertMovimiento(
            MonturaMovimiento(
                id = "mov2",
                monturaId = "m1",
                tipo = "SALIDA_VENTA",
                cantidad = 3,
                stockPrevio = 10,
                stockNuevo = 7,
                opticaId = "o1",
                fecha = LocalDate.of(2026, 6, 10),
            ),
        )
        movimientoDao.insertMovimiento(
            MonturaMovimiento(
                id = "mov3",
                monturaId = "m2",
                tipo = "ENTRADA",
                cantidad = 5,
                stockPrevio = 0,
                stockNuevo = 5,
                opticaId = "o1",
                fecha = LocalDate.of(2026, 6, 5),
            ),
        )

        val result = coordinator.syncStockFromMovimientos("o1")

        assertTrue(result is Resource.Success)
        val m1 = monturaDao.getMonturaByIdForOptica("m1", "o1")
        assertEquals(7, m1?.stockActual)
        val m2 = monturaDao.getMonturaByIdForOptica("m2", "o1")
        assertEquals(5, m2?.stockActual)
    }

    @Test
    fun syncStockFromMovimientos_emptyMovimientos_keepsCurrentStock() = runBlocking {
        monturaDao.insertMontura(
            Montura(
                id = "m1", sku = "S1", marca = "A", modelo = "X",
                color = "N", talla = "M", costo = 50.0, precio = 100.0,
                stockActual = 15, stockMinimo = 2, activo = true, opticaId = "o1",
            ),
        )

        val result = coordinator.syncStockFromMovimientos("o1")

        assertTrue(result is Resource.Success)
        val m1 = monturaDao.getMonturaByIdForOptica("m1", "o1")
        assertEquals(15, m1?.stockActual)
    }

    @Test
    fun syncStockFromMovimientos_respectsOpticaId() = runBlocking {
        monturaDao.insertMontura(
            Montura(
                id = "m1", sku = "S1", marca = "A", modelo = "X",
                color = "N", talla = "M", costo = 50.0, precio = 100.0,
                stockActual = 0, stockMinimo = 2, activo = true, opticaId = "o1",
            ),
        )
        movimientoDao.insertMovimiento(
            MonturaMovimiento(
                id = "mov1",
                monturaId = "m1",
                tipo = "ENTRADA",
                cantidad = 5,
                stockPrevio = 0,
                stockNuevo = 5,
                opticaId = "o2",
                fecha = LocalDate.of(2026, 6, 1),
            ),
        )

        val result = coordinator.syncStockFromMovimientos("o1")

        assertTrue(result is Resource.Success)
        val m1 = monturaDao.getMonturaByIdForOptica("m1", "o1")
        assertEquals(0, m1?.stockActual)
    }
}
