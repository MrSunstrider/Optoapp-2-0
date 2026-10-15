package com.example.optoapp.domain

import com.example.optoapp.data.costolc.CostoLcEntity

// Picking a price for an unknown modalidad is only safe when the matrix leaves no choice.
object ModalidadLcMapper {

    private val PREFIJOS = mapOf(
        "diari" to "diario",
        "quincen" to "quincenal",
        "mensual" to "mensual",
        "anual" to "anual",
    )

    fun inferir(raw: String?): String? {
        val texto = TipoLcMapper.normalizar(raw) ?: return null
        val encontradas = PREFIJOS.filterKeys { texto.contains(it) }.values.toSet()
        return encontradas.singleOrNull()
    }

    fun elegir(candidatosMasRecientesPrimero: List<CostoLcEntity>, modalidad: String?): CostoLcEntity? {
        if (modalidad != null) return candidatosMasRecientesPrimero.firstOrNull { it.modalidad == modalidad }
        return candidatosMasRecientesPrimero.distinctBy { it.modalidad }.singleOrNull()
    }
}
