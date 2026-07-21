package com.example.optoapp.domain

import com.example.optoapp.data.ConflictDao
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SyncEntityState
import com.example.optoapp.domain.sync.EntitySnapshotSerializer
import com.example.optoapp.util.AppLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
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
    private val conflictHelper: com.example.optoapp.domain.sync.ConflictHelper,
    private val conflictDao: com.example.optoapp.data.ConflictDao,
) {

    companion object {
        private const val TAG = "SyncPacientes"
        private const val TABLE = "pacientes"
    }

    suspend operator fun invoke(
        opticaId: String,
        downloadAfterUpload: Boolean = true,
        skipUpload: Boolean = false,
    ): Resource<PacientesSyncResult> = try {
        AppLogger.d(TAG, "Pacientes: inicio sync (opticaId=$opticaId, download=$downloadAfterUpload, skipUpload=$skipUpload)")
        val uploaded = if (skipUpload) 0 else upload(opticaId)
        val downloaded = if (downloadAfterUpload) download(opticaId) else 0
        AppLogger.d(TAG, "Pacientes: fin OK (subidos=$uploaded, bajados=$downloaded)")
        Resource.Success(PacientesSyncResult(uploaded, downloaded))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppLogger.e(TAG, "Error en red sincronizando pacientes: ${e.message}", e)
        Resource.Error("Error sincronizando pacientes: ${e.localizedMessage}")
    } catch (e: Exception) {
        AppLogger.e(TAG, "Error inesperado sincronizando pacientes: ${e.message}", e)
        Resource.Error("Error sincronizando pacientes: ${e.localizedMessage}")
    }

    private suspend fun upload(opticaId: String): Int {
        val pacientes = repository.getPacientesSnapshotForOptica(opticaId)

        if (pacientes.isEmpty()) {
            AppLogger.d(TAG, "Upload pacientes: 0 filas locales para optica_id=$opticaId")
            syncStateTracker.markSynced(opticaId, "upload_pacientes", "batch")
            return 0
        }

        val rows = pacientes.map { it.toRemoto().copy(opticaId = opticaId) }

        // Fetch all remote pacientes once and derive both dedup and conflict maps.
        // null = network error, empty list = genuinely no remote data.
        val rawRemoteRows = fetchAllRemotePacientes(opticaId)
        val batchFetchFailed = rawRemoteRows == null
        val allRemoteRows = rawRemoteRows ?: emptyList()
        val remoteByHistoria = allRemoteRows
            .mapNotNull { remoto ->
                val key = normalizedHistoriaKey(remoto.historiaOptometrica) ?: return@mapNotNull null
                key to remoto.id
            }
            .toMap()
        val remoteUpdatedAtMap = allRemoteRows
            .mapNotNull { remoto -> remoto.updatedAt?.let { ts -> remoto.id to ts } }
            .toMap()

        // When the batch fetch definitively failed, pass null so filterConflicts
        // falls back to per-entity conflict checking. When it succeeded (even with
        // zero remote rows), the empty map correctly signals "no remote data exists".
        val effectiveRemoteMap: Map<String, String>? = if (batchFetchFailed && rows.isNotEmpty()) {
            AppLogger.w(
                TAG,
                "fetchAllRemotePacientes failed for optica_id=$opticaId " +
                    "but ${rows.size} local entities exist. " +
                    "Falling back to per-entity conflict check.",
            )
            null
        } else {
            remoteUpdatedAtMap
        }

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
                    "HO duplicada local: ${row.historiaOptometrica}. Se omite para evitar conflicto único.",
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
                    "HO ya existe en remoto con otro ID ($remoteId). Se omite para evitar conflicto único.",
                )
                continue
            }
            filteredRows += row
        }

        val deduplicated = filteredRows.distinctBy { it.id }

        // Detección de conflictos usando el mapa de timestamps ya obtenido
        val conflictSafe = conflictHelper.filterConflicts(
            tableName = TABLE,
            opticaId = opticaId,
            entityType = "paciente",
            localEntities = deduplicated.map { com.example.optoapp.domain.sync.LocalEntity(it.id, it.updatedAt, EntitySnapshotSerializer.serialize(it)) },
            remoteUpdatedAtMap = effectiveRemoteMap,
        )
        val conflictIds = conflictSafe.mapTo(mutableSetOf()) { it.id }
        val finalRows = deduplicated.filter { it.id in conflictIds }

        // F5: Double-failure guard — batch fetch demonstrably failed AND per-entity
        // fallback returned all entities as conflict-safe (also likely failed).
        // Abort upload to prevent silent data loss. Only triggers when we have
        // PROOF that the batch fetch failed (batchFetchFailed == true), not just
        // when the remote dataset happens to be empty.
        if (batchFetchFailed && deduplicated.isNotEmpty() && finalRows.size == deduplicated.size) {
            val hasCheckable = deduplicated.any { it.updatedAt != null }
            if (hasCheckable) {
                AppLogger.w(
                    TAG,
                    "upload: double fetch failure for optica_id=$opticaId — aborting upload",
                )
                syncStateTracker.markError(opticaId, "upload_pacientes", "batch", "Double fetch failure")
                return 0
            }
        }

        if (finalRows.isEmpty()) {
            syncStateTracker.markSynced(opticaId, "upload_pacientes", "batch")
            return 0
        }
        AppLogger.d(
            TAG,
            "Upload pacientes: ${finalRows.size}/${pacientes.size} filas tras prevalidación de HO, optica_id=$opticaId",
        )
        try {
            supabase.postgrest[TABLE].upsert(finalRows)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppLogger.e(TAG, "Error en red subiendo pacientes: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_pacientes", "batch", e.message)
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error inesperado subiendo pacientes: ${e.message}", e)
            syncStateTracker.markError(opticaId, "upload_pacientes", "batch", e.message)
            throw e
        }
        syncStateTracker.markSynced(opticaId, "upload_pacientes", "batch")
        finalRows.forEach { p ->
            syncStateTracker.markSynced(opticaId, "paciente", p.id)
        }

        AppLogger.d(TAG, "Subidos ${finalRows.size} pacientes a Supabase (optica_id=$opticaId).")
        return finalRows.size
    }

    private suspend fun download(opticaId: String): Int {
        // Phase 1: Retry pending remote deletes for paciente type before download.
        // This prevents dead entries in Supabase from being re-downloaded after a
        // prior partial failure (local delete succeeded, remote delete failed).
        try {
            val pendingPacienteDeletions = syncStateTracker.dao.getPendingDeletions(opticaId)
                .filter { it.entityType == "paciente" }
            for (tombstone in pendingPacienteDeletions) {
                try {
                    supabase.postgrest[TABLE].delete {
                        filter {
                            eq("id", tombstone.entityId)
                            eq("optica_id", opticaId)
                        }
                    }
                    syncStateTracker.dao.clearEntityState(opticaId, "paciente", tombstone.entityId)
                    AppLogger.d(TAG, "Propagated pending delete for paciente ${tombstone.entityId}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    // Remote delete still failing — log and continue. The download
                    // loop below will skip this ID, preventing resurrection.
                    AppLogger.e(
                        TAG,
                        "Pending delete still failing for paciente ${tombstone.entityId}: ${e.message}",
                        e,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error during Phase 1 pending-delete retry for paciente type: ${e.message}", e)
        }

        // Phase 2: Determine which remote IDs to skip during download
        val conflictedIds = try {
            conflictDao.getConflictEntityIds(opticaId, "paciente").toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying conflict IDs, proceeding without guard: ${e.message}", e)
            emptySet()
        }

        val deletedIds = try {
            syncStateTracker.dao.getPendingDeletions(opticaId)
                .filter { it.entityType == "paciente" }
                .map { it.entityId }
                .toSet()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error querying deleted IDs, proceeding without guard: ${e.message}", e)
            emptySet()
        }

        val skipIds = conflictedIds + deletedIds

        val remotos = fetchRemotePacientesForDownload(opticaId)

        if (remotos.isEmpty()) return 0

        var upserted = 0
        remotos.forEach { remoto ->
            if (remoto.id in skipIds) return@forEach
            try {
                val local = remoto.toEntity()
                repository.withTransaction {
                    repository.upsertPaciente(local)
                    syncStateTracker.markSynced(opticaId, "paciente", local.id)
                }
                upserted++
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AppLogger.e(TAG, "Error en red descargando paciente: ${e.message}", e)
                syncStateTracker.markError(opticaId, "paciente", remoto.id, e.message)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error inesperado descargando paciente: ${e.message}", e)
                syncStateTracker.markError(opticaId, "paciente", remoto.id, e.message)
            }
        }

        AppLogger.d(TAG, "Descargados $upserted pacientes desde Supabase.")
        return upserted
    }

    /**
     * Test seam — fetches remote pacientes for download. Override in tests
     * to return controlled data instead of hitting Supabase.
     */
    internal open suspend fun fetchRemotePacientesForDownload(opticaId: String): List<PacienteRemoto> {
        return supabase.postgrest[TABLE]
            .select {
                filter { eq("optica_id", opticaId) }
            }
            .decodeList<PacienteRemoto>()
    }

    /**
     * Fetches all remote pacientes for the given optica. Used to derive both
     * the historia-optometrica dedup map and the id→updatedAt conflict map
     * from a single network call, avoiding the redundant fetch that previously
     * happened when filterConflicts queried the same table again.
     */
    /**
     * Returns null when the network call fails, empty list when there are genuinely
     * no remote rows. Callers use null to distinguish "fetch error" from "no data".
     */
    private suspend fun fetchAllRemotePacientes(opticaId: String): List<PacienteRemoto>? {
        return try {
            supabase.postgrest[TABLE]
                .select {
                    filter { eq("optica_id", opticaId) }
                }
                .decodeList<PacienteRemoto>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "fetchAllRemotePacientes failed for optica_id=$opticaId: ${e.message}", e)
            null
        }
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
    @SerialName("optica_id") val opticaId: String = Paciente.LEGACY_OPTICA_ID,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
) {
    fun toEntity(): Paciente = Paciente(
        id = id,
        nombreCompleto = nombreCompleto,
        edad = edad,
        telefono = telefono,
        fechaCreacion = LocalDate.parse(fechaCreacion),
        dni = dni,
        fechaNacimiento = fechaNacimiento?.let(LocalDate::parse),
        sexo = sexo,
        email = email,
        historiaOptometrica = historiaOptometrica,
        direccion = direccion,
        distrito = distrito,
        ocupacion = ocupacion,
        acompanante = acompanante,
        hobbies = hobbies,
        ultimasEtiquetas = ultimasEtiquetas
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        opticaId = opticaId.ifBlank { Paciente.LEGACY_OPTICA_ID },
        updatedAt = updatedAt,
        updatedBy = updatedBy,
    )
}

private fun Paciente.toRemoto(): PacienteRemoto = PacienteRemoto(
    id = id,
    nombreCompleto = nombreCompleto.ifBlank { "-" },
    edad = edad,
    telefono = telefono.ifBlank { "-" },
    fechaCreacion = fechaCreacion.toString(),
    dni = dni ?: "",
    fechaNacimiento = fechaNacimiento?.toString(),
    sexo = sexo ?: "",
    email = email ?: "",
    historiaOptometrica = historiaOptometrica ?: "",
    direccion = direccion ?: "",
    distrito = distrito ?: "",
    ocupacion = ocupacion ?: "",
    acompanante = acompanante ?: "",
    hobbies = hobbies ?: "",
    ultimasEtiquetas = ultimasEtiquetas.joinToString(","),
    opticaId = opticaId.ifBlank { Paciente.LEGACY_OPTICA_ID },
    updatedAt = updatedAt,
    updatedBy = updatedBy,
)

data class PacientesSyncResult(val uploaded: Int, val downloaded: Int)
