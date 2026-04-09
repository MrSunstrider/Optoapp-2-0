package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

/**
 * FASE 3 – Paso 3.1
 * Sincronización bidireccional de Pacientes.
 *
 * Estrategia:
 *  1. SUBIDA  → Upsert de todos los pacientes locales en la tabla remota "pacientes".
 *  2. BAJADA  → Descarga de pacientes remotos con el mismo opticaId y los guarda en Room.
 *
 * La tabla "pacientes" en Supabase debe tener las mismas columnas que la Entity Room
 * más una columna updated_at (timestamp) para resolver conflictos futuros.
 */
class SyncPacientesUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "SyncPacientes"
        private const val TABLE = "pacientes"
    }

    /**
     * Ejecuta el ciclo completo: subida → bajada.
     * @return Resource.Success con el nº de registros procesados o Resource.Error con el mensaje.
     */
    suspend operator fun invoke(opticaId: String): Resource<PacientesSyncResult> {
        return try {
            val uploaded = upload(opticaId)
            val downloaded = download(opticaId)
            Resource.Success(PacientesSyncResult(uploaded, downloaded))
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización de pacientes", e)
            Resource.Error("Error sincronizando pacientes: ${e.localizedMessage}")
        }
    }

    // ─── SUBIDA ──────────────────────────────────────────────────────────────

    private suspend fun upload(opticaId: String): Int {
        val pacientes = repository.getAllPacientesSnapshot()
            .filter { it.opticaId == opticaId }

        if (pacientes.isEmpty()) return 0

        val rows = pacientes.map { it.toRemoteMap() }

        supabase.postgrest[TABLE].upsert(rows)

        Log.d(TAG, "Subidos ${pacientes.size} pacientes a Supabase.")
        return pacientes.size
    }

    // ─── BAJADA ──────────────────────────────────────────────────────────────

    private suspend fun download(opticaId: String): Int {
        val remotos = supabase.postgrest[TABLE]
            .select {
                filter { eq("optica_id", opticaId) }
            }
            .decodeList<PacienteRemoto>()

        if (remotos.isEmpty()) return 0

        remotos.forEach { remoto ->
            val local = remoto.toEntity()
            repository.upsertPaciente(local)
        }

        Log.d(TAG, "Descargados ${remotos.size} pacientes desde Supabase.")
        return remotos.size
    }
}

// ─── Extensión: Entity → Mapa para Supabase ──────────────────────────────────

private fun Paciente.toRemoteMap(): Map<String, Any?> = mapOf(
    "id"                  to id,
    "nombre_completo"     to nombreCompleto,
    "edad"                to edad,
    "telefono"            to telefono,
    "fecha_creacion"      to fechaCreacion,
    "dni"                 to dni,
    "fecha_nacimiento"    to fechaNacimiento,
    "sexo"                to sexo,
    "email"               to email,
    "direccion"           to direccion,
    "distrito"            to distrito,
    "ocupacion"           to ocupacion,
    "acompanante"         to acompanante,
    "hobbies"             to hobbies,
    "ultimas_etiquetas"   to ultimasEtiquetas.joinToString(","),
    "optica_id"           to opticaId
)

// ─── DTO de bajada ────────────────────────────────────────────────────────────

@Serializable
data class PacienteRemoto(
    val id: String,
    val nombre_completo: String,
    val edad: Int,
    val telefono: String,
    val fecha_creacion: Long,
    val dni: String? = null,
    val fecha_nacimiento: String? = null,
    val sexo: String? = null,
    val email: String? = null,
    val direccion: String? = null,
    val distrito: String? = null,
    val ocupacion: String? = null,
    val acompanante: String? = null,
    val hobbies: String? = null,
    val ultimas_etiquetas: String? = null,
    val optica_id: String = "mi_optica_base"
) {
    fun toEntity(): Paciente = Paciente(
        id               = id,
        nombreCompleto   = nombre_completo,
        edad             = edad,
        telefono         = telefono,
        fechaCreacion    = fecha_creacion,
        dni              = dni,
        fechaNacimiento  = fecha_nacimiento,
        sexo             = sexo,
        email            = email,
        direccion        = direccion,
        distrito         = distrito,
        ocupacion        = ocupacion,
        acompanante      = acompanante,
        hobbies          = hobbies,
        ultimasEtiquetas = ultimas_etiquetas
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        opticaId         = optica_id
    )
}

// ─── Resultado de sincronización ─────────────────────────────────────────────

data class PacientesSyncResult(val uploaded: Int, val downloaded: Int)
