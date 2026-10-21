package com.example.optoapp.ui.navigation

import com.example.optoapp.ui.screens.rutaVerTodosGastos
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class CostosYGastosRouteTest {

    @Test
    fun `the single costs route registers optional tab and month arguments`() {
        assertEquals("costos_y_gastos?tab={tab}&mes={mes}", Route.CostosYGastos.route)
    }

    @Test
    fun `the default entry opens the gastos tab without a month`() {
        assertEquals("costos_y_gastos?tab=gastos", Route.CostosYGastos.createRoute())
    }

    @Test
    fun `an entry with a month carries the tab and the month`() {
        assertEquals(
            "costos_y_gastos?tab=lc&mes=2026-05",
            Route.CostosYGastos.createRoute(CostosTab.LC, YearMonth.of(2026, 5)),
        )
    }

    @Test
    fun `tab order matches the tab row of the screen`() {
        assertEquals(listOf("gastos", "lentes", "biselado", "lc", "servicios"), CostosTab.entries.map { it.arg })
    }

    @Test
    fun `known tab arguments resolve to their tab`() {
        assertEquals(CostosTab.BISELADO, CostosTab.fromArg("biselado"))
        assertEquals(CostosTab.SERVICIOS, CostosTab.fromArg("servicios"))
        CostosTab.entries.forEach { assertEquals(it, CostosTab.fromArg(it.arg)) }
    }

    @Test
    fun `unknown or missing tab arguments fall back to gastos`() {
        assertEquals(CostosTab.GASTOS, CostosTab.fromArg("xyz"))
        assertEquals(CostosTab.GASTOS, CostosTab.fromArg(""))
        assertEquals(CostosTab.GASTOS, CostosTab.fromArg(null))
    }

    @Test
    fun `Ver todos of the gastos card opens the gastos tab of the selected month`() {
        val ruta = rutaVerTodosGastos(YearMonth.of(2026, 5))

        assertEquals("costos_y_gastos?tab=gastos&mes=2026-05", ruta)
        assertEquals(CostosTab.GASTOS, CostosTab.fromArg(ruta.substringAfter("tab=").substringBefore("&")))
    }
}
