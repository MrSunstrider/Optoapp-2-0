package com.example.optoapp.data.membership

import com.example.optoapp.data.OpticaMembership

sealed class MembershipFetch {
    data class Ok(val memberships: List<OpticaMembership>) : MembershipFetch()
    data object Empty : MembershipFetch()
    data class Error(val cause: Throwable) : MembershipFetch()

    /**
     * Maps Error→emptyList() ONLY for sync callers that already used List.
     * AuthDelegate.resolvePostLogin MUST use the sealed type, not asList().
     */
    fun asList(): List<OpticaMembership> = when (this) {
        is Ok -> memberships
        Empty, is Error -> emptyList()
    }

    companion object {
        fun mapRow(opticaId: String, nombre: String, rol: String): OpticaMembership? {
            if (rol.isBlank()) return null
            return OpticaMembership(opticaId = opticaId, nombre = nombre, rol = rol)
        }

        fun fromMapped(rows: List<OpticaMembership>): MembershipFetch =
            if (rows.isEmpty()) Empty else Ok(rows)

        fun fromCaught(cause: Throwable): MembershipFetch = Error(cause)
    }
}
