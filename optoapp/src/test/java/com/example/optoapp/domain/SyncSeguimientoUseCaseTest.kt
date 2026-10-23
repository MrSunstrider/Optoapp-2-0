package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSeguimientoUseCaseTest {

    @Test
    fun upload_skipsRowsWithoutPeriodoOrEstado() = runTest {
        val dao = mockk<FeedbackRecomendacionDao>()
        coEvery { dao.getByOpticaId("o1") } returns listOf(
            FeedbackRecomendacionEntity(
                id = "o1|2026-07|r1",
                recomendacionId = "r1",
                opticaId = "o1",
                fueUtil = true,
                estado = "",
                periodo = "2026-07",
                updatedAt = "2026-07-01T00:00:00Z",
            ),
            FeedbackRecomendacionEntity(
                id = "o1|2026-07|r2",
                recomendacionId = "r2",
                opticaId = "o1",
                fueUtil = true,
                estado = "HECHO",
                periodo = "",
                updatedAt = "2026-07-01T00:00:00Z",
            ),
        )
        val tracker = mockk<SyncStateTracker>(relaxed = true)
        val useCase = SyncSeguimientoUseCase(
            feedbackRecomendacionDao = dao,
            supabase = mockk(relaxed = true),
            syncStateTracker = tracker,
            networkRetryHelper = mockk(relaxed = true),
        )

        val n = useCase.upload("o1")
        assertEquals(0, n)
        coVerify { tracker.markSynced("o1", "upload_feedback_recomendacion", "batch") }
    }
}
