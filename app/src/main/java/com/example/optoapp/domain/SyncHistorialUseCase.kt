package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.util.rethrowIfCancellation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject

/**
 * FASE 3 – Paso 3.2
 * Sincronización de Evaluaciones Clínicas (historial clínico).
 *
 * DTOs (EvaluacionRemota, HistorialSyncResult) y extensiones (toRemoto) viven en SyncHistorialDto.kt.
 */
class SyncHistorialUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: com.example.optoapp.data.SyncStateTracker
) {

    companion object {
        private const val TAG = "SyncHistorial"
        private const val TABLE = "evaluaciones"
    }

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true
    ): Resource<HistorialSyncResult> {
        return try {
            Log.d(TAG, "Evaluaciones: inicio sync (opticaId=$opticaId, download=$downloadAfterUpload)")
            val uploaded = upload(opticaId)
            val downloaded = if (downloadAfterUpload) download(opticaId) else 0
            Log.d(TAG, "Evaluaciones: fin OK (subidas=$uploaded, bajadas=$downloaded)")
            Resource.Success(HistorialSyncResult(uploaded, downloaded))
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            Log.e(TAG, "Error en sincronización de evaluaciones", e)
            Resource.Error("Error sincronizando historial clínico: ${e.localizedMessage}")
        }
    }

    private suspend fun upload(opticaId: String): Int {
        val evaluaciones = repository.getEvaluacionesSnapshotForOptica(opticaId)

        if (evaluaciones.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_evaluaciones", "batch")
            return 0
        }

        val localPacientes = repository.getPacientesSnapshotForOptica(opticaId)
        val remotePacientes = runCatching {
            supabase.postgrest["pacientes"]
                .select {
                    filter { eq("optica_id", opticaId) }
                }
                .decodeList<PacienteRemoto>()
        }.onFailure { e ->
            Log.w(TAG, "Error al consultar pacientes remotos para FK check: ${e.localizedMessage}")
        }.getOrDefault(emptyList())

        val remoteByHistoria = remotePacientes
            .mapNotNull { rp ->
                val key = normalizedHistoriaKey(rp.historiaOptometrica) ?: return@mapNotNull null
                key to rp.id
            }
            .toMap()

        val remapPacienteId = localPacientes.mapNotNull { lp ->
            val key = normalizedHistoriaKey(lp.historiaOptometrica) ?: return@mapNotNull null
            val remoteId = remoteByHistoria[key] ?: return@mapNotNull null
            if (remoteId == lp.id) return@mapNotNull null
            lp.id to remoteId
        }.toMap()

        val remotePacienteIds = remotePacientes.map { it.id }.toSet()
        val rows = evaluaciones.mapNotNull { ev ->
            val finalPacienteId = remapPacienteId[ev.pacienteId] ?: ev.pacienteId
            if (finalPacienteId !in remotePacienteIds) {
                syncStateTracker.markError(
                    opticaId,
                    "evaluacion",
                    ev.id,
                    "Paciente remoto inexistente para paciente_id=$finalPacienteId. Se omite para evitar FK."
                )
                return@mapNotNull null
            }
            ev.toRemoto().copy(opticaId = opticaId, pacienteId = finalPacienteId)
        }

        val finalRows = rows.distinctBy { it.id }
        if (finalRows.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_evaluaciones", "batch")
            return 0
        }
        try {
            supabase.postgrest[TABLE].upsert(finalRows)
        } catch (e: Exception) {
            rethrowIfCancellation(e)
            syncStateTracker.markError(opticaId, "upload_evaluaciones", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_evaluaciones", "batch")
        evaluaciones.forEach { ev ->
            syncStateTracker.markSynced(opticaId, "evaluacion", ev.id)
        }

        Log.d(TAG, "Subidas ${evaluaciones.size} evaluaciones a Supabase (forzando ID: $opticaId).")
        return finalRows.size
    }

    private suspend fun download(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE]
            .select {
                filter { eq("optica_id", opticaId) }
            }
            .decodeList<EvaluacionRemota>()

        if (remotos.isEmpty()) return 0

        remotos.forEach { remoto ->
            try {
                val local = remoto.toEntity()
                repository.insertEvaluacion(local)
                syncStateTracker.markSynced(opticaId, "evaluacion", local.id)
            } catch (e: Exception) {
                rethrowIfCancellation(e)
                syncStateTracker.markError(opticaId, "evaluacion", remoto.id, e.message)
            }
        }

        Log.d(TAG, "Descargadas ${remotos.size} evaluaciones desde Supabase.")
        return remotos.size
    }

    internal fun normalizedHistoriaKey(historia: String?): String? {
        val normalized = historia?.trim()?.uppercase().orEmpty()
        return normalized.ifBlank { null }
    }
}

// PacienteRemoto is defined in SyncPacientesUseCase.kt (same package).
// It's reused here for the FK check during evaluaciones upload.
