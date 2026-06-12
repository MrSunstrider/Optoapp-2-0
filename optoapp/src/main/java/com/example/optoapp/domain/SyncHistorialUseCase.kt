package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import com.example.optoapp.sync.errorLabelForException
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.time.Instant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

/**
 * FASE 3 – Paso 3.2
 * Sincronización de Evaluaciones Clínicas (historial clínico).
 *
 * DTOs (EvaluacionRemota, HistorialSyncResult) y extensiones (toRemoto) viven en SyncHistorialDto.kt.
 *
 * La orquestación upload → download ocurre en [invoke].
 * La lógica de FK-map se delega a [buildUploadRows] (función pura testeable).
 */
open class SyncHistorialUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: com.example.optoapp.data.SyncStateTracker,
    private val conflictHelper: com.example.optoapp.domain.sync.ConflictHelper
) {

    companion object {
        private const val TAG = "SyncHistorial"
        private const val TABLE = "evaluaciones"
    }

    /**
     * Ejecuta la sincronización completa: upload local → download remoto.
     */
    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true
    ): Resource<HistorialSyncResult> {
        return try {
            Log.d(TAG, "Evaluaciones: inicio sync (opticaId=$opticaId, download=$downloadAfterUpload)")
            val uploaded = uploadEvaluaciones(opticaId)
            val downloaded = if (downloadAfterUpload) downloadEvaluaciones(opticaId) else 0
            Log.d(TAG, "Evaluaciones: fin OK (subidas=$uploaded, bajadas=$downloaded)")
            Resource.Success(HistorialSyncResult(uploaded, downloaded))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red sincronizando evaluaciones: ${e.message}", e)
            Resource.Error("Error sincronizando historial clínico: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado sincronizando evaluaciones: ${e.message}", e)
            Resource.Error("Error sincronizando historial clínico: ${e.localizedMessage}")
        }
    }

    private suspend fun uploadEvaluaciones(opticaId: String): Int {
        val evaluaciones = repository.getEvaluacionesSnapshotForOptica(opticaId)

        if (evaluaciones.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_evaluaciones", "batch")
            return 0
        }

        val localPacientes = repository.getPacientesSnapshotForOptica(opticaId)
        var remotePacientes = runCatching {
            supabase.postgrest["pacientes"]
                .select {
                    filter { eq("optica_id", opticaId) }
                }
                .decodeList<PacienteRemoto>()
        }.onFailure { e ->
            Log.w(TAG, "Error al consultar pacientes remotos para FK check: ${e.localizedMessage}")
        }.getOrDefault(emptyList())

        // Self-healing: si hay evaluaciones cuyo paciente existe localmente pero no en remoto,
        // subir ese paciente primero para evitar el error de FK.
        val remotePacienteIds = remotePacientes.map { it.id }.toSet()
        val localPacienteIds = localPacientes.map { it.id }.toSet()
        val orphanPacienteIds = evaluaciones
            .map { it.pacienteId }
            .distinct()
            .filter { pid -> pid !in remotePacienteIds && pid in localPacienteIds }

        if (orphanPacienteIds.isNotEmpty()) {
            Log.w(TAG, "Self-heal: ${orphanPacienteIds.size} pacientes locales no existen en remoto. Subiendo antes de evaluaciones.")
            val orphanPacientes = localPacientes.filter { it.id in orphanPacienteIds }
            val pacienteRows = orphanPacientes.map { p ->
                PacienteRemoto(
                    id = p.id,
                    nombreCompleto = p.nombreCompleto.ifBlank { "-" },
                    edad = p.edad,
                    telefono = p.telefono.ifBlank { "-" },
                    fechaCreacion = p.fechaCreacion.toString(),
                    dni = p.dni ?: "",
                    fechaNacimiento = p.fechaNacimiento?.toString(),
                    sexo = p.sexo ?: "",
                    email = p.email ?: "",
                    historiaOptometrica = p.historiaOptometrica ?: "",
                    direccion = p.direccion ?: "",
                    distrito = p.distrito ?: "",
                    ocupacion = p.ocupacion ?: "",
                    acompanante = p.acompanante ?: "",
                    hobbies = p.hobbies ?: "",
                    ultimasEtiquetas = p.ultimasEtiquetas.joinToString(","),
                    opticaId = opticaId,
                    updatedAt = Instant.now().toString()
                )
            }
            runCatching {
                supabase.postgrest["pacientes"].upsert(pacienteRows)
            }.onFailure { e ->
                Log.e(TAG, "Self-heal: error al subir pacientes huérfanos: ${e.localizedMessage}")
            }

            // Re-consultar pacientes remotos para incluir los recién subidos
            remotePacientes = runCatching {
                supabase.postgrest["pacientes"]
                    .select {
                        filter { eq("optica_id", opticaId) }
                    }
                    .decodeList<PacienteRemoto>()
            }.onFailure { e ->
                Log.e(TAG, "Self-heal: error al re-consultar pacientes remotos: ${e.localizedMessage}")
            }.getOrDefault(remotePacientes)
        }

        val builtRows = buildUploadRows(evaluaciones, localPacientes, remotePacientes, opticaId)

        val inputIds = evaluaciones.map { it.id }.toSet()
        val outputIds = builtRows.map { it.id }.toSet()
        (inputIds - outputIds).forEach { skippedId ->
            syncStateTracker.markError(
                opticaId, "evaluacion", skippedId,
                "Paciente remoto inexistente. Se omite para evitar FK."
            )
        }

        // Detección de conflictos
        val conflictSafe = conflictHelper.filterConflicts(
            tableName = TABLE,
            opticaId = opticaId,
            entityType = "evaluacion",
            localEntities = builtRows.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt) }
        )
        val finalRows = builtRows.filter { r -> conflictSafe.any { it.id == r.id } }

        if (finalRows.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_evaluaciones", "batch")
            return 0
        }
        try {
            supabase.postgrest[TABLE].upsert(finalRows)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo evaluaciones: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_evaluaciones", "batch", e.message)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo evaluaciones: ${e.message}", e)
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

    private suspend fun downloadEvaluaciones(opticaId: String): Int {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red descargando evaluación: ${e.message}", e)
                syncStateTracker.markError(opticaId, "evaluacion", remoto.id, e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando evaluación: ${e.message}", e)
                syncStateTracker.markError(opticaId, "evaluacion", remoto.id, e.message)
            }
        }

        Log.d(TAG, "Descargadas ${remotos.size} evaluaciones desde Supabase.")
        return remotos.size
    }
}

