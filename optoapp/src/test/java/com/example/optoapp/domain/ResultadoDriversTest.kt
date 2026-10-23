package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultadoDriversTest {

    @Test
    fun volumePlusTicket_reconstructsDeltaVentas_whenPriorQtyPositive() {
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
        assertEquals(2_000.0, volumen, 0.001)
        assertEquals(1_000.0, ticket, 0.001)
        assertEquals(3_000.0, volumen + ticket, 0.001)
        assertEquals(-1_000.0, mom.drivers.first { it.tipo == DriverTipo.COSTO }.soles, 0.001)
    }

    @Test
    fun priorQtyZero_marksSinBase_andOmitsAllDrivers() {
        val mom = ResultadoDrivers.compute(
            cantidadVentas = 2,
            ticketPromedio = 2_500.0,
            costoMes = 1_000.0,
            cantidadVentasMesAnterior = 0,
            ticketPromedioMesAnterior = 0.0,
            costoMesAnterior = 0.0,
        )

        assertTrue(mom.sinBaseMom)
        assertTrue(mom.drivers.isEmpty())
    }

    @Test
    fun mixEffect_shareShiftTimesPriorGmTimesCurrentVentas() {
        // Prior: A 80 (gm 50%), B 20 (gm 25%); V0=100
        // Current: A 40, B 60; V1=100 → shareA -0.4, shareB +0.4
        // mix = (-0.4)(0.5)(100) + (0.4)(0.25)(100) = -20 + 10 = -10
        val mix = ResultadoDrivers.mixEffect(
            ventasMes = 100.0,
            ventasMesAnterior = 100.0,
            categorias = listOf(
                MargenCategoria("A", 40.0, 20.0, 50.0),
                MargenCategoria("B", 60.0, 45.0, 25.0),
            ),
            categoriasAnterior = listOf(
                MargenCategoria("A", 80.0, 40.0, 50.0),
                MargenCategoria("B", 20.0, 15.0, 25.0),
            ),
        )
        assertEquals(-10.0, mix!!, 0.001)
    }

    @Test
    fun mixEffect_nullWhenNoPriorVentas() {
        assertNull(
            ResultadoDrivers.mixEffect(
                ventasMes = 100.0,
                ventasMesAnterior = 0.0,
                categorias = listOf(MargenCategoria("A", 100.0, 50.0, 50.0)),
                categoriasAnterior = emptyList(),
            ),
        )
    }

    @Test
    fun top_includesMixWhenPresent() {
        val mom = ResultadoDrivers.compute(
            cantidadVentas = 10,
            ticketPromedio = 1_200.0,
            costoMes = 2_000.0,
            cantidadVentasMesAnterior = 8,
            ticketPromedioMesAnterior = 1_000.0,
            costoMesAnterior = 5_000.0,
            ventasMes = 12_000.0,
            ventasMesAnterior = 8_000.0,
            categorias = listOf(MargenCategoria("A", 12_000.0, 4_000.0, 66.0)),
            categoriasAnterior = listOf(MargenCategoria("A", 8_000.0, 4_000.0, 50.0)),
        )
        assertTrue(mom.drivers.any { it.tipo == DriverTipo.MIX })
        assertEquals(DriverTipo.COSTO, mom.top(4)[0].tipo)
    }
}
