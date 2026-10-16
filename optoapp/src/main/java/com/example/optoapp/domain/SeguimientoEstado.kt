package com.example.optoapp.domain

/**
 * Period-scoped follow-up on a recommendation (coach plan de acción).
 * Persisted per óptica + yyyy-MM period.
 */
enum class SeguimientoEstado {
    HECHO,
    MAS_TARDE,
    NO_APLICA,
    ;

    /** Legacy useful flag: acted counts as useful for older analytics. */
    val fueUtil: Boolean
        get() = this == HECHO

    companion object {
        fun fromStorage(raw: String?): SeguimientoEstado? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.name == raw }
        }
    }
}
