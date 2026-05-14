package com.example.optoapp.data.membership

import android.util.Log
import com.example.optoapp.data.OpticaDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OpticaQueryHelper @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun fetchOpticaNombre(opticaId: String): String {
        return try {
            val list = supabase.postgrest[TABLE_OPTICAS]
                .select { filter { eq("id", opticaId) } }
                .decodeList<OpticaDto>()
            list.firstOrNull()?.nombre.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "fetchOpticaNombre: opticaId=$opticaId", e)
            ""
        } catch (e: Exception) {
            Log.e(TAG, "fetchOpticaNombre: opticaId=$opticaId", e)
            ""
        }
    }

    companion object {
        private const val TAG = "OpticaQueryHelper"
        private const val TABLE_OPTICAS = "opticas"
    }
}
