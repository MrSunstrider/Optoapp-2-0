package com.example.optoapp.data.regaloservicio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RegaloServicioExtraDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(regalo: RegaloServicioExtraEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(regalo: RegaloServicioExtraEntity)

    @Query(
        "SELECT * FROM regalos_servicio_extra WHERE servicio_extra_id = :servicioId AND optica_id = :opticaId",
    )
    suspend fun getByServicioId(servicioId: String, opticaId: String): List<RegaloServicioExtraEntity>

    @Query("SELECT * FROM regalos_servicio_extra WHERE optica_id = :opticaId")
    suspend fun getByOpticaId(opticaId: String): List<RegaloServicioExtraEntity>

    @Query("DELETE FROM regalos_servicio_extra WHERE servicio_extra_id = :servicioId AND optica_id = :opticaId")
    suspend fun deleteByServicioId(servicioId: String, opticaId: String)

    @Query("DELETE FROM regalos_servicio_extra WHERE id = :id AND optica_id = :opticaId")
    suspend fun deleteById(id: String, opticaId: String): Int
}
