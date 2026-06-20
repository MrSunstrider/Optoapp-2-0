package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.Montura
import com.example.optoapp.data.MonturaMovimiento
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.sync.errorLabelForException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

/**
 * Sincronización de inventario: monturas + movimientos de stock.
 *
 * Upload -> Download para paridad entre dispositivos.
 */
open class SyncInventarioUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: com.example.optoapp.data.SyncStateTracker,
    private val conflictHelper: com.example.optoapp.domain.sync.ConflictHelper
) {
    companion object {
        private const val TAG = "SyncInventario"
        private const val TABLE_MONTURAS = "monturas"
        private const val TABLE_MOVIMIENTOS = "montura_movimientos"
        private const val UPSERT_BATCH_SIZE = 200
    }

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false
    ): Resource<InventarioSyncResult> {
        return try {
            Log.d(TAG, "Inventario: inicio (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")
            val montUp = if (skipUpload) 0 else uploadMonturas(opticaId)
            val movUp = if (skipUpload) 0 else uploadMovimientos(opticaId)
            val montDown: Int
            val movDown: Int
            if (downloadAfterUpload) {
                montDown = downloadMonturas(opticaId)
                movDown = downloadMovimientos(opticaId)
                Log.d(TAG, "Inventario: fin OK (monturas=$montDown movimientos=$movDown)")
            } else {
                montDown = 0
                movDown = 0
                Log.d(TAG, "Inventario: fin upload-only OK")
            }

            Resource.Success(
                InventarioSyncResult(
                    uploadedMonturas = montUp,
                    uploadedMovimientos = movUp,
                    downloadedMonturas = montDown,
                    downloadedMovimientos = movDown
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red sincronizando inventario: ${e.message}", e)
            Resource.Error("Error sincronizando inventario: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado sincronizando inventario: ${e.message}", e)
            Resource.Error("Error sincronizando inventario: ${e.localizedMessage}")
        }
    }

    private suspend fun uploadMonturas(opticaId: String): Int {
        val rows = repository.getMonturasSnapshotForOptica(opticaId)
            .map { it.toRemoto().copy(opticaId = opticaId) }
            .distinctBy { it.id }
        // Detección de conflictos antes de upsert
        val safeIds = conflictHelper.filterConflicts(
            tableName = TABLE_MONTURAS,
            opticaId = opticaId,
            entityType = "montura",
            localEntities = rows.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt) }
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
            Log.e(TAG, "Error en red subiendo monturas: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_monturas", "batch", e.message)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo monturas: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_monturas", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_monturas", "batch")
        rows2.forEach { m -> syncStateTracker.markSynced(opticaId, "montura", m.id) }
        return rows.size
    }

    private suspend fun uploadMovimientos(opticaId: String): Int {
        val rows = repository.getMovimientosMonturaSnapshotForOptica(opticaId)
            .map { it.toRemoto().copy(opticaId = opticaId) }
            .distinctBy { it.id }
        if (rows.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_montura_movimientos", "batch")
            return 0
        }
        try {
            rows.chunked(UPSERT_BATCH_SIZE).forEach { chunk ->
                supabase.postgrest[TABLE_MOVIMIENTOS].upsert(chunk)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo movimientos: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_montura_movimientos", "batch", e.message)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo movimientos: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_montura_movimientos", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_montura_movimientos", "batch")
        rows.forEach { r -> syncStateTracker.markSynced(opticaId, "montura_movimiento", r.id) }
        return rows.size
    }

    private suspend fun downloadMonturas(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_MONTURAS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<MonturaRemota>()
        remotos.forEach { r ->
            try {
                repository.upsertMontura(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red descargando monturas: ${e.message}", e)
                syncStateTracker.markError(opticaId, "montura", r.id, e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando monturas: ${e.message}", e)
                syncStateTracker.markError(opticaId, "montura", r.id, e.message)
            }
        }
        return remotos.size
    }

    private suspend fun downloadMovimientos(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE_MOVIMIENTOS]
            .select { filter { eq("optica_id", opticaId) } }
            .decodeList<MonturaMovimientoRemoto>()
        remotos.forEach { r ->
            try {
                repository.upsertMonturaMovimiento(r.toEntity())
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red descargando movimientos: ${e.message}", e)
                syncStateTracker.markError(opticaId, "montura_movimiento", r.id, e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando movimientos: ${e.message}", e)
                syncStateTracker.markError(opticaId, "montura_movimiento", r.id, e.message)
            }
        }
        return remotos.size
    }
}

data class InventarioSyncResult(
    val uploadedMonturas: Int,
    val uploadedMovimientos: Int,
    val downloadedMonturas: Int,
    val downloadedMovimientos: Int
)

@Serializable
private data class MonturaRemota(
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
    @SerialName("optica_id") val opticaId: String,
    @SerialName("updated_at") val updatedAt: String? = null
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
        opticaId = opticaId,
        updatedAt = updatedAt
    )
}

@Serializable
data class MonturaMovimientoRemoto(
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
    @SerialName("tipo_documento") val tipoDocumento: String = ""
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
        tipoDocumento = tipoDocumento
    )
}

private fun Montura.toRemoto(): MonturaRemota = MonturaRemota(
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
    opticaId = opticaId,
    updatedAt = updatedAt
)

fun MonturaMovimiento.toRemoto(): MonturaMovimientoRemoto = MonturaMovimientoRemoto(
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
    tipoDocumento = tipoDocumento
)

