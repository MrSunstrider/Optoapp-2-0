package com.example.optoapp.data

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.LocalDate

/**
 * Repositorio especializado en operaciones de Paciente y EvaluacionClinica.
 * Extraído de [OptoRepository] para reducir el God class.
 */
class PacienteRepository(
    private val pacienteDao: PacienteDao,
    private val evaluacionDao: EvaluacionDao
) {
    fun pacientesFlowForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteDao.getPacientesByOptica(opticaId)

    fun countPacientesForOptica(opticaId: String): Flow<Int> =
        pacienteDao.countByOptica(opticaId)

    fun searchPacientesForOptica(opticaId: String, query: String): Flow<List<Paciente>> =
        if (query.isEmpty()) pacienteDao.getPacientesByOptica(opticaId)
        else pacienteDao.searchPacientesForOptica(opticaId, query)

    fun getPacientesWithPendingBalanceForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteDao.getPacientesWithPendingBalanceForOptica(opticaId)

    fun getPacientesWithPendingDeliveryForOptica(opticaId: String): Flow<List<Paciente>> =
        pacienteDao.getPacientesWithPendingDeliveryForOptica(opticaId)

    suspend fun getPacienteById(id: String): Resource<Paciente> {
        return try {
            val paciente = pacienteDao.getPacienteById(id)
            if (paciente != null) Resource.Success(paciente)
            else Resource.Error("Paciente no encontrado")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "getPacienteById: id=$id", e)
            Resource.Error("Error de red al obtener paciente")
        } catch (e: Exception) {
            Log.e(TAG, "getPacienteById: id=$id", e)
            Resource.Error(e.message ?: "Error al obtener paciente")
        }
    }

    suspend fun insertPaciente(paciente: Paciente) {
        pacienteDao.insertPaciente(paciente)
    }

    suspend fun updatePaciente(paciente: Paciente) {
        pacienteDao.updatePaciente(paciente)
    }

    suspend fun deletePaciente(paciente: Paciente) = pacienteDao.deletePaciente(paciente)

    suspend fun getPacientesSnapshotForOptica(opticaId: String): List<Paciente> =
        pacienteDao.getPacientesListByOptica(opticaId)

    /** Siguiente correlativo `HO-<año>-####` para historia optométrica en la óptica activa. */
    suspend fun suggestNextHistoriaOptometrica(opticaId: String): String {
        val historias = pacienteDao.getHistoriasOptometricasByOptica(opticaId)
        val year = LocalDate.now().year.toString()
        val regex = Regex("^HO-$year-(\\d+)$", RegexOption.IGNORE_CASE)
        var max = 0
        for (historia in historias) {
            regex.find(historia.trim())?.groupValues?.get(1)?.toIntOrNull()?.let { if (it > max) max = it }
        }
        val next = max + 1
        return "HO-$year-" + next.toString().padStart(4, '0')
    }

    /** True si ya existe esa historia optométrica en la misma óptica (ignorando mayúsculas/espacios). */
    suspend fun existsDuplicateHistoriaOptometrica(opticaId: String, historia: String, excludePacienteId: String?): Boolean {
        val n = historia.trim()
        if (n.isEmpty()) return false
        val ex = excludePacienteId.orEmpty()
        return pacienteDao.countPacientesByHistoriaOptometrica(opticaId, n, ex) > 0
    }

    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>> =
        evaluacionDao.getEvaluacionesByPaciente(pacienteId)

    fun getEvaluacionesProximaCitaEnRango(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<EvaluacionClinica>> =
        evaluacionDao.getEvaluacionesConProximaCitaEnRango(opticaId, start, end)

    fun countEvaluacionesInRange(start: LocalDate, end: LocalDate): Flow<Int> =
        evaluacionDao.countEvaluacionesInRange(start, end)

    fun countEvaluacionesInRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<Int> =
        evaluacionDao.countEvaluacionesInRangeForOptica(start, end, opticaId)

    suspend fun getEvaluacionById(id: String): Resource<EvaluacionClinica> {
        return try {
            val evaluacion = evaluacionDao.getEvaluacionById(id)
            if (evaluacion != null) Resource.Success(evaluacion)
            else Resource.Error("Evaluación no encontrada")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "getEvaluacionById: id=$id", e)
            Resource.Error("Error de red al obtener evaluación")
        } catch (e: Exception) {
            Log.e(TAG, "getEvaluacionById: id=$id", e)
            Resource.Error(e.message ?: "Error al obtener evaluación")
        }
    }

    suspend fun getLastEvaluacionByPacienteId(pacienteId: String): Resource<EvaluacionClinica> {
        return try {
            val eval = evaluacionDao.getLastEvaluacionByPacienteId(pacienteId)
            if (eval != null) Resource.Success(eval)
            else Resource.Error("No hay evaluaciones")
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "getLastEvaluacionByPacienteId: pacienteId=$pacienteId", e)
            Resource.Error("Error de red al obtener evaluación")
        } catch (e: Exception) {
            Log.e(TAG, "getLastEvaluacionByPacienteId: pacienteId=$pacienteId", e)
            Resource.Error(e.message ?: "Error al obtener evaluación")
        }
    }

    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica) = evaluacionDao.deleteEvaluacion(evaluacion)

    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica) {
        evaluacionDao.insertEvaluacion(evaluacion)
    }

    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica) {
        evaluacionDao.updateEvaluacion(evaluacion)
    }

    suspend fun getEvaluacionesSnapshotForOptica(opticaId: String): List<EvaluacionClinica> =
        evaluacionDao.getEvaluacionesListByOptica(opticaId)

    suspend fun reassignFromLegacyMiOpticaBase(currentOpticaId: String): Int {
        val p = pacienteDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        val e = evaluacionDao.reassignFromLegacyMiOpticaBase(currentOpticaId)
        return p + e
    }

    suspend fun resolveDuplicatePacientesByHistoria(opticaId: String, database: OptoDatabase): DuplicateHoResolutionResult {
        val pacientes = pacienteDao.getPacientesListByOptica(opticaId)
        val grouped = pacientes
            .mapNotNull { p ->
                val ho = p.historiaOptometrica?.trim()?.uppercase().orEmpty()
                if (ho.isBlank()) null else ho to p
            }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }
        if (grouped.isEmpty()) return DuplicateHoResolutionResult()

        var mergedPacientes = 0
        var movedEvaluaciones = 0
        var movedDispensaciones = 0
        var movedServicios = 0

        database.withTransaction {
            grouped.forEach { (_, rows) ->
                val canonical = rows.minByOrNull { it.fechaCreacion } ?: return@forEach
                rows.forEach { duplicate ->
                    if (duplicate.id == canonical.id) return@forEach

                    val mergedCanonical = mergePacienteData(canonical, duplicate)
                    pacienteDao.updatePaciente(mergedCanonical)
                    movedEvaluaciones += pacienteDao.reassignEvaluacionesPaciente(duplicate.id, canonical.id)
                    movedDispensaciones += pacienteDao.reassignDispensacionesPaciente(duplicate.id, canonical.id)
                    movedServicios += pacienteDao.reassignServiciosPaciente(duplicate.id, canonical.id)
                    pacienteDao.deletePacienteById(duplicate.id)
                    mergedPacientes++
                }
            }
        }
        return DuplicateHoResolutionResult(
            mergedPacientes = mergedPacientes,
            movedEvaluaciones = movedEvaluaciones,
            movedDispensaciones = movedDispensaciones,
            movedServicios = movedServicios
        )
    }

    companion object {
        private const val TAG = "PacienteRepository"
    }
}

