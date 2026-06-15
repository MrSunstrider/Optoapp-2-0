package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
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
 * FASE 3 – Paso 3.1
 * Sincronización bidireccional de Pacientes.
 */
open class SyncPacientesUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient,
    private val syncStateTracker: com.example.optoapp.data.SyncStateTracker,
    private val conflictHelper: com.example.optoapp.domain.sync.ConflictHelper
) {

    companion object {
        private const val TAG = "SyncPacientes"
        private const val TABLE = "pacientes"
    }

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false
    ): Resource<PacientesSyncResult> {
        return try {
            Log.d(TAG, "Pacientes: inicio sync (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")
            val uploaded = if (skipUpload) 0 else upload(opticaId)
            val downloaded = if (downloadAfterUpload) download(opticaId) else 0
            Log.d(TAG, "Pacientes: fin OK (subidos=$uploaded, bajados=$downloaded)")
            Resource.Success(PacientesSyncResult(uploaded, downloaded))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red sincronizando pacientes: ${e.message}", e)
            Resource.Error("Error sincronizando pacientes: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado sincronizando pacientes: ${e.message}", e)
            Resource.Error("Error sincronizando pacientes: ${e.localizedMessage}")
        }
    }

    private suspend fun upload(opticaId: String): Int {
        val pacientes = repository.getPacientesSnapshotForOptica(opticaId)

        if (pacientes.isEmpty()) {
            Log.d(TAG, "Upload pacientes: 0 filas locales para optica_id=$opticaId")
            syncStateTracker.markSynced(opticaId, "upload_pacientes", "batch")
            return 0
        }

        val rows = pacientes.map { it.toRemoto().copy(opticaId = opticaId) }
        val remoteByHistoria = fetchRemoteHistoriaMap(opticaId)
        val localHistoriaKeys = mutableMapOf<String, String>()
        val filteredRows = mutableListOf<PacienteRemoto>()

        for (row in rows) {
            val historiaKey = normalizedHistoriaKey(row.historiaOptometrica)
            if (historiaKey == null) {
                filteredRows += row
                continue
            }

            val duplicatedLocalId = localHistoriaKeys[historiaKey]
            if (duplicatedLocalId != null && duplicatedLocalId != row.id) {
                syncStateTracker.markError(
                    opticaId,
                    "paciente",
                    row.id,
                    "HO duplicada local: ${row.historiaOptometrica}. Se omite para evitar conflicto único."
                )
                continue
            }
            localHistoriaKeys[historiaKey] = row.id

            val remoteId = remoteByHistoria[historiaKey]
            if (remoteId != null && remoteId != row.id) {
                syncStateTracker.markError(
                    opticaId,
                    "paciente",
                    row.id,
                    "HO ya existe en remoto con otro ID ($remoteId). Se omite para evitar conflicto único."
                )
                continue
            }
            filteredRows += row
        }

        val deduplicated = filteredRows.distinctBy { it.id }

        // Detección de conflictos: comparar updated_at local vs remoto
        val conflictSafe = conflictHelper.filterConflicts(
            tableName = TABLE,
            opticaId = opticaId,
            entityType = "paciente",
            localEntities = deduplicated.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt) }
        )
        val finalRows = deduplicated.filter { r -> conflictSafe.any { it.id == r.id } }

        if (finalRows.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_pacientes", "batch")
            return 0
        }
        Log.d(
            TAG,
            "Upload pacientes: ${finalRows.size}/${pacientes.size} filas tras prevalidación de HO, optica_id=$opticaId"
        )
        try {
            supabase.postgrest[TABLE].upsert(finalRows)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "Error en red subiendo pacientes: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_pacientes", "batch", e.message)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado subiendo pacientes: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_pacientes", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_pacientes", "batch")
        finalRows.forEach { p ->
            syncStateTracker.markSynced(opticaId, "paciente", p.id)
        }

        Log.d(TAG, "Subidos ${finalRows.size} pacientes a Supabase (optica_id=$opticaId).")
        return finalRows.size
    }

    private suspend fun download(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE]
            .select {
                filter { eq("optica_id", opticaId) }
            }
            .decodeList<PacienteRemoto>()

        if (remotos.isEmpty()) return 0

        remotos.forEach { remoto ->
            try {
                val local = remoto.toEntity()
                repository.upsertPaciente(local)
                syncStateTracker.markSynced(opticaId, "paciente", local.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red descargando paciente: ${e.message}", e)
                syncStateTracker.markError(opticaId, "paciente", remoto.id, e.message)
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado descargando paciente: ${e.message}", e)
                syncStateTracker.markError(opticaId, "paciente", remoto.id, e.message)
            }
        }

        Log.d(TAG, "Descargados ${remotos.size} pacientes desde Supabase.")
        return remotos.size
    }

    private suspend fun fetchRemoteHistoriaMap(opticaId: String): Map<String, String> {
        return runCatching {
            supabase.postgrest[TABLE]
                .select {
                    filter { eq("optica_id", opticaId) }
                }
                .decodeList<PacienteRemoto>()
                .mapNotNull { remoto ->
                    val key = normalizedHistoriaKey(remoto.historiaOptometrica) ?: return@mapNotNull null
                    key to remoto.id
                }
                .toMap()
        }.getOrDefault(emptyMap())
    }
}

