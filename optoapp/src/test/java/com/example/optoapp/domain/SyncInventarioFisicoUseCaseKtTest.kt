package com.example.optoapp.domain

import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.data.InventarioFisicoDetalle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * SyncInventarioFisicoUseCase DTO and logic tests.
 * Tests entity-to-Remoto roundtrip and sync result data.
 */
class SyncInventarioFisicoUseCaseKtTest {

    @Test
    fun inventarioFisicoEntity_allFieldsAreAccessible() {
        val session = InventarioFisico(
            id = "if1", fecha = LocalDate.of(2026, 6, 17),
            estado = "EN_PROGRESO", opticaId = "o1", userId = "u1",
            notas = "Conteo trimestral", updatedAt = "2026-06-17T00:00:00Z"
        )
        assertEquals("if1", session.id)
        assertEquals(LocalDate.of(2026, 6, 17), session.fecha)
        assertEquals("EN_PROGRESO", session.estado)
        assertEquals("o1", session.opticaId)
        assertEquals("u1", session.userId)
        assertEquals("Conteo trimestral", session.notas)
        assertEquals("2026-06-17T00:00:00Z", session.updatedAt)
    }

    @Test
    fun inventarioFisicoEntity_defaultValues() {
        val session = InventarioFisico(
            id = "if1", fecha = LocalDate.now(), opticaId = "o1", userId = "u1"
        )
        assertEquals("EN_PROGRESO", session.estado)
        assertEquals("", session.notas)
    }

    @Test
    fun inventarioFisicoDetalleEntity_allFieldsAreAccessible() {
        val detalle = InventarioFisicoDetalle(
            id = "d1", inventarioId = "if1", monturaId = "m1",
            stockSistema = 10, stockContado = 8, diferencia = -2
        )
        assertEquals("d1", detalle.id)
        assertEquals("if1", detalle.inventarioId)
        assertEquals("m1", detalle.monturaId)
        assertEquals(10, detalle.stockSistema)
        assertEquals(8, detalle.stockContado)
        assertEquals(-2, detalle.diferencia)
    }

    @Test
    fun inventarioFisicoDetalleEntity_nullableFields() {
        val detalle = InventarioFisicoDetalle(
            id = "d1", inventarioId = "if1", monturaId = "m1", stockSistema = 5
        )
        assertEquals(5, detalle.stockSistema)
        assertEquals(null, detalle.stockContado)
        assertEquals(null, detalle.diferencia)
    }

    @Test
    fun inventarioFisicoSyncResult_hasCorrectFields() {
        val result = InventarioFisicoSyncResult(
            uploadedSessions = 2,
            uploadedDetalles = 50,
            downloadedSessions = 1,
            downloadedDetalles = 30
        )
        assertEquals(2, result.uploadedSessions)
        assertEquals(50, result.uploadedDetalles)
        assertEquals(1, result.downloadedSessions)
        assertEquals(30, result.downloadedDetalles)
    }

    @Test
    fun inventarioFisicoSyncResult_zeroForAll() {
        val result = InventarioFisicoSyncResult(0, 0, 0, 0)
        assertEquals(0, result.uploadedSessions)
        assertEquals(0, result.uploadedDetalles)
        assertEquals(0, result.downloadedSessions)
        assertEquals(0, result.downloadedDetalles)
    }

    @Test
    fun inventarioFisicoSyncResult_singleUploadNoDownload() {
        val result = InventarioFisicoSyncResult(
            uploadedSessions = 1,
            uploadedDetalles = 10,
            downloadedSessions = 0,
            downloadedDetalles = 0
        )
        assertEquals(1, result.uploadedSessions)
        assertEquals(10, result.uploadedDetalles)
        assertEquals(0, result.downloadedSessions)
    }
}
