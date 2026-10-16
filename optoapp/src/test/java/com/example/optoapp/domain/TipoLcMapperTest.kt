package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TipoLcMapperTest {

    @Test
    fun enumValuesMapToTheServerDomain() {
        assertEquals("cosmetico", TipoLcMapper.map("lente_contacto_cosmetico"))
        assertEquals("graduado", TipoLcMapper.map("lente_contacto_medida"))
    }

    @Test
    fun cosmeticTextMapsToCosmeticoWithOrWithoutAccents() {
        listOf("Cosmético", "COSMÉTICO", "COSMETICO", "cosmetic lens", "Cosmética").forEach {
            assertEquals(it, "cosmetico", TipoLcMapper.map(it))
        }
    }

    @Test
    fun medidaAndGraduatedTextMapToGraduado() {
        listOf("MEDIDA", "medida", "Graduado", "graduadas", " Graduado ").forEach {
            assertEquals(it, "graduado", TipoLcMapper.map(it))
        }
    }

    @Test
    fun therapeuticTextMapsToTerapeuticoWithOrWithoutAccents() {
        listOf("Terapéutico", "terapéutico", "Terapeutico", "TERAP").forEach {
            assertEquals(it, "terapeutico", TipoLcMapper.map(it))
        }
    }

    @Test
    fun unknownBlankOrNullIsNeverGuessed() {
        assertNull(TipoLcMapper.map("desconocido"))
        assertNull(TipoLcMapper.map("Lentes de Contacto"))
        assertNull(TipoLcMapper.map("  "))
        assertNull(TipoLcMapper.map(null))
    }
}
