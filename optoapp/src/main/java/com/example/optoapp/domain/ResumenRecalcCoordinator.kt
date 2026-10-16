package com.example.optoapp.domain

import com.example.optoapp.data.PendingRecalcMarker
import com.example.optoapp.data.SyncStateTracker
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class VentanaRecalc(val desde: LocalDate, val hasta: LocalDate) {
    fun contiene(fecha: LocalDate): Boolean = !fecha.isBefore(desde) && !fecha.isAfter(hasta)
}

// Eligibility uses markedAt rather than the sync start time so a day whose RPC failed is retried
// by any later sync, even one that uploads nothing.
open class ResumenRecalcCoordinator @Inject constructor(
    private val syncStateTracker: SyncStateTracker,
    private val networkRetryHelper: NetworkRetryHelper,
    private val supabase: SupabaseClient,
) {
    // Best-effort: a failed recompute must never fail the finanzas sync.
    suspend fun flush(opticaId: String): Int = try {
        val elegibles = syncStateTracker.pendingRecalcMarkers(opticaId).filter { esElegible(opticaId, it) }
        ventanas(elegibles.map { it.fecha }).sumOf { ventana -> recalcularVentana(opticaId, ventana, elegibles) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppLogger.w(TAG, "resumen_diario range recalc skipped (non-fatal): ${e.message}", e)
        0
    }

    private suspend fun recalcularVentana(opticaId: String, ventana: VentanaRecalc, elegibles: List<PendingRecalcMarker>): Int =
        try {
            networkRetryHelper.retryNetwork("rpc:$RPC_RANGO:${ventana.desde}") {
                recalcularRango(opticaId, ventana.desde, ventana.hasta)
            }
            elegibles.filter { ventana.contiene(it.fecha) }.forEach { syncStateTracker.clearPendingRecalc(opticaId, it) }
            (ChronoUnit.DAYS.between(ventana.desde, ventana.hasta) + 1).toInt()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(TAG, "$RPC_RANGO ${ventana.desde}..${ventana.hasta} failed; markers kept: ${e.message}", e)
            0
        }

    internal open suspend fun recalcularRango(opticaId: String, desde: LocalDate, hasta: LocalDate) {
        val params = buildJsonObject {
            put("p_optica_id", opticaId)
            put("p_desde", desde.toString())
            put("p_hasta", hasta.toString())
        }
        supabase.postgrest.rpc(RPC_RANGO, params)
    }

    private suspend fun esElegible(opticaId: String, marker: PendingRecalcMarker): Boolean {
        val lotes = LOTES_HIJOS[marker.causeType] ?: return false
        if (!sincronizadoDesde(opticaId, marker.causeType, marker.causeId, marker.markedAt)) return false
        return lotes.all { sincronizadoDesde(opticaId, it, LOTE_ID, marker.markedAt) }
    }

    private suspend fun sincronizadoDesde(opticaId: String, entityType: String, entityId: String, markedAt: Long): Boolean {
        val estado = syncStateTracker.stateOf(opticaId, entityType, entityId) ?: return false
        return estado.status == ESTADO_SYNCED && estado.updatedAt >= markedAt
    }

    companion object {
        private const val TAG = "ResumenRecalc"
        private const val RPC_RANGO = "recalcular_resumen_diario_rango"
        private const val ESTADO_SYNCED = "synced"
        private const val LOTE_ID = "batch"

        // The RPC rejects longer ranges from members without the admin/gerente role.
        const val MAX_DIAS_VENTANA = 31L

        private val LOTES_HIJOS = mapOf(
            ResumenRecalcMarker.CAUSA_DISPENSACION to listOf("upload_dispensacion_items", "upload_regalos"),
            ResumenRecalcMarker.CAUSA_SERVICIO_EXTRA to listOf("upload_servicio_extra_items", "upload_regalos_servicio_extra"),
        )

        fun ventanas(fechas: Collection<LocalDate>): List<VentanaRecalc> {
            val ventanas = mutableListOf<VentanaRecalc>()
            fechas.distinct().sorted().forEach { fecha ->
                val ultima = ventanas.lastOrNull()
                if (ultima != null && ultima.hasta.plusDays(1) == fecha && ChronoUnit.DAYS.between(ultima.desde, fecha) < MAX_DIAS_VENTANA) {
                    ventanas[ventanas.lastIndex] = ultima.copy(hasta = fecha)
                } else {
                    ventanas += VentanaRecalc(fecha, fecha)
                }
            }
            return ventanas
        }
    }
}
