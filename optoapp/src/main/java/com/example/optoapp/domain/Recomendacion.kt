package com.example.optoapp.domain

enum class RecomendacionTipo {
    COBRAR, ALERTA_CAIDA, MEJORAR_PRECIO, LIQUIDAR_STOCK,
    VENDER_MAS_DE, REDUCIR_GASTO
}

enum class Prioridad { ALTA, MEDIA, BAJA }

data class DatosAccion(
    val pacienteIds: List<String>? = null,
    val productoIds: List<String>? = null,
    val montoTotal: Double? = null
)

data class Recomendacion(
    val id: String,
    val tipo: RecomendacionTipo,
    val titulo: String,
    val detalle: String,
    val impactoEstimado: String? = null,
    val prioridad: Prioridad,
    val accion: String? = null,
    val datosAccion: DatosAccion? = null
)
