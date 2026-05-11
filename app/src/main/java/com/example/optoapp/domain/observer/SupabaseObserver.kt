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
        // Usamos opticaId en el nombre del canal para asegurar aislamiento por instancia
        val channel = supabase.realtime.channel("realtime_${tableName}_$opticaId")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = tableName
            // Nota: El filtrado fino por optica_id se delega a las políticas RLS de Supabase 
            // para garantizar que el cliente solo reciba lo que le corresponde.
        }
    }
}
