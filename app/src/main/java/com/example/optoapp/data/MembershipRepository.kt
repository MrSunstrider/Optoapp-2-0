package com.example.optoapp.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException
import java.util.UUID
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

    suspend fun fetchMembersForOptica(opticaId: String): List<OpticaMemberRow> {
        if (supabase.auth.currentUserOrNull() == null) return emptyList()
        return try {
            supabase.postgrest[TABLE_OPTICA_MEMBERS]
                .select { filter { eq("optica_id", opticaId) } }
                .decodeList<OpticaMemberRow>()
                .sortedWith(compareBy({ it.email.lowercase() }, { it.userId }))
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun assignRoleByEmail(opticaId: String, email: String, rol: String): Result<Unit> {
        if (supabase.auth.currentUserOrNull() == null) {
            return Result.failure(IllegalStateException("Sin sesión"))
        }
        val normalizedEmail = email.trim().lowercase()
        val normalizedRole = rol.trim().lowercase()
        if (normalizedEmail.isBlank()) return Result.failure(IllegalArgumentException("Email requerido"))
        if (normalizedRole.isBlank()) return Result.failure(IllegalArgumentException("Rol requerido"))
        return try {
            val user = supabase.postgrest[TABLE_USER_PROFILES]
                .select { filter { eq("email", normalizedEmail) } }
                .decodeList<UserProfileRow>()
                .firstOrNull()
                ?: return Result.failure(IllegalArgumentException("No existe una cuenta con ese email."))

            supabase.postgrest[TABLE_UO].upsert(
                listOf(
                    UsuarioOpticaUpsertDto(
                    userId = user.userId,
                    opticaId = opticaId,
                    rol = normalizedRole
                    )
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOpticaForCurrentUser(nombreOptica: String): Result<OpticaMembership> {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: return Result.failure(IllegalStateException("Sin sesión"))
        val nombre = nombreOptica.trim()
        if (nombre.isBlank()) return Result.failure(IllegalArgumentException("Nombre de óptica requerido"))
        val opticaId = "opt_" + UUID.randomUUID().toString().replace("-", "").take(16)
        return try {
            supabase.postgrest[TABLE_OPTICAS].insert(
                listOf(OpticaInsertDto(id = opticaId, nombre = nombre, plan = "free"))
            )
            supabase.postgrest[TABLE_UO].insert(
                listOf(UsuarioOpticaUpsertDto(userId = uid, opticaId = opticaId, rol = "admin"))
            )
            Result.success(OpticaMembership(opticaId = opticaId, nombre = nombre, rol = "admin"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    /** Plan de suscripción (`free` | `pro` | …) desde Supabase; null si no hay sesión o error. */
    suspend fun fetchOpticaPlan(opticaId: String): String? {
        if (supabase.auth.currentUserOrNull() == null) return null
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            list.firstOrNull()?.plan?.lowercase()?.trim() ?: "free"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Lee contacto de laboratorio desde Supabase. `null` si no hay sesión, error de red o RLS.
     * Par vacío `"" to ""` es válido cuando la fila existe sin datos aún.
     */
    suspend fun fetchOpticaLaboratorioSettings(opticaId: String): Pair<String, String>? {
        if (supabase.auth.currentUserOrNull() == null) return null
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            val row = list.firstOrNull() ?: return "" to ""
            row.laboratorioNombre to row.laboratorioContacto
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Persiste en Supabase. Falla si no hay sesión, red o políticas RLS.
     */
    suspend fun updateOpticaLaboratorioSettings(
        opticaId: String,
        laboratorioNombre: String,
        laboratorioContacto: String
    ): Result<Unit> {
        if (supabase.auth.currentUserOrNull() == null) {
            return Result.failure(IllegalStateException("Sin sesión"))
        }
        return try {
            supabase.postgrest[TABLE_OPTICAS].update(
                OpticaLaboratorioPatch(
                    laboratorioNombre = laboratorioNombre.trim(),
                    laboratorioContacto = laboratorioContacto.trim()
                )
            ) {
                filter { eq("id", opticaId) }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TABLE_UO = "usuario_optica"
        private const val TABLE_OPTICAS = "opticas"
        private const val TABLE_USER_PROFILES = "user_profiles"
        private const val TABLE_OPTICA_MEMBERS = "optica_members"
    }
}

@Serializable
private data class UsuarioOpticaDto(
    @SerialName("user_id") val userId: String,
    @SerialName("optica_id") val opticaId: String,
    val rol: String = "admin"
)

@Serializable
data class OpticaMemberRow(
    @SerialName("optica_id") val opticaId: String,
    @SerialName("user_id") val userId: String,
    val email: String = "",
    val rol: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
private data class UserProfileRow(
    @SerialName("user_id") val userId: String,
    val email: String
)

@Serializable
private data class UsuarioOpticaUpsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("optica_id") val opticaId: String,
    val rol: String
)

@Serializable
private data class OpticaDto(
    val id: String,
    val nombre: String = "",
    val plan: String = "free",
    @SerialName("laboratorio_nombre") val laboratorioNombre: String = "",
    @SerialName("laboratorio_contacto") val laboratorioContacto: String = ""
)

@Serializable
private data class OpticaInsertDto(
    val id: String,
    val nombre: String,
    val plan: String = "free"
)

@Serializable
private data class OpticaLaboratorioPatch(
    @SerialName("laboratorio_nombre") val laboratorioNombre: String,
    @SerialName("laboratorio_contacto") val laboratorioContacto: String
)
