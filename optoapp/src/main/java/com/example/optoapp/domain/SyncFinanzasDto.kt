package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate


@Serializable
data class DispensacionRemota(
    val id: String,
    val ot: String? = null,
    @SerialName("montura_id") val monturaId: String? = null,
    @SerialName("paciente_id") val pacienteId: String,
    val fecha: String,
    @SerialName("optica_id") val opticaId: String,
    @SerialName("tipo_montura") val tipoMontura: String? = null,
    @SerialName("material_montura") val materialMontura: String? = null,
    @SerialName("tipo_lente") val tipoLente: String? = null,
    @SerialName("material_lente") val materialLente: String? = null,
    val tratamientos: String? = null,
    @SerialName("color_lente") val colorLente: String? = null,
    @SerialName("notas_diseno") val notasDiseno: String? = null,
    @SerialName("origen_montura") val origenMontura: String? = null,
    @SerialName("tipo_aro") val tipoAro: String? = null,
    @SerialName("descripcion_montura") val descripcionMontura: String? = null,
    @SerialName("monto_total") val montoTotal: Double = 0.0,
    @SerialName("metodo_pago") val metodoPago: String? = null,
    @SerialName("monto_pagado") val montoPagado: Double = 0.0,
    @SerialName("estado_entrega") val estadoEntrega: String? = null,
    @SerialName("fecha_entrega") val fechaEntrega: String? = null,
    @SerialName("fecha_vencimiento_garantia") val fechaVencimientoGarantia: String? = null,
    @SerialName("distancia_lente") val distanciaLente: String? = null,
    val altura: String? = null,
    @SerialName("sub_tipo_bifocal") val subTipoBifocal: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
) {
    fun toEntity() = DispensacionOptica(
        id = id, ot = ot ?: "", monturaId = monturaId ?: "", pacienteId = pacienteId,
        fecha = LocalDate.parse(fecha), opticaId = optId(opticaId),
        tipoMontura = tipoMontura ?: "", materialMontura = materialMontura ?: "",
        tipoLente = tipoLente ?: "", materialLente = materialLente ?: "",
        tratamientos = tratamientos?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        colorLente = colorLente ?: "", notasDiseno = notasDiseno ?: "",
        origenMontura = origenMontura ?: "", tipoAro = tipoAro ?: "",
        descripcionMontura = descripcionMontura ?: "", montoTotal = montoTotal,
        metodoPago = metodoPago ?: "", montoPagado = montoPagado,
        estadoEntrega = estadoEntrega ?: "",
        fechaEntrega = fechaEntrega?.let(LocalDate::parse),
        fechaVencimientoGarantia = fechaVencimientoGarantia?.let(LocalDate::parse),
        distanciaLente = distanciaLente ?: "",
        altura = altura ?: "",
        subTipoBifocal = subTipoBifocal ?: "",
        updatedAt = updatedAt,
        updatedBy = updatedBy
    )

    internal fun optId(remoteId: String) = remoteId.ifBlank { "mi_optica_base" }
}

@Serializable
data class ServicioRemoto(
    val id: String,
    val ot: String = "",
    val descripcion: String = "",
    @SerialName("monto_total") val montoTotal: Double = 0.0,
    @SerialName("a_cuenta") val aCuenta: Double = 0.0,
    val estado: String = "",
    val fecha: String,
    @SerialName("paciente_id") val pacienteId: String? = null,
    @SerialName("metodo_pago") val metodoPago: String = "",
    @SerialName("optica_id") val opticaId: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
) {
    fun toEntity() = ServicioExtra(
        id = id,
        ot = ot.trim().remotoOtServicioExtraToLocal(),
        descripcion = descripcion.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.DESCRIPCION_VACIA },
        montoTotal = montoTotal.coerceAtLeast(0.0),
        aCuenta = aCuenta.coerceAtLeast(0.0).coerceAtMost(montoTotal.coerceAtLeast(0.0)),
        estado = estado.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.ESTADO_VACIO },
        fecha = LocalDate.parse(fecha),
        pacienteId = pacienteId?.takeIf { it.isNotBlank() },
        metodoPago = metodoPago.remotoServicioExtraMetodoToLocal(),
        opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK },
        updatedAt = updatedAt,
        updatedBy = updatedBy
    )
}

@Serializable
data class PagoRemoto(
    val id: String,
    @SerialName("dispensacion_id") val dispensacionId: String? = null,
    @SerialName("servicio_extra_id") val servicioExtraId: String? = null,
    val fecha: String,
    val tipo: String = "",
    val monto: Double = 0.0,
    @SerialName("metodo_pago") val metodoPago: String = "",
    val nota: String? = null,
    @SerialName("optica_id") val opticaId: String = "mi_optica_base",
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
) {
    fun toEntity() = Pago(
        id = id,
        dispensacionId = dispensacionId.normalizeOptionalFk(),
        servicioExtraId = servicioExtraId.normalizeOptionalFk(),
        fecha = LocalDate.parse(fecha),
        tipo = tipo,
        monto = monto,
        metodoPago = metodoPago,
        nota = nota ?: "",
        opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK },
        updatedAt = updatedAt,
        updatedBy = updatedBy
    )
}

