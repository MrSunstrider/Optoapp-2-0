package com.example.optoapp.domain

import com.example.optoapp.data.AppRoles
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity

data class ConfiguracionFinancieraDraft(
    val margenNetoObjetivo: Double = 15.0,
    val ticketPromedioObjetivo: Double? = null,
    val caidaVentasAlertaPct: Double = 10.0,
    val deudaViejaAlertaDias: Int = 30,
    val deudaTotalAlertaMonto: Double = 3000.0,
    val stockEstancadoAlertaDias: Int = 180,
    val stockBajoAlertaUnidades: Int = 2,
    val minVentasParaRecomendar: Int = 5,
    val frecuenciaRecalculoDias: Int = 1,
) {
    fun toEntity(opticaId: String) = ConfiguracionFinancieraEntity(
        opticaId = opticaId,
        margenNetoObjetivo = margenNetoObjetivo,
        ticketPromedioObjetivo = ticketPromedioObjetivo,
        caidaVentasAlertaPct = caidaVentasAlertaPct,
        deudaViejaAlertaDias = deudaViejaAlertaDias,
        deudaTotalAlertaMonto = deudaTotalAlertaMonto,
        stockEstancadoAlertaDias = stockEstancadoAlertaDias,
        stockBajoAlertaUnidades = stockBajoAlertaUnidades,
        minVentasParaRecomendar = minVentasParaRecomendar,
        frecuenciaRecalculoDias = frecuenciaRecalculoDias,
    )

    companion object {
        fun fromEntity(entity: ConfiguracionFinancieraEntity) = ConfiguracionFinancieraDraft(
            margenNetoObjetivo = entity.margenNetoObjetivo,
            ticketPromedioObjetivo = entity.ticketPromedioObjetivo,
            caidaVentasAlertaPct = entity.caidaVentasAlertaPct,
            deudaViejaAlertaDias = entity.deudaViejaAlertaDias,
            deudaTotalAlertaMonto = entity.deudaTotalAlertaMonto,
            stockEstancadoAlertaDias = entity.stockEstancadoAlertaDias,
            stockBajoAlertaUnidades = entity.stockBajoAlertaUnidades,
            minVentasParaRecomendar = entity.minVentasParaRecomendar,
            frecuenciaRecalculoDias = entity.frecuenciaRecalculoDias,
        )
    }
}

object ConfiguracionFinancieraPolicy {
    fun canWrite(rol: String?): Boolean =
        !rol.isNullOrBlank() && AppRoles.canManageUsers(rol)

    fun validate(draft: ConfiguracionFinancieraDraft): String? {
        if (draft.margenNetoObjetivo < 0) return "Margen neto objetivo no puede ser negativo"
        if (draft.caidaVentasAlertaPct < 0) return "Caída de ventas alerta no puede ser negativa"
        if (draft.deudaViejaAlertaDias < 0) return "Días de deuda alerta no puede ser negativo"
        if (draft.deudaTotalAlertaMonto < 0) return "Monto deuda alerta no puede ser negativo"
        if (draft.stockEstancadoAlertaDias < 0) return "Días stock estancado no puede ser negativo"
        if (draft.stockBajoAlertaUnidades < 0) return "Unidades stock bajo no puede ser negativo"
        if (draft.minVentasParaRecomendar < 0) return "Mínimo ventas no puede ser negativo"
        if (draft.frecuenciaRecalculoDias < 1) return "Frecuencia de recálculo debe ser al menos 1"
        draft.ticketPromedioObjetivo?.let {
            if (it < 0) return "Ticket promedio objetivo no puede ser negativo"
        }
        return null
    }
}
