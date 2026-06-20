package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.InventarioFisico
import com.example.optoapp.data.InventarioFisicoDetalle
import com.example.optoapp.data.InventarioFisicoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.domain.sync.LocalEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

open class SyncInventarioFisicoUseCase @Inject constructor(
    private val repository: InventarioFisicoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val conflictHelper: ConflictHelper
) {
    companion object {
        private const val TAG = "SyncInventarioFisico"
        private const val TABLE_SESSIONS = "inventario_fisico"
        private const val TABLE_DETALLES = "inventario_fisico_detalle"
        private const val UPSERT_BATCH_SIZE = 200
    }

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false
    ): Resource<InventarioFisicoSyncResult> {
        return try {
            Log.d(TAG, "InventarioFisico: inicio (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")
            val sessUp = if (skipUpload) 0 else uploadSessions(opticaId)
            val detUp = if (skipUpload) 0 else uploadDetalles(opticaId)
            val sessDown: Int
            val detDown: Int
            if (downloadAfterUpload) {
                sessDown = downloadSessions(opticaId)
                detDown = downloadDetalles(opticaId)
                Log.d(TAG, "InventarioFisico: fin OK (sessions=$sessDown detalles=$detDown)")
            } else {
                sessDown = 0
                detDown = 0
                Log.d(TAG, "InventarioFisico: fin upload-only OK")
            }
            Resource.Success(
                InventarioFisicoSyncResult(
                    uploadedSessions = sessUp,
                    uploadedDetalles = detUp,
                    downloadedSessions = sessDown,
                    downloadedDetalles = detDown
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red sincronizando inventario fisico: ${e.message}", e)
            Resource.Error("Error sincronizando inventario fisico: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado sincronizando inventario fisico: ${e.message}", e)
            Resource.Error("Error sincronizando inventario fisico: ${e.localizedMessage}")
        }
    }

    private suspend fun uploadSessions(opticaId: String): Int {
        return try {
            val list = repository.getListByOptica(opticaId)
            val remotos = list.map { it.toRemoto() }.distinctBy { it.id }
            if (remotos.isEmpty()) return 0

            val safeIds = conflictHelper.filterConflicts(
                tableName = TABLE_SESSIONS,
                opticaId = opticaId,
                entityType = "inventario_fisico",
                localEntities = remotos.map { LocalEntity(it.id, it.updatedAt) }
            ).map { it.id }.toSet()
            val safeRows = remotos.filter { it.id in safeIds }
            if (safeRows.isEmpty()) return 0

            safeRows.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
                supabase.postgrest[TABLE_SESSIONS].upsert(chunk)
            }
            safeRows.forEach { r -> syncStateTracker.markSynced(opticaId, "inventario_fisico", r.id) }
            list.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error subiendo sesiones de inventario fisico", e)
            0
        }
    }

    private suspend fun uploadDetalles(opticaId: String): Int {
        return try {
            val sessions = repository.getListByOptica(opticaId)
            val allDetalles = sessions.flatMap { s -> repository.getDetalles(s.id) }
                .map { it.toRemoto(opticaId) }
                .distinctBy { it.id }
            if (allDetalles.isEmpty()) return 0

            allDetalles.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
                supabase.postgrest[TABLE_DETALLES].upsert(chunk)
            }
            allDetalles.forEach { r -> syncStateTracker.markSynced(opticaId, "inventario_fisico_detalle", r.id) }
            allDetalles.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error subiendo detalles de inventario fisico", e)
            0
        }
    }

    private suspend fun downloadSessions(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_SESSIONS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<IFRemoto>()
        remotos.forEach { r ->
            try {
                repository.upsertSession(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando sesion ${r.id}: ${e.message}", e)
            }
        }
        return remotos.size
    }

    private suspend fun downloadDetalles(opticaId: String): Int {
        val sessions = repository.getListByOptica(opticaId)
        val sessionIds = sessions.map { it.id }.toSet()
        val remotos = supabase.postgrest[TABLE_DETALLES]
            .select {
                filter {
                    eq("optica_id", opticaId)
                }
            }
            .decodeList<IFDetalleRemoto>()
        remotos.forEach { r ->
            try {
                repository.upsertDetalle(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando detalle ${r.id}: ${e.message}", e)
            }
        }
        return remotos.size
    }
}

data class InventarioFisicoSyncResult(
    val uploadedSessions: Int,
    val uploadedDetalles: Int,
    val downloadedSessions: Int,
    val downloadedDetalles: Int
)

@Serializable
private data class IFRemoto(
    val id: String,
    val fecha: String? = null,
    val estado: String = "EN_PROGRESO",
    @SerialName("optica_id") val opticaId: String = "",
    @SerialName("user_id") val userId: String = "",
    val notas: String = "",
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toEntity() = InventarioFisico(
        id = id,
        fecha = fecha?.let { LocalDate.parse(it.take(10)) } ?: LocalDate.now(),
        estado = estado, opticaId = opticaId, userId = userId,
        notas = notas, updatedAt = updatedAt
    )
}

@Serializable
internal data class IFDetalleRemoto(
    val id: String,
    @SerialName("inventario_id") val inventarioId: String,
    @SerialName("montura_id") val monturaId: String,
    @SerialName("stock_sistema") val stockSistema: Int,
    @SerialName("stock_contado") val stockContado: Int? = null,
    val diferencia: Int? = null,
    @SerialName("optica_id") val opticaId: String = ""
) {
    fun toEntity() = InventarioFisicoDetalle(
        id = id, inventarioId = inventarioId, monturaId = monturaId,
        stockSistema = stockSistema, stockContado = stockContado,
        diferencia = diferencia
    )
}

private fun InventarioFisico.toRemoto(): IFRemoto = IFRemoto(
    id = id, fecha = fecha.toString(), estado = estado,
    opticaId = opticaId, userId = userId, notas = notas, updatedAt = updatedAt
)

internal fun InventarioFisicoDetalle.toRemoto(opticaId: String): IFDetalleRemoto = IFDetalleRemoto(
    id = id, inventarioId = inventarioId, monturaId = monturaId,
    stockSistema = stockSistema, stockContado = stockContado, diferencia = diferencia,
    opticaId = opticaId
)
