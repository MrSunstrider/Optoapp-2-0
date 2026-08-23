package com.example.optoapp.domain

import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfiguracionFinancieraMapperTest {

    @Test
    fun toRemoto_roundTripsAllFields() {
        val entity = ConfiguracionFinancieraEntity(
            opticaId = "opt-1",
            margenNetoObjetivo = 18.0,
            ticketPromedioObjetivo = 250.0,
            caidaVentasAlertaPct = 12.0,
            deudaViejaAlertaDias = 45,
            deudaTotalAlertaMonto = 5000.0,
            stockEstancadoAlertaDias = 90,
            stockBajoAlertaUnidades = 3,
            minVentasParaRecomendar = 8,
            frecuenciaRecalculoDias = 2,
        )

        val remoto = entity.toRemoto()
        val back = remoto.toEntity()

        assertEquals(entity, back)
        assertEquals("opt-1", remoto.opticaId)
        assertEquals(18.0, remoto.margenNetoObjetivo, 0.001)
        assertEquals(250.0, remoto.ticketPromedioObjetivo!!, 0.001)
    }

    @Test
    fun toRemoto_nullTicket_roundTrips() {
        val entity = ConfiguracionFinancieraEntity(opticaId = "opt-2", ticketPromedioObjetivo = null)
        assertNull(entity.toRemoto().toEntity().ticketPromedioObjetivo)
    }
}
