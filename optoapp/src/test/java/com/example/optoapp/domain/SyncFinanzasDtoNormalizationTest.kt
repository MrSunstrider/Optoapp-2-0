package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Tests that PagoRemoto and ServicioRemoto normalize "Sin especificar" correctly
 * via the shared [remotoServicioExtraMetodoToLocal] extension function.
 */
class SyncFinanzasDtoNormalizationTest {

    @Test
    fun pagoRemoto_withSinEspecificar_normalizesToEmptyString() {
        val remoto = PagoRemoto(
            id = "p1",
            fecha = "2026-06-17",
            metodoPago = "Sin especificar",
            opticaId = "optica-1"
        )
        val entity = remoto.toEntity()
        assertEquals("", entity.metodoPago)
    }

    @Test
    fun pagoRemoto_withKnownMethod_keepsValue() {
        val remoto = PagoRemoto(
            id = "p2",
            fecha = "2026-06-17",
            metodoPago = "Efectivo",
            opticaId = "optica-1"
        )
        val entity = remoto.toEntity()
        assertEquals("Efectivo", entity.metodoPago)
    }

    @Test
    fun pagoRemoto_withEmptyString_keepsEmpty() {
        val remoto = PagoRemoto(
            id = "p3",
            fecha = "2026-06-17",
            metodoPago = "",
            opticaId = "optica-1"
        )
        val entity = remoto.toEntity()
        assertEquals("", entity.metodoPago)
    }

    @Test
    fun servicioRemoto_withSinEspecificar_normalizesToEmptyString() {
        val remoto = ServicioRemoto(
            id = "s1",
            fecha = "2026-06-17",
            metodoPago = "Sin especificar",
            opticaId = "optica-1"
        )
        val entity = remoto.toEntity()
        assertEquals("", entity.metodoPago)
    }

    @Test
    fun servicioRemoto_withKnownMethod_keepsValue() {
        val remoto = ServicioRemoto(
            id = "s2",
            fecha = "2026-06-17",
            metodoPago = "Tarjeta",
            opticaId = "optica-1"
        )
        val entity = remoto.toEntity()
        assertEquals("Tarjeta", entity.metodoPago)
    }

    @Test
    fun pagoAndServicio_normalizeIdentically() {
        val pagoRemoto = PagoRemoto(
            id = "p",
            fecha = "2026-06-17",
            metodoPago = "Sin especificar",
            opticaId = "optica-1"
        )
        val servRemoto = ServicioRemoto(
            id = "s",
            fecha = "2026-06-17",
            metodoPago = "Sin especificar",
            opticaId = "optica-1"
        )
        assertEquals(
            "Both should normalize to the same value",
            pagoRemoto.toEntity().metodoPago,
            servRemoto.toEntity().metodoPago
        )
    }

    @Test
    fun remotoServicioExtraMetodoToLocal_extension_mapsSinEspecificar() {
        val result = "Sin especificar".remotoServicioExtraMetodoToLocal()
        assertEquals("", result)
    }

    @Test
    fun remotoServicioExtraMetodoToLocal_extension_passesThroughOtherValues() {
        assertEquals("Efectivo", "Efectivo".remotoServicioExtraMetodoToLocal())
        assertEquals("Tarjeta", "Tarjeta".remotoServicioExtraMetodoToLocal())
        assertEquals("", "".remotoServicioExtraMetodoToLocal())
        assertEquals("Transferencia", "Transferencia".remotoServicioExtraMetodoToLocal())
    }
}
