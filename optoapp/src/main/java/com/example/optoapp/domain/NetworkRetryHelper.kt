package com.example.optoapp.domain

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject

/**
 * Retry logic for transient network failures during sync.
 * Extracted from [SyncFinanzasUseCase].
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
                val backoffMs = 400L * (attempt + 1)
                Log.w(TAG, "$opName fallo de red (intento ${attempt + 1}/$NETWORK_RETRY_ATTEMPTS). Reintentando en ${backoffMs}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado en $opName: ${e.message}", e)
                lastError = e
                val shouldRetry = isTransientNetworkError(e)
                if (!shouldRetry || attempt == NETWORK_RETRY_ATTEMPTS - 1) throw e
                val backoffMs = 400L * (attempt + 1)
                Log.w(TAG, "$opName fallo de red (intento ${attempt + 1}/$NETWORK_RETRY_ATTEMPTS). Reintentando en ${backoffMs}ms")
                delay(backoffMs)
            }
        }
        lastError?.let { throw it }
    }

    fun isTransientNetworkError(e: Exception): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return msg.contains("timeout") ||
            msg.contains("timed out") ||
            msg.contains("connect") && msg.contains("failed") ||
            msg.contains("unable to resolve host") ||
            msg.contains("network is unreachable") ||
            msg.contains("connection reset")
    }
}
