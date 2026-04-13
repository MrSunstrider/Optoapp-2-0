package com.example.optoapp.domain

import android.util.Log
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * FASE 3 – Paso 3.1
 * Sincronización bidireccional de Pacientes.
 */
class SyncPacientesUseCase @Inject constructor(
    private val repository: OptoRepository,
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "SyncPacientes"
        private const val TABLE = "pacientes"
    }

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

    private suspend fun upload(opticaId: String): Int {
        val pacientes = repository.getPacientesSnapshotForOptica(opticaId)

        if (pacientes.isEmpty()) return 0

        val rows = pacientes.map { it.toRemoto().copy(opticaId = opticaId) }
        supabase.postgrest[TABLE].upsert(rows)

        Log.d(TAG, "Subidos ${pacientes.size} pacientes a Supabase (forzando ID: $opticaId).")
        return pacientes.size
    }

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

@Serializable
data class PacienteRemoto(
    val id: String,
    @SerialName("nombre_completo") val nombreCompleto: String,
    val edad: Int,
    val telefono: String,
    @SerialName("fecha_creacion") val fechaCreacion: Long,
    val dni: String? = null,
    @SerialName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null,
    val email: String? = null,
    val direccion: String? = null,
    val distrito: String? = null,
    val ocupacion: String? = null,
    val acompanante: String? = null,
    val hobbies: String? = null,
    @SerialName("ultimas_etiquetas") val ultimasEtiquetas: String? = null,
    @SerialName("optica_id") val opticaId: String = "mi_optica_base"
) {
    fun toEntity(): Paciente = Paciente(
        id               = id,
        nombreCompleto   = nombreCompleto,
        edad             = edad,
        telefono         = telefono,
        fechaCreacion    = fechaCreacion,
        dni              = dni,
        fechaNacimiento  = fechaNacimiento,
        sexo             = sexo,
        email            = email,
        direccion        = direccion,
        distrito         = distrito,
        ocupacion        = ocupacion,
        acompanante      = acompanante,
        hobbies          = hobbies,
        ultimasEtiquetas = ultimasEtiquetas
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        opticaId         = opticaId.ifBlank { "mi_optica_base" }
    )
}

private fun Paciente.toRemoto(): PacienteRemoto = PacienteRemoto(
    id                = id,
    nombreCompleto   = nombreCompleto, // Corregido: ya es nombreCompleto en la entity
    edad              = edad,
    telefono          = telefono,
    fechaCreacion    = fechaCreacion,
    dni               = dni,
    fechaNacimiento  = fechaNacimiento,
    sexo              = sexo,
    email             = email,
    direccion         = direccion,
    distrito          = distrito,
    ocupacion         = ocupacion,
    acompanante       = acompanante,
    hobbies           = hobbies,
    ultimasEtiquetas = ultimasEtiquetas.joinToString(","),
    opticaId         = opticaId
)

data class PacientesSyncResult(val uploaded: Int, val downloaded: Int)