private fun mergePacienteData(canonical: Paciente, other: Paciente): Paciente {
    fun chooseText(primary: String?, fallback: String?): String? =
        primary?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }
    return canonical.copy(
        nombreCompleto = if (canonical.nombreCompleto.isNotBlank()) canonical.nombreCompleto else other.nombreCompleto,
        edad = maxOf(canonical.edad, other.edad),
        telefono = chooseText(canonical.telefono, other.telefono).orEmpty(),
        dni = chooseText(canonical.dni, other.dni),
        fechaNacimiento = canonical.fechaNacimiento ?: other.fechaNacimiento,
        sexo = chooseText(canonical.sexo, other.sexo),
        email = chooseText(canonical.email, other.email),
        historiaOptometrica = chooseText(canonical.historiaOptometrica, other.historiaOptometrica),
        direccion = chooseText(canonical.direccion, other.direccion),
        distrito = chooseText(canonical.distrito, other.distrito),
        ocupacion = chooseText(canonical.ocupacion, other.ocupacion),
        acompanante = chooseText(canonical.acompanante, other.acompanante),
        hobbies = chooseText(canonical.hobbies, other.hobbies),
        ultimasEtiquetas = (canonical.ultimasEtiquetas + other.ultimasEtiquetas).distinct()
    )
}
