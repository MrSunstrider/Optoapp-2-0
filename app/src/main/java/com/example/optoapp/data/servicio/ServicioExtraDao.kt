package com.example.optoapp.data.servicio

import androidx.room.*
import com.example.optoapp.data.ServicioExtra
import kotlinx.coroutines.flow.Flow

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
