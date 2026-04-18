package com.example.optoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PacienteDao {
    @Query("SELECT * FROM pacientes ORDER BY nombreCompleto ASC")
    fun getAllPacientes(): Flow<List<Paciente>>

    @Query("SELECT * FROM pacientes WHERE opticaId = :opticaId ORDER BY nombreCompleto ASC")
    fun getPacientesByOptica(opticaId: String): Flow<List<Paciente>>

    @Query("SELECT COUNT(*) FROM pacientes WHERE opticaId = :opticaId")
    fun countByOptica(opticaId: String): Flow<Int>

    @Query("SELECT * FROM pacientes WHERE opticaId = :opticaId ORDER BY nombreCompleto ASC")
    suspend fun getPacientesListByOptica(opticaId: String): List<Paciente>

    @Query("SELECT * FROM pacientes WHERE id = :id")
    suspend fun getPacienteById(id: String): Paciente?

    @Query("SELECT * FROM pacientes WHERE nombreCompleto LIKE '%' || :query || '%' OR id LIKE '%' || :query || '%' OR telefono LIKE '%' || :query || '%'")
    fun searchPacientes(query: String): Flow<List<Paciente>>

    @Query("""
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
            nombreCompleto LIKE '%' || :query || '%' OR id LIKE '%' || :query || '%' OR telefono LIKE '%' || :query || '%'
        )
    """)
    fun searchPacientesForOptica(opticaId: String, query: String): Flow<List<Paciente>>

    @Upsert
    suspend fun insertPaciente(paciente: Paciente)

    @Update
    suspend fun updatePaciente(paciente: Paciente)

    @Delete
    suspend fun deletePaciente(paciente: Paciente)
    
    @Query("DELETE FROM pacientes")
    suspend fun deleteAll()

    @Query("UPDATE pacientes SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("""
        SELECT * FROM pacientes 
        WHERE id IN (SELECT pacienteId FROM dispensaciones WHERE (montoTotal - montoPagado) > 0)
        OR id IN (SELECT pacienteId FROM servicios_extra WHERE (montoTotal - aCuenta) > 0)
        ORDER BY nombreCompleto ASC
    """)
    fun getPacientesWithPendingBalance(): Flow<List<Paciente>>

    @Query("""
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
        id IN (SELECT pacienteId FROM dispensaciones WHERE opticaId = :opticaId AND (montoTotal - montoPagado) > 0)
        OR id IN (SELECT pacienteId FROM servicios_extra WHERE opticaId = :opticaId AND (montoTotal - aCuenta) > 0)
        ) ORDER BY nombreCompleto ASC
    """)
    fun getPacientesWithPendingBalanceForOptica(opticaId: String): Flow<List<Paciente>>

    @Query("""
        SELECT * FROM pacientes 
        WHERE id IN (SELECT pacienteId FROM dispensaciones WHERE estadoEntrega = 'Pendiente')
        OR id IN (SELECT pacienteId FROM servicios_extra WHERE estado = 'Pendiente')
        ORDER BY nombreCompleto ASC
    """)
    fun getPacientesWithPendingDelivery(): Flow<List<Paciente>>

    @Query("""
        SELECT * FROM pacientes WHERE opticaId = :opticaId AND (
        id IN (SELECT pacienteId FROM dispensaciones WHERE opticaId = :opticaId AND estadoEntrega = 'Pendiente')
        OR id IN (SELECT pacienteId FROM servicios_extra WHERE opticaId = :opticaId AND estado = 'Pendiente')
        ) ORDER BY nombreCompleto ASC
    """)
    fun getPacientesWithPendingDeliveryForOptica(opticaId: String): Flow<List<Paciente>>
}

@Dao
interface EvaluacionDao {
    @Query("SELECT * FROM evaluaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getEvaluacionesByPaciente(pacienteId: String): Flow<List<EvaluacionClinica>>

    @Query("SELECT * FROM evaluaciones WHERE id = :id")
    suspend fun getEvaluacionById(id: String): EvaluacionClinica?

    @Upsert
    suspend fun insertEvaluacion(evaluacion: EvaluacionClinica)
    
    @Update
    suspend fun updateEvaluacion(evaluacion: EvaluacionClinica)
    
    @Delete
    suspend fun deleteEvaluacion(evaluacion: EvaluacionClinica)
    
    @Query("DELETE FROM evaluaciones")
    suspend fun deleteAll()

    @Query("UPDATE evaluaciones SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int
    
    @Query("SELECT * FROM evaluaciones")
    suspend fun getAllEvaluaciones(): List<EvaluacionClinica>

    @Query("SELECT * FROM evaluaciones WHERE opticaId = :opticaId")
    suspend fun getEvaluacionesListByOptica(opticaId: String): List<EvaluacionClinica>

    @Query("SELECT COUNT(*) FROM evaluaciones WHERE fecha >= :start AND fecha <= :end")
    fun countEvaluacionesInRange(start: LocalDate, end: LocalDate): Flow<Int>

    @Query("SELECT COUNT(*) FROM evaluaciones WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId")
    fun countEvaluacionesInRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<Int>

    @Query(
        """
        SELECT * FROM evaluaciones
        WHERE opticaId = :opticaId
        AND proximaCita IS NOT NULL
        AND proximaCita >= :start
        AND proximaCita <= :end
        ORDER BY proximaCita ASC
        """
    )
    fun getEvaluacionesConProximaCitaEnRango(opticaId: String, start: LocalDate, end: LocalDate): Flow<List<EvaluacionClinica>>
}

@Dao
interface DispensacionDao {
    @Query("SELECT * FROM dispensaciones WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getDispensacionesByPaciente(pacienteId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones")
    fun getAllDispensaciones(): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE opticaId = :opticaId")
    fun getAllDispensacionesForOptica(opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT SUM(montoTotal) FROM dispensaciones")
    fun getTotalVendido(): Flow<Double?>

    @Query("SELECT SUM(montoPagado) FROM dispensaciones")
    fun getTotalPagado(): Flow<Double?>

    @Query("SELECT * FROM dispensaciones WHERE id = :id")
    suspend fun getDispensacionById(id: String): DispensacionOptica?

    @Upsert
    suspend fun insertDispensacion(dispensacion: DispensacionOptica)
    
    @Update
    suspend fun updateDispensacion(dispensacion: DispensacionOptica)
    
    @Query("DELETE FROM dispensaciones")
    suspend fun deleteAll()

    @Query("UPDATE dispensaciones SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int
    
    @Query("SELECT * FROM dispensaciones")
    suspend fun getAllDispensacionesList(): List<DispensacionOptica>

    @Query("SELECT * FROM dispensaciones WHERE opticaId = :opticaId")
    suspend fun getDispensacionesListByOptica(opticaId: String): List<DispensacionOptica>

    @Query("SELECT * FROM dispensaciones WHERE fecha >= :start AND fecha <= :end")
    fun getDispensacionesByDateRange(start: LocalDate, end: LocalDate): Flow<List<DispensacionOptica>>

    @Query("SELECT * FROM dispensaciones WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId")
    fun getDispensacionesByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<DispensacionOptica>>

    @Query("SELECT SUM(montoTotal) FROM dispensaciones WHERE opticaId = :opticaId")
    fun getTotalVendidoForOptica(opticaId: String): Flow<Double?>

    @Query("SELECT SUM(montoPagado) FROM dispensaciones WHERE opticaId = :opticaId")
    fun getTotalPagadoForOptica(opticaId: String): Flow<Double?>

    /** excludeId vacío = contar todas las coincidencias (nueva dispensación). */
    @Query(
        """
        SELECT COUNT(*) FROM dispensaciones
        WHERE opticaId = :opticaId
        AND UPPER(TRIM(ot)) = UPPER(TRIM(:otNorm))
        AND (:excludeId = '' OR id != :excludeId)
        """
    )
    suspend fun countDispensacionesWithSameOt(opticaId: String, otNorm: String, excludeId: String): Int

    @Query("SELECT ot FROM dispensaciones WHERE opticaId = :opticaId AND ot LIKE ('OT-' || :year || '-%')")
    suspend fun getOtsWithYearPrefix(opticaId: String, year: String): List<String>
}

@Dao
interface PagoDao {
    @Query("SELECT * FROM pagos WHERE id = :id")
    suspend fun getPagoById(id: String): Pago?

    @Query("SELECT * FROM pagos WHERE dispensacionId = :dispensacionId ORDER BY fecha DESC")
    fun getPagosByDispensacion(dispensacionId: String): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE servicioExtraId = :servicioExtraId ORDER BY fecha DESC")
    fun getPagosByServicioExtra(servicioExtraId: String): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE fecha >= :start AND fecha <= :end ORDER BY fecha DESC")
    fun getPagosByDateRange(start: LocalDate, end: LocalDate): Flow<List<Pago>>

    @Query("SELECT * FROM pagos WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getPagosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<Pago>>

    @Upsert
    suspend fun insertPago(pago: Pago)

    @Update
    suspend fun updatePago(pago: Pago)

    @Delete
    suspend fun deletePago(pago: Pago)
    
    @Query("DELETE FROM pagos")
    suspend fun deleteAll()

    @Query("UPDATE pagos SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int
    
    @Query("SELECT * FROM pagos")
    suspend fun getAllPagos(): List<Pago>

    @Query("SELECT * FROM pagos WHERE opticaId = :opticaId")
    suspend fun getPagosListByOptica(opticaId: String): List<Pago>
}

@Dao
interface ServicioExtraDao {
    @Query("SELECT * FROM servicios_extra ORDER BY fecha DESC")
    fun getAllServicios(): Flow<List<ServicioExtra>>

    @Query("SELECT * FROM servicios_extra WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getAllServiciosForOptica(opticaId: String): Flow<List<ServicioExtra>>

    @Query("SELECT * FROM servicios_extra WHERE pacienteId = :pacienteId ORDER BY fecha DESC")
    fun getServiciosByPaciente(pacienteId: String): Flow<List<ServicioExtra>>

    @Query("SELECT * FROM servicios_extra WHERE id = :id")
    suspend fun getServicioById(id: String): ServicioExtra?

    @Upsert
    suspend fun insertServicio(servicio: ServicioExtra)

    @Update
    suspend fun updateServicio(servicio: ServicioExtra)

    @Delete
    suspend fun deleteServicio(servicio: ServicioExtra)
    
    @Query("DELETE FROM servicios_extra")
    suspend fun deleteAll()

    @Query("UPDATE servicios_extra SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("SELECT * FROM servicios_extra WHERE opticaId = :opticaId")
    suspend fun getServiciosListByOptica(opticaId: String): List<ServicioExtra>
}

@Dao
interface MonturaDao {
    @Query("SELECT * FROM monturas WHERE opticaId = :opticaId ORDER BY activo DESC, marca ASC, modelo ASC")
    fun getMonturasByOptica(opticaId: String): Flow<List<Montura>>

    @Query("SELECT * FROM monturas WHERE id = :id")
    suspend fun getMonturaById(id: String): Montura?

    @Query("UPDATE monturas SET stockActual = stockActual + :delta WHERE id = :monturaId AND opticaId = :opticaId AND (stockActual + :delta) >= 0")
    suspend fun adjustStock(monturaId: String, opticaId: String, delta: Int): Int

    @Upsert
    suspend fun insertMontura(montura: Montura)

    @Update
    suspend fun updateMontura(montura: Montura)

    @Delete
    suspend fun deleteMontura(montura: Montura)
}

@Dao
interface MonturaMovimientoDao {
    @Query("SELECT * FROM montura_movimientos WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getMovimientosByOptica(opticaId: String): Flow<List<MonturaMovimiento>>

    @Query("SELECT * FROM montura_movimientos WHERE monturaId = :monturaId ORDER BY fecha DESC")
    fun getMovimientosByMontura(monturaId: String): Flow<List<MonturaMovimiento>>

    @Upsert
    suspend fun insertMovimiento(movimiento: MonturaMovimiento)
}
