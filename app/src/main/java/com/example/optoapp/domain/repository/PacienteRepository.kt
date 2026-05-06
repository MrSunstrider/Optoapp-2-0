package com.example.optoapp.domain.repository

import com.example.optoapp.domain.model.PacienteModel
import kotlinx.coroutines.flow.Flow

interface PacienteRepository {
    fun getPacientes(opticaId: String): Flow<List<PacienteModel>>
    fun searchPacientes(opticaId: String, query: String): Flow<List<PacienteModel>>
    suspend fun getPacienteById(id: String): PacienteModel?
    suspend fun insertPaciente(paciente: PacienteModel)
    suspend fun updatePaciente(paciente: PacienteModel)
    suspend fun deletePaciente(paciente: PacienteModel)
}
