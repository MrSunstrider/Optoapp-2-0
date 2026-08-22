package com.example.optoapp.data.regalodispensacion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RegaloDispensacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(regalo: RegaloDispensacionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(regalo: RegaloDispensacionEntity)

    @Query("SELECT * FROM regalos_dispensacion WHERE dispensacion_id = :dispId AND optica_id = :opticaId")
    suspend fun getByDispensacionId(dispId: String, opticaId: String): List<RegaloDispensacionEntity>

    @Query("SELECT * FROM regalos_dispensacion WHERE optica_id = :opticaId")
    suspend fun getByOpticaId(opticaId: String): List<RegaloDispensacionEntity>

    @Query("DELETE FROM regalos_dispensacion WHERE id = :id AND optica_id = :opticaId")
    suspend fun deleteById(id: String, opticaId: String): Int

    @Query("DELETE FROM regalos_dispensacion WHERE dispensacion_id = :dispId AND optica_id = :opticaId")
    suspend fun deleteByDispensacionId(dispId: String, opticaId: String)

    @Query("UPDATE regalos_dispensacion SET dispensacion_id = :targetId WHERE dispensacion_id = :sourceId AND optica_id = :opticaId")
    suspend fun reassignRegalosDispensacion(sourceId: String, targetId: String, opticaId: String): Int
}
