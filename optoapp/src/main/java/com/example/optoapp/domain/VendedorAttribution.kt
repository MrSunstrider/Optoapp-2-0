package com.example.optoapp.domain

/**
 * Seller for Reportes Equipo / commissions is the membership auth uid, not a free-text name.
 * Create defaults to the signed-in user; edits keep the persisted attribution until an
 * admin/gerente reassignment UI writes a new value.
 */
object VendedorAttribution {
    fun forCreate(currentUserId: String?): String? =
        currentUserId?.trim()?.takeIf { it.isNotBlank() }

    fun forEdit(edited: String?, persisted: String?): String? =
        edited?.trim()?.takeIf { it.isNotBlank() } ?: persisted?.trim()?.takeIf { it.isNotBlank() }
}
