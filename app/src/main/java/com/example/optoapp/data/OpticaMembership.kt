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
    /** Asesor comercial: sin BI ni reportes financieros globales. */
    fun canViewBiAndReports(rol: String): Boolean {
        val r = rol.lowercase().trim()
        return r != "asesor" && r != "asesora" && r != "ventas"
    }

    fun canViewCierreCaja(rol: String): Boolean = canViewBiAndReports(rol)
}
