package com.example.optoapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** S9: Análisis / BI stays admin|gerente; no seller/commission expansion here. */
class AppRolesAnalisisGateTest {

    @Test
    fun canViewBiAndReports_allowsAdminAndGerente() {
        assertTrue(AppRoles.canViewBiAndReports("admin"))
        assertTrue(AppRoles.canViewBiAndReports("Gerente"))
    }

    @Test
    fun canViewBiAndReports_deniesEspecialistaAndVentas() {
        assertFalse(AppRoles.canViewBiAndReports("especialista"))
        assertFalse(AppRoles.canViewBiAndReports("ventas"))
        assertFalse(AppRoles.canViewBiAndReports("asesor"))
    }
}
