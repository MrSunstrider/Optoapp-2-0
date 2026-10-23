package com.example.optoapp.domain

enum class SeguimientoEstado {
    HECHO,
    EN_CURSO,
    NO_APLICA,
    ;

    val fueUtil: Boolean
        get() = this == HECHO

    companion object {
        fun fromStorage(raw: String?): SeguimientoEstado? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            if (trimmed == "MAS_TARDE") return EN_CURSO
            return entries.firstOrNull { it.name == trimmed }
        }
    }
}
