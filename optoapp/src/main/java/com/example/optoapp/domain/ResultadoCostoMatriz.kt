package com.example.optoapp.domain

sealed interface ResultadoCostoMatriz {
    data class Creado(val id: String) : ResultadoCostoMatriz
    data class Versionado(val id: String) : ResultadoCostoMatriz
    // WHY: same open-row price must not create a new matrix version on every OT save.
    data class SinCambio(val id: String) : ResultadoCostoMatriz
    data class Rechazado(val motivo: String) : ResultadoCostoMatriz
}
