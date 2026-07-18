package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteDao {
    @Query("SELECT * FROM pacientes WHERE opticaId = :opticaId ORDER BY nombreCompleto ASC")
    fun getPacientesByOptica(opticaId: String): Flow<List<Paciente>>

    @Query("SELECT COUNT(*) FROM pacientes WHERE opticaId = :opticaId")
    fun countByOptica(opticaId: String): Flow<Int>

    @Query("SELECT * FROM pacientes WHERE opticaId = :opticaId ORDER BY nombreCompleto ASC")
    suspend fun getPacientesListByOptica(opticaId: String): List<Paciente>

    @Query("SELECT historiaOptometrica FROM pacientes WHERE opticaId = :opticaId AND ifnull(historiaOptometrica, '') <> ''")
    suspend fun getHistoriasOptometricasByOptica(opticaId: String): List<String>

    @Query(
        """
        SELECT COUNT(*) FROM pacientes
        WHERE opticaId = :opticaId
          AND UPPER(TRIM(ifnull(historiaOptometrica, ''))) = UPPER(TRIM(:historiaNorm))
          AND (:excludeId = '' OR id != :excludeId)
        """
    )
    suspend fun countPacientesByHistoriaOptometrica(opticaId: String, historiaNorm: String, excludeId: String): Int

    @Query("SELECT * FROM pacientes WHERE id = :id")
    suspend fun getPacienteById(id: String): Paciente?

    @Query("""
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
            nombreCompleto LIKE '%' || :query || '%' OR id LIKE '%' || :query || '%' OR telefono LIKE '%' || :query || '%' OR ifnull(historiaOptometrica, '') LIKE '%' || :query || '%'
        )
    """)
    fun searchPacientesForOptica(opticaId: String, query: String): Flow<List<Paciente>>

    @Upsert
    suspend fun insertPaciente(paciente: Paciente)

    @Query("""
        UPDATE pacientes SET nombreCompleto=:nombreCompleto, edad=:edad,
        telefono=:telefono, fechaCreacion=:fechaCreacion, dni=:dni,
        fechaNacimiento=:fechaNacimiento, sexo=:sexo, email=:email,
        historiaOptometrica=:historiaOptometrica, direccion=:direccion,
        distrito=:distrito, ocupacion=:ocupacion, acompanante=:acompanante,
        hobbies=:hobbies, ultimasEtiquetas=:ultimasEtiquetas,
        opticaId=:opticaId, updatedAt=:updatedAt, updatedBy=:updatedBy
        WHERE id=:id AND opticaId=:opticaId
    """)
    suspend fun updatePaciente(
        id: String, opticaId: String, nombreCompleto: String, edad: Int,
        telefono: String, fechaCreacion: java.time.LocalDate, dni: String?,
        fechaNacimiento: java.time.LocalDate?, sexo: String?, email: String?,
        historiaOptometrica: String?, direccion: String?, distrito: String?,
        ocupacion: String?, acompanante: String?, hobbies: String?,
        ultimasEtiquetas: List<String>, updatedAt: String?, updatedBy: String?
    ): Int

    @Query("DELETE FROM pacientes WHERE id = :id AND opticaId = :opticaId")
    suspend fun deletePaciente(id: String, opticaId: String)

    @Query("UPDATE pacientes SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("UPDATE evaluaciones SET pacienteId = :targetPacienteId WHERE pacienteId = :sourcePacienteId")
    suspend fun reassignEvaluacionesPaciente(sourcePacienteId: String, targetPacienteId: String): Int

    @Query("UPDATE dispensaciones SET pacienteId = :targetPacienteId WHERE pacienteId = :sourcePacienteId")
    suspend fun reassignDispensacionesPaciente(sourcePacienteId: String, targetPacienteId: String): Int

    @Query("UPDATE servicios_extra SET pacienteId = :targetPacienteId WHERE pacienteId = :sourcePacienteId")
    suspend fun reassignServiciosPaciente(sourcePacienteId: String, targetPacienteId: String): Int

    @Query("DELETE FROM pacientes WHERE id = :id AND opticaId = :opticaId")
    suspend fun deletePacienteById(id: String, opticaId: String): Int

    @Query("""
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
        id IN (SELECT pacienteId FROM dispensaciones WHERE opticaId = :opticaId AND (montoTotal - montoPagado) > 0)
        OR id IN (SELECT pacienteId FROM servicios_extra WHERE opticaId = :opticaId AND (montoTotal - aCuenta) > 0)
        ) ORDER BY nombreCompleto ASC
    """)
    fun getPacientesWithPendingBalanceForOptica(opticaId: String): Flow<List<Paciente>>

    @Query("""
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
        id IN (SELECT pacienteId FROM dispensaciones WHERE opticaId = :opticaId AND estadoEntrega = 'Pendiente')
        OR id IN (SELECT pacienteId FROM servicios_extra WHERE opticaId = :opticaId AND estado = 'Pendiente')
        ) ORDER BY nombreCompleto ASC
    """)
    fun getPacientesWithPendingDeliveryForOptica(opticaId: String): Flow<List<Paciente>>
}
