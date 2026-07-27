package com.example.optoapp.domain

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OrdenCompra
import com.example.optoapp.data.OrdenCompraItem
import com.example.optoapp.data.OrdenCompraRepository
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

/**
 * Pipeline stage 5: purchase orders sync runs after proveedores, before inventario_fisico.
 * Uploads OC + items, then downloads remote changes.
 */
open class SyncOrdenesCompraUseCase @Inject constructor(
    private val repository: OrdenCompraRepository,
    private val supabase: SupabaseClient,
    private val database: OptoDatabase,
    private val syncStateTracker: SyncStateTracker,
    private val conflictHelper: ConflictHelper,
    private val conflictDao: ConflictDao,
) {
    companion object {
        private const val TAG = "SyncOrdenesCompra"
        private const val TABLE_OC = "ordenes_compra"
        private const val TABLE_ITEMS = "orden_compra_items"
        private const val UPSERT_BATCH_SIZE = 200
    }

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false,
    ): Resource<OrdenesCompraSyncResult> = try {
        AppLogger.d(TAG, "OrdenesCompra: inicio (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")
        val ocUp = if (skipUpload) 0 else uploadOrdenesCompra(opticaId)
        val itemsUp = if (skipUpload) 0 else uploadItems(opticaId)
        val ocDown: Int
        val itemsDown: Int
        if (downloadAfterUpload) {
            ocDown = downloadOrdenesCompra(opticaId)
            itemsDown = downloadItems(opticaId)
            AppLogger.d(TAG, "OrdenesCompra: fin OK (oc=$ocDown items=$itemsDown)")
        } else {
            ocDown = 0
            itemsDown = 0
            AppLogger.d(TAG, "OrdenesCompra: fin upload-only OK")
        }
        Resource.Success(
            OrdenesCompraSyncResult(
                uploadedCompras = ocUp,
                uploadedItems = itemsUp,
                downloadedCompras = ocDown,
                downloadedItems = itemsDown,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppLogger.e(TAG, "Error en red sincronizando OC: ${e.message}", e)
        Resource.Error("Error sincronizando órdenes de compra: ${e.localizedMessage}")
    } catch (e: Exception) {
        AppLogger.e(TAG, "Error inesperado sincronizando OC: ${e.message}", e)
        Resource.Error("Error sincronizando órdenes de compra: ${e.localizedMessage}")
    }

    private suspend fun uploadOrdenesCompra(opticaId: String): Int {
        val rows = repository.getListByOptica(opticaId)
            .map { it.toRemoto() }
            .distinctBy { it.id }
        if (rows.isEmpty()) return 0

        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_OC,
            opticaId = opticaId,
            entityType = "orden_compra",
            localEntities = rows.map { LocalEntity(it.id, it.updatedAt, EntitySnapshotSerializer.serialize(it)) },
        ).map { it.id }.toSet()
        val safeRows = rows.filter { it.id in safeIds }
        if (safeRows.isEmpty()) return 0

        safeRows.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
            supabase.postgrest[TABLE_OC].upsert(chunk)
        }
        database.withTransaction {
            safeRows.forEach { r -> syncStateTracker.markSynced(opticaId, "orden_compra", r.id) }
        }
        return rows.size
    }

    private suspend fun uploadItems(opticaId: String): Int {
        val ocs = repository.getListByOptica(opticaId)
        val allItems = ocs.flatMap { oc -> repository.getItems(oc.id) }
            .map { it.toRemoto() }
            .distinctBy { it.id }
        if (allItems.isEmpty()) return 0

        allItems.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
            supabase.postgrest[TABLE_ITEMS].upsert(chunk)
        }
        database.withTransaction {
            allItems.forEach { r -> syncStateTracker.markSynced(opticaId, "orden_compra_item", r.id) }
        }
        return allItems.size
    }

    private suspend fun downloadOrdenesCompra(opticaId: String): Int {
        val conflictedIds = try {
            conflictDao.getConflictEntityIds(opticaId, "orden_compra").toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying conflict IDs, proceeding without guard: ${e.message}", e)
            emptySet()
        }

        val remotos = supabase.postgrest[TABLE_OC]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<OrdenCompraRemota>()
        remotos.forEach { r ->
            if (r.id in conflictedIds) return@forEach
            try {
                repository.upsertOrdenCompra(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error descargando OC ${r.id}: ${e.message}", e)
            }
        }
        return remotos.size
    }

    private suspend fun downloadItems(opticaId: String): Int {
        val conflictedIds = try {
            conflictDao.getConflictEntityIds(opticaId, "orden_compra_item").toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying conflict IDs, proceeding without guard: ${e.message}", e)
            emptySet()
        }

        val remotos = supabase.postgrest[TABLE_ITEMS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<OrdenCompraItemRemoto>()
        remotos.forEach { r ->
            if (r.id in conflictedIds) return@forEach
            try {
                repository.upsertItem(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error descargando item ${r.id}: ${e.message}", e)
            }
        }
        return remotos.size
    }
}

data class OrdenesCompraSyncResult(
    val uploadedCompras: Int,
    val uploadedItems: Int,
    val downloadedCompras: Int,
    val downloadedItems: Int,
)

@Serializable
private data class OrdenCompraRemota(
    val id: String,
    val numero: String,
    @SerialName("proveedor_id") val proveedorId: String,
    val fecha: String? = null,
    val estado: String = "PENDIENTE",
    val total: Double = 0.0,
    @SerialName("optica_id") val opticaId: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
) {
    fun toEntity() = OrdenCompra(
        id = id, numero = numero, proveedorId = proveedorId,
        fecha = fecha?.let { java.time.LocalDate.parse(it.take(10)) }
            ?: java.time.LocalDate.now(),
        estado = estado, total = total, opticaId = opticaId,
        updatedAt = updatedAt, updatedBy = updatedBy,
    )
}

@Serializable
private data class OrdenCompraItemRemoto(
    val id: String,
    @SerialName("orden_id") val ordenId: String,
    @SerialName("montura_id") val monturaId: String,
    val cantidad: Int,
    @SerialName("costo_unitario") val costoUnitario: Double = 0.0,
    val recibido: Int = 0,
    @SerialName("optica_id") val opticaId: String = "",
) {
    fun toEntity() = OrdenCompraItem(
        id = id,
        ordenId = ordenId,
        monturaId = monturaId,
        cantidad = cantidad,
        costoUnitario = costoUnitario,
        recibido = recibido,
    )
}

private fun OrdenCompra.toRemoto(): OrdenCompraRemota = OrdenCompraRemota(
    id = id, numero = numero, proveedorId = proveedorId,
    fecha = fecha.toString(), estado = estado, total = total,
    opticaId = opticaId, updatedAt = updatedAt, updatedBy = updatedBy,
)

private fun OrdenCompraItem.toRemoto(): OrdenCompraItemRemoto = OrdenCompraItemRemoto(
    id = id,
    ordenId = ordenId,
    monturaId = monturaId,
    cantidad = cantidad,
    costoUnitario = costoUnitario,
    recibido = recibido,
)
