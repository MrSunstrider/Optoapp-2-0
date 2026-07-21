package com.example.optoapp.data.configuracionfinanciera

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
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

@RunWith(RobolectricTestRunner::class)
class ConfiguracionFinancieraDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: ConfiguracionFinancieraDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.configuracionFinancieraDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGetByOpticaId_returnsConfig() = runBlocking {
        val config = ConfiguracionFinancieraEntity(
            opticaId = "optica1",
            margenNetoObjetivo = 15.0,
            deudaViejaAlertaDias = 30,
            minVentasParaRecomendar = 5,
        )
        dao.upsert(config)

        val retrieved = dao.getByOpticaId("optica1").first()
        assertNotNull(retrieved)
        assertEquals("optica1", retrieved!!.opticaId)
        assertEquals(15.0, retrieved.margenNetoObjetivo, 0.001)
        assertEquals(30, retrieved.deudaViejaAlertaDias)
        assertEquals(5, retrieved.minVentasParaRecomendar)
    }

    // F8-CONFIG-DAO RED tests

    @Test
    fun getByOpticaIdOnce_returnsCorrectConfig() = runBlocking {
        val config = ConfiguracionFinancieraEntity(
            opticaId = "optica2",
            margenNetoObjetivo = 25.0,
            deudaViejaAlertaDias = 45,
            minVentasParaRecomendar = 10,
        )
        dao.upsert(config)

        val retrieved = dao.getByOpticaIdOnce("optica2")
        assertNotNull(retrieved)
        assertEquals("optica2", retrieved!!.opticaId)
        assertEquals(25.0, retrieved.margenNetoObjetivo, 0.001)
        assertEquals(45, retrieved.deudaViejaAlertaDias)
        assertEquals(10, retrieved.minVentasParaRecomendar)
    }

    @Test
    fun getByOpticaIdOnce_returnsNullForMissing() = runBlocking {
        val result = dao.getByOpticaIdOnce("nonexistent")
        assertNull(result)
    }

    @Test
    fun upsertOverwritesExisting() = runBlocking {
        val config1 = ConfiguracionFinancieraEntity(
            opticaId = "optica1",
            margenNetoObjetivo = 15.0,
            deudaViejaAlertaDias = 30,
        )
        dao.upsert(config1)

        val config2 = ConfiguracionFinancieraEntity(
            opticaId = "optica1",
            margenNetoObjetivo = 20.0,
            deudaViejaAlertaDias = 60,
        )
        dao.upsert(config2)

        val retrieved = dao.getByOpticaId("optica1").first()
        assertNotNull(retrieved)
        assertEquals(20.0, retrieved!!.margenNetoObjetivo, 0.001)
        assertEquals(60, retrieved.deudaViejaAlertaDias)
    }
}
