package com.example.optoapp.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** S9/S10: Análisis home must not grow into Reportes commissions/payroll. */
class AnalisisScopeGuardTest {

    @Test
    fun analisisScreen_hasNoCommissionOrPayrollCopy() {
        val source = MainSources.read(MainSources.ANALISIS_NEGOCIO_SCREEN).lowercase()
        listOf("comision", "comisión", "liquidacion", "liquidación", "bono", "payroll").forEach { banned ->
            assertFalse("AnalisisNegocioScreen must not mention '$banned'", source.contains(banned))
        }
        assertTrue(source.contains("canviewbiandreports"))
    }
}
