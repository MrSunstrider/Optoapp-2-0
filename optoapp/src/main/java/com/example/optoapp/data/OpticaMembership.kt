package com.example.optoapp.data

data class OpticaMembership(
    val opticaId: String,
    val nombre: String,
    val rol: String,
)

object AppRoles {
    private fun norm(rol: String): String = rol.lowercase().trim()

    private val ROLES_GESTION = setOf("admin", "gerente")
    private val ROLES_CON_BI = setOf("admin", "gerente")
    private val ROLES_CIERRE_CAJA = setOf("admin", "gerente", "especialista")
    private val ROLES_OPERACION_HOY = setOf("admin", "gerente", "especialista", "asesor", "asesora", "ventas")
    private val ROLES_COMERCIALES = setOf("asesor", "asesora", "ventas")
    private val ROLES_ESCRITURA_CLINICA = setOf("admin", "gerente", "especialista", "asesor", "asesora", "ventas")
    private val ROLES_ESCRITURA_EVALUACIONES = setOf("admin", "gerente", "especialista")
    private val ROLES_BORRADO = setOf("admin", "gerente")
    private val ROLES_EXPORTAR = setOf("admin", "gerente", "especialista", "asesor", "asesora", "ventas")
    private val SOLO_ADMIN = setOf("admin")

    fun canViewBiAndReports(rol: String): Boolean = norm(rol) in ROLES_CON_BI

    fun canViewCierreCaja(rol: String): Boolean = norm(rol) in ROLES_CIERRE_CAJA

    fun canViewOperacionHoy(rol: String): Boolean = norm(rol) in ROLES_OPERACION_HOY

    fun canCreateEditPacientes(rol: String): Boolean = norm(rol) in ROLES_ESCRITURA_CLINICA

    fun canCreateEditEvaluaciones(rol: String): Boolean = norm(rol) in ROLES_ESCRITURA_EVALUACIONES

    fun canCreateEditDispensaciones(rol: String): Boolean = norm(rol) in ROLES_ESCRITURA_CLINICA

    fun canEditInventory(rol: String): Boolean = norm(rol) in ROLES_ESCRITURA_CLINICA

    fun canExportOperationalReports(rol: String): Boolean = canViewBiAndReports(rol)

    fun canExportPendientes(rol: String): Boolean = norm(rol) in ROLES_EXPORTAR

    fun canExportCierreCaja(rol: String): Boolean = canViewCierreCaja(rol)

    fun canExportInventario(rol: String): Boolean = norm(rol) in ROLES_EXPORTAR

    fun canDeleteRecords(rol: String): Boolean = norm(rol) in ROLES_BORRADO

    fun canManageUsers(rol: String): Boolean = norm(rol) in ROLES_GESTION

    fun canManagePlans(rol: String): Boolean = norm(rol) in ROLES_GESTION

    fun canManageBackups(rol: String): Boolean = norm(rol) in SOLO_ADMIN

    fun canAssignAdminRole(rol: String): Boolean = norm(rol) in SOLO_ADMIN
}
