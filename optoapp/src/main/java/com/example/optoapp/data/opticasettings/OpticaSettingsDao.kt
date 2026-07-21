package com.example.optoapp.data.opticasettings

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface OpticaSettingsDao {
    @Query("SELECT * FROM optica_settings WHERE opticaId = :opticaId")
    fun getByOpticaId(opticaId: String): Flow<OpticaSettingsEntity?>

    @Query("SELECT * FROM optica_settings WHERE opticaId = :opticaId")
    suspend fun getByOpticaIdOnce(opticaId: String): OpticaSettingsEntity?

    @Upsert
    suspend fun upsert(settings: OpticaSettingsEntity)
}
