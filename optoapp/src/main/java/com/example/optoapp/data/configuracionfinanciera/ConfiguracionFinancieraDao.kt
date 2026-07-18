package com.example.optoapp.data.configuracionfinanciera

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracionFinancieraDao {
    @Query("SELECT * FROM configuracion_financiera WHERE opticaId = :opticaId")
    fun getByOpticaId(opticaId: String): Flow<ConfiguracionFinancieraEntity?>

    @Query("SELECT * FROM configuracion_financiera WHERE opticaId = :opticaId")
    suspend fun getByOpticaIdOnce(opticaId: String): ConfiguracionFinancieraEntity?

    @Upsert
    suspend fun upsert(config: ConfiguracionFinancieraEntity)

}
