package com.example.optoapp.domain.observer

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseObserver @Inject constructor(
    private val supabase: SupabaseClient
) {
    /**
     * Observa cambios en una tabla específica para una óptica.
     * Convierte los eventos de Supabase Realtime en un Flow de acciones (Insert, Update, Delete).
     */
    fun observeTable(tableName: String, opticaId: String): Flow<PostgresAction> {
        val channel = supabase.realtime.channel("realtime_$tableName")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = tableName
            // El filtrado por optica_id se maneja usualmente vía RLS en Realtime o parámetros adicionales
        }
    }
}
