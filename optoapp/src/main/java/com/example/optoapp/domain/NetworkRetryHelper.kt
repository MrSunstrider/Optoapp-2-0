package com.example.optoapp.domain

import android.util.Log
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.random.Random
import java.io.IOException
import javax.inject.Inject

/**
 * Supabase sync must survive transient network blips without failing the entire batch.
 * Extracted from [SyncFinanzasUseCase] to share retry logic across upload and download.
 */
class NetworkRetryHelper @Inject constructor() {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val NETWORK_RETRY_ATTEMPTS = 3
    }

    suspend fun retryNetwork(
        opName: String,
        block: suspend () -> Unit
    ) {
        var lastError: Exception? = null
        repeat(NETWORK_RETRY_ATTEMPTS) { attempt ->
            try {
                block()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Error en red en $opName: ${e.message}", e)
                lastError = e
                val shouldRetry = isTransientNetworkError(e)
                if (!shouldRetry || attempt == NETWORK_RETRY_ATTEMPTS - 1) throw e
                val backoffMs = (400L * (attempt + 1)) + Random.nextLong(0, 200)
                Log.w(TAG, "$opName fallo de red (intento ${attempt + 1}/$NETWORK_RETRY_ATTEMPTS). Reintentando en ${backoffMs}ms")
                delay(backoffMs)
            } catch (e: RestException) {
                Log.e(TAG, "Error REST en $opName (${e.statusCode}): ${e.message}", e)
                lastError = e
                val shouldRetry = isTransientNetworkError(e) || e.statusCode == 429
                if (!shouldRetry || attempt == NETWORK_RETRY_ATTEMPTS - 1) throw e
                val backoffMs = (400L * (attempt + 1)) + Random.nextLong(0, 200)
                Log.w(TAG, "$opName fallo REST (intento ${attempt + 1}/$NETWORK_RETRY_ATTEMPTS). Reintentando en ${backoffMs}ms")
                delay(backoffMs)
            }
        }
        lastError?.let { throw it }
    }

    fun isTransientNetworkError(e: Exception): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return msg.contains("timeout") ||
            msg.contains("timed out") ||
            msg.contains("429") ||
            msg.contains("too many requests") ||
            msg.contains("connect") && msg.contains("failed") ||
            msg.contains("unable to resolve host") ||
            msg.contains("network is unreachable") ||
            msg.contains("connection reset")
    }
}
