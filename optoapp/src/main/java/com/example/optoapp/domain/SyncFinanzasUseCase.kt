package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.Resource
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

            val dispUp = if (skipUpload) 0 else uploadSyncCoordinator.uploadDispensaciones(opticaId)
            Log.d(TAG, "Finanzas: upload dispensaciones=$dispUp")
            val itemsUp = if (skipUpload) 0 else uploadSyncCoordinator.uploadDispensacionItems(opticaId)
            Log.d(TAG, "Finanzas: upload dispensacion_items=$itemsUp")
            val servUp = if (skipUpload) 0 else uploadSyncCoordinator.uploadServicios(opticaId)
            Log.d(TAG, "Finanzas: upload servicios_extra=$servUp")
            val pagosUp = if (skipUpload) 0 else uploadSyncCoordinator.uploadPagos(opticaId)
            Log.d(TAG, "Finanzas: upload pagos=$pagosUp")
            if (!skipUpload) {
                val arqueosUp = uploadSyncCoordinator.uploadArqueos(opticaId)
                Log.d(TAG, "Finanzas: upload arqueo_caja=$arqueosUp")
            }

            val dispDown: Int
            val itemsDown: Int
            val servDown: Int
            val ventasDown: Int
            val pagosDown: Int
            if (downloadAfterUpload) {
                val arqueosDown = downloadSyncCoordinator.downloadArqueos(opticaId)
                Log.d(TAG, "Finanzas: download arqueo_caja=$arqueosDown")
                dispDown = downloadSyncCoordinator.downloadDispensaciones(opticaId)
                Log.d(TAG, "Finanzas: download dispensaciones=$dispDown")
                itemsDown = downloadSyncCoordinator.downloadDispensacionItems(opticaId)
                Log.d(TAG, "Finanzas: download dispensacion_items=$itemsDown")
                servDown = downloadSyncCoordinator.downloadServicios(opticaId)
                Log.d(TAG, "Finanzas: download servicios_extra=$servDown")
                ventasDown = downloadSyncCoordinator.downloadVentas(opticaId)
                Log.d(TAG, "Finanzas: download ventas=$ventasDown")
                pagosDown = downloadSyncCoordinator.downloadPagos(opticaId)
                Log.d(TAG, "Finanzas: download pagos=$pagosDown; fin OK")
            } else {
                dispDown = 0
                itemsDown = 0
                servDown = 0
                ventasDown = 0
                pagosDown = 0
                Log.d(TAG, "Finanzas: fin upload-only OK")
            }

            Resource.Success(
                FinanzasSyncResult(
                    uploadedDispensaciones = dispUp,
                    uploadedDispensacionItems = itemsUp,
                    uploadedServicios = servUp,
                    uploadedPagos = pagosUp,
                    downloadedDispensaciones = dispDown,
                    downloadedDispensacionItems = itemsDown,
                    downloadedServicios = servDown,
                    downloadedPagos = pagosDown,
                    downloadedVentas = ventasDown
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
}
