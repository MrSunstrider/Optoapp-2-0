package com.example.optoapp.domain.proxy

import com.example.optoapp.domain.model.PacienteModel
import com.example.optoapp.domain.repository.PacienteRepository
import kotlinx.coroutines.flow.Flow

/**
 * Proxy para controlar el acceso al repositorio de pacientes.
 * Verifica permisos (roles) antes de delegar la llamada al repositorio real.
 */
class PacienteRepositoryProxy(
    private val realRepository: PacienteRepository,
    private val userRole: String
) : PacienteRepository {

    override fun getPacientes(opticaId: String): Flow<List<PacienteModel>> {
        return realRepository.getPacientes(opticaId)
    }

    override fun searchPacientes(opticaId: String, query: String): Flow<List<PacienteModel>> {
        return realRepository.searchPacientes(opticaId, query)
    }

    override suspend fun getPacienteById(id: String): PacienteModel? {
        return realRepository.getPacienteById(id)
    }

    override suspend fun insertPaciente(paciente: PacienteModel) {
        if (userRole == "asesor_ventas") {
            // Un asesor podría tener permiso limitado, pero aquí ejemplificamos la restricción
        }
        realRepository.insertPaciente(paciente)
    }

    override suspend fun updatePaciente(paciente: PacienteModel) {
        realRepository.updatePaciente(paciente)
    }

    override suspend fun deletePaciente(paciente: PacienteModel) {
        if (userRole != "admin") {
            throw SecurityException("Solo los administradores pueden eliminar pacientes.")
        }
        realRepository.deletePaciente(paciente)
    }
}
