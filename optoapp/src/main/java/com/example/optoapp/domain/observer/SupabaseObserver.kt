package com.example.optoapp.domain.observer

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresAction.Insert
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseObserver @Inject constructor(
    private val supabase: SupabaseClient
) {
    fun observeTable(tableName: String, opticaId: String): Flow<PostgresAction> {
        // Usamos opticaId en el nombre del canal para asegurar aislamiento por instancia
        val channel = supabase.realtime.channel("realtime_${tableName}_$opticaId")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = tableName
            // Nota: El filtrado fino por optica_id se delega a las políticas RLS de Supabase
            // para garantizar que el cliente solo reciba lo que le corresponde.
        }
    }

    fun observeNewMembershipForCurrentUser(): Flow<Insert> = flow {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@flow
        val channelId = "membership_watch_${userId.take(8)}"
        val channel = supabase.realtime.channel(channelId)
        val events = channel.postgresChangeFlow<Insert>(schema = "public") {
            table = "usuario_optica"
            filter = "user_id=eq.$userId"
        }
        channel.subscribe()
        try {
            emitAll(events)
        } finally {
            runCatching { supabase.realtime.removeChannel(channel) }
        }
    }
}
