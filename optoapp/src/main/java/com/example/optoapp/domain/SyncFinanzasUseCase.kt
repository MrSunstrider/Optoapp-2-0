package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.Resource
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * FASE 3 – Paso 3.3
 * Sincronización de Dispensaciones, Pagos y Servicios Extra.
 *
 * P0-T2 — Orden de subida obligatorio: **Dispensaciones → servicios_extra → pagos**
 * (padres antes que pagos; pagos referencian dispensación y/o servicio).
 *
 * Orden de bajada: el mismo, para que existan padres antes de insertar pagos locales.
 *
 * Delega la ejecución a helpers extraídos para mantener la clase por debajo de 250 líneas.
 */

/**
 * Thrown by upload methods to carry the partial uploaded count when an IOException
 * interrupts a chunked batch. [safeUpload] catches this and returns the partial count
 * instead of 0, so FinanzasSyncResult reflects actual progress.
 */
class UploadPartialException(
    val uploadedCount: Int,
    cause: IOException
) : IOException("Partial upload: $uploadedCount succeeded before error", cause)
open class SyncFinanzasUseCase @Inject constructor(
    private val deletionSyncHelper: DeletionSyncHelper,
    private val uploadSyncCoordinator: UploadSyncCoordinator,
    private val downloadSyncCoordinator: DownloadSyncCoordinator,
    private val networkRetryHelper: NetworkRetryHelper
) {
    companion object {
        private const val TAG = "SyncFinanzas"
    }

    /**
     * Ejecuta la sincronización completa del módulo financiero (Upload -> Download).
     */
    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false
    ): Resource<FinanzasSyncResult> {
        return try {
            Log.d(TAG, "Finanzas: inicio (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")

            deletionSyncHelper.pushPendingDeletions(opticaId)

            var dispUp = 0
            var itemsUp = 0
            var servUp = 0
            var costosUp = 0
            var pagosUp = 0
            var gastosUp = 0
            var regalosUp = 0

            if (!skipUpload) {
                dispUp = safeUpload("dispensaciones") { uploadSyncCoordinator.uploadDispensaciones(opticaId) }
                Log.d(TAG, "Finanzas: upload dispensaciones=$dispUp")
                itemsUp = safeUpload("dispensacion_items") { uploadSyncCoordinator.uploadDispensacionItems(opticaId) }
                Log.d(TAG, "Finanzas: upload dispensacion_items=$itemsUp")
                servUp = safeUpload("servicios_extra") { uploadSyncCoordinator.uploadServicios(opticaId) }
                Log.d(TAG, "Finanzas: upload servicios_extra=$servUp")
                costosUp = safeUpload("costos_productos") { uploadSyncCoordinator.uploadCostosProductos(opticaId) }
                Log.d(TAG, "Finanzas: upload costos_productos=$costosUp")
                pagosUp = safeUpload("pagos") { uploadSyncCoordinator.uploadPagos(opticaId) }
                Log.d(TAG, "Finanzas: upload pagos=$pagosUp")
                gastosUp = safeUpload("gastos_operativos") { uploadSyncCoordinator.uploadGastosOperativos(opticaId) }
                Log.d(TAG, "Finanzas: upload gastos_operativos=$gastosUp")
                regalosUp = safeUpload("regalos") { uploadSyncCoordinator.uploadRegalos(opticaId) }
                Log.d(TAG, "Finanzas: upload regalos=$regalosUp")
            }

            val dispDown: Int
            val itemsDown: Int
            val servDown: Int
            val pagosDown: Int
            val regalosDown: Int
            val costosDown: Int
            val biseladoDown: Int
            val resumenDown: Int
            val configDown: Int
            val gastosDown: Int
            if (downloadAfterUpload) {
                dispDown = downloadSyncCoordinator.downloadDispensaciones(opticaId)
                Log.d(TAG, "Finanzas: download dispensaciones=$dispDown")
                itemsDown = downloadSyncCoordinator.downloadDispensacionItems(opticaId)
                Log.d(TAG, "Finanzas: download dispensacion_items=$itemsDown")
                servDown = downloadSyncCoordinator.downloadServicios(opticaId)
                Log.d(TAG, "Finanzas: download servicios_extra=$servDown")
                resumenDown = downloadSyncCoordinator.downloadResumenDiario(opticaId)
                Log.d(TAG, "Finanzas: download resumen_diario=$resumenDown")
                configDown = downloadSyncCoordinator.downloadConfiguracionFinanciera(opticaId)
                Log.d(TAG, "Finanzas: download configuracion_financiera=$configDown")
                costosDown = downloadSyncCoordinator.downloadCostosProductos(opticaId)
                Log.d(TAG, "Finanzas: download costos_productos=$costosDown")
                biseladoDown = downloadSyncCoordinator.downloadCostosBiselado(opticaId)
                Log.d(TAG, "Finanzas: download costos_biselado=$biseladoDown")
                pagosDown = downloadSyncCoordinator.downloadPagos(opticaId)
                Log.d(TAG, "Finanzas: download pagos=$pagosDown")
                regalosDown = downloadSyncCoordinator.downloadRegalos(opticaId)
                Log.d(TAG, "Finanzas: download regalos=$regalosDown")
                gastosDown = downloadSyncCoordinator.downloadGastosOperativos(opticaId)
                Log.d(TAG, "Finanzas: download gastos_operativos=$gastosDown")
            } else {
                dispDown = 0
                itemsDown = 0
                servDown = 0
                costosDown = 0
                biseladoDown = 0
                pagosDown = 0
                regalosDown = 0
                resumenDown = 0
                configDown = 0
                gastosDown = 0
                Log.d(TAG, "Finanzas: fin upload-only OK")
            }

            Resource.Success(
                FinanzasSyncResult(
                    uploadedDispensaciones = dispUp,
                    uploadedDispensacionItems = itemsUp,
                    uploadedServicios = servUp,
                    uploadedCostosProductos = costosUp,
                    uploadedPagos = pagosUp,
                    uploadedGastosOperativos = gastosUp,
                    uploadedRegalos = regalosUp,
                    downloadedDispensaciones = dispDown,
                    downloadedDispensacionItems = itemsDown,
                    downloadedServicios = servDown,
                    downloadedPagos = pagosDown,
                    downloadedRegalos = regalosDown,
                    downloadedCostosProductos = costosDown,
                    downloadedCostosBiselado = biseladoDown,
                    downloadedResumenesDiarios = resumenDown,
                    downloadedConfiguracionesFinancieras = configDown,
                    downloadedGastosOperativos = gastosDown
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red sincronizando finanzas: ${e.message}", e)
            Resource.Error("Error sincronizando finanzas: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado sincronizando finanzas: ${e.message}", e)
            Resource.Error("Error sincronizando finanzas: ${e.localizedMessage}")
        }
    }

    /**
     * Ejecuta un paso de upload individual; si falla, registra el error y retorna 0
     * para que los pasos restantes puedan continuar.
     *
     * NOTE: counts may be 0 even when partial data was uploaded before the error.
     * The per-chunk count is propagated via [UploadPartialException] for methods
     * that use [UploadSyncCoordinator.executeSimpleUpsert].
     *
     * H7: Partial upload count limitation is accepted because:
     * 1. Partial uploads are idempotent — unconfirmed rows are re-uploaded on the next sync cycle.
     * 2. Marking partial state per chunk would require API changes to upload coordinator
     *    return types (currently returns Int, not a per-chunk summary).
     * When [executeSimpleUpsert] throws IOException, the exception propagates through the
     * upload method and is caught here, so the chunk-level count is lost. This is acceptable
     * as long as idempotency holds.
     */
    private suspend fun safeUpload(
        entityName: String,
        block: suspend () -> Int
    ): Int {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UploadPartialException) {
            Log.w(TAG, "Upload parcial de $entityName: ${e.uploadedCount} subidos antes del error", e)
            e.uploadedCount
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo $entityName: ${e.message}", e)
            0
        } catch (e: RestException) {
            // Auth/permission errors should NOT be silenced
            if (e.statusCode == 401 || e.statusCode == 403 || e.statusCode == 409) {
                throw e
            }
            Log.e(TAG, "Error REST subiendo $entityName (${e.statusCode}): ${e.message}", e)
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo $entityName: ${e.message}", e)
            0
        }
    }
}
