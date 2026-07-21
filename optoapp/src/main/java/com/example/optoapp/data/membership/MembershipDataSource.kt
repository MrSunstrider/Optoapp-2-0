package com.example.optoapp.data.membership

import android.util.Log
import com.example.optoapp.data.OpticaMemberRow
import com.example.optoapp.data.OpticaMembership
import com.example.optoapp.data.UsuarioOpticaDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MembershipDataSource @Inject internal constructor(
    private val supabase: SupabaseClient,
    private val opticaQueryHelper: OpticaQueryHelper,
) {
    suspend fun fetchMembershipsForCurrentUser(): List<OpticaMembership> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        val rows = try {
            supabase.postgrest[TABLE_UO]
                .select { filter { eq("user_id", uid) } }
                .decodeList<UsuarioOpticaDto>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "fetchMembershipsForCurrentUser failed", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "fetchMembershipsForCurrentUser failed", e)
            emptyList()
        }
        if (rows.isEmpty()) return emptyList()
        val out = mutableListOf<OpticaMembership>()
        for (row in rows) {
            val nombre = opticaQueryHelper.fetchOpticaNombre(row.opticaId)
            out.add(
                OpticaMembership(
                    opticaId = row.opticaId,
                    nombre = nombre.ifBlank { row.opticaId },
                    rol = row.rol.ifBlank { "admin" },
                ),
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "fetchMembersForOptica: opticaId=$opticaId", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "fetchMembersForOptica: opticaId=$opticaId", e)
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
            supabase.postgrest.rpc(
                "assign_optica_role_by_email",
                buildJsonObject {
                    put("p_optica_id", opticaId)
                    put("p_email", normalizedEmail)
                    put("p_rol", normalizedRole)
                },
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "assignRoleByEmail failed for opticaId=$opticaId", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "assignRoleByEmail failed for opticaId=$opticaId", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "MembershipDataSource"
        private const val TABLE_UO = "usuario_optica"
        private const val TABLE_OPTICA_MEMBERS = "optica_members"
    }
}
