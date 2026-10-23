package com.example.optoapp.domain

enum class SeveridadAlerta {
    ROJO,
    AMBAR,
}

data class AlertaNegocio(
    val id: String,
    val severidad: SeveridadAlerta,
    val titulo: String,
    val detalle: String,
    val recomendacionId: String,
)

object AlertasDesdeRecomendaciones {
    const val LIMITE_DEFAULT = 5

    fun from(
        recomendaciones: List<Recomendacion>,
        limite: Int = LIMITE_DEFAULT,
    ): List<AlertaNegocio> {
        require(limite > 0) { "limite must be positive" }
        return recomendaciones
            .asSequence()
            .mapNotNull { rec ->
                val severidad = when (rec.prioridad) {
                    Prioridad.ALTA -> SeveridadAlerta.ROJO
                    Prioridad.MEDIA -> SeveridadAlerta.AMBAR
                    Prioridad.BAJA -> return@mapNotNull null
                }
                AlertaNegocio(
                    id = "alerta_${rec.id}",
                    severidad = severidad,
                    titulo = rec.titulo,
                    detalle = rec.hallazgo.ifBlank { rec.detalle },
                    recomendacionId = rec.id,
                )
            }
            .sortedBy { if (it.severidad == SeveridadAlerta.ROJO) 0 else 1 }
            .take(limite)
            .toList()
    }
}
