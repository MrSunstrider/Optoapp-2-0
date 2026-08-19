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

    @Query("SELECT MAX(CAST(SUBSTR(historiaOptometrica, 9) AS INTEGER)) FROM pacientes WHERE opticaId = :opticaId AND historiaOptometrica LIKE 'HO-' || :year || '-%'")
    suspend fun getMaxHistoriaNum(opticaId: String, year: String): Int?

    @Query(
        """
        SELECT COUNT(*) FROM pacientes
        WHERE opticaId = :opticaId
          AND UPPER(TRIM(ifnull(historiaOptometrica, ''))) = UPPER(TRIM(:historiaNorm))
          AND (:excludeId = '' OR id != :excludeId)
        """,
    )
    suspend fun countPacientesByHistoriaOptometrica(opticaId: String, historiaNorm: String, excludeId: String): Int

    @Query("SELECT * FROM pacientes WHERE id = :id AND opticaId = :opticaId")
    suspend fun getPacienteByIdScoped(id: String, opticaId: String): Paciente?

    @Query(
        """
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
            nombreCompleto LIKE '%' || :query || '%'
            OR id LIKE '%' || :query || '%'
            OR telefono LIKE '%' || :query || '%'
            OR ifnull(historiaOptometrica, '') LIKE '%' || :query || '%'
            OR id IN (
                SELECT pacienteId FROM dispensaciones
                WHERE opticaId = :opticaId AND ot LIKE '%' || :query || '%'
            )
            OR id IN (
                SELECT pacienteId FROM servicios_extra
                WHERE opticaId = :opticaId
                  AND pacienteId IS NOT NULL
                  AND ot LIKE '%' || :query || '%'
            )
        )
    """,
    )
    fun searchPacientesForOptica(opticaId: String, query: String): Flow<List<Paciente>>

    // Historical name — this is an upsert, not a plain insert. Use upsertPaciente for clarity.
    @Upsert
    suspend fun insertPaciente(paciente: Paciente)

    @Upsert
    suspend fun upsertPaciente(paciente: Paciente)

    @Query("DELETE FROM pacientes WHERE id = :id AND opticaId = :opticaId")
    suspend fun deletePaciente(id: String, opticaId: String): Int

    @Query("UPDATE pacientes SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("UPDATE evaluaciones SET pacienteId = :targetPacienteId WHERE pacienteId = :sourcePacienteId")
    suspend fun reassignEvaluacionesPaciente(sourcePacienteId: String, targetPacienteId: String): Int

    @Query("UPDATE dispensaciones SET pacienteId = :targetPacienteId WHERE pacienteId = :sourcePacienteId")
    suspend fun reassignDispensacionesPaciente(sourcePacienteId: String, targetPacienteId: String): Int

    @Query("UPDATE servicios_extra SET pacienteId = :targetPacienteId WHERE pacienteId = :sourcePacienteId")
    suspend fun reassignServiciosPaciente(sourcePacienteId: String, targetPacienteId: String): Int

    // Alias for deletePaciente — kept for callers that expect the ById suffix.
    @Query("DELETE FROM pacientes WHERE id = :id AND opticaId = :opticaId")
    suspend fun deletePacienteById(id: String, opticaId: String): Int

    @Query(
        """
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
        id IN (
            SELECT d.pacienteId FROM dispensaciones d
            WHERE d.opticaId = :opticaId
              AND d.estadoEntrega NOT IN ('Anulado', 'Reclamada')
              AND (
                d.montoTotal - COALESCE((
                    SELECT SUM(
                        CASE
                            WHEN p.tipo IN ('Abono', 'Pago completo') THEN p.monto
                            WHEN p.tipo IN ('Reembolso', 'Reverso') THEN -p.monto
                            ELSE 0
                        END
                    )
                    FROM pagos p
                    WHERE p.dispensacionId = d.id
                ), d.montoPagado)
              ) > 0
        )
        OR id IN (
            SELECT s.pacienteId FROM servicios_extra s
            WHERE s.opticaId = :opticaId
              AND s.estado != 'Anulado'
              AND s.pacienteId IS NOT NULL
              AND (
                s.montoTotal - COALESCE((
                    SELECT SUM(
                        CASE
                            WHEN p.tipo IN ('Abono', 'Pago completo') THEN p.monto
                            WHEN p.tipo IN ('Reembolso', 'Reverso') THEN -p.monto
                            ELSE 0
                        END
                    )
                    FROM pagos p
                    WHERE p.servicioExtraId = s.id
                ), s.aCuenta)
              ) > 0
        )
        ) ORDER BY nombreCompleto ASC
    """,
    )
    fun getPacientesWithPendingBalanceForOptica(opticaId: String): Flow<List<Paciente>>

    @Query(
        """
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
        id IN (
            SELECT pacienteId FROM dispensaciones
            WHERE opticaId = :opticaId
              AND estadoEntrega = 'Pendiente'
              AND fechaEntrega IS NULL
        )
        OR id IN (
            SELECT pacienteId FROM servicios_extra
            WHERE opticaId = :opticaId
              AND estado = 'Pendiente'
              AND fecha_entrega IS NULL
        )
        ) ORDER BY nombreCompleto ASC
    """,
    )
    fun getPacientesWithPendingDeliveryForOptica(opticaId: String): Flow<List<Paciente>>
}
