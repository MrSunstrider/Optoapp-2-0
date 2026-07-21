package com.example.optoapp.data

import com.example.optoapp.domain.toRemoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Verifies that toRemoto() does NOT generate a client-side timestamp when updatedAt is null.
 *
 * Generating Instant.now() on every call causes artificial drift: the server sees a
 * locally-fabricated timestamp and interprets the record as newer than what it holds,
 * producing false conflicts on every sync cycle.
 *
 * After RC-1 the contract is:
 *  - toRemoto() on a null-updatedAt entity yields null in the DTO (not a generated timestamp)
 *  - Calling toRemoto() twice on the same entity yields the same updatedAt (no drift)
 */
class SyncDtoTimestampTest {

    private val testDate = LocalDate.of(2024, 6, 15)

    // ── EvaluacionClinica ─────────────────────────────────────────────────────

    private fun evaluacion(updatedAt: String? = null) = EvaluacionClinica(
        id = "ev-1",
        pacienteId = "p-1",
        fecha = testDate,
        opticaId = "optica-1",
        updatedAt = updatedAt,
    )

    @Test
    fun toRemoto_doesNotCallInstantNow_whenUpdatedAtIsNull() {
        val remoto = evaluacion().toRemoto()
        assertNull(
            "EvaluacionClinica.toRemoto() must not fabricate a timestamp — " +
                "client-generated timestamps trigger false conflicts on the server",
            remoto.updatedAt,
        )
    }

    @Test
    fun toRemoto_preservesUpdatedAt_acrossMultipleCalls() {
        val entity = evaluacion()
        val first = entity.toRemoto()
        val second = entity.toRemoto()
        assertEquals(
            "Repeated toRemoto() calls on the same entity must produce identical updatedAt — " +
                "any difference is phantom drift",
            first.updatedAt,
            second.updatedAt,
        )
    }

    @Test
    fun toRemoto_preservesExistingUpdatedAt_whenNotNull() {
        val ts = "2024-06-15T10:00:00Z"
        val remoto = evaluacion(updatedAt = ts).toRemoto()
        assertEquals(ts, remoto.updatedAt)
    }

    // ── DispensacionOptica ────────────────────────────────────────────────────

    private fun dispensacion(updatedAt: String? = null) = DispensacionOptica(
        id = "d-1",
        pacienteId = "p-1",
        fecha = testDate,
        opticaId = "optica-1",
        updatedAt = updatedAt,
    )

    @Test
    fun dispensacionToRemoto_doesNotCallInstantNow_whenUpdatedAtIsNull() {
        val remoto = dispensacion().toRemoto()
        assertNull(
            "DispensacionOptica.toRemoto() must not fabricate a timestamp when updatedAt is null",
            remoto.updatedAt,
        )
    }

    @Test
    fun dispensacionToRemoto_preservesUpdatedAt_acrossMultipleCalls() {
        val entity = dispensacion()
        assertEquals(entity.toRemoto().updatedAt, entity.toRemoto().updatedAt)
    }

    @Test
    fun dispensacionToRemoto_preservesExistingUpdatedAt_whenNotNull() {
        val ts = "2024-06-15T10:00:00Z"
        assertEquals(ts, dispensacion(updatedAt = ts).toRemoto().updatedAt)
    }

    // ── Pago ──────────────────────────────────────────────────────────────────

    private fun pago(updatedAt: String? = null) = Pago(
        id = "pg-1",
        fecha = testDate,
        tipo = "Abono",
        monto = 100.0,
        opticaId = "optica-1",
        updatedAt = updatedAt,
    )

    @Test
    fun pagoToRemoto_doesNotCallInstantNow_whenUpdatedAtIsNull() {
        assertNull(
            "Pago.toRemoto() must not fabricate a timestamp when updatedAt is null",
            pago().toRemoto().updatedAt,
        )
    }

    @Test
    fun pagoToRemoto_preservesUpdatedAt_acrossMultipleCalls() {
        val entity = pago()
        assertEquals(entity.toRemoto().updatedAt, entity.toRemoto().updatedAt)
    }

    @Test
    fun pagoToRemoto_preservesExistingUpdatedAt_whenNotNull() {
        val ts = "2024-06-15T10:00:00Z"
        assertEquals(ts, pago(updatedAt = ts).toRemoto().updatedAt)
    }

    // ── ServicioExtra ─────────────────────────────────────────────────────────

    private fun servicio(updatedAt: String? = null) = ServicioExtra(
        id = "se-1",
        descripcion = "Test",
        montoTotal = 50.0,
        aCuenta = 0.0,
        estado = "Pendiente",
        fecha = testDate,
        opticaId = "optica-1",
        updatedAt = updatedAt,
    )

    @Test
    fun servicioToRemoto_doesNotCallInstantNow_whenUpdatedAtIsNull() {
        assertNull(
            "ServicioExtra.toRemoto() must not fabricate a timestamp when updatedAt is null",
            servicio().toRemoto().updatedAt,
        )
    }

    @Test
    fun servicioToRemoto_preservesUpdatedAt_acrossMultipleCalls() {
        val entity = servicio()
        assertEquals(entity.toRemoto().updatedAt, entity.toRemoto().updatedAt)
    }

    @Test
    fun servicioToRemoto_preservesExistingUpdatedAt_whenNotNull() {
        val ts = "2024-06-15T10:00:00Z"
        assertEquals(ts, servicio(updatedAt = ts).toRemoto().updatedAt)
    }
}
