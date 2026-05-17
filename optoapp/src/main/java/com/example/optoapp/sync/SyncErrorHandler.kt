package com.example.optoapp.sync

import java.io.IOException

/**
 * Returns a human-readable label for the given exception type.
 *
 * - [IOException] → "Error en red"
 * - Other exceptions → "Error inesperado"
 *
 * [CancellationException] is NOT handled here — callers MUST catch it separately
 * before calling this function.
 */
internal fun errorLabelForException(e: Throwable): String = when (e) {
    is IOException -> "Error en red"
    else -> "Error inesperado"
}
