package com.example.optoapp.domain

/** Outcome of persisting an OT cost into a cost matrix (lenses or bevel). */
sealed interface ResultadoCostoMatriz {
    data class Creado(val id: String) : ResultadoCostoMatriz
    data class Versionado(val id: String) : ResultadoCostoMatriz
    data class Rechazado(val motivo: String) : ResultadoCostoMatriz
}
