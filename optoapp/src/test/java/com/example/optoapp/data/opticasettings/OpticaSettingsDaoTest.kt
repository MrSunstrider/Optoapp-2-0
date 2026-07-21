package com.example.optoapp.data.opticasettings

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
class OpticaSettingsDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: OpticaSettingsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.opticaSettingsDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndGetByOpticaId_returnsSettings() = runBlocking {
        val settings = OpticaSettingsEntity(
            opticaId = "optica1",
            configJson = """{"business_hours": "Lunes a Viernes de 9am a 7pm"}""",
        )
        dao.upsert(settings)

        val retrieved = dao.getByOpticaId("optica1").first()
        assertNotNull(retrieved)
        assertEquals("optica1", retrieved!!.opticaId)
        assertEquals("""{"business_hours": "Lunes a Viernes de 9am a 7pm"}""", retrieved.configJson)
    }

    @Test
    fun getByOpticaIdOnce_returnsCorrectSettings() = runBlocking {
        val settings = OpticaSettingsEntity(
            opticaId = "optica2",
            configJson = """{"business_hours": "Martes a Sabado de 10am a 6pm"}""",
        )
        dao.upsert(settings)

        val retrieved = dao.getByOpticaIdOnce("optica2")
        assertNotNull(retrieved)
        assertEquals("optica2", retrieved!!.opticaId)
        assertEquals("""{"business_hours": "Martes a Sabado de 10am a 6pm"}""", retrieved.configJson)
    }

    @Test
    fun getByOpticaIdOnce_returnsNullForMissing() = runBlocking {
        val result = dao.getByOpticaIdOnce("nonexistent")
        assertNull(result)
    }

    @Test
    fun upsertOverwritesExisting() = runBlocking {
        val settings1 = OpticaSettingsEntity(
            opticaId = "optica1",
            configJson = """{"business_hours": "Lunes a Viernes de 9am a 7pm"}""",
        )
        dao.upsert(settings1)

        val settings2 = OpticaSettingsEntity(
            opticaId = "optica1",
            configJson = """{"business_hours": "Lunes a Sabado de 8am a 8pm"}""",
        )
        dao.upsert(settings2)

        val retrieved = dao.getByOpticaId("optica1").first()
        assertNotNull(retrieved)
        assertEquals("""{"business_hours": "Lunes a Sabado de 8am a 8pm"}""", retrieved!!.configJson)
    }

    @Test
    fun upsertWithDefaultConfigJson() = runBlocking {
        val settings = OpticaSettingsEntity(opticaId = "optica3")
        dao.upsert(settings)

        val retrieved = dao.getByOpticaId("optica3").first()
        assertNotNull(retrieved)
        assertEquals("{}", retrieved!!.configJson)
    }
}
