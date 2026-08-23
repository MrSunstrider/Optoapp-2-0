package com.example.optoapp.viewmodel

import com.example.optoapp.data.AppRoles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CierreCajaExportPolicyTest {

    @Test
    fun export_hidden_whenRolNull() {
        val access = CierreCajaUiPolicy.resolveAccess(null)
        assertFalse(access.canExport)
        assertTrue(access.isRestricted)
    }

    @Test
    fun export_hidden_whenUnauthorizedRole() {
        assertFalse(AppRoles.canExportCierreCaja("asesor"))
        val access = CierreCajaUiPolicy.resolveAccess("asesor")
        assertFalse(access.canExport)
        assertTrue(access.isRestricted)
    }

    @Test
    fun export_visible_forAdminGerenteEspecialista() {
        listOf("admin", "gerente", "especialista").forEach { rol ->
            assertTrue("expected export for $rol", AppRoles.canExportCierreCaja(rol))
            val access = CierreCajaUiPolicy.resolveAccess(rol)
            assertTrue("expected canExport for $rol", access.canExport)
            assertFalse(access.isRestricted)
        }
    }
}
