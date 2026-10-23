package com.example.optoapp.domain

import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionDao
import com.example.optoapp.data.feedbackrecomendacion.FeedbackRecomendacionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedbackRecomendacionUseCaseSeguimientoTest {

    private val dao: FeedbackRecomendacionDao = mockk(relaxed = true)
    private val syncStateTracker: SyncStateTracker = mockk(relaxed = true)
    private lateinit var useCase: FeedbackRecomendacionUseCase

    @Before
    fun setUp() {
        useCase = FeedbackRecomendacionUseCase(dao, syncStateTracker)
    }

    @Test
    fun marcarSeguimiento_persistsStableIdPeriodoAndEstado() = runBlocking {
        val entitySlot = slot<FeedbackRecomendacionEntity>()

        useCase.marcarSeguimiento(
            recomendacionId = "rec-abc",
            opticaId = "optica1",
            periodo = "2026-10",
            estado = SeguimientoEstado.HECHO,
        )

        coVerify { dao.upsert(capture(entitySlot)) }
        val saved = entitySlot.captured
        assertEquals("optica1|2026-10|rec-abc", saved.id)
        assertEquals("rec-abc", saved.recomendacionId)
        assertEquals("optica1", saved.opticaId)
        assertEquals("2026-10", saved.periodo)
        assertEquals(SeguimientoEstado.HECHO.name, saved.estado)
        assertTrue(saved.fueUtil)
    }

    @Test
    fun marcarSeguimiento_enCurso_setsFueUtilFalseAndUpdatedAt() = runBlocking {
        val entitySlot = slot<FeedbackRecomendacionEntity>()

        useCase.marcarSeguimiento("rec-1", "optica1", "2026-10", SeguimientoEstado.EN_CURSO)

        coVerify { dao.upsert(capture(entitySlot)) }
        assertEquals(SeguimientoEstado.EN_CURSO.name, entitySlot.captured.estado)
        assertEquals(false, entitySlot.captured.fueUtil)
        assertTrue(entitySlot.captured.updatedAt.isNotBlank())
    }

    @Test
    fun listarPorPeriodo_mapsEstadoByRecomendacionId() = runBlocking {
        coEvery { dao.getByOpticaIdAndPeriodo("optica1", "2026-10") } returns listOf(
            FeedbackRecomendacionEntity(
                id = "optica1|2026-10|rec-a",
                recomendacionId = "rec-a",
                opticaId = "optica1",
                fueUtil = true,
                estado = SeguimientoEstado.HECHO.name,
                periodo = "2026-10",
            ),
            FeedbackRecomendacionEntity(
                id = "optica1|2026-10|rec-b",
                recomendacionId = "rec-b",
                opticaId = "optica1",
                fueUtil = false,
                estado = SeguimientoEstado.NO_APLICA.name,
                periodo = "2026-10",
            ),
        )

        val map = useCase.listarPorPeriodo("optica1", "2026-10")
        assertEquals(SeguimientoEstado.HECHO, map["rec-a"])
        assertEquals(SeguimientoEstado.NO_APLICA, map["rec-b"])
    }
}
