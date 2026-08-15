package com.example.optoapp.util

import android.content.Context
import com.example.optoapp.data.BackgroundError
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistencia de errores de background. Sin esto el historial muere con el proceso y
 * el usuario nunca llega a copiarlo para soporte.
 */
interface BackgroundErrorStore {
    fun load(): List<BackgroundError>
    fun save(errors: List<BackgroundError>)

    object NoOp : BackgroundErrorStore {
        override fun load(): List<BackgroundError> = emptyList()
        override fun save(errors: List<BackgroundError>) = Unit
    }
}

/** Serialización con separadores de control: los mensajes traen saltos de línea y JSON. */
object BackgroundErrorCodec {
    private const val FIELD = "\u001F"
    private const val RECORD = "\u001E"

    fun encode(errors: List<BackgroundError>): String =
        errors.joinToString(RECORD) { "${it.timestampMs}$FIELD${it.source}$FIELD${it.message}" }

    fun decode(raw: String?): List<BackgroundError> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECORD).mapNotNull { record ->
            val parts = record.split(FIELD)
            if (parts.size < 3) return@mapNotNull null
            val ts = parts[0].toLongOrNull() ?: return@mapNotNull null
            BackgroundError(
                source = parts[1],
                message = parts.drop(2).joinToString(FIELD),
                timestampMs = ts,
            )
        }
    }
}

@Singleton
class SharedPrefsBackgroundErrorStore @Inject constructor(
    @ApplicationContext context: Context,
) : BackgroundErrorStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): List<BackgroundError> =
        runCatching { BackgroundErrorCodec.decode(prefs.getString(KEY_ERRORS, null)) }.getOrDefault(emptyList())

    override fun save(errors: List<BackgroundError>) {
        runCatching {
            prefs.edit().putString(KEY_ERRORS, BackgroundErrorCodec.encode(errors)).apply()
        }
    }

    private companion object {
        const val PREFS_NAME = "sync_background_errors"
        const val KEY_ERRORS = "errors"
    }
}
