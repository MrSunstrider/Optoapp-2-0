package com.example.optoapp.domain

import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraDao
import com.example.optoapp.data.costobiselado.CostoBiseladoDao
import com.example.optoapp.data.costoproducto.CostoProductoDao
import com.example.optoapp.data.resumendiario.ResumenDiarioDao
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * Extracted from [SyncFinanzasUseCase] so finanzas sync can download each entity type
 * independently while sharing retry, skip-deletion, and tracking logic.
 */
class DownloadSyncCoordinator @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: SyncStateTracker,
    private val deletionSyncHelper: DeletionSyncHelper,
    private val networkRetryHelper: NetworkRetryHelper,
    private val resumenDiarioDao: ResumenDiarioDao,
    private val configuracionFinancieraDao: ConfiguracionFinancieraDao,
    private val costoProductoDao: CostoProductoDao,
    private val costoBiseladoDao: CostoBiseladoDao,
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val TABLE_DISPENSACIONES = "dispensaciones"
        private const val TABLE_DISPENSACION_ITEMS = "dispensacion_items"
        private const val TABLE_SERVICIOS = "servicios_extra"
        private const val TABLE_PAGOS = "pagos"
        private const val TABLE_RESUMEN_DIARIO = "resumen_diario"
        private const val TABLE_CONFIGURACION_FINANCIERA = "configuracion_financiera"
        private const val TABLE_REGALOS = "regalos_dispensacion"
        private const val TABLE_GASTOS_OPERATIVOS = "gastos_operativos"
        private const val TABLE_COSTOS_PRODUCTOS = "costos_productos"
        private const val TABLE_COSTOS_BISELADO = "costos_biselado"
    }

    private suspend inline fun <reified T : Any> downloadTable(
        opticaId: String,
        tableName: String,
        entityType: String,
        skipDeletions: Boolean,
        crossinline getId: (T) -> String,
        crossinline upsert: suspend (T) -> Unit,
    ): Int {
        val skipIds = if (skipDeletions) deletionSyncHelper.deletedIds(opticaId) else emptySet()
        val remotos: List<T>
        try {
            var result: List<T> = emptyList()
            networkRetryHelper.retryNetwork("download:$tableName") {
                result = supabase.postgrest[tableName]
                    .select { filter { eq("optica_id", opticaId) } }
                    .decodeList<T>()
            }
            remotos = result
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppLogger.e(TAG, "Error de red descargando $entityType: ${e.message}", e)
            syncStateTracker.markError(opticaId, "download_$entityType", "batch", e.message)
            return 0
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error inesperado descargando $entityType: ${e.message}", e)
            syncStateTracker.markError(opticaId, "download_$entityType", "batch", e.message)
            return 0
        }
        var persisted = 0
        remotos.forEach { r ->
            if (skipDeletions && getId(r) in skipIds) return@forEach
            try {
                repository.withTransaction {
                    upsert(r)
                    syncStateTracker.markSynced(opticaId, entityType, getId(r))
                }
                persisted++
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AppLogger.e(TAG, "Error de red descargando item $entityType ${getId(r)}: ${e.message}", e)
                syncStateTracker.markError(opticaId, entityType, getId(r), e.message)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error inesperado descargando item $entityType ${getId(r)}: ${e.message}", e)
                syncStateTracker.markError(opticaId, entityType, getId(r), e.message)
            }
        }
        return persisted
    }

    suspend fun downloadDispensacionItems(opticaId: String): Int = downloadTable<DispensacionItemRemota>(
        opticaId,
        TABLE_DISPENSACION_ITEMS,
        "dispensacion_item",
        skipDeletions = false,
        getId = { it.id },
    ) { r ->
        repository.upsertDispensacionItemFromRemote(r.toEntity())
    }

    suspend fun downloadDispensaciones(opticaId: String): Int = downloadTable<DispensacionRemota>(
        opticaId,
        TABLE_DISPENSACIONES,
        "dispensacion",
        skipDeletions = true,
        getId = { it.id },
    ) { r ->
        repository.upsertDispensacionFromRemote(r.toEntity())
    }

    suspend fun downloadServicios(opticaId: String): Int = downloadTable<ServicioRemoto>(
        opticaId,
        TABLE_SERVICIOS,
        "servicio_extra",
        skipDeletions = true,
        getId = { it.id },
    ) { r ->
        repository.upsertServicioFromRemote(r.toEntity())
    }

    suspend fun downloadPagos(opticaId: String): Int = downloadTable<PagoRemoto>(
        opticaId,
        TABLE_PAGOS,
        "pago",
        skipDeletions = true,
        getId = { it.id },
    ) { r ->
        repository.upsertPagoFromRemote(r.toEntity())
    }

    suspend fun downloadRegalos(opticaId: String): Int = downloadTable<RegaloDispensacionRemota>(
        opticaId,
        TABLE_REGALOS,
        "regalo_dispensacion",
        skipDeletions = true,
        getId = { it.id },
    ) { r ->
        repository.upsertRegaloFromRemote(r.toEntity())
    }

    suspend fun downloadResumenDiario(opticaId: String): Int = try {
        var remotos: List<ResumenDiarioRemoto> = emptyList()
        networkRetryHelper.retryNetwork("download:$TABLE_RESUMEN_DIARIO") {
            remotos = supabase.postgrest[TABLE_RESUMEN_DIARIO]
                .select { filter { eq("optica_id", opticaId) } }
                .decodeList<ResumenDiarioRemoto>()
        }
        remotos.forEach { r ->
            try {
                repository.withTransaction {
                    resumenDiarioDao.upsert(r.toEntity())
                    syncStateTracker.markSynced(opticaId, "resumen_diario", r.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.w(TAG, "resumen_diario upsert failed for ${r.id}", e)
            }
        }
        remotos.size
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.w(TAG, "resumen_diario download failed", e)
        0
    }

    suspend fun downloadGastosOperativos(opticaId: String): Int = downloadTable<GastoOperativoRemoto>(
        opticaId,
        TABLE_GASTOS_OPERATIVOS,
        "gasto_operativo",
        skipDeletions = true,
        getId = { it.id },
    ) { r ->
        repository.upsertGastoOperativoFromRemote(r.toEntity())
    }

    suspend fun downloadConfiguracionFinanciera(opticaId: String): Int = try {
        var remotos: List<ConfiguracionFinancieraRemoto> = emptyList()
        networkRetryHelper.retryNetwork("download:$TABLE_CONFIGURACION_FINANCIERA") {
            remotos = supabase.postgrest[TABLE_CONFIGURACION_FINANCIERA]
                .select { filter { eq("optica_id", opticaId) } }
                .decodeList<ConfiguracionFinancieraRemoto>()
        }
        remotos.forEach { r ->
            try {
                repository.withTransaction {
                    configuracionFinancieraDao.upsert(r.toEntity())
                    syncStateTracker.markSynced(opticaId, "configuracion_financiera", r.opticaId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.w(TAG, "configuracion_financiera upsert failed for ${r.opticaId}", e)
            }
        }
        remotos.size
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.w(TAG, "configuracion_financiera download failed", e)
        0
    }

    suspend fun downloadCostosProductos(opticaId: String): Int = downloadTable<CostoProductoRemoto>(
        opticaId,
        TABLE_COSTOS_PRODUCTOS,
        "costo_producto",
        skipDeletions = true,
        getId = { it.id },
    ) { r ->
        costoProductoDao.upsertAll(listOf(r.toEntity()))
    }

    suspend fun downloadCostosBiselado(opticaId: String): Int = downloadTable<CostoBiseladoRemoto>(
        opticaId,
        TABLE_COSTOS_BISELADO,
        "costo_biselado",
        skipDeletions = true,
        getId = { it.id },
    ) { r ->
        costoBiseladoDao.upsertAll(listOf(r.toEntity()))
    }
}
