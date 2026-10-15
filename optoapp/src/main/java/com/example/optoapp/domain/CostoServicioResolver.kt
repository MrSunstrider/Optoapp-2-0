package com.example.optoapp.domain

import com.example.optoapp.data.serviciotipo.ServicioTipoEntity
import com.example.optoapp.util.MontoDraftFormatting

data class CostoServicioDraft(
    val tipo: ServicioTipoEntity? = null,
    val servicioTipoId: String? = null,
    val realizadoPor: String? = null,
    val realizadoPorNombre: String = "",
    val costoDraft: String = "",
    val costoEditado: Boolean = false,
) {
    fun costoReal(): Double = MontoDraftFormatting.parseDraft(costoDraft) ?: 0.0
}

/**
 * The catalog only suggests a cost: once the user types one it is theirs, because the real cost of this
 * job may differ from the catalog price and must not be silently replaced by a later selection.
 */
object CostoServicioResolver {
    const val PROPIO = "propio"
    const val TERCERO = "tercero"

    fun seleccionarTipo(draft: CostoServicioDraft, tipo: ServicioTipoEntity?): CostoServicioDraft =
        precargar(draft.copy(tipo = tipo, servicioTipoId = tipo?.id))

    fun seleccionarRealizadoPor(draft: CostoServicioDraft, realizadoPor: String): CostoServicioDraft =
        precargar(
            draft.copy(
                realizadoPor = realizadoPor,
                realizadoPorNombre = if (realizadoPor == TERCERO) draft.realizadoPorNombre else "",
            ),
        )

    fun editarNombre(draft: CostoServicioDraft, nombre: String): CostoServicioDraft =
        if (draft.realizadoPor == TERCERO) draft.copy(realizadoPorNombre = nombre) else draft

    fun editarCosto(draft: CostoServicioDraft, costoDraft: String): CostoServicioDraft =
        draft.copy(costoDraft = costoDraft, costoEditado = true)

    private fun precargar(draft: CostoServicioDraft): CostoServicioDraft {
        if (draft.costoEditado) return draft
        val sugerido = when {
            draft.tipo == null -> null
            draft.realizadoPor == TERCERO -> draft.tipo.costoTercero
            draft.realizadoPor == PROPIO -> draft.tipo.costoPropio
            else -> null
        }
        return draft.copy(costoDraft = MontoDraftFormatting.formatDraft(sugerido))
    }
}
