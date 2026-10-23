package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultadoDriversTest {

    @Test
    fun volumePlusTicket_reconstructsDeltaVentas_whenPriorQtyPositive() {
        // Q0=4 T0=2000 V0=8000; Q1=5 T1=2200 V1=11000; ΔV=3000
        val mom = ResultadoDrivers.compute(
            cantidadVentas = 5,
            ticketPromedio = 2_200.0,
            costoMes = 4_000.0,
            cantidadVentasMesAnterior = 4,
            ticketPromedioMesAnterior = 2_000.0,
            costoMesAnterior = 3_000.0,
        )

        assertFalse(mom.sinBaseMom)
        val volumen = mom.drivers.first { it.tipo == DriverTipo.VOLUMEN }.soles
        val ticket = mom.drivers.first { it.tipo == DriverTipo.TICKET }.soles
        assertEquals(2_000.0, volumen, 0.001) // (5-4)*2000
        assertEquals(1_000.0, ticket, 0.001) // 5*(2200-2000)
        assertEquals(3_000.0, volumen + ticket, 0.001)
        assertEquals(-1_000.0, mom.drivers.first { it.tipo == DriverTipo.COSTO }.soles, 0.001)
    }

    @Test
    fun priorQtyZero_marksSinBase_andOmitsVolumeTicket() {
        val mom = ResultadoDrivers.compute(
            cantidadVentas = 2,
            ticketPromedio = 2_500.0,
            costoMes = 1_000.0,
            cantidadVentasMesAnterior = 0,
            ticketPromedioMesAnterior = 0.0,
            costoMesAnterior = 0.0,
        )

        assertTrue(mom.sinBaseMom)
        assertEquals(1, mom.drivers.size)
        assertEquals(DriverTipo.COSTO, mom.drivers.single().tipo)
        assertEquals(-1_000.0, mom.drivers.single().soles, 0.001)
    }

    @Test
    fun top_ordersByAbsoluteSoles() {
        val mom = ResultadoDrivers.compute(
            cantidadVentas = 10,
            ticketPromedio = 1_200.0,
            costoMes = 2_000.0,
            cantidadVentasMesAnterior = 8,
            ticketPromedioMesAnterior = 1_000.0,
            costoMesAnterior = 5_000.0,
        )
        // volumen=(10-8)*1000=2000; ticket=10*200=2000; costo=-(2000-5000)=+3000
        val top = mom.top(3)
        assertEquals(DriverTipo.COSTO, top[0].tipo)
        assertEquals(3_000.0, top[0].soles, 0.001)
    }
}
