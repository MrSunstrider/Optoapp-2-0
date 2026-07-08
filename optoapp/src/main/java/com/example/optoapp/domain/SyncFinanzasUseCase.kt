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
            var pagosUp = 0
            var ventasUp = 0
            var gastosUp = 0

            if (!skipUpload) {
                dispUp = safeUpload("dispensaciones") { uploadSyncCoordinator.uploadDispensaciones(opticaId) }
                Log.d(TAG, "Finanzas: upload dispensaciones=$dispUp")
                itemsUp = safeUpload("dispensacion_items") { uploadSyncCoordinator.uploadDispensacionItems(opticaId) }
                Log.d(TAG, "Finanzas: upload dispensacion_items=$itemsUp")
                servUp = safeUpload("servicios_extra") { uploadSyncCoordinator.uploadServicios(opticaId) }
                Log.d(TAG, "Finanzas: upload servicios_extra=$servUp")
                pagosUp = safeUpload("pagos") { uploadSyncCoordinator.uploadPagos(opticaId) }
                Log.d(TAG, "Finanzas: upload pagos=$pagosUp")
                ventasUp = safeUpload("ventas") { uploadSyncCoordinator.uploadVentas(opticaId) }
                Log.d(TAG, "Finanzas: upload ventas=$ventasUp")
                gastosUp = safeUpload("gastos_operativos") { uploadSyncCoordinator.uploadGastosOperativos(opticaId) }
                Log.d(TAG, "Finanzas: upload gastos_operativos=$gastosUp")
            }

            val dispDown: Int
            val itemsDown: Int
            val servDown: Int
            val ventasDown: Int
            val pagosDown: Int
            val resumenDown: Int
            val configDown: Int
            if (downloadAfterUpload) {
                dispDown = downloadSyncCoordinator.downloadDispensaciones(opticaId)
                Log.d(TAG, "Finanzas: download dispensaciones=$dispDown")
                itemsDown = downloadSyncCoordinator.downloadDispensacionItems(opticaId)
                Log.d(TAG, "Finanzas: download dispensacion_items=$itemsDown")
                servDown = downloadSyncCoordinator.downloadServicios(opticaId)
                Log.d(TAG, "Finanzas: download servicios_extra=$servDown")
                ventasDown = downloadSyncCoordinator.downloadVentas(opticaId)
                Log.d(TAG, "Finanzas: download ventas=$ventasDown")
                resumenDown = downloadSyncCoordinator.downloadResumenDiario(opticaId)
                Log.d(TAG, "Finanzas: download resumen_diario=$resumenDown")
                configDown = downloadSyncCoordinator.downloadConfiguracionFinanciera(opticaId)
                Log.d(TAG, "Finanzas: download configuracion_financiera=$configDown")
                pagosDown = downloadSyncCoordinator.downloadPagos(opticaId)
                Log.d(TAG, "Finanzas: download pagos=$pagosDown")
            } else {
                dispDown = 0
                itemsDown = 0
                servDown = 0
                ventasDown = 0
                pagosDown = 0
                resumenDown = 0
                configDown = 0
                Log.d(TAG, "Finanzas: fin upload-only OK")
            }

            Resource.Success(
                FinanzasSyncResult(
                    uploadedDispensaciones = dispUp,
                    uploadedDispensacionItems = itemsUp,
                    uploadedServicios = servUp,
                    uploadedPagos = pagosUp,
                    uploadedVentas = ventasUp,
                    uploadedGastosOperativos = gastosUp,
                    downloadedDispensaciones = dispDown,
                    downloadedDispensacionItems = itemsDown,
                    downloadedServicios = servDown,
                    downloadedPagos = pagosDown,
                    downloadedVentas = ventasDown,
                    downloadedResumenesDiarios = resumenDown,
                    downloadedConfiguracionesFinancieras = configDown
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
     */
    private suspend fun safeUpload(
        entityName: String,
        block: suspend () -> Int
    ): Int {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo $entityName: ${e.message}", e)
            0
        } catch (e: RestException) {
            // Auth/permission errors should NOT be silenced
            if (e.statusCode == 401 || e.statusCode == 403) {
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
