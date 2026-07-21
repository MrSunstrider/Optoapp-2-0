package com.example.optoapp.fakes

import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [EvaluacionClinica] repository for E2E tests.
 *
 * Mirrors evaluacion-specific operations from the production
 * [com.example.optoapp.data.PacienteRepository] (which handles both
 * pacientes and evaluaciones). Tests that only need evaluacion data
 * can use this focused fake instead of the full [FakePacienteRepository].
 */
class FakeEvaluacionRepository {

    private val evaluaciones = mutableListOf<EvaluacionClinica>()
    private val _flow = MutableStateFlow<List<EvaluacionClinica>>(emptyList())

    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>> = _flow.map { list ->
        list.filter { it.pacienteId == pacienteId }.sortedByDescending { it.fecha }
    }

    suspend fun getEvaluacionById(id: String): Resource<EvaluacionClinica> {
        val ev = evaluaciones.find { it.id == id }
        return if (ev != null) {
            Resource.Success(ev)
        } else {
            Resource.Error("Evaluación no encontrada")
        }
    }

    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica) {
        evaluaciones.add(evaluacion)
        _flow.value = evaluaciones.toList()
    }

    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica) {
        val index = evaluaciones.indexOfFirst { it.id == evaluacion.id }
        if (index >= 0) {
            evaluaciones[index] = evaluacion
            _flow.value = evaluaciones.toList()
        }
    }

    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica) {
        evaluaciones.removeAll { it.id == evaluacion.id }
        _flow.value = evaluaciones.toList()
    }

    suspend fun getEvaluacionesSnapshotForOptica(opticaId: String): List<EvaluacionClinica> = evaluaciones.filter { it.opticaId == opticaId }

    /** Remove all data — call in `@After` to reset state between tests. */
    fun clear() {
        evaluaciones.clear()
        _flow.value = emptyList()
    }
}
