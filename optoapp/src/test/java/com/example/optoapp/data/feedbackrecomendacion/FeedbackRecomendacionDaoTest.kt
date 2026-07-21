package com.example.optoapp.data.feedbackrecomendacion

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.OptoDatabase
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
class FeedbackRecomendacionDaoTest {

    private lateinit var db: OptoDatabase
    private lateinit var dao: FeedbackRecomendacionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OptoDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.feedbackRecomendacionDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_insertsNewFeedback() = runBlocking {
        val feedback = FeedbackRecomendacionEntity(
            id = "fb-1",
            recomendacionId = "rec-abc",
            opticaId = "optica1",
            fueUtil = true,
            fecha = System.currentTimeMillis(),
        )
        dao.upsert(feedback)

        val result = dao.getByOpticaId("optica1")
        assertEquals(1, result.size)
        assertEquals("rec-abc", result[0].recomendacionId)
        assertEquals("optica1", result[0].opticaId)
        assertTrue(result[0].fueUtil)
        assertEquals("fb-1", result[0].id)
    }

    @Test
    fun upsert_updatesExistingFeedback() = runBlocking {
        val fb1 = FeedbackRecomendacionEntity(
            id = "fb-1",
            recomendacionId = "rec-abc",
            opticaId = "optica1",
            fueUtil = false,
            fecha = 1000L,
        )
        dao.upsert(fb1)

        val fb2 = FeedbackRecomendacionEntity(
            id = "fb-1",
            recomendacionId = "rec-abc",
            opticaId = "optica1",
            fueUtil = true,
            fecha = 2000L,
        )
        dao.upsert(fb2)

        val result = dao.getByOpticaId("optica1")
        assertEquals(1, result.size)
        assertTrue(result[0].fueUtil)
        // Same id → upsert replaced the row
        assertEquals("fb-1", result[0].id)
    }

    @Test
    fun getByOpticaId_returnsAllForOptica() = runBlocking {
        // Insert 2 for optica1 and 1 for optica2
        dao.upsert(
            FeedbackRecomendacionEntity(
                id = "fb-1",
                recomendacionId = "rec-1",
                opticaId = "optica1",
                fueUtil = true,
                fecha = 1000L,
            ),
        )
        dao.upsert(
            FeedbackRecomendacionEntity(
                id = "fb-2",
                recomendacionId = "rec-2",
                opticaId = "optica1",
                fueUtil = false,
                fecha = 2000L,
            ),
        )
        dao.upsert(
            FeedbackRecomendacionEntity(
                id = "fb-3",
                recomendacionId = "rec-3",
                opticaId = "optica2",
                fueUtil = true,
                fecha = 1500L,
            ),
        )

        val resultO1 = dao.getByOpticaId("optica1")
        assertEquals(2, resultO1.size)
        // Ordered by fecha DESC
        assertEquals(2000L, resultO1[0].fecha)
        assertEquals(1000L, resultO1[1].fecha)

        val resultO2 = dao.getByOpticaId("optica2")
        assertEquals(1, resultO2.size)
        assertEquals("rec-3", resultO2[0].recomendacionId)
    }
}
