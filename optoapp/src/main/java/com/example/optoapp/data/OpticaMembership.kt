package com.example.optoapp.data

/**
 * Membresía usuario ↔ óptica (tabla `usuario_optica` en Supabase).
 */
data class OpticaMembership(
    val opticaId: String,
    val nombre: String,
    val rol: String
)

/**
 * Matriz de permisos por rol (define qué ve y hace cada perfil en la app).
 *
 * | Acción                      | admin | gerente | especialista | asesor/ventas | invitado |
 * |-----------------------------|:-----:|:-------:|:------------:|:-------------:|:--------:|
 * | Ver pacientes               |   ✅  |    ✅   |      ✅      |      ✅       |    ✅    |
 * | Crear/editar pacientes      |   ✅  |    ✅   |      ✅      |      ✅       |    ❌    |
 * | Ver evaluaciones            |   ✅  |    ✅   |      ✅      |      ✅       |    ✅    |
 * | Crear/editar evaluaciones   |   ✅  |    ✅   |      ✅      |      ❌       |    ❌    |
 * | Ver dispensaciones          |   ✅  |    ✅   |      ✅      |      ✅       |    ✅    |
 * | Crear/editar dispensaciones |   ✅  |    ✅   |      ✅      |      ✅       |    ❌    |
 * | Ver inventario              |   ✅  |    ✅   |      ✅      |      ✅       |    ✅    |
 * | Editar inventario           |   ✅  |    ✅   |      ✅      |      ✅       |    ❌    |
 * | Dashboard (Operación Hoy)   |   ✅  |    ✅   |      ✅      |      ✅       |    ❌    |
 * | BI / Reportes financieros   |   ✅  |    ✅   |      ❌      |      ❌       |    ❌    |
 * | Cierre de caja              |   ✅  |    ✅   |      ✅      |      ❌       |    ❌    |
 * | Gestionar usuarios          |   ✅  |    ✅   |      ❌      |      ❌       |    ❌    |
 * | Gestionar planes            |   ✅  |    ✅   |      ❌      |      ❌       |    ❌    |
 * | Backup / Restore            |   ✅  |    ❌   |      ❌      |      ❌       |    ❌    |
 * | Borrar registros            |   ✅  |    ✅   |      ❌      |      ❌       |    ❌    |
 * | Asignar rol admin           |   ✅  |    ❌   |      ❌      |      ❌       |    ❌    |
 */
object AppRoles {
    private fun norm(rol: String): String = rol.lowercase().trim()

    // ── Grupos de roles ────────────────────────────────────────────────────

    /** Admin y gerente: gestión completa */
    private val ROLES_GESTION = setOf("admin", "gerente")

    /** Roles que ven BI y reportes financieros */
    private val ROLES_CON_BI = setOf("admin", "gerente")

    /** Roles que ven Cierre de Caja */
    private val ROLES_CIERRE_CAJA = setOf("admin", "gerente", "especialista")

    /** Roles que ven Operación Hoy (dashboard) */
    private val ROLES_OPERACION_HOY = setOf("admin", "gerente", "especialista", "asesor", "asesora", "ventas")

    /** Roles comerciales (sin BI) */
    private val ROLES_COMERCIALES = setOf("asesor", "asesora", "ventas")

    /** Roles que pueden crear/modificar datos clínicos */
    private val ROLES_ESCRITURA_CLINICA = setOf("admin", "gerente", "especialista", "asesor", "asesora", "ventas")

    /** Roles que pueden crear/modificar evaluaciones */
    private val ROLES_ESCRITURA_EVALUACIONES = setOf("admin", "gerente", "especialista")

    /** Roles que pueden borrar registros */
    private val ROLES_BORRADO = setOf("admin", "gerente")

    /** Roles que pueden exportar inventario */
    private val ROLES_EXPORTAR = setOf("admin", "gerente", "especialista", "asesor", "asesora", "ventas")

    /** Solo admin */
    private val SOLO_ADMIN = setOf("admin")

    // ── Funciones de permiso ───────────────────────────────────────────────

    fun canViewBiAndReports(rol: String): Boolean = norm(rol) in ROLES_CON_BI

    fun canViewCierreCaja(rol: String): Boolean = norm(rol) in ROLES_CIERRE_CAJA

    fun canViewOperacionHoy(rol: String): Boolean = norm(rol) in ROLES_OPERACION_HOY

    fun canCreateEditPacientes(rol: String): Boolean = norm(rol) in ROLES_ESCRITURA_CLINICA

    fun canCreateEditEvaluaciones(rol: String): Boolean = norm(rol) in ROLES_ESCRITURA_EVALUACIONES

    fun canCreateEditDispensaciones(rol: String): Boolean = norm(rol) in ROLES_ESCRITURA_CLINICA

    fun canEditInventory(rol: String): Boolean = norm(rol) in ROLES_ESCRITURA_CLINICA

    /** Exportaciones rápidas para supervisión o cierre. */
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
