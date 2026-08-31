package com.example.optoapp.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerNavigationStructureTest {

    @Test
    fun quickAccess_alwaysIncludesPacientesAndServicios() {
        val entries = buildQuickAccessEntries(showCierreCaja = false)
        assertEquals(2, entries.size)
        assertEquals("pacientes", entries[0].route)
        assertEquals("servicios_extra", entries[1].route)
    }

    @Test
    fun quickAccess_includesCierreCajaWhenPermitted() {
        val entries = buildQuickAccessEntries(showCierreCaja = true)
        assertEquals(3, entries.size)
        assertTrue(entries.any { it.route == "cierre_caja" && it.label == "Cierre de Caja" })
    }

    @Test
    fun drawerSections_withoutBiOrCierre_omitsFinanzas() {
        val sections = buildDrawerSections(
            showOperacionHoy = true,
            showBiYReportes = false,
            showConfiguracion = true,
            showCierreCaja = false,
            conflictCount = 0,
        )
        assertFalse(sections.any { it.title == "FINANZAS" })
        assertEquals(listOf("OPERACIÓN", "INVENTARIO", "SISTEMA"), sections.map { it.title })
    }

    @Test
    fun drawerSections_withCierreOnly_includesFinanzasWithCierre() {
        val sections = buildDrawerSections(
            showOperacionHoy = false,
            showBiYReportes = false,
            showConfiguracion = false,
            showCierreCaja = true,
            conflictCount = 0,
        )
        val finanzas = sections.single { it.title == "FINANZAS" }
        assertEquals(listOf("Cierre de Caja"), finanzas.entries.map { it.label })
    }

    @Test
    fun drawerSections_withBi_includesFinanzasReportes() {
        val sections = buildDrawerSections(
            showOperacionHoy = true,
            showBiYReportes = true,
            showConfiguracion = false,
            showCierreCaja = false,
            conflictCount = 0,
        )
        val finanzas = sections.single { it.title == "FINANZAS" }
        assertEquals(
            listOf("Análisis Financiero", "Reportes", "Costos y Gastos"),
            finanzas.entries.map { it.label },
        )
    }

    @Test
    fun drawerSections_operacionHoyControlsDashboard() {
        val withDashboard = buildDrawerSections(
            showOperacionHoy = true,
            showBiYReportes = false,
            showConfiguracion = false,
            showCierreCaja = false,
            conflictCount = 0,
        ).single { it.title == "OPERACIÓN" }
        assertTrue(withDashboard.entries.any { it.label == "Dashboard" })

        val withoutDashboard = buildDrawerSections(
            showOperacionHoy = false,
            showBiYReportes = false,
            showConfiguracion = false,
            showCierreCaja = false,
            conflictCount = 0,
        ).single { it.title == "OPERACIÓN" }
        assertFalse(withoutDashboard.entries.any { it.label == "Dashboard" })
        assertTrue(withoutDashboard.entries.any { it.label == "Agenda" })
    }

    @Test
    fun drawerSections_withCierreAndBi_ordersCierreFirst() {
        val sections = buildDrawerSections(
            showOperacionHoy = false,
            showBiYReportes = true,
            showConfiguracion = false,
            showCierreCaja = true,
            conflictCount = 0,
        )
        val finanzas = sections.single { it.title == "FINANZAS" }
        assertEquals(
            listOf("Cierre de Caja", "Análisis Financiero", "Reportes", "Costos y Gastos"),
            finanzas.entries.map { it.label },
        )
    }

    @Test
    fun drawerSections_conflictBadgeOnlyWhenCountPositive() {
        val withConflict = buildDrawerSections(
            showOperacionHoy = false,
            showBiYReportes = false,
            showConfiguracion = true,
            showCierreCaja = false,
            conflictCount = 3,
        ).single { it.title == "SISTEMA" }
        val conflict = withConflict.entries.single { it.label == "Conflictos" }
        assertEquals(3, conflict.badgeCount)

        val withoutConflict = buildDrawerSections(
            showOperacionHoy = false,
            showBiYReportes = false,
            showConfiguracion = true,
            showCierreCaja = false,
            conflictCount = 0,
        ).single { it.title == "SISTEMA" }
        assertFalse(withoutConflict.entries.any { it.label == "Conflictos" })
    }
}
