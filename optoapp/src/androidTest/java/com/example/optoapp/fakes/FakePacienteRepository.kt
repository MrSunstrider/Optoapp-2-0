package com.example.optoapp.fakes

import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Paciente
import com.example.optoapp.data.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [com.example.optoapp.data.PacienteRepository] for E2E tests.
 *
 * Stores patients and evaluations in mutable lists. All operations are
 * synchronous — no Room, no Supabase. Follows the same method signatures
 * as the production repository so tests can swap implementations.
 */
class FakePacienteRepository {

    private val pacientes = mutableListOf<Paciente>()
    private val evaluaciones = mutableListOf<EvaluacionClinica>()

    private val _pacientesFlow = MutableStateFlow<List<Paciente>>(emptyList())
    private val _evaluacionesFlow = MutableStateFlow<List<EvaluacionClinica>>(emptyList())

    // ── Paciente ─────────────────────────────────────────────────────────────

    fun pacientesFlowForOptica(opticaId: String): Flow<List<Paciente>> = _pacientesFlow.map { list -> list.filter { it.opticaId == opticaId } }

    fun countPacientesForOptica(opticaId: String): Flow<Int> = _pacientesFlow.map { list -> list.count { it.opticaId == opticaId } }

    fun searchPacientesForOptica(opticaId: String, query: String): Flow<List<Paciente>> = _pacientesFlow.map { list ->
        list.filter { p ->
            p.opticaId == opticaId &&
                (
                    p.nombreCompleto.contains(query, ignoreCase = true) ||
                        p.telefono.contains(query)
                    )
        }
    }

    suspend fun getPacienteById(id: String): Resource<Paciente> {
        val paciente = pacientes.find { it.id == id }
        return if (paciente != null) {
            Resource.Success(paciente)
        } else {
            Resource.Error("Paciente no encontrado")
        }
    }

    suspend fun insertPaciente(paciente: Paciente) {
        pacientes.add(paciente)
        _pacientesFlow.value = pacientes.toList()
    }

    suspend fun updatePaciente(paciente: Paciente) {
        val index = pacientes.indexOfFirst { it.id == paciente.id }
        if (index >= 0) {
            pacientes[index] = paciente
            _pacientesFlow.value = pacientes.toList()
        }
    }

    suspend fun deletePaciente(paciente: Paciente) {
        pacientes.removeAll { it.id == paciente.id }
        _pacientesFlow.value = pacientes.toList()
    }

    suspend fun getPacientesSnapshotForOptica(opticaId: String): List<Paciente> = pacientes.filter { it.opticaId == opticaId }

    // ── Evaluación ───────────────────────────────────────────────────────────

    fun getEvaluacionesByPaciente(pacienteId: String, opticaId: String): Flow<List<EvaluacionClinica>> = _evaluacionesFlow.map { list ->
        list.filter { it.pacienteId == pacienteId && it.opticaId == opticaId }.sortedByDescending { it.fecha }
    }

    suspend fun getEvaluacionById(id: String, opticaId: String): Resource<EvaluacionClinica> {
        val evaluacion = evaluaciones.find { it.id == id }
        return if (evaluacion != null) {
            Resource.Success(evaluacion)
        } else {
            Resource.Error("Evaluación no encontrada")
        }
    }

    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica) {
        evaluaciones.add(evaluacion)
        _evaluacionesFlow.value = evaluaciones.toList()
    }

    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica) {
        val index = evaluaciones.indexOfFirst { it.id == evaluacion.id }
        if (index >= 0) {
            evaluaciones[index] = evaluacion
            _evaluacionesFlow.value = evaluaciones.toList()
        }
    }

    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica) {
        evaluaciones.removeAll { it.id == evaluacion.id }
        _evaluacionesFlow.value = evaluaciones.toList()
    }

    suspend fun getEvaluacionesSnapshotForOptica(opticaId: String): List<EvaluacionClinica> = evaluaciones.filter { it.opticaId == opticaId }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Remove all data — call in `@After` to reset state between tests. */
    fun clear() {
        pacientes.clear()
        evaluaciones.clear()
        _pacientesFlow.value = emptyList()
        _evaluacionesFlow.value = emptyList()
    }
}
