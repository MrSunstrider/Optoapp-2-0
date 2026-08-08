package com.example.optoapp.domain

import com.example.optoapp.data.CategoriaMontura
import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.Proveedor
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.domain.sync.EntitySnapshotSerializer
import com.example.optoapp.domain.sync.LocalEntity
import com.example.optoapp.util.AppLogger
import androidx.room.withTransaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import javax.inject.Inject

open class SyncProveedoresUseCase @Inject constructor(
    private val repository: ProveedorRepository,
    private val supabase: SupabaseClient,
    private val database: OptoDatabase,
    private val syncStateTracker: SyncStateTracker,
    private val conflictHelper: ConflictHelper,
    private val conflictDao: ConflictDao,
) {
    companion object {
        private const val TAG = "SyncProveedores"
        private const val TABLE_PROVEEDORES = "proveedores"
        private const val TABLE_CATEGORIAS = "categorias_montura"
        private const val UPSERT_BATCH_SIZE = 200
    }

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false,
    ): Resource<ProveedoresSyncResult> = try {
        AppLogger.d(TAG, "Proveedores: inicio (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")
        val provUp = if (skipUpload) 0 else uploadProveedores(opticaId)
        val catUp = if (skipUpload) 0 else uploadCategorias(opticaId)
        val provDown: Int
        val catDown: Int
        if (downloadAfterUpload) {
            provDown = downloadProveedores(opticaId)
            catDown = downloadCategorias(opticaId)
            AppLogger.d(TAG, "Proveedores: fin OK (proveedores=$provDown categorias=$catDown)")
        } else {
            provDown = 0
            catDown = 0
            AppLogger.d(TAG, "Proveedores: fin upload-only OK")
        }
        Resource.Success(
            ProveedoresSyncResult(
                uploadedProveedores = provUp,
                uploadedCategorias = catUp,
                downloadedProveedores = provDown,
                downloadedCategorias = catDown,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppLogger.e(TAG, "Error en red sincronizando proveedores: ${e.message}", e)
        Resource.Error("Error sincronizando proveedores: ${e.localizedMessage}")
    } catch (e: Exception) {
        AppLogger.e(TAG, "Error inesperado sincronizando proveedores: ${e.message}", e)
        Resource.Error("Error sincronizando proveedores: ${e.localizedMessage}")
    }

    internal open suspend fun fetchRemoteProveedoresForLookup(opticaId: String): List<ProveedorRemotoLookup> =
        supabase.postgrest[TABLE_PROVEEDORES]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<ProveedorRemotoLookup>()

    private suspend fun uploadProveedores(opticaId: String): Int {
        val rows = repository.getListByOptica(opticaId)
            .map { it.toRemoto() }
            .distinctBy { it.id }
        if (rows.isEmpty()) return 0

        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_PROVEEDORES,
            opticaId = opticaId,
            entityType = "proveedor",
            localEntities = rows.map { LocalEntity(it.id, it.updatedAt, EntitySnapshotSerializer.serialize(it)) },
        ).map { it.id }.toSet()
        var safeRows = rows.filter { it.id in safeIds }
        if (safeRows.isEmpty()) return 0

        // Reconcile IDs with remote: two devices may create the same proveedor
        // with different UUIDs. The UNIQUE constraint is on (ruc, optica_id).
        try {
            val remotos = fetchRemoteProveedoresForLookup(opticaId)
            val remoteIdByRuc = remotos
                .filter { it.ruc.isNotBlank() }
                .associateBy { it.ruc.trim() }
            safeRows = safeRows.map { row ->
                val key = row.ruc.trim()
                val remoteId = if (key.isNotBlank()) remoteIdByRuc[key]?.id else null
                if (remoteId != null && remoteId != row.id) row.copy(id = remoteId) else row
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Reconciliation fetch failed for proveedores, skipping: ${e.message}", e)
            return 0
        }

        safeRows.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
            supabase.postgrest[TABLE_PROVEEDORES].upsert(chunk)
        }
        database.withTransaction {
            safeRows.forEach { r -> syncStateTracker.markSynced(opticaId, "proveedor", r.id) }
        }
        return safeRows.size
    }

    internal open suspend fun fetchRemoteCategoriasForLookup(opticaId: String): List<CategoriaRemotaLookup> =
        supabase.postgrest[TABLE_CATEGORIAS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<CategoriaRemotaLookup>()

    private suspend fun uploadCategorias(opticaId: String): Int {
        val rows = repository.getCategoriaListByOptica(opticaId)
            .map { it.toRemoto() }
            .distinctBy { it.id }
        if (rows.isEmpty()) return 0

        // Reconcile IDs with remote: two devices may create the same category
        // with different UUIDs. The UNIQUE constraint is on (nombre, optica_id).
        val reconciledRows = try {
            val remotos = fetchRemoteCategoriasForLookup(opticaId)
            val remoteIdByNombre = remotos
                .filter { it.nombre.isNotBlank() }
                .associateBy { it.nombre.trim().lowercase() }
            rows.map { row ->
                val key = row.nombre.trim().lowercase()
                val remoteId = if (key.isNotBlank()) remoteIdByNombre[key]?.id else null
                if (remoteId != null && remoteId != row.id) row.copy(id = remoteId) else row
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Reconciliation fetch failed for categorias, skipping: ${e.message}", e)
            return 0
        }

        reconciledRows.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
            supabase.postgrest[TABLE_CATEGORIAS].upsert(chunk)
        }
        database.withTransaction {
            reconciledRows.forEach { r -> syncStateTracker.markSynced(opticaId, "categoria_montura", r.id) }
        }
        return reconciledRows.size
    }

    private suspend fun downloadProveedores(opticaId: String): Int {
        val conflictedIds = try {
            conflictDao.getConflictEntityIds(opticaId, "proveedor").toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying conflict IDs, proceeding without guard: ${e.message}", e)
            emptySet()
        }

        val remotos = supabase.postgrest[TABLE_PROVEEDORES]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<ProveedorRemoto>()
        remotos.forEach { r ->
            if (r.id in conflictedIds) return@forEach
            try {
                repository.upsertProveedor(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error descargando proveedor ${r.id}: ${e.message}", e)
            }
        }
        return remotos.size
    }

    private suspend fun downloadCategorias(opticaId: String): Int {
        val conflictedIds = try {
            conflictDao.getConflictEntityIds(opticaId, "categoria_montura").toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying conflict IDs, proceeding without guard: ${e.message}", e)
            emptySet()
        }

        val remotos = supabase.postgrest[TABLE_CATEGORIAS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<CategoriaRemota>()
        remotos.forEach { r ->
            if (r.id in conflictedIds) return@forEach
            try {
                repository.upsertCategoria(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error descargando categoria ${r.id}: ${e.message}", e)
            }
        }
        return remotos.size
    }
}

data class ProveedoresSyncResult(
    val uploadedProveedores: Int,
    val uploadedCategorias: Int,
    val downloadedProveedores: Int,
    val downloadedCategorias: Int,
)

@Serializable
internal data class ProveedorRemoto(
    val id: String,
    val nombre: String,
    val ruc: String,
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val contacto: String = "",
    val activo: Boolean = true,
    @SerialName("optica_id") val opticaId: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
) {
    fun toEntity() = Proveedor(
        id = id, nombre = nombre, ruc = ruc, telefono = telefono,
        email = email, direccion = direccion, contacto = contacto,
        activo = activo, opticaId = opticaId, updatedAt = updatedAt, updatedBy = updatedBy,
    )
}

@Serializable
private data class CategoriaRemota(
    val id: String,
    val nombre: String,
    val descripcion: String = "",
    @SerialName("optica_id") val opticaId: String = "",
) {
    fun toEntity() = CategoriaMontura(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        opticaId = opticaId,
    )
}

@Serializable
internal data class ProveedorRemotoLookup(
    val id: String,
    val ruc: String,
    @SerialName("optica_id") val opticaId: String,
)

@Serializable
internal data class CategoriaRemotaLookup(
    val id: String,
    val nombre: String,
    @SerialName("optica_id") val opticaId: String,
)

private fun Proveedor.toRemoto(): ProveedorRemoto = ProveedorRemoto(
    id = id, nombre = nombre, ruc = ruc, telefono = telefono,
    email = email, direccion = direccion, contacto = contacto,
    activo = activo, opticaId = opticaId, updatedAt = updatedAt, updatedBy = updatedBy,
)

private fun CategoriaMontura.toRemoto(): CategoriaRemota = CategoriaRemota(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    opticaId = opticaId,
)
