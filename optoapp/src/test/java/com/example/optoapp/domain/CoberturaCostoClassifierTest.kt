package com.example.optoapp.domain

import com.example.optoapp.domain.ClaseCobertura.ESTIMADO
import com.example.optoapp.domain.ClaseCobertura.FALTANTE
import com.example.optoapp.domain.ClaseCobertura.REAL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Same vectors as COV2 in supabase/tests/test_margen_real_costos.sql. */
class CoberturaCostoClassifierTest {

    @Test
    fun item_manualMatrizMixto_areRealEvenWithZeroTotal() {
        assertEquals(REAL, CoberturaCostoClassifier.claseItem("manual", 10.0))
        assertEquals(REAL, CoberturaCostoClassifier.claseItem("matriz", 0.0))
        assertEquals(REAL, CoberturaCostoClassifier.claseItem("mixto", 5.0))
    }

    @Test
    fun item_legacyWithoutOrigin_isEstimadoOnlyWhenTotalIsPositive() {
        assertEquals(ESTIMADO, CoberturaCostoClassifier.claseItem(null, 50.0))
        assertEquals(ESTIMADO, CoberturaCostoClassifier.claseItem(null, 20.0))
        assertEquals(FALTANTE, CoberturaCostoClassifier.claseItem(null, 0.0))
        assertEquals(FALTANTE, CoberturaCostoClassifier.claseItem(null, null))
    }

    @Test
    fun item_legacyThreeItems_yieldsTwoEstimadoAndOneFaltante() {
        val classes = listOf(50.0, 20.0, 0.0).map { CoberturaCostoClassifier.claseItem(null, it) }

        assertEquals(2, classes.count { it == ESTIMADO })
        assertEquals(1, classes.count { it == FALTANTE })
    }

    @Test
    fun item_faltanteWinsOverPartialCost() {
        assertEquals(FALTANTE, CoberturaCostoClassifier.claseItem("faltante", 35.0))
    }

    @Test
    fun item_unknownOrigin_isFaltante() {
        assertEquals(FALTANTE, CoberturaCostoClassifier.claseItem("desconocido", 40.0))
    }

    @Test
    fun servicio_typedZeroCost_isReal() {
        assertEquals(REAL, CoberturaCostoClassifier.claseServicio("propio", 0.0, "tipo-1"))
    }

    @Test
    fun servicio_untypedZeroCost_isFaltante() {
        assertEquals(FALTANTE, CoberturaCostoClassifier.claseServicio("propio", 0.0, null))
    }

    @Test
    fun servicio_withoutExecutor_isFaltanteEvenWithCostOrType() {
        assertEquals(FALTANTE, CoberturaCostoClassifier.claseServicio(null, 25.0, null))
        assertEquals(FALTANTE, CoberturaCostoClassifier.claseServicio(null, 0.0, "tipo-1"))
    }

    @Test
    fun servicio_tercerosWithCost_isReal() {
        assertEquals(REAL, CoberturaCostoClassifier.claseServicio("tercero", 60.0, null))
    }

    @Test
    fun servicio_nullCost_isTreatedAsZero() {
        assertEquals(FALTANTE, CoberturaCostoClassifier.claseServicio("propio", null, null))
        assertEquals(REAL, CoberturaCostoClassifier.claseServicio("propio", null, "tipo-1"))
    }

    @Test
    fun cobertura_sinDatos_onlyWhenNoLines() {
        assertTrue(CoberturaCosto(lineasTotal = 0).sinDatos)
        assertFalse(CoberturaCosto(lineasTotal = 1, lineasReales = 1, pctReal = 100.0).sinDatos)
    }
}