/**
 * Construye las filas [EvaluacionRemota] listas para upsert a Supabase,
 * mapeando FK de pacientes locales → remotos vía historia optométrica.
 *
 * Es una función pura (sin side effects) para facilitar tests unitarios.
 *
 * @param evaluaciones evaluaciones locales a subir
 * @param localPacientes pacientes locales (para leer historiaOptometrica)
 * @param remotePacientes pacientes remotos (para resolver FK)
 * @param opticaId óptica a la que pertenecen los datos
 * @return lista de filas deduplicadas por id, lista para upsert
 */
internal fun buildUploadRows(
    evaluaciones: List<EvaluacionClinica>,
    localPacientes: List<Paciente>,
    remotePacientes: List<PacienteRemoto>,
    opticaId: String
): List<EvaluacionRemota> {
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
        if (finalPacienteId !in remotePacienteIds) return@mapNotNull null
        ev.toRemoto().copy(opticaId = opticaId, pacienteId = finalPacienteId)
    }

    return rows.distinctBy { it.id }
}

/**
 * Normaliza una historia clínica para comparación como clave única.
 *
 * Aplica trim + uppercase, y retorna `null` si el resultado es blank.
 * Usada por [SyncHistorialUseCase] y [SyncPacientesUseCase] para mapear
 * pacientes locales a remotos vía su historia optométrica.
 */
internal fun normalizedHistoriaKey(historia: String?): String? {
    val normalized = historia?.trim()?.uppercase().orEmpty()
    return normalized.ifBlank { null }
}

// PacienteRemoto is defined in SyncPacientesUseCase.kt (same package).
// It's reused here for the FK check during evaluaciones upload.
