package com.example.optoapp.data.configuracionfinanciera

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracionFinancieraDao {
    @Query("SELECT * FROM configuracion_financiera WHERE opticaId = :opticaId")
    fun getByOpticaId(opticaId: String): Flow<ConfiguracionFinancieraEntity?>

    @Upsert
    suspend fun upsert(config: ConfiguracionFinancieraEntity)

    @Query("DELETE FROM configuracion_financiera")
    suspend fun deleteAll()
}
