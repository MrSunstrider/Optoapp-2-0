package com.example.optoapp.domain

/**
 * Pre-upsert domain validation for finanzas upload quarantine.
 * Reasons always use the `quarantine:` prefix (never markSynced).
 */
object FinanzasUploadValidator {
    private val PAGO_TIPOS = setOf("Abono", "Pago completo", "Reembolso", "Reverso", "Anulación")
    private val DISP_ESTADOS = setOf("Pendiente", "Entregado", "Anulado", "Reclamada")
    private val SERV_ESTADOS = setOf("Pendiente", "Entregado", "Anulado")

    fun validatePago(
        tipo: String,
        monto: Double,
        dispensacionId: String?,
        servicioExtraId: String?,
        reversaPagoId: String?,
    ): String? {
        val t = tipo.trim()
        if (t !in PAGO_TIPOS) return "quarantine:invalid_tipo:$t"
        if (monto < 0.0) return "quarantine:negative_monto"
        val hasDisp = !dispensacionId.isNullOrBlank()
        val hasServ = !servicioExtraId.isNullOrBlank()
        if (hasDisp == hasServ) return "quarantine:xor_origen"
        if (t == "Reverso") {
            if (reversaPagoId.isNullOrBlank()) return "quarantine:reverso_missing_link"
        } else if (!reversaPagoId.isNullOrBlank()) {
            return "quarantine:reversa_on_non_reverso"
        }
        return null
    }

    fun validateDispensacionEstado(estadoEntrega: String): String? {
        val e = estadoEntrega.trim()
        if (e !in DISP_ESTADOS) return "quarantine:invalid_estado_entrega:$e"
        return null
    }

    fun validateServicioEstado(estado: String): String? {
        val e = estado.trim()
        if (e !in SERV_ESTADOS) return "quarantine:invalid_estado:$e"
        return null
    }

    /** Remote CHECK requires parent cache >= 0; floor only on upload payload, not local ledger. */
    fun safeParentBalanceForUpload(effectSum: Double): Double = effectSum.coerceAtLeast(0.0)

    fun isConstraintViolation(message: String?): Boolean {
        val m = message.orEmpty()
        return m.contains("23514") || m.contains("check constraint", ignoreCase = true)
    }

    fun isIsolatableUploadFailure(message: String?): Boolean {
        val m = message.orEmpty()
        return isConstraintViolation(m) ||
            m.contains("42501") ||
            m.contains("row-level security", ignoreCase = true)
    }

    fun parentMissingReason(parentKind: String, parentId: String): String =
        "quarantine:parent_missing:$parentKind:$parentId"
}
