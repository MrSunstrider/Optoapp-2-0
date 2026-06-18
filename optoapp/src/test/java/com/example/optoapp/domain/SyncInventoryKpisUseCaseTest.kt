package com.example.optoapp.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.Montura
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.Resource
import com.example.optoapp.data.montura.MonturaDashboardKpiRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncInventoryKpisUseCaseTest {

    private lateinit var db: OptoDatabase
    private lateinit var repository: MonturaDashboardKpiRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = MonturaDashboardKpiRepository(db.monturaDao(), db.monturaMovimientoDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun syncKpis_collectsSummaryData() = runBlocking {
        db.monturaDao().insertMontura(
            Montura(id = "m1", sku = "S1", marca = "A", modelo = "X",
                color = "N", talla = "M", costo = 50.0, precio = 100.0,
                stockActual = 3, stockMinimo = 5, activo = true, opticaId = "o1")
        )
        db.monturaDao().insertMontura(
            Montura(id = "m2", sku = "S2", marca = "B", modelo = "Y",
                color = "N", talla = "M", costo = 60.0, precio = 120.0,
                stockActual = 10, stockMinimo = 2, activo = true, opticaId = "o1",
                categoria = "SOL")
        )

        val useCase = SyncInventoryKpisUseCase(repository)
        val result = useCase("o1")

        assertTrue(result is Resource.Success)
        val summary = (result as Resource.Success).data!!
        assertEquals(2, summary.totalModelos)
        assertEquals(1, summary.lowStockCount)
        assertEquals(1, summary.stockByCategory.size)
    }

    @Test
    fun syncKpis_emptyOptica_returnsZeros() = runBlocking {
        val useCase = SyncInventoryKpisUseCase(repository)
        val result = useCase("vacia")

        assertTrue(result is Resource.Success)
        val summary = (result as Resource.Success).data!!
        assertEquals(0, summary.totalModelos)
        assertEquals(0, summary.lowStockCount)
    }
}
