package com.example.optoapp.data.repository.impl

import com.example.optoapp.data.PacienteDao
import com.example.optoapp.data.mappers.PacienteMapper
import com.example.optoapp.domain.model.PacienteModel
import com.example.optoapp.domain.repository.PacienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PacienteRepositoryImpl @Inject constructor(
    private val pacienteDao: PacienteDao
) : PacienteRepository {

    override fun getPacientes(opticaId: String): Flow<List<PacienteModel>> {
        return pacienteDao.getPacientesByOptica(opticaId).map { entities ->
            PacienteMapper.toDomainList(entities)
        }
    }

    override fun searchPacientes(opticaId: String, query: String): Flow<List<PacienteModel>> {
        val flow = if (query.isEmpty()) {
            pacienteDao.getPacientesByOptica(opticaId)
        } else {
            pacienteDao.searchPacientesForOptica(opticaId, query)
        }
        return flow.map { entities ->
            PacienteMapper.toDomainList(entities)
        }
    }

    override suspend fun getPacienteById(id: String): PacienteModel? {
        return pacienteDao.getPacienteById(id)?.let { PacienteMapper.toDomain(it) }
    }

    override suspend fun insertPaciente(paciente: PacienteModel) {
        pacienteDao.insertPaciente(PacienteMapper.toEntity(paciente))
    }

    override suspend fun updatePaciente(paciente: PacienteModel) {
        pacienteDao.updatePaciente(PacienteMapper.toEntity(paciente))
    }

    override suspend fun deletePaciente(paciente: PacienteModel) {
        pacienteDao.deletePaciente(PacienteMapper.toEntity(paciente))
    }
}
