package com.example.optoapp.domain

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import kotlin.random.Random

/**
 * Supabase sync must survive transient network blips without failing the entire batch.
 * Extracted from [SyncFinanzasUseCase] to share retry logic across upload and download.
 *
 * Uses [SyncLogger] instead of android.util.Log directly (A4 POC).
 */
class NetworkRetryHelper @Inject constructor(
    private val logger: SyncLogger,
    private val supabase: SupabaseClient,
) {
    companion object {
        private const val TAG = "SyncFinanzas"
        private const val NETWORK_RETRY_ATTEMPTS = 3
    }

    suspend fun retryNetwork(
        opName: String,
        block: suspend () -> Unit,
    ) {
        var lastError: Exception? = null
        repeat(NETWORK_RETRY_ATTEMPTS) { attempt ->
            try {
                block()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                logger.e(TAG, "Error en red en $opName: ${e.message}", e)
                lastError = e
                val shouldRetry = isRetryable(e)
                if (!shouldRetry || attempt == NETWORK_RETRY_ATTEMPTS - 1) throw e
                val backoffMs = (400L * (1 shl attempt)) + Random.nextLong(0, 200)
                logger.w(TAG, "$opName fallo de red (intento ${attempt + 1}/$NETWORK_RETRY_ATTEMPTS). Reintentando en ${backoffMs}ms")
                delay(backoffMs)
            } catch (e: RestException) {
                // REQ-JWT-003/004: intercept JWT-expired before normal retry
                if (isJwtExpired(e)) {
                    logger.w(TAG, "$opName: JWT expirado detectado, refrescando sesión")
                    if (refreshSessionForRetry()) {
                        try {
                            block()
                            return
                        } catch (retryError: CancellationException) {
                            throw retryError
                        } catch (retryError: Exception) {
                            logger.e(TAG, "$opName: reintento post-refresh JWT falló: ${retryError.message}", retryError)
                            throw retryError
                        }
                    }
                    logger.e(TAG, "$opName: no se pudo refrescar sesión para reintento JWT", e)
                    throw e
                }
                logger.e(TAG, "Error REST en $opName (${e.statusCode}): ${e.message}", e)
                lastError = e
                val shouldRetry = isRetryable(e)
                if (!shouldRetry || attempt == NETWORK_RETRY_ATTEMPTS - 1) throw e
                val backoffMs = (400L * (1 shl attempt)) + Random.nextLong(0, 200)
                logger.w(TAG, "$opName fallo REST (intento ${attempt + 1}/$NETWORK_RETRY_ATTEMPTS). Reintentando en ${backoffMs}ms")
                delay(backoffMs)
            }
        }
        lastError?.let { throw it }
    }

    internal fun isRetryable(e: Exception): Boolean {
        if (e is RestException) return e.statusCode in 429..599
        if (e is IOException) return true
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("timeout") || msg.contains("timed out") || msg.contains("connection")
    }

    private fun isJwtExpired(e: RestException): Boolean {
        if (e.statusCode != 401) return false
        val body = e.error ?: return false
        return "JWT expired" in body || "PGRST303" in body
    }

    private suspend fun refreshSessionForRetry(): Boolean {
        return try {
            supabase.auth.refreshCurrentSession()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(TAG, "Error refrescando sesión para reintento JWT: ${e.message}", e)
            false
        }
    }
}
