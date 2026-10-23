package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionEntity
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedbackRecomendacionUseCaseTest {

    private val dao: FeedbackRecomendacionDao = mockk(relaxed = true)
    private val syncStateTracker: SyncStateTracker = mockk(relaxed = true)
    private lateinit var useCase: FeedbackRecomendacionUseCase

    @Before
    fun setUp() {
        useCase = FeedbackRecomendacionUseCase(dao, syncStateTracker)
    }

    @Test
    fun marcarSeguimiento_hecho_setsFueUtilTrue() = runTest {
        val entitySlot = slot<FeedbackRecomendacionEntity>()

        useCase.marcarSeguimiento("rec-abc", "optica1", "2026-10", SeguimientoEstado.HECHO)

        coVerify { dao.upsert(capture(entitySlot)) }
        assertEquals("rec-abc", entitySlot.captured.recomendacionId)
        assertEquals("optica1", entitySlot.captured.opticaId)
        assertEquals("2026-10", entitySlot.captured.periodo)
        assertTrue(entitySlot.captured.fueUtil)
        assertEquals(SeguimientoEstado.HECHO.name, entitySlot.captured.estado)
        assertTrue(entitySlot.captured.id.isNotBlank())
    }

    @Test
    fun marcarSeguimiento_marksLocallyModifiedForSync() = runTest {
        val stableId = FeedbackRecomendacionUseCase.stableId("optica1", "2026-10", "rec-abc")

        useCase.marcarSeguimiento("rec-abc", "optica1", "2026-10", SeguimientoEstado.HECHO)

        coVerify {
            syncStateTracker.markLocallyModified("optica1", "feedback_recomendacion", stableId)
        }
    }

    @Test
    fun marcarSeguimiento_noAplica_setsFueUtilFalse() = runTest {
        val entitySlot = slot<FeedbackRecomendacionEntity>()

        useCase.marcarSeguimiento("rec-xyz", "optica1", "2026-10", SeguimientoEstado.NO_APLICA)

        coVerify { dao.upsert(capture(entitySlot)) }
        assertEquals("rec-xyz", entitySlot.captured.recomendacionId)
        assertEquals("optica1", entitySlot.captured.opticaId)
        assertFalse(entitySlot.captured.fueUtil)
        assertEquals(SeguimientoEstado.NO_APLICA.name, entitySlot.captured.estado)
    }

    @Test
    fun marcarSeguimiento_twice_isIdempotentSameStableId() = runTest {
        useCase.marcarSeguimiento("rec-abc", "optica1", "2026-10", SeguimientoEstado.HECHO)
        useCase.marcarSeguimiento("rec-abc", "optica1", "2026-10", SeguimientoEstado.HECHO)

        coVerify(exactly = 2) { dao.upsert(any()) }
        coVerify(exactly = 2) {
            syncStateTracker.markLocallyModified(
                "optica1",
                "feedback_recomendacion",
                FeedbackRecomendacionUseCase.stableId("optica1", "2026-10", "rec-abc"),
            )
        }
    }
}
