package com.example.optoapp.data.servicio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ServicioExtraItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ServicioExtraItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ServicioExtraItem)

    @Query(
        "SELECT * FROM servicio_extra_items WHERE servicio_extra_id = :servicioId AND optica_id = :opticaId ORDER BY rowid",
    )
    suspend fun getByServicioId(servicioId: String, opticaId: String): List<ServicioExtraItem>

    @Query("SELECT * FROM servicio_extra_items WHERE optica_id = :opticaId")
    suspend fun getByOpticaId(opticaId: String): List<ServicioExtraItem>

    @Query("DELETE FROM servicio_extra_items WHERE servicio_extra_id = :servicioId AND optica_id = :opticaId")
    suspend fun deleteByServicioId(servicioId: String, opticaId: String)

    @Query("DELETE FROM servicio_extra_items WHERE id = :id AND optica_id = :opticaId")
    suspend fun deleteById(id: String, opticaId: String): Int
}
