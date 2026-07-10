package com.example.optoapp.data.regalodispensacion

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RegaloDispensacionDao {
    @Insert
    suspend fun insert(regalo: RegaloDispensacionEntity)

    @Query("SELECT * FROM regalos_dispensacion WHERE dispensacion_id = :dispId")
    suspend fun getByDispensacionId(dispId: String): List<RegaloDispensacionEntity>

    @Query("DELETE FROM regalos_dispensacion WHERE dispensacion_id = :dispId")
    suspend fun deleteByDispensacionId(dispId: String)
}
