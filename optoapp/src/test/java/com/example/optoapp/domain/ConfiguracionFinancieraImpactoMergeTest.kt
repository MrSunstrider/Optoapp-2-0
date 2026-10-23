package com.example.optoapp.domain

import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfiguracionFinancieraImpactoMergeTest {

    @Test
    fun toEntityPreservingLocalImpacto_keepsLocalColumns() {
        val local = ConfiguracionFinancieraEntity(
            opticaId = "optica1",
            margenNetoObjetivo = 12.0,
            impactoMargenObjetivoPct = 30.0,
            impactoRecuperoStockPct = 0.55,
            impactoUpsideVentasPct = 0.2,
            impactoCorteGastoPct = 0.15,
            impactoRecuperoCobranzaPct = 0.8,
            margenBajoAlertaPct = 12.0,
            margenAltoAlertaPct = 40.0,
            contribucionAltaRatio = 0.3,
            gastoFueraRangoRatio = 0.5,
        )
        val remoto = ConfiguracionFinancieraRemoto(
            opticaId = "optica1",
            margenNetoObjetivo = 18.0,
        )

        val merged = remoto.toEntityPreservingLocalImpacto(local)

        assertEquals(18.0, merged.margenNetoObjetivo, 0.0)
        assertEquals(30.0, merged.impactoMargenObjetivoPct, 0.0)
        assertEquals(0.55, merged.impactoRecuperoStockPct, 0.0)
        assertEquals(0.2, merged.impactoUpsideVentasPct, 0.0)
        assertEquals(0.15, merged.impactoCorteGastoPct, 0.0)
        assertEquals(0.8, merged.impactoRecuperoCobranzaPct, 0.0)
        assertEquals(12.0, merged.margenBajoAlertaPct, 0.0)
        assertEquals(40.0, merged.margenAltoAlertaPct, 0.0)
        assertEquals(0.3, merged.contribucionAltaRatio, 0.0)
        assertEquals(0.5, merged.gastoFueraRangoRatio, 0.0)
    }

    @Test
    fun toEntityPreservingLocalImpacto_defaultsWhenNoLocal() {
        val remoto = ConfiguracionFinancieraRemoto(opticaId = "optica1")
        val merged = remoto.toEntityPreservingLocalImpacto(null)

        assertEquals(25.0, merged.impactoMargenObjetivoPct, 0.0)
        assertEquals(0.70, merged.impactoRecuperoStockPct, 0.0)
        assertEquals(0.10, merged.impactoUpsideVentasPct, 0.0)
        assertEquals(0.10, merged.impactoCorteGastoPct, 0.0)
        assertEquals(1.0, merged.impactoRecuperoCobranzaPct, 0.0)
        assertEquals(10.0, merged.margenBajoAlertaPct, 0.0)
        assertEquals(35.0, merged.margenAltoAlertaPct, 0.0)
        assertEquals(0.25, merged.contribucionAltaRatio, 0.0)
        assertEquals(0.4, merged.gastoFueraRangoRatio, 0.0)
    }
}
