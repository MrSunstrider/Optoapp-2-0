package com.example.optoapp.data.categoriaproducto

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class CategoriaProductoDaoTest {
    private lateinit var db: OptoDatabase
    private lateinit var dao: CategoriaProductoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.categoriaProductoDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAll_returnsAll9Categories() = runBlocking {
        val seed = listOf(
            CategoriaProductoEntity("lente_progresivo", "Lentes Progresivos", "lente", 1),
            CategoriaProductoEntity("lente_monofocal", "Lentes Monofocales", "lente", 2),
            CategoriaProductoEntity("lente_bifocal", "Lentes Bifocales", "lente", 3),
            CategoriaProductoEntity("lente_otro", "Otros Lentes", "lente", 9),
            CategoriaProductoEntity("montura_premium", "Monturas Premium", "montura", 4),
            CategoriaProductoEntity("montura_estandar", "Monturas Estándar", "montura", 5),
            CategoriaProductoEntity("montura_economica", "Monturas Económicas", "montura", 6),
            CategoriaProductoEntity("servicio_extra", "Servicios Extra", "servicio", 7),
            CategoriaProductoEntity("servicio_garantia", "Garantías Extendidas", "servicio", 8),
        )
        dao.insertAll(seed)
        val result = dao.getAll().first()
        assertEquals(9, result.size)
        assertEquals("Lentes Progresivos", result.first().nombre)
    }

    @Test
    fun getByFamilia_returnsFilteredCategories() = runBlocking {
        val seed = listOf(
            CategoriaProductoEntity("lente_progresivo", "Lentes Progresivos", "lente", 1),
            CategoriaProductoEntity("montura_premium", "Monturas Premium", "montura", 4),
        )
        dao.insertAll(seed)
        val lentes = dao.getByFamilia("lente").first()
        assertEquals(1, lentes.size)
        assertEquals("lente", lentes.first().familia)
    }
}
