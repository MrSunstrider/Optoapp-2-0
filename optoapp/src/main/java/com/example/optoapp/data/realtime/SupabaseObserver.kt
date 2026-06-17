package com.example.optoapp.data.realtime

import com.example.optoapp.domain.observer.MembershipObserver
import com.example.optoapp.domain.observer.TableObserver
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresAction.Insert
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseObserver @Inject constructor(
    private val supabase: SupabaseClient
) : MembershipObserver, TableObserver {

    override fun observeTable(tableName: String, opticaId: String): Flow<Unit> {
        // opticaId en el nombre del canal garantiza aislamiento por instancia
        val channel = supabase.realtime.channel("realtime_${tableName}_$opticaId")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = tableName
        }.map { }
    }

    override fun observeNewMembership(): Flow<Unit> = flow {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@flow
        val channelId = "membership_watch_${userId.take(8)}"
        val channel = supabase.realtime.channel(channelId)
        val events = channel.postgresChangeFlow<Insert>(schema = "public") {
            table = "usuario_optica"
            filter("user_id", FilterOperator.EQ, userId)
        }
        channel.subscribe()
        try {
            emitAll(events.map { })
        } finally {
            runCatching { supabase.realtime.removeChannel(channel) }
        }
    }
}
