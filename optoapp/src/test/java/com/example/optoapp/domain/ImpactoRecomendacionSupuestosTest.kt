package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImpactoRecomendacionSupuestosTest {

    @Test
    fun mejorarPrecio_usesEditableTarget() {
        val supuestos = SupuestosImpacto(margenObjetivoPct = 20.0)
        val impacto = ImpactoRecomendacion.mejorarPrecio(1_000.0, 10.0, supuestos)
        assertEquals(100.0, impacto!!.monto!!, 0.001)
    }

    @Test
    fun liquidarStock_usesEditableRecovery() {
        val supuestos = SupuestosImpacto(recuperoStockPct = 0.5)
        val impacto = ImpactoRecomendacion.liquidarStock(1_000.0, supuestos)
        assertEquals(500.0, impacto.monto!!, 0.001)
    }

    @Test
    fun venderMasDe_usesEditableUpside() {
        val supuestos = SupuestosImpacto(upsideVentasPct = 0.20)
        val impacto = ImpactoRecomendacion.venderMasDe(2_000.0, 40.0, supuestos)
        assertEquals(160.0, impacto!!.monto!!, 0.001)
    }

    @Test
    fun reducirGasto_usesEditableCut() {
        val supuestos = SupuestosImpacto(corteGastoPct = 0.05)
        val impacto = ImpactoRecomendacion.reducirGasto(2_000.0, supuestos)
        assertEquals(100.0, impacto.monto!!, 0.001)
    }

    @Test
    fun cobrar_usesEditableRecovery() {
        val supuestos = SupuestosImpacto(recuperoCobranzaPct = 0.5)
        val impacto = ImpactoRecomendacion.cobrar(1_000.0, supuestos)
        assertEquals(500.0, impacto.monto!!, 0.001)
    }

    @Test
    fun mejorarPrecio_nullWhenAtTarget() {
        assertNull(ImpactoRecomendacion.mejorarPrecio(1_000.0, 25.0, SupuestosImpacto()))
    }
}
