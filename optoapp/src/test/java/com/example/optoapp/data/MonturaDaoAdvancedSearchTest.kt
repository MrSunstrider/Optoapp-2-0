package com.example.optoapp.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.montura.MonturaDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class MonturaDaoAdvancedSearchTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: MonturaDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.monturaDao()

        runBlocking {
            dao.insertMontura(
                Montura(
                    id = "m1", sku = "SKU-001", marca = "RayBan", modelo = "Aviator",
                    color = "Negro", costo = 100.0, precio = 250.0,
                    stockActual = 10, stockMinimo = 3, activo = true,
                    tipoAro = "METALICO", materialMontura = "Metal",
                    categoria = "SOL", coleccion = "Classic", temporada = "2026-VERANO",
                    estadoComercial = "ACTIVO", genero = "UNISEX", opticaId = "o1",
                ),
            )
            dao.insertMontura(
                Montura(
                    id = "m2", sku = "SKU-002", marca = "Oakley", modelo = "Holbrook",
                    color = "Azul", costo = 120.0, precio = 300.0,
                    stockActual = 5, stockMinimo = 5, activo = true,
                    tipoAro = "PASTA", materialMontura = "Acetato",
                    categoria = "SOL", coleccion = "Sport", temporada = "2026-VERANO",
                    estadoComercial = "PROMOCION", genero = "MASCULINO", opticaId = "o1",
                ),
            )
            dao.insertMontura(
                Montura(
                    id = "m3", sku = "SKU-003", marca = "RayBan", modelo = "Wayfarer",
                    color = "Negro", costo = 80.0, precio = 200.0,
                    stockActual = 2, stockMinimo = 10, activo = true,
                    tipoAro = "PASTA", materialMontura = "Plastico",
                    categoria = "GRADUADA", coleccion = "Classic", temporada = "2025-INVIERNO",
                    estadoComercial = "ACTIVO", genero = "FEMENINO", opticaId = "o1",
                ),
            )
            dao.insertMontura(
                Montura(
                    id = "m4", sku = "SKU-004", marca = "Nike", modelo = "SportFlex",
                    color = "Rojo", costo = 90.0, precio = 220.0,
                    stockActual = 15, stockMinimo = 5, activo = true,
                    tipoAro = "METALICO", materialMontura = "Titanio",
                    categoria = "DEPORTIVA", coleccion = "Run", temporada = "2026-OTOÑO",
                    estadoComercial = "DISCONTINUADO", genero = "MASCULINO", opticaId = "o1",
                ),
            )
        }
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun searchByMarca_returnsMatchingResults() = runBlocking {
        val results = dao.searchMonturas(opticaId = "o1", marca = "RayBan")
        assertEquals(2, results.size)
        assertTrue(results.all { it.marca == "RayBan" })
    }

    @Test
    fun searchByMaterial_returnsMatchingResults() = runBlocking {
        val results = dao.searchMonturas(opticaId = "o1", material = "Metal")
        assertEquals(1, results.size)
        assertEquals("RayBan", results[0].marca)
    }

    @Test
    fun searchByCategoria_returnsMatchingResults() = runBlocking {
        val results = dao.searchMonturas(opticaId = "o1", categoria = "SOL")
        assertEquals(2, results.size)
        assertTrue(results.all { it.categoria == "SOL" })
    }

    @Test
    fun searchByPriceRange_returnsFilteredResults() = runBlocking {
        val results = dao.searchMonturas(
            opticaId = "o1",
            precioMin = 220.0,
            precioMax = 300.0,
        )
        assertEquals(3, results.size)
    }

    @Test
    fun searchByStockLevel_returnsLowStock() = runBlocking {
        val results = dao.searchMonturas(opticaId = "o1", stockBajo = 1)
        assertEquals(2, results.size)
    }

    @Test
    fun searchByCombinedFilters_returnsIntersection() = runBlocking {
        val results = dao.searchMonturas(
            opticaId = "o1",
            marca = "RayBan",
            categoria = "GRADUADA",
        )
        assertEquals(1, results.size)
        assertEquals("Wayfarer", results[0].modelo)
    }

    @Test
    fun searchAllParamsNull_returnsAllActive() = runBlocking {
        val results = dao.searchMonturas(opticaId = "o1")
        assertEquals(4, results.size)
    }
}