@Serializable
data class DispensacionItemRemota(
    val id: String,
    @SerialName("dispensacion_id") val dispensacionId: String,
    @SerialName("tipo_lente") val tipoLente: String? = null,
    @SerialName("material_lente") val materialLente: String? = null,
    val tratamientos: String? = null,
    @SerialName("color_lente") val colorLente: String? = null,
    @SerialName("distancia_lente") val distanciaLente: String? = null,
    val altura: String? = null,
    @SerialName("sub_tipo_bifocal") val subTipoBifocal: String? = null,
    @SerialName("notas_diseno") val notasDiseno: String? = null,
    @SerialName("montura_id") val monturaId: String? = null,
    @SerialName("origen_montura") val origenMontura: String? = null,
    @SerialName("tipo_aro") val tipoAro: String? = null,
    @SerialName("material_montura") val materialMontura: String? = null,
    @SerialName("descripcion_montura") val descripcionMontura: String? = null,
    @SerialName("tipo_montura") val tipoMontura: String? = null,
    @SerialName("optica_id") val opticaId: String = "mi_optica_base"
) {
    fun toEntity() = DispensacionItem(
        id = id, dispensacionId = dispensacionId,
        tipoLente = tipoLente ?: "", materialLente = materialLente ?: "",
        tratamientos = tratamientos?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        colorLente = colorLente ?: "", distanciaLente = distanciaLente ?: "",
        altura = altura ?: "", subTipoBifocal = subTipoBifocal ?: "",
        notasDiseno = notasDiseno ?: "",
        monturaId = monturaId ?: "", origenMontura = origenMontura ?: "",
        tipoAro = tipoAro ?: "", materialMontura = materialMontura ?: "",
        descripcionMontura = descripcionMontura ?: "", tipoMontura = tipoMontura ?: "",
        opticaId = opticaId.ifBlank { "mi_optica_base" }
    )
}

data class FinanzasSyncResult(
    val uploadedDispensaciones: Int,
    val uploadedDispensacionItems: Int = 0,
    val uploadedServicios: Int,
    val uploadedPagos: Int,
    val downloadedDispensaciones: Int,
    val downloadedDispensacionItems: Int = 0,
    val downloadedServicios: Int,
    val downloadedPagos: Int
)

@Serializable
internal data class ServicioRemotoLookup(
    val id: String,
    val ot: String = ""
)

@Serializable
internal data class DispensacionRemotaLookup(
    val id: String,
    val ot: String? = null
)


fun DispensacionOptica.toRemoto(): DispensacionRemota = DispensacionRemota(
    id = id, ot = ot, monturaId = monturaId, pacienteId = pacienteId, fecha = fecha.toString(), opticaId = opticaId,
    tipoMontura = tipoMontura, materialMontura = materialMontura,
    tipoLente = tipoLente, materialLente = materialLente,
    tratamientos = tratamientos.joinToString(","), colorLente = colorLente,
    notasDiseno = notasDiseno, origenMontura = origenMontura,
    tipoAro = tipoAro, descripcionMontura = descripcionMontura,
    montoTotal = montoTotal, metodoPago = metodoPago,
    montoPagado = montoPagado, estadoEntrega = estadoEntrega,
    fechaEntrega = fechaEntrega?.toString(),
    fechaVencimientoGarantia = fechaVencimientoGarantia?.toString(),
    distanciaLente = distanciaLente, altura = altura, subTipoBifocal = subTipoBifocal,
    updatedAt = updatedAt, updatedBy = updatedBy
)

fun Pago.toRemoto(): PagoRemoto = PagoRemoto(
    id = id,
    dispensacionId = dispensacionId.normalizeOptionalFk(),
    servicioExtraId = servicioExtraId.normalizeOptionalFk(),
    fecha = fecha.toString(),
    tipo = tipo.trim().ifBlank { FinanzasRemoteDefaults.Pago.TIPO_VACIO },
    monto = monto,
    metodoPago = metodoPago.trim().ifBlank { FinanzasRemoteDefaults.Pago.METODO_PAGO_VACIO },
    nota = nota.trim(),
    opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK },
    updatedAt = updatedAt,
    updatedBy = updatedBy
)

fun DispensacionItem.toRemoto(): DispensacionItemRemota = DispensacionItemRemota(
    id = id, dispensacionId = dispensacionId,
    tipoLente = tipoLente, materialLente = materialLente,
    tratamientos = tratamientos.joinToString(","), colorLente = colorLente,
    distanciaLente = distanciaLente, altura = altura,
    subTipoBifocal = subTipoBifocal, notasDiseno = notasDiseno,
    monturaId = monturaId, origenMontura = origenMontura,
    tipoAro = tipoAro, materialMontura = materialMontura,
    descripcionMontura = descripcionMontura, tipoMontura = tipoMontura,
    opticaId = opticaId.ifBlank { "mi_optica_base" }
)

fun ServicioExtra.toRemoto(): ServicioRemoto = ServicioRemoto(
    id = id,
    ot = ot.trim(),
    descripcion = descripcion.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.DESCRIPCION_VACIA },
    montoTotal = montoTotal.coerceAtLeast(0.0),
    aCuenta = aCuenta.coerceAtLeast(0.0).coerceAtMost(montoTotal.coerceAtLeast(0.0)),
    estado = estado.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.ESTADO_VACIO },
    fecha = fecha.toString(),
    pacienteId = pacienteId?.trim()?.takeIf { it.isNotBlank() },
    metodoPago = metodoPago.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW },
    opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK },
    updatedAt = updatedAt,
    updatedBy = updatedBy
)


internal fun String?.normalizeOptionalFk(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

internal fun String.remotoServicioExtraMetodoToLocal(): String =
    if (this == FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW) "" else this

internal fun String.remotoOtServicioExtraToLocal(): String =
    if (this == FinanzasRemoteDefaults.ServicioExtra.OT_VACIA) "" else this

internal fun normalizedOtForUnique(ot: String?): String? =
    ot?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
