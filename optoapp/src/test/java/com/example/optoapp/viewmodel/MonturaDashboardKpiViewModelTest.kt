package com.example.optoapp.viewmodel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OpticaFiscalSettingsStore
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.montura.MonturaDashboardKpiRepository
import com.example.optoapp.data.montura.MonturaDao
import com.example.optoapp.data.montura.MonturaMovimientoDao
import com.example.optoapp.util.BackgroundErrorCollector
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class MonturaDashboardKpiViewModelTest {

    private lateinit var db: OptoDatabase
    private lateinit var monturaDao: MonturaDao
    private lateinit var movimientoDao: MonturaMovimientoDao
    private lateinit var repository: MonturaDashboardKpiRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        monturaDao = db.monturaDao()
        movimientoDao = db.monturaMovimientoDao()
        repository = MonturaDashboardKpiRepository(monturaDao, movimientoDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun totalModelos_countsActiveMonturas() = runBlocking {
        insertMontura(id = "m1", activo = true)
        insertMontura(id = "m2", activo = true)
        insertMontura(id = "m3", activo = false)

        val count = repository.totalModelos("o1").first()

        assertEquals(2, count)
    }

    @Test
    fun stockByCategory_groupsActiveMonturasWithCategory() = runBlocking {
        insertMontura(id = "m1", categoria = "SOL", activo = true)
        insertMontura(id = "m2", categoria = "SOL", activo = true)
        insertMontura(id = "m3", categoria = "GRADUADA", activo = true)
        insertMontura(id = "m4", categoria = "", activo = true)

        val categories = repository.stockByCategory("o1").first()

        assertEquals(2, categories.size)
        val solCategory = categories.find { it.categoria == "SOL" }
        assertNotNull(solCategory)
        assertEquals(2, solCategory!!.cnt)
        val gradCategory = categories.find { it.categoria == "GRADUADA" }
        assertNotNull(gradCategory)
        assertEquals(1, gradCategory!!.cnt)
    }

    @Test
    fun lowStockCount_countsMonturasBelowThreshold() = runBlocking {
        insertMontura(id = "m1", stockActual = 0, stockMinimo = 3, activo = true)
        insertMontura(id = "m2", stockActual = 1, stockMinimo = 0, activo = true)
        insertMontura(id = "m3", stockActual = 10, stockMinimo = 2, activo = true)

        val count = repository.lowStockCount("o1", defaultThreshold = 2).first()

        assertEquals(2, count)
    }

    @Test
    fun lastMovementDate_returnsMaxDate() = runBlocking {
        insertMontura(id = "m1", activo = true)
        insertMontura(id = "m2", activo = true)
        val fecha1 = LocalDate.of(2026, 6, 10)
        val fecha2 = LocalDate.of(2026, 6, 15)
        movimientoDao.insertMovimiento(
            MonturaMovimiento(id = "mov1", monturaId = "m1", tipo = "ENTRADA",
                cantidad = 5, stockPrevio = 0, stockNuevo = 5, opticaId = "o1", fecha = fecha1)
        )
        movimientoDao.insertMovimiento(
            MonturaMovimiento(id = "mov2", monturaId = "m2", tipo = "SALIDA_VENTA",
                cantidad = 1, stockPrevio = 5, stockNuevo = 4, opticaId = "o1", fecha = fecha2)
        )

        val lastDate = repository.lastMovementDate("o1").first()

        assertEquals(fecha2, lastDate)
    }

    @Test
    fun lastMovementDate_noMovements_returnsNull() = runBlocking {
        val lastDate = repository.lastMovementDate("o1").first()

        assertEquals(null, lastDate)
    }

    @Test
    fun totalModelos_emptyOptica_returnsZero() = runBlocking {
        val count = repository.totalModelos("optica_vacia").first()

        assertEquals(0, count)
    }

    @Test
    fun lowStockCount_usesPerMonturaThresholdOverGlobal() = runBlocking {
        insertMontura(id = "m1", stockActual = 4, stockMinimo = 5, activo = true)
        insertMontura(id = "m2", stockActual = 4, stockMinimo = 0, activo = true)

        val count = repository.lowStockCount("o1", defaultThreshold = 2).first()

        assertEquals(1, count)
    }

    private suspend fun insertMontura(
        id: String,
        activo: Boolean = true,
        categoria: String = "",
        stockActual: Int = 10,
        stockMinimo: Int = 2
    ) {
        monturaDao.insertMontura(
            Montura(
                id = id,
                sku = "SKU-$id",
                marca = "Marca",
                modelo = "Modelo",
                color = "Negro",
                talla = "M",
                costo = 50.0,
                precio = 100.0,
                stockActual = stockActual,
                stockMinimo = stockMinimo,
                activo = activo,
                categoria = categoria,
                opticaId = "o1"
            )
        )
    }
}
