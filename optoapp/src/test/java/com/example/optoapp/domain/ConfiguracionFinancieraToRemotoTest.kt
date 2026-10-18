package com.example.optoapp.domain

import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfiguracionFinancieraToRemotoTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun toRemoto_mapsBiseladoFieldsAndRoundTrips() {
        val entity = ConfiguracionFinancieraEntity(
            opticaId = "optica-1",
            margenNetoObjetivo = 18.0,
            origenBiseladoDefault = OrigenBiselado.PROPIO,
            costoBiseladoPropio = 12.5,
        )

        val remoto = entity.toRemoto()

        assertEquals("optica-1", remoto.opticaId)
        assertEquals(18.0, remoto.margenNetoObjetivo, 0.0)
        assertEquals(OrigenBiselado.PROPIO, remoto.origenBiseladoDefault)
        assertEquals(12.5, remoto.costoBiseladoPropio, 0.0)
        assertEquals(entity.origenBiseladoDefault, remoto.toEntity().origenBiseladoDefault)
        assertEquals(entity.costoBiseladoPropio, remoto.toEntity().costoBiseladoPropio, 0.0)
    }

    @Test
    fun toRemoto_normalizesUnknownOrigenAndClampsNegativeCosto() {
        val remoto = ConfiguracionFinancieraEntity(
            opticaId = "o",
            origenBiseladoDefault = "weird",
            costoBiseladoPropio = -3.0,
        ).toRemoto()

        assertEquals(OrigenBiselado.TERCERO, remoto.origenBiseladoDefault)
        assertEquals(0.0, remoto.costoBiseladoPropio, 0.0)
        val encoded = json.encodeToString(ConfiguracionFinancieraRemoto.serializer(), remoto)
        assertEquals(true, encoded.contains("\"origen_biselado_default\":\"Tercero\""))
        assertEquals(true, encoded.contains("\"costo_biselado_propio\":0.0"))
    }
}
