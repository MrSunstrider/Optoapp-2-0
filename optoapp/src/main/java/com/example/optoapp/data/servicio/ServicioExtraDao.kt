package com.example.optoapp.data.servicio

import androidx.room.*
import com.example.optoapp.data.ServicioExtra
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ServicioExtraDao {
    @Query("SELECT * FROM servicios_extra WHERE opticaId = :opticaId ORDER BY fecha DESC")
    fun getAllServiciosForOptica(opticaId: String): Flow<List<ServicioExtra>>

    @Query("SELECT * FROM servicios_extra WHERE id = :id")
    suspend fun getServicioById(id: String): ServicioExtra?

    @Upsert
    suspend fun insertServicio(servicio: ServicioExtra)

    @Query(
        """
        UPDATE servicios_extra SET ot=:ot, descripcion=:descripcion,
        montoTotal=:montoTotal, aCuenta=:aCuenta, estado=:estado,
        fecha=:fecha, pacienteId=:pacienteId, metodoPago=:metodoPago,
        opticaId=:opticaId, fecha_entrega=:fechaEntrega,
        updatedAt=:updatedAt, updatedBy=:updatedBy
        WHERE id=:id AND opticaId=:opticaId
    """,
    )
    suspend fun updateServicio(
        id: String,
        opticaId: String,
        ot: String,
        descripcion: String,
        montoTotal: Double,
        aCuenta: Double,
        estado: String,
        fecha: java.time.LocalDate,
        pacienteId: String?,
        metodoPago: String,
        fechaEntrega: java.time.LocalDate?,
        updatedAt: String?,
        updatedBy: String?,
    ): Int

    @Query("SELECT * FROM servicios_extra WHERE fecha >= :start AND fecha <= :end AND opticaId = :opticaId ORDER BY fecha DESC")
    fun getServiciosByDateRangeForOptica(start: LocalDate, end: LocalDate, opticaId: String): Flow<List<ServicioExtra>>

    @Query("SELECT * FROM servicios_extra WHERE id IN (:ids) AND opticaId = :opticaId")
    suspend fun getServiciosByIds(ids: List<String>, opticaId: String): List<ServicioExtra>

    @Query("DELETE FROM servicios_extra WHERE id = :id AND opticaId = :opticaId")
    suspend fun deleteServicio(id: String, opticaId: String): Int

    @Query("UPDATE servicios_extra SET opticaId = :newOpticaId WHERE opticaId = 'mi_optica_base'")
    suspend fun reassignFromLegacyMiOpticaBase(newOpticaId: String): Int

    @Query("SELECT * FROM servicios_extra WHERE opticaId = :opticaId")
    suspend fun getServiciosListByOptica(opticaId: String): List<ServicioExtra>
}
