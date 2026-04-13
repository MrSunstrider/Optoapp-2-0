package com.example.optoapp.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembershipRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    /**
     * Carga las ópticas a las que tiene acceso el usuario autenticado actual.
     */
    suspend fun fetchMembershipsForCurrentUser(): List<OpticaMembership> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        val rows = try {
            supabase.postgrest[TABLE_UO]
                .select { filter { eq("user_id", uid) } }
                .decodeList<UsuarioOpticaDto>()
        } catch (_: Exception) {
            emptyList()
        }
        if (rows.isEmpty()) return emptyList()
        val out = mutableListOf<OpticaMembership>()
        for (row in rows) {
            val nombre = fetchOpticaNombre(row.opticaId)
            out.add(
                OpticaMembership(
                    opticaId = row.opticaId,
                    nombre = nombre.ifBlank { row.opticaId },
                    rol = row.rol.ifBlank { "admin" }
                )
            )
        }
        return out
    }

    private suspend fun fetchOpticaNombre(opticaId: String): String {
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            list.firstOrNull()?.nombre.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private const val TABLE_UO = "usuario_optica"
        private const val TABLE_OPTICAS = "opticas"
    }
}

@Serializable
private data class UsuarioOpticaDto(
    @SerialName("user_id") val userId: String,
    @SerialName("optica_id") val opticaId: String,
    val rol: String = "admin"
)

@Serializable
private data class OpticaDto(
    val id: String,
    val nombre: String = ""
)
