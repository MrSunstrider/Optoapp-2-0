package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfiguracionFinancieraPolicyTest {

    @Test
    fun canWrite_adminAndGerente_allowed() {
        assertTrue(ConfiguracionFinancieraPolicy.canWrite("admin"))
        assertTrue(ConfiguracionFinancieraPolicy.canWrite("gerente"))
        assertTrue(ConfiguracionFinancieraPolicy.canWrite("Admin"))
    }

    @Test
    fun canWrite_especialistaOrNull_denied() {
        assertFalse(ConfiguracionFinancieraPolicy.canWrite("especialista"))
        assertFalse(ConfiguracionFinancieraPolicy.canWrite(null))
        assertFalse(ConfiguracionFinancieraPolicy.canWrite("  "))
    }

    @Test
    fun validate_rejectsNegativePercentages() {
        val draft = ConfiguracionFinancieraDraft(margenNetoObjetivo = -1.0)
        assertEquals("Margen neto objetivo no puede ser negativo", ConfiguracionFinancieraPolicy.validate(draft))
    }

    @Test
    fun validate_acceptsValidDraft() {
        val draft = ConfiguracionFinancieraDraft(
            margenNetoObjetivo = 18.0,
            caidaVentasAlertaPct = 10.0,
            deudaViejaAlertaDias = 30,
            deudaTotalAlertaMonto = 3000.0,
            stockEstancadoAlertaDias = 180,
            stockBajoAlertaUnidades = 2,
            minVentasParaRecomendar = 5,
            frecuenciaRecalculoDias = 1,
        )
        assertNull(ConfiguracionFinancieraPolicy.validate(draft))
    }
}
