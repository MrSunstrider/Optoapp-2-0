package com.example.optoapp.domain.decorator

import android.util.Log
import com.example.optoapp.domain.model.PacienteModel
import com.example.optoapp.domain.repository.PacienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Decorador para añadir Logging a cualquier repositorio de pacientes.
 */
class LoggingPacienteRepository(
    private val inner: PacienteRepository
) : PacienteRepository {

    override fun getPacientes(opticaId: String): Flow<List<PacienteModel>> {
        Log.d("REPO_LOG", "Consultando pacientes para optica: $opticaId")
        return inner.getPacientes(opticaId).onEach { 
            Log.d("REPO_LOG", "Recibidos ${it.size} pacientes")
        }
    }

    override fun searchPacientes(opticaId: String, query: String): Flow<List<PacienteModel>> {
        Log.d("REPO_LOG", "Buscando pacientes con query: $query")
        return inner.searchPacientes(opticaId, query)
    }

    override suspend fun getPacienteById(id: String): PacienteModel? {
        Log.d("REPO_LOG", "Obteniendo paciente con ID: $id")
        return inner.getPacienteById(id)
    }

    override suspend fun insertPaciente(paciente: PacienteModel) {
        Log.d("REPO_LOG", "Insertando paciente: ${paciente.nombreCompleto}")
        inner.insertPaciente(paciente)
    }

    override suspend fun updatePaciente(paciente: PacienteModel) {
        Log.d("REPO_LOG", "Actualizando paciente: ${paciente.id}")
        inner.updatePaciente(paciente)
    }

    override suspend fun deletePaciente(paciente: PacienteModel) {
        Log.d("REPO_LOG", "Eliminando paciente: ${paciente.id}")
        inner.deletePaciente(paciente)
    }
}
