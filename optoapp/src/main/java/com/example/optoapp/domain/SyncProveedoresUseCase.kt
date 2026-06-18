package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.CategoriaMontura
import com.example.optoapp.data.Proveedor
import com.example.optoapp.data.ProveedorRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.domain.sync.LocalEntity
import com.example.optoapp.sync.errorLabelForException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

/** Pipeline stage 4: supplier sync runs after finanzas, before ordenes_compra. */
open class SyncProveedoresUseCase @Inject constructor(
    private val repository: ProveedorRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val conflictHelper: ConflictHelper
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
        skipUpload: Boolean = false
    ): Resource<ProveedoresSyncResult> {
        return try {
            Log.d(TAG, "Proveedores: inicio (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")
            val provUp = if (skipUpload) 0 else uploadProveedores(opticaId)
            val catUp = if (skipUpload) 0 else uploadCategorias(opticaId)
            val provDown: Int
            val catDown: Int
            if (downloadAfterUpload) {
                provDown = downloadProveedores(opticaId)
                catDown = downloadCategorias(opticaId)
                Log.d(TAG, "Proveedores: fin OK (proveedores=$provDown categorias=$catDown)")
            } else {
                provDown = 0
                catDown = 0
                Log.d(TAG, "Proveedores: fin upload-only OK")
            }
            Resource.Success(
                ProveedoresSyncResult(
                    uploadedProveedores = provUp,
                    uploadedCategorias = catUp,
                    downloadedProveedores = provDown,
                    downloadedCategorias = catDown
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red sincronizando proveedores: ${e.message}", e)
            Resource.Error("Error sincronizando proveedores: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado sincronizando proveedores: ${e.message}", e)
            Resource.Error("Error sincronizando proveedores: ${e.localizedMessage}")
        }
    }

    private suspend fun uploadProveedores(opticaId: String): Int {
        val rows = repository.getListByOptica(opticaId)
            .map { it.toRemoto() }
            .distinctBy { it.id }
        if (rows.isEmpty()) return 0

        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_PROVEEDORES,
            opticaId = opticaId,
            entityType = "proveedor",
            localEntities = rows.map { LocalEntity(it.id, it.updatedAt) }
        ).map { it.id }.toSet()
        val safeRows = rows.filter { it.id in safeIds }
        if (safeRows.isEmpty()) return 0

        safeRows.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
            supabase.postgrest[TABLE_PROVEEDORES].upsert(chunk)
        }
        safeRows.forEach { r -> syncStateTracker.markSynced(opticaId, "proveedor", r.id) }
        return rows.size
    }

    private suspend fun uploadCategorias(opticaId: String): Int {
        val rows = repository.getCategoriaListByOptica(opticaId)
            .map { it.toRemoto() }
            .distinctBy { it.id }
        if (rows.isEmpty()) return 0

        rows.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
            supabase.postgrest[TABLE_CATEGORIAS].upsert(chunk)
        }
        rows.forEach { r -> syncStateTracker.markSynced(opticaId, "categoria_montura", r.id) }
        return rows.size
    }

    private suspend fun downloadProveedores(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_PROVEEDORES]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<ProveedorRemoto>()
        remotos.forEach { r ->
            try {
                repository.upsertProveedor(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando proveedor ${r.id}: ${e.message}", e)
            }
        }
        return remotos.size
    }

    private suspend fun downloadCategorias(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_CATEGORIAS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<CategoriaRemota>()
        remotos.forEach { r ->
            try {
                repository.upsertCategoria(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error descargando categoria ${r.id}: ${e.message}", e)
            }
        }
        return remotos.size
    }
}

data class ProveedoresSyncResult(
    val uploadedProveedores: Int,
    val uploadedCategorias: Int,
    val downloadedProveedores: Int,
    val downloadedCategorias: Int
)

// ─── Remota DTOs ───────────────────────────────────────────────────────────

@Serializable
private data class ProveedorRemoto(
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
    @SerialName("updated_by") val updatedBy: String? = null
) {
    fun toEntity() = Proveedor(
        id = id, nombre = nombre, ruc = ruc, telefono = telefono,
        email = email, direccion = direccion, contacto = contacto,
        activo = activo, opticaId = opticaId, updatedAt = updatedAt, updatedBy = updatedBy
    )
}

@Serializable
private data class CategoriaRemota(
    val id: String,
    val nombre: String,
    val descripcion: String = "",
    @SerialName("optica_id") val opticaId: String = ""
) {
    fun toEntity() = CategoriaMontura(
        id = id, nombre = nombre, descripcion = descripcion, opticaId = opticaId
    )
}

private fun Proveedor.toRemoto(): ProveedorRemoto = ProveedorRemoto(
    id = id, nombre = nombre, ruc = ruc, telefono = telefono,
    email = email, direccion = direccion, contacto = contacto,
    activo = activo, opticaId = opticaId, updatedAt = updatedAt, updatedBy = updatedBy
)

private fun CategoriaMontura.toRemoto(): CategoriaRemota = CategoriaRemota(
    id = id, nombre = nombre, descripcion = descripcion, opticaId = opticaId
)
