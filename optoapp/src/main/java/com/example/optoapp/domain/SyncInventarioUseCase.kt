package com.example.optoapp.domain

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.domain.sync.ConflictHelper
import com.example.optoapp.domain.sync.EntitySnapshotSerializer
import com.example.optoapp.util.AppLogger
import androidx.room.withTransaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

/**
 * Evita divergencia de stock entre dispositivos de la misma óptica.
 * Sube cambios locales antes de descargar remotos para reducir la ventana de conflicto
 * cuando dos sucursales actualizan el mismo SKU simultáneamente.
 */
open class SyncInventarioUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val database: OptoDatabase,
    private val syncStateTracker: com.example.optoapp.data.SyncStateTracker,
    private val conflictHelper: com.example.optoapp.domain.sync.ConflictHelper,
    private val conflictDao: ConflictDao,
) {
    companion object {
        private const val TAG = "SyncInventario"
        private const val TABLE_MONTURAS = "monturas"
        private const val TABLE_MOVIMIENTOS = "montura_movimientos"
        private const val UPSERT_BATCH_SIZE = 200
        private const val MOVIMIENTO_FETCH_PAGE_SIZE = 500L
    }

    internal open suspend fun <T> runInTransaction(block: suspend () -> T): T =
        database.withTransaction(block)

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false,
    ): Resource<InventarioSyncResult> = try {
        AppLogger.d(TAG, "Inventario: inicio (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")
        val montUp = if (skipUpload) 0 else uploadMonturas(opticaId)
        val movOutcome = if (skipUpload) MovimientoUploadOutcome(0, 0) else uploadMovimientos(opticaId)
        val montDown: Int
        val movDown: Int
        if (downloadAfterUpload) {
            montDown = downloadMonturas(opticaId)
            movDown = downloadMovimientos(opticaId)
            AppLogger.d(TAG, "Inventario: fin OK (monturas=$montDown movimientos=$movDown)")
        } else {
            montDown = 0
            movDown = 0
            AppLogger.d(TAG, "Inventario: fin upload-only OK")
        }

        Resource.Success(
            InventarioSyncResult(
                uploadedMonturas = montUp,
                uploadedMovimientos = movOutcome.uploaded,
                downloadedMonturas = montDown,
                downloadedMovimientos = movDown,
                reconciledMovimientos = movOutcome.reconciled,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        System.err.println("[$TAG] ERROR: Error en red sincronizando inventario: ${e.message}")
        Resource.Error("Error sincronizando inventario: ${e.localizedMessage}")
    } catch (e: Exception) {
        System.err.println("[$TAG] ERROR: Error inesperado sincronizando inventario: ${e.message}")
        Resource.Error("Error sincronizando inventario: ${e.localizedMessage}")
    }

    private suspend fun uploadMonturas(opticaId: String): Int {
        val rows = repository.getMonturasSnapshotForOptica(opticaId)
            .map { it.toRemoto().copy(opticaId = opticaId) }
            .distinctBy { it.id }
        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_MONTURAS,
            opticaId = opticaId,
            entityType = "montura",
            localEntities = rows.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt, EntitySnapshotSerializer.serialize(it)) },
        ).map { it.id }.toSet()
        val rows2 = rows.filter { it.id in safeIds }
        if (rows2.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_monturas", "batch")
            return 0
        }
        try {
            rows2.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
                supabase.postgrest[TABLE_MONTURAS].upsert(chunk)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            System.err.println("[$TAG] ERROR: Error en red subiendo monturas: ${e.message}")
            syncStateTracker.markError(opticaId, "upload_monturas", "batch", e.message)
            throw e
        } catch (e: Exception) {
            System.err.println("[$TAG] ERROR: Error inesperado subiendo monturas: ${e.message}")
            syncStateTracker.markError(opticaId, "upload_monturas", "batch", e.message)
            throw e
        }
        runInTransaction {
            rows2.forEach { m -> syncStateTracker.markSynced(opticaId, "montura", m.id) }
        }
        syncStateTracker.markSynced(opticaId, "upload_monturas", "batch")
        return rows2.size
    }

    private suspend fun uploadMovimientos(opticaId: String): MovimientoUploadOutcome {
        val localMovimientos = repository.getMovimientosMonturaSnapshotForOptica(opticaId)
        if (localMovimientos.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_montura_movimientos", "batch")
            return MovimientoUploadOutcome(0, 0)
        }

        // Supabase unique index idx_movimientos_conflict is on (referencia_id, tipo, montura_id),
        // not on PK — distinctBy PK would let edited movements slip through as duplicates.
        val deduped = localMovimientos
            .sortedByDescending { it.updatedAt.orEmpty() }
            .distinctBy { Triple(it.referenciaId, it.tipo, it.monturaId) }

        val plan = conflictHelper.filterConflictMovimientos(opticaId, deduped)
        if (!plan.remoteFetchSucceeded) {
            syncStateTracker.markError(opticaId, "upload_montura_movimientos", "batch", "remote fetch failed")
            throw IOException("No se pudo leer movimientos remotos para reconciliar")
        }
        val safeIdSet = plan.safeIds.toSet()
        val safeMovimientos = deduped.filter { it.id in safeIdSet }
        val partition = ConflictHelper.partitionMovimientosForUpload(
            safeMovimientos,
            plan.remoteByKey,
        )

        try {
            runInTransaction {
                for ((local, remoteId) in partition.toReconcileLocally) {
                    repository.upsertMonturaMovimiento(local.copy(id = remoteId))
                    if (local.id != remoteId) {
                        repository.deleteMonturaMovimiento(local.id, opticaId)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            System.err.println("[$TAG] ERROR: Error reconciliando movimientos localmente: ${e.message}")
            syncStateTracker.markError(opticaId, "upload_montura_movimientos", "batch", e.message)
            throw e
        }

        val rows = partition.toUpload
            .map { it.toRemoto().copy(opticaId = opticaId) }
        if (rows.isEmpty() && partition.toReconcileLocally.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_montura_movimientos", "batch")
            return MovimientoUploadOutcome(0, 0)
        }
        try {
            if (rows.isNotEmpty()) {
                rows.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
                    upsertMovimientosBatch(chunk)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            System.err.println("[$TAG] ERROR: Error en red subiendo movimientos: ${e.message}")
            syncStateTracker.markError(opticaId, "upload_montura_movimientos", "batch", e.message)
            throw e
        } catch (e: Exception) {
            System.err.println("[$TAG] ERROR: Error inesperado subiendo movimientos: ${e.message}")
            syncStateTracker.markError(opticaId, "upload_montura_movimientos", "batch", e.message)
            throw e
        }
        runInTransaction {
            partition.toUpload.forEach { r -> syncStateTracker.markSynced(opticaId, "montura_movimiento", r.id) }
            partition.toReconcileLocally.forEach { (local, remoteId) ->
                syncStateTracker.markSynced(opticaId, "montura_movimiento", remoteId)
                if (local.id != remoteId) {
                    syncStateTracker.markSynced(opticaId, "montura_movimiento", local.id)
                }
            }
        }
        syncStateTracker.markSynced(opticaId, "upload_montura_movimientos", "batch")
        return MovimientoUploadOutcome(
            uploaded = partition.toUpload.size,
            reconciled = partition.toReconcileLocally.size,
        )
    }

    internal open suspend fun upsertMovimientosBatch(chunk: List<MonturaMovimientoRemoto>) {
        supabase.postgrest[TABLE_MOVIMIENTOS].upsert(chunk)
    }

    private suspend fun downloadMonturas(opticaId: String): Int {
        val conflictedIds = try {
            conflictDao.getConflictEntityIds(opticaId, "montura").toSet()
        } catch (e: Exception) {
            System.err.println("[$TAG] ERROR: Error querying conflict IDs, proceeding without guard: ${e.message}")
            emptySet()
        }

        val remotos = supabase.postgrest[TABLE_MONTURAS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<MonturaRemota>()
        remotos.forEach { r ->
            if (r.id in conflictedIds) return@forEach
            try {
                repository.upsertMontura(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AppLogger.e(TAG, "Error en red descargando monturas: ${e.message}")
                syncStateTracker.markError(opticaId, "montura", r.id, e.message)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error inesperado descargando monturas: ${e.message}")
                syncStateTracker.markError(opticaId, "montura", r.id, e.message)
            }
        }
        return remotos.size
    }

    private suspend fun downloadMovimientos(opticaId: String): Int {
        val conflictedIds = try {
            conflictDao.getConflictEntityIds(opticaId, "montura_movimiento").toSet()
        } catch (e: Exception) {
            System.err.println("[$TAG] ERROR: Error querying conflict IDs, proceeding without guard: ${e.message}")
            emptySet()
        }

        val remotos = fetchAllRemoteMovimientos(opticaId)
        remotos.forEach { r ->
            if (r.id in conflictedIds) return@forEach
            try {
                repository.upsertMonturaMovimiento(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AppLogger.e(TAG, "Error en red descargando movimientos: ${e.message}")
                syncStateTracker.markError(opticaId, "montura_movimiento", r.id, e.message)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error inesperado descargando movimientos: ${e.message}")
                syncStateTracker.markError(opticaId, "montura_movimiento", r.id, e.message)
            }
        }
        return remotos.size
    }

    private suspend fun fetchAllRemoteMovimientos(opticaId: String): List<MonturaMovimientoRemoto> {
        val all = mutableListOf<MonturaMovimientoRemoto>()
        var offset = 0L
        while (true) {
            val page = supabase.postgrest[TABLE_MOVIMIENTOS]
                .select {
                    filter { eq("optica_id", opticaId) }
                    order("id", Order.ASCENDING)
                    range(offset..offset + MOVIMIENTO_FETCH_PAGE_SIZE - 1)
                }
                .decodeList<MonturaMovimientoRemoto>()
            all.addAll(page)
            if (page.size < MOVIMIENTO_FETCH_PAGE_SIZE) break
            offset += MOVIMIENTO_FETCH_PAGE_SIZE
        }
        return all
    }
}

data class InventarioSyncResult(
    val uploadedMonturas: Int,
    val uploadedMovimientos: Int,
    val downloadedMonturas: Int,
    val downloadedMovimientos: Int,
    val reconciledMovimientos: Int = 0,
)

private data class MovimientoUploadOutcome(
    val uploaded: Int,
    val reconciled: Int,
)

@Serializable
internal data class MonturaRemota(
    val id: String,
    val sku: String = "",
    val marca: String = "",
    val modelo: String = "",
    val color: String = "",
    val talla: String = "",
    val costo: Double = 0.0,
    val precio: Double = 0.0,
    @SerialName("stock_actual") val stockActual: Int = 0,
    @SerialName("stock_minimo") val stockMinimo: Int = 0,
    val activo: Boolean = true,
    @SerialName("tipo_aro") val tipoAro: String = "",
    @SerialName("material_montura") val materialMontura: String = "",
    @SerialName("categoria") val categoria: String = "",
    @SerialName("coleccion") val coleccion: String = "",
    @SerialName("temporada") val temporada: String = "",
    @SerialName("estado_comercial") val estadoComercial: String = "",
    @SerialName("genero") val genero: String = "",
    @SerialName("optica_id") val opticaId: String,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toEntity() = Montura(
        id = id,
        sku = sku,
        marca = marca,
        modelo = modelo,
        color = color,
        talla = talla,
        costo = costo,
        precio = precio,
        stockActual = stockActual,
        stockMinimo = stockMinimo,
        activo = activo,
        tipoAro = tipoAro,
        materialMontura = materialMontura,
        categoria = categoria,
        coleccion = coleccion,
        temporada = temporada,
        estadoComercial = estadoComercial,
        genero = genero,
        opticaId = opticaId,
        updatedAt = updatedAt,
    )
}

@Serializable
internal data class MonturaMovimientoRemoto(
    val id: String,
    @SerialName("montura_id") val monturaId: String,
    val fecha: String,
    val tipo: String,
    val cantidad: Int,
    @SerialName("stock_previo") val stockPrevio: Int,
    @SerialName("stock_nuevo") val stockNuevo: Int,
    @SerialName("referencia_id") val referenciaId: String = "",
    val nota: String = "",
    @SerialName("optica_id") val opticaId: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("costo_unitario") val costoUnitario: Double = 0.0,
    @SerialName("tipo_documento") val tipoDocumento: String = "",
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun toEntity() = MonturaMovimiento(
        id = id,
        monturaId = monturaId,
        fecha = LocalDate.parse(fecha),
        tipo = tipo,
        cantidad = cantidad,
        stockPrevio = stockPrevio,
        stockNuevo = stockNuevo,
        referenciaId = referenciaId,
        nota = nota,
        opticaId = opticaId,
        userId = userId,
        costoUnitario = costoUnitario,
        tipoDocumento = tipoDocumento,
        updatedBy = updatedBy,
        updatedAt = updatedAt,
    )
}

internal fun Montura.toRemoto(): MonturaRemota = MonturaRemota(
    id = id,
    sku = sku.trim(),
    marca = marca.trim(),
    modelo = modelo.trim(),
    color = color.trim(),
    talla = talla.trim(),
    costo = costo,
    precio = precio,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    activo = activo,
    tipoAro = tipoAro.trim(),
    materialMontura = materialMontura.trim(),
    categoria = categoria.trim(),
    coleccion = coleccion.trim(),
    temporada = temporada.trim(),
    estadoComercial = estadoComercial.trim(),
    genero = genero.trim(),
    opticaId = opticaId,
    updatedAt = com.example.optoapp.domain.sync.coalesceUploadUpdatedAt(updatedAt),
)

internal fun MonturaMovimiento.toRemoto(): MonturaMovimientoRemoto = MonturaMovimientoRemoto(
    id = id,
    monturaId = monturaId,
    fecha = fecha.toString(),
    tipo = tipo.trim(),
    cantidad = cantidad,
    stockPrevio = stockPrevio,
    stockNuevo = stockNuevo,
    referenciaId = referenciaId.trim(),
    nota = nota.trim(),
    opticaId = opticaId,
    userId = userId,
    costoUnitario = costoUnitario,
    tipoDocumento = tipoDocumento,
    updatedBy = updatedBy,
    updatedAt = com.example.optoapp.domain.sync.coalesceUploadUpdatedAt(updatedAt),
)
