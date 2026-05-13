package com.example.optoapp.viewmodel.auth

import com.example.optoapp.data.ISessionManager
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.util.BackupImportValidator
import com.google.gson.Gson
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/**
 * Encapsula toda la lógica de backup/restore: exportación, importación y
 * verificación de permisos vía RPC.
 *
 * ADR-2: Delegado separado inyectado → SRP, testeable con fakes.
 */
class BackupDelegate @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: ISessionManager,
    private val supabase: SupabaseClient,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "BackupDelegate"

        /** Pure logic: check if role is admin, returns error message or null. */
        fun checkExportAdmin(rol: String): String? {
            if (rol.trim().lowercase() != "admin") return "Solo admin puede descargar respaldo total."
            return null
        }

        /** Pure logic: check if role is admin for restore, returns error message or null. */
        fun checkRestoreAdmin(rol: String): String? {
            if (rol.trim().lowercase() != "admin") return "Solo admin puede restaurar respaldos."
            return null
        }

        /** Pure logic: validate sourceOpticaId is present. */
        fun validateSourceOpticaId(sourceOpticaId: String?): String? {
            val id = sourceOpticaId?.trim().orEmpty()
            if (id.isBlank()) return "Respaldo sin origen de óptica. Exporta uno nuevo con la versión actual."
            return null
        }

        /** Pure logic: validate sourceOpticaId matches currentOpticaId. */
        fun validateOpticaIdMatch(sourceOpticaId: String, currentOpticaId: String): String? {
            if (!sourceOpticaId.equals(currentOpticaId, ignoreCase = false))
                return "Este respaldo pertenece a otra óptica y no puede restaurarse aquí."
            return null
        }
    }

    /**
     * Exporta la base de datos local a JSON.
     * Solo admin puede descargar respaldo total.
     */
    suspend fun getBackupJson(): String {
        val oid = sessionManager.opticaId.first()
        val rol = sessionManager.opticaRol.first().trim().lowercase()
        checkExportAdmin(rol)?.let { throw IllegalStateException(it) }
        assertBackupOperationAllowed("export", oid, oid)
        return gson.toJson(repository.getBackupDataForOptica(oid))
    }

    /**
     * Restaura un respaldo JSON en la base de datos local.
     * Valida que el respaldo pertenezca a la óptica activa.
     */
    suspend fun restoreBackup(json: String): String {
        val rol = sessionManager.opticaRol.first().trim().lowercase()
        checkRestoreAdmin(rol)?.let { return it }
        val data = BackupImportValidator.parse(json).getOrElse { e ->
            return e.message ?: "No se pudo validar el respaldo."
        }
        return try {
            val currentOpticaId = sessionManager.opticaId.first()
            val sourceOpticaId = data.sourceOpticaId?.trim().orEmpty()
            validateSourceOpticaId(data.sourceOpticaId)?.let { return it }
            validateOpticaIdMatch(sourceOpticaId, currentOpticaId)?.let { return it }
            assertBackupOperationAllowed("restore", sourceOpticaId, currentOpticaId)
            repository.restoreBackup(data, currentOpticaId)
            "Base de datos restaurada correctamente."
        } catch (e: Exception) {
            "Error al restaurar: ${e.message ?: "desconocido"}"
        }
    }

    private suspend fun assertBackupOperationAllowed(
        action: String,
        sourceOpticaId: String,
        targetOpticaId: String
    ) {
        supabase.postgrest.rpc(
            "assert_backup_operation_allowed",
            buildJsonObject {
                put("p_action", action)
                put("p_source_optica_id", sourceOpticaId)
                put("p_target_optica_id", targetOpticaId)
            }
        )
    }
}
