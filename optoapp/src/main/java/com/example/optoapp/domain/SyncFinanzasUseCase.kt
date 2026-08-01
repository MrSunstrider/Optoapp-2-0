package com.example.optoapp.domain

import com.example.optoapp.data.Resource
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject

/**
 * P0-T2 — Upload order: Dispensaciones → servicios_extra → pagos
 * (parents before payments; payments reference dispensación and/or service).
 * Download order: same, so parents exist before inserting local payments.
 */

/**
 * Thrown by upload methods to carry the partial uploaded count when an IOException
 * interrupts a chunked batch. [safeUpload] catches this and returns the partial count
 * instead of 0, so FinanzasSyncResult reflects actual progress.
 */
class UploadPartialException(
    val uploadedCount: Int,
    cause: IOException,
) : IOException("Partial upload: $uploadedCount succeeded before error", cause)
open class SyncFinanzasUseCase @Inject constructor(
    private val deletionSyncHelper: DeletionSyncHelper,
    private val uploadSyncCoordinator: UploadSyncCoordinator,
    private val downloadSyncCoordinator: DownloadSyncCoordinator,
    private val networkRetryHelper: NetworkRetryHelper,
) {
    companion object {
        private const val TAG = "SyncFinanzas"
    }

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false,
    ): Resource<FinanzasSyncResult> = try {
        AppLogger.d(TAG, "Finanzas: inicio (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")

        deletionSyncHelper.pushPendingDeletions(opticaId)

        var dispUp = 0
        var itemsUp = 0
        var servUp = 0
        var costosUp = 0
        var biseladoUp = 0
        var pagosUp = 0
        var gastosUp = 0
        var regalosUp = 0

        if (!skipUpload) {
            dispUp = safeUpload("dispensaciones") { uploadSyncCoordinator.uploadDispensaciones(opticaId) }
            AppLogger.d(TAG, "Finanzas: upload dispensaciones=$dispUp")
            itemsUp = safeUpload("dispensacion_items") { uploadSyncCoordinator.uploadDispensacionItems(opticaId) }
            AppLogger.d(TAG, "Finanzas: upload dispensacion_items=$itemsUp")
            servUp = safeUpload("servicios_extra") { uploadSyncCoordinator.uploadServicios(opticaId) }
            AppLogger.d(TAG, "Finanzas: upload servicios_extra=$servUp")
            costosUp = safeUpload("costos_productos") { uploadSyncCoordinator.uploadCostosProductos(opticaId) }
            AppLogger.d(TAG, "Finanzas: upload costos_productos=$costosUp")
            biseladoUp = safeUpload("costos_biselado") { uploadSyncCoordinator.uploadCostosBiselado(opticaId) }
            AppLogger.d(TAG, "Finanzas: upload costos_biselado=$biseladoUp")
            pagosUp = safeUpload("pagos") { uploadSyncCoordinator.uploadPagos(opticaId) }
            AppLogger.d(TAG, "Finanzas: upload pagos=$pagosUp")
            gastosUp = safeUpload("gastos_operativos") { uploadSyncCoordinator.uploadGastosOperativos(opticaId) }
            AppLogger.d(TAG, "Finanzas: upload gastos_operativos=$gastosUp")
            regalosUp = safeUpload("regalos") { uploadSyncCoordinator.uploadRegalos(opticaId) }
            AppLogger.d(TAG, "Finanzas: upload regalos=$regalosUp")
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
            dispDown = safeDownload("dispensaciones") { downloadSyncCoordinator.downloadDispensaciones(opticaId) }
            AppLogger.d(TAG, "Finanzas: download dispensaciones=$dispDown")
            itemsDown = safeDownload("dispensacion_items") { downloadSyncCoordinator.downloadDispensacionItems(opticaId) }
            AppLogger.d(TAG, "Finanzas: download dispensacion_items=$itemsDown")
            servDown = safeDownload("servicios_extra") { downloadSyncCoordinator.downloadServicios(opticaId) }
            AppLogger.d(TAG, "Finanzas: download servicios_extra=$servDown")
            resumenDown = safeDownload("resumen_diario") { downloadSyncCoordinator.downloadResumenDiario(opticaId) }
            AppLogger.d(TAG, "Finanzas: download resumen_diario=$resumenDown")
            configDown = safeDownload("configuracion_financiera") { downloadSyncCoordinator.downloadConfiguracionFinanciera(opticaId) }
            AppLogger.d(TAG, "Finanzas: download configuracion_financiera=$configDown")
            costosDown = safeDownload("costos_productos") { downloadSyncCoordinator.downloadCostosProductos(opticaId) }
            AppLogger.d(TAG, "Finanzas: download costos_productos=$costosDown")
            biseladoDown = safeDownload("costos_biselado") { downloadSyncCoordinator.downloadCostosBiselado(opticaId) }
            AppLogger.d(TAG, "Finanzas: download costos_biselado=$biseladoDown")
            pagosDown = safeDownload("pagos") { downloadSyncCoordinator.downloadPagos(opticaId) }
            AppLogger.d(TAG, "Finanzas: download pagos=$pagosDown")
            regalosDown = safeDownload("regalos") { downloadSyncCoordinator.downloadRegalos(opticaId) }
            AppLogger.d(TAG, "Finanzas: download regalos=$regalosDown")
            gastosDown = safeDownload("gastos_operativos") { downloadSyncCoordinator.downloadGastosOperativos(opticaId) }
            AppLogger.d(TAG, "Finanzas: download gastos_operativos=$gastosDown")
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
            AppLogger.d(TAG, "Finanzas: fin upload-only OK")
        }

        Resource.Success(
            FinanzasSyncResult(
                uploadedDispensaciones = dispUp,
                uploadedDispensacionItems = itemsUp,
                uploadedServicios = servUp,
                uploadedCostosProductos = costosUp,
                uploadedCostosBiselado = biseladoUp,
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
                downloadedGastosOperativos = gastosDown,
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppLogger.e(TAG, "Error en red sincronizando finanzas: ${e.message}", e)
        Resource.Error("Error sincronizando finanzas: ${e.localizedMessage}")
    } catch (e: Exception) {
        AppLogger.e(TAG, "Error inesperado sincronizando finanzas: ${e.message}", e)
        Resource.Error("Error sincronizando finanzas: ${e.localizedMessage}")
    }

    // Isolated try-catch so one entity's failure doesn't block other downloads
    private suspend fun safeDownload(name: String, block: suspend () -> Int): Int = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.e(TAG, "Download $name failed", e)
        0
    }

    /**
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
        block: suspend () -> Int,
    ): Int = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: UploadPartialException) {
        AppLogger.w(TAG, "Upload parcial de $entityName: ${e.uploadedCount} subidos antes del error", e)
        e.uploadedCount
    } catch (e: IOException) {
        AppLogger.e(TAG, "Error en red subiendo $entityName: ${e.message}", e)
        var lastError = e
        repeat(3) { attempt ->
            val backoffMs = 1000L * (1L shl attempt)
            delay(backoffMs)
            try {
                val retryResult = block()
                return retryResult
            } catch (e2: CancellationException) {
                throw e2
            } catch (e2: UploadPartialException) {
                AppLogger.w(TAG, "Upload parcial de $entityName en reintento: ${e2.uploadedCount} subidos antes del error", e2)
                return e2.uploadedCount
            } catch (e2: IOException) {
                lastError = e2
                AppLogger.e(TAG, "Error en red subiendo $entityName (intento ${attempt + 2}): ${e2.message}", e2)
            }
        }
        throw lastError
    } catch (e: RestException) {
        AppLogger.e(TAG, "Error REST subiendo $entityName (${e.statusCode}): ${e.message}", e)
        throw e
    } catch (e: Exception) {
        AppLogger.e(TAG, "Error inesperado subiendo $entityName: ${e.message}", e)
        throw e
    }
}
