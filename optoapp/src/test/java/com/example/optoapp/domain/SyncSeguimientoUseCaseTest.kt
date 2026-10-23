package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionEntity
import com.example.optoapp.domain.sync.RemoteTimestamp
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSeguimientoUseCaseTest {

    private fun entity(
        id: String = "o1|2026-07|r1",
        estado: String = "HECHO",
        periodo: String = "2026-07",
        updatedAt: String = "2026-07-01T00:00:00Z",
    ) = FeedbackRecomendacionEntity(
        id = id,
        recomendacionId = "r1",
        opticaId = "o1",
        fueUtil = true,
        estado = estado,
        periodo = periodo,
        updatedAt = updatedAt,
    )

    private fun retryHelper(): NetworkRetryHelper {
        val helper = mockk<NetworkRetryHelper>(relaxed = true)
        coEvery { helper.retryNetwork(any(), any()) } coAnswers {
            secondArg<suspend () -> Unit>().invoke()
        }
        return helper
    }

    @Test
    fun upload_skipsRowsWithoutPeriodoOrEstado() = runTest {
        val dao = mockk<FeedbackRecomendacionDao>()
        coEvery { dao.getByOpticaId("o1") } returns listOf(
            entity(id = "o1|2026-07|r1", estado = ""),
            entity(id = "o1|2026-07|r2", periodo = ""),
        )
        val tracker = mockk<SyncStateTracker>(relaxed = true)
        coEvery { tracker.syncedIds("o1", "feedback_recomendacion") } returns emptySet()
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

    @Test
    fun upload_skipsStalePendingWhenRemoteNewer() = runTest {
        val id = "o1|2026-07|r1"
        val dao = mockk<FeedbackRecomendacionDao>()
        coEvery { dao.getByOpticaId("o1") } returns listOf(entity(id = id, updatedAt = "2026-07-01T00:00:00Z"))
        val tracker = mockk<SyncStateTracker>(relaxed = true)
        coEvery { tracker.syncedIds("o1", "feedback_recomendacion") } returns emptySet()
        val useCase = object : SyncSeguimientoUseCase(dao, mockk(relaxed = true), tracker, retryHelper()) {
            override suspend fun selectRemoteUpdatedAtChunk(opticaId: String, ids: List<String>) =
                listOf(RemoteTimestamp(id = id, updatedAt = "2026-07-02T00:00:00Z"))
        }

        val n = useCase.upload("o1")
        assertEquals(0, n)
        coVerify(exactly = 0) { tracker.markSynced("o1", "feedback_recomendacion", id) }
        coVerify { tracker.markSynced("o1", "upload_feedback_recomendacion", "batch") }
    }

    @Test
    fun download_keepsPendingLocalNewerThanRemote() = runTest {
        val id = "o1|2026-07|r1"
        val dao = mockk<FeedbackRecomendacionDao>(relaxed = true)
        coEvery { dao.getById(id) } returns entity(id = id, updatedAt = "2026-07-03T00:00:00Z")
        val tracker = mockk<SyncStateTracker>(relaxed = true)
        coEvery { tracker.isSynced("o1", "feedback_recomendacion", id) } returns false
        val useCase = object : SyncSeguimientoUseCase(dao, mockk(relaxed = true), tracker, retryHelper()) {
            override suspend fun fetchRemoteRows(opticaId: String) = listOf(
                FeedbackRecomendacionRemoto(
                    id = id,
                    opticaId = "o1",
                    recomendacionId = "r1",
                    fueUtil = false,
                    estado = "EN_CURSO",
                    periodo = "2026-07",
                    updatedAt = "2026-07-01T00:00:00Z",
                ),
            )
        }

        val applied = useCase.download("o1")
        assertEquals(0, applied)
        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { tracker.markSynced("o1", "feedback_recomendacion", id) }
    }
}
