package com.example.optoapp.domain.observer

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresListDataFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseObserver @Inject constructor(
    private val supabase: SupabaseClient
) {
    /**
     * Observa cambios en una tabla específica para una óptica.
     * Convierte los eventos de Supabase Realtime en un Flow de acciones.
     */
    fun observeTable(tableName: String, opticaId: String): Flow<List<PostgresAction>> {
        val channel = supabase.realtime.channel("realtime_$tableName")
        return channel.postgresListDataFlow(
            schema = "public",
            table = tableName,
            filter = "optica_id=eq.$opticaId"
        )
    }
}
