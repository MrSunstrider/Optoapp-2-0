package com.example.optoapp.domain

import org.junit.Assert.*
import org.junit.Test

class OpticalCatalogTest {

    @Test
    fun materiales_hasFourValues_resinaFirst() {
        assertEquals(4, OpticalCatalog.MATERIALES.size)
        assertEquals("Resina", OpticalCatalog.MATERIALES[0])
        assertTrue(OpticalCatalog.MATERIALES.contains("Policarbonato"))
        assertTrue(OpticalCatalog.MATERIALES.contains("Cristal"))
        assertTrue(OpticalCatalog.MATERIALES.contains("Trivex"))
    }

    @Test
    fun tipoLente_includesMultifocal_notProgresivo() {
        assertTrue(OpticalCatalog.TIPO_LENTE.contains("Multifocal"))
        assertFalse(OpticalCatalog.TIPO_LENTE.contains("Progresivo"))
    }

    @Test
    fun tipoLente_includesLentesDeContacto() {
        assertTrue(OpticalCatalog.TIPO_LENTE.contains("Lentes de Contacto"))
    }

    @Test
    fun tratamientos_hasThirteenValues_includesCircadian() {
        assertEquals(13, OpticalCatalog.TRATAMIENTOS.size)
        assertTrue(OpticalCatalog.TRATAMIENTOS.contains("Circadian"))
        assertTrue(OpticalCatalog.TRATAMIENTOS.contains("UV 400"))
        assertTrue(OpticalCatalog.TRATAMIENTOS.contains("Alto Índice Rose 1.7"))
    }

    @Test
    fun materialesMontura_includesAluminio() {
        assertEquals(6, OpticalCatalog.MATERIALES_MONTURA.size)
        assertTrue(OpticalCatalog.MATERIALES_MONTURA.contains("Acetato"))
        assertTrue(OpticalCatalog.MATERIALES_MONTURA.contains("Aluminio"))
        assertEquals("Aluminio", OpticalCatalog.MATERIALES_MONTURA.last())
    }

    @Test
    fun tipoAro_threeKeys_correctInternalValues() {
        assertEquals(3, OpticalCatalog.TIPO_ARO.size)
        assertEquals("aro_completo", OpticalCatalog.TIPO_ARO["Aro Completo"])
        assertEquals("semi_aire", OpticalCatalog.TIPO_ARO["Semi al aire"])
        assertEquals("al_aire", OpticalCatalog.TIPO_ARO["Al aire"])
    }

    @Test
    fun series_hasFourEntries() {
        assertEquals(4, OpticalCatalog.SERIES.size)
        assertEquals(Integer.valueOf(1), OpticalCatalog.SERIES.values.firstOrNull { it == 1 })
        assertEquals(Integer.valueOf(2), OpticalCatalog.SERIES.values.firstOrNull { it == 2 })
        assertEquals(Integer.valueOf(3), OpticalCatalog.SERIES.values.firstOrNull { it == 3 })
        val claves = OpticalCatalog.SERIES.keys
        assertTrue(claves.any { it.contains("1ra") })
        assertTrue(claves.any { it.contains("2da") })
        assertTrue(claves.any { it.contains("3ra") })
        assertTrue(claves.any { it.contains("Fabricación") || it.contains("Fabricacion") })
    }
}
