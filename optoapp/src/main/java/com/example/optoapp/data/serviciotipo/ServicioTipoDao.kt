package com.example.optoapp.data.serviciotipo

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// No delete operation on purpose: types are deactivated so past servicios keep resolving their name.
@Dao
interface ServicioTipoDao {

    @Query("SELECT * FROM servicio_tipos WHERE opticaId = :opticaId ORDER BY nombre COLLATE NOCASE")
    fun observeByOptica(opticaId: String): Flow<List<ServicioTipoEntity>>

    @Query("SELECT * FROM servicio_tipos WHERE opticaId = :opticaId AND activo = 1 ORDER BY nombre COLLATE NOCASE")
    fun observeActivosByOptica(opticaId: String): Flow<List<ServicioTipoEntity>>

    @Query("SELECT * FROM servicio_tipos WHERE opticaId = :opticaId")
    suspend fun getByOpticaIdList(opticaId: String): List<ServicioTipoEntity>

    @Query("SELECT * FROM servicio_tipos WHERE id = :id AND opticaId = :opticaId")
    suspend fun getById(id: String, opticaId: String): ServicioTipoEntity?

    @Upsert
    suspend fun upsert(entity: ServicioTipoEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ServicioTipoEntity>)
}
