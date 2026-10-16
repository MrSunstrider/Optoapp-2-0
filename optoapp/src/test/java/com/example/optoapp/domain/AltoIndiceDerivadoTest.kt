package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AltoIndiceDerivadoTest {

    @Test
    fun highIndexTreatment_yieldsItsNormalizedIndex() {
        assertEquals("1.70", AltoIndiceNormalizer.fromTratamientos(listOf("Antirayas", "Alto Índice Blanco 1.7")))
        assertEquals("1.80", AltoIndiceNormalizer.fromTratamientos(listOf("Alto Índice Blanco 1.8")))
        assertEquals("1.70", AltoIndiceNormalizer.fromTratamientos(listOf("Alto Índice Rose 1.7")))
    }

    @Test
    fun catalogTreatments_allParse() {
        val altoIndice = OpticalCatalog.TRATAMIENTOS.filter { it.startsWith("Alto") }

        assertEquals(listOf("1.70", "1.70", "1.80"), altoIndice.map { AltoIndiceNormalizer.fromTratamientos(listOf(it)) })
    }

    @Test
    fun noHighIndexTreatment_isNotDeterminable() {
        assertNull(AltoIndiceNormalizer.fromTratamientos(listOf("Antirayas", "Filtro azul")))
        assertNull(AltoIndiceNormalizer.fromTratamientos(emptyList()))
    }

    @Test
    fun accentlessAndLowercaseVariants_stillParse() {
        assertEquals("1.70", AltoIndiceNormalizer.fromTratamientos(listOf("alto indice blanco 1.7")))
        assertEquals("1.67", AltoIndiceNormalizer.fromTratamientos(listOf("ALTO INDICE 1,67")))
    }

    @Test
    fun twoHighIndexTreatments_areAmbiguousAndNeverGuessed() {
        assertNull(AltoIndiceNormalizer.fromTratamientos(listOf("Alto Índice Blanco 1.7", "Alto Índice Blanco 1.8")))
    }

    @Test
    fun highIndexTreatmentWithoutValue_isNotDeterminable() {
        assertNull(AltoIndiceNormalizer.fromTratamientos(listOf("Alto Índice")))
    }
}
