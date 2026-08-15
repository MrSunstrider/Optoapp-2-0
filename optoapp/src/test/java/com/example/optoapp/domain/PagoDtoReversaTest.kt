package com.example.optoapp.domain

import com.example.optoapp.data.Pago
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PagoDtoReversaTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun roundTrip_and_snakeCase() {
        val remoto = PagoRemoto(
            id = "rev-1", fecha = "2026-08-14", tipo = "Reverso", monto = 80.0,
            reversaPagoId = "pago-orig-1",
        )
        val entity = remoto.toEntity()
        assertEquals("pago-orig-1", entity.reversaPagoId)
        assertEquals("pago-orig-1", entity.toRemoto().reversaPagoId)
        assertTrue(json.encodeToString(PagoRemoto.serializer(), remoto).contains("\"reversa_pago_id\":\"pago-orig-1\""))
        assertNull(
            Pago(id = "p1", fecha = LocalDate.of(2026, 8, 14), tipo = "Abono", monto = 50.0).reversaPagoId,
        )
    }
}
