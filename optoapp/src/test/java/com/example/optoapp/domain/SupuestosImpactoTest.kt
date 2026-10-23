package com.example.optoapp.domain

import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SupuestosImpactoTest {

    @Test
    fun defaults_matchLegacyImpactoConstants() {
        val s = SupuestosImpacto()
        assertEquals(25.0, s.margenObjetivoPct, 0.0)
        assertEquals(0.70, s.recuperoStockPct, 0.0)
        assertEquals(0.10, s.upsideVentasPct, 0.0)
        assertEquals(0.10, s.corteGastoPct, 0.0)
        assertEquals(1.0, s.recuperoCobranzaPct, 0.0)
    }

    @Test
    fun fromConfig_readsPersistedAssumptions() {
        val config = ConfiguracionFinancieraEntity(
            opticaId = "o1",
            impactoMargenObjetivoPct = 30.0,
            impactoRecuperoStockPct = 0.5,
            impactoUpsideVentasPct = 0.15,
            impactoCorteGastoPct = 0.2,
            impactoRecuperoCobranzaPct = 0.8,
        )
        val s = SupuestosImpacto.from(config)
        assertEquals(30.0, s.margenObjetivoPct, 0.0)
        assertEquals(0.5, s.recuperoStockPct, 0.0)
        assertEquals(0.15, s.upsideVentasPct, 0.0)
        assertEquals(0.2, s.corteGastoPct, 0.0)
        assertEquals(0.8, s.recuperoCobranzaPct, 0.0)
    }
}