@Serializable
data class PacienteRemoto(
    val id: String,
    @SerialName("nombre_completo") val nombreCompleto: String,
    val edad: Int,
    val telefono: String,
    @SerialName("fecha_creacion") val fechaCreacion: String,
    val dni: String? = null,
    @SerialName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null,
    val email: String? = null,
    @SerialName("historia_optometrica") val historiaOptometrica: String? = null,
    val direccion: String? = null,
    val distrito: String? = null,
    val ocupacion: String? = null,
    val acompanante: String? = null,
    val hobbies: String? = null,
    @SerialName("ultimas_etiquetas") val ultimasEtiquetas: String? = null,
    @SerialName("optica_id") val opticaId: String = "mi_optica_base",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
) {
    fun toEntity(): Paciente = Paciente(
        id               = id,
        nombreCompleto   = nombreCompleto,
        edad             = edad,
        telefono         = telefono,
        fechaCreacion    = LocalDate.parse(fechaCreacion),
        dni              = dni,
        fechaNacimiento  = fechaNacimiento?.let(LocalDate::parse),
        sexo             = sexo,
        email            = email,
        historiaOptometrica = historiaOptometrica,
        direccion        = direccion,
        distrito         = distrito,
        ocupacion        = ocupacion,
        acompanante      = acompanante,
        hobbies          = hobbies,
        ultimasEtiquetas = ultimasEtiquetas
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        opticaId         = opticaId.ifBlank { "mi_optica_base" },
        updatedAt        = updatedAt,
        updatedBy        = updatedBy
    )
}

private fun Paciente.toRemoto(): PacienteRemoto = PacienteRemoto(
    id                = id,
    nombreCompleto   = nombreCompleto.ifBlank { "-" },
    edad              = edad,
    telefono          = telefono.ifBlank { "-" },
    fechaCreacion    = fechaCreacion.toString(),
    dni               = dni ?: "",
    fechaNacimiento  = fechaNacimiento?.toString(),
    sexo              = sexo ?: "",
    email             = email ?: "",
    historiaOptometrica = historiaOptometrica ?: "",
    direccion         = direccion ?: "",
    distrito          = distrito ?: "",
    ocupacion         = ocupacion ?: "",
    acompanante       = acompanante ?: "",
    hobbies           = hobbies ?: "",
    ultimasEtiquetas = ultimasEtiquetas.joinToString(","),
    opticaId         = opticaId.ifBlank { "mi_optica_base" },
    updatedAt        = updatedAt,
    updatedBy        = updatedBy
)

data class PacientesSyncResult(val uploaded: Int, val downloaded: Int)
