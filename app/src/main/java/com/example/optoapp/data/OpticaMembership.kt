package com.example.optoapp.data

/**
 * Membresía usuario ↔ óptica (tabla `usuario_optica` en Supabase).
 */
data class OpticaMembership(
    val opticaId: String,
    val nombre: String,
    val rol: String
)

object AppRoles {
    private fun norm(rol: String): String = rol.lowercase().trim()
    private val ROLES_CON_BI = setOf("admin", "especialista", "gerente")
    private val ROLES_COMERCIALES = setOf("asesor", "asesora", "ventas")
    private val ROLES_SIN_EXPORTACION = setOf("invitado")

    /** Asesor comercial: sin BI ni reportes financieros globales. */
    fun canViewBiAndReports(rol: String): Boolean {
        val r = norm(rol)
        if (r in ROLES_CON_BI) return true
        if (r in ROLES_COMERCIALES) return false
        if (r in ROLES_SIN_EXPORTACION) return false
        // Rol no catalogado: comportamiento conservador para visibilidad financiera.
        return false
    }

    fun canViewCierreCaja(rol: String): Boolean = canViewBiAndReports(rol)

    /** Operación diaria requiere visibilidad de gestión. */
    fun canViewOperacionHoy(rol: String): Boolean = canViewBiAndReports(rol)

    /** Exportaciones rápidas para supervisión o cierre. */
    fun canExportOperationalReports(rol: String): Boolean = canViewBiAndReports(rol)

    /** Permisos más finos por tipo de exportación. */
    fun canExportPendientes(rol: String): Boolean {
        val r = norm(rol)
        return r in ROLES_CON_BI || r in ROLES_COMERCIALES
    }

    fun canExportCierreCaja(rol: String): Boolean = canViewCierreCaja(rol)

    fun canExportInventario(rol: String): Boolean {
        val r = norm(rol)
        return r in ROLES_CON_BI || r in ROLES_COMERCIALES
    }

    fun canManageUsers(rol: String): Boolean {
        val r = norm(rol)
        return r == "admin" || r == "gerente"
    }
}
