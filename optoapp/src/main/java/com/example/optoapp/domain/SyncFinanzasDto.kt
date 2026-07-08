package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import com.example.optoapp.data.venta.Venta
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
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
    @SerialName("filtro_discromatopsia_tipo") val filtroDiscromatopsiaTipo: String = "",
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
        filtroDiscromatopsiaTipo = filtroDiscromatopsiaTipo,
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
    @SerialName("fecha_entrega") val fechaEntrega: String? = null,
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
        updatedBy = updatedBy,
        fechaEntrega = fechaEntrega?.let(LocalDate::parse)
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
    @SerialName("updated_by") val updatedBy: String? = null,
    @SerialName("venta_id") val ventaId: String? = null
) {
    fun toEntity() = Pago(
        id = id,
        dispensacionId = dispensacionId.normalizeOptionalFk(),
        servicioExtraId = servicioExtraId.normalizeOptionalFk(),
        fecha = LocalDate.parse(fecha),
        tipo = tipo,
        monto = monto,
        metodoPago = metodoPago.remotoServicioExtraMetodoToLocal(),
        nota = nota ?: "",
        opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK },
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        ventaId = ventaId
    )
}

@Serializable
data class VentaRemota(
    val id: String,
    @SerialName("optica_id") val opticaId: String,
    val origen: String,
    @SerialName("origen_id") val origenId: String,
    @SerialName("paciente_id") val pacienteId: String = "",
    val fecha: String,
    @SerialName("fecha_entrega") val fechaEntrega: String? = null,
    @SerialName("monto_total") val montoTotal: Double,
    @SerialName("costo_unitario_snapshot") val costoUnitarioSnapshot: Double? = null,
    val estado: String,
    @SerialName("categoria_producto_id") val categoriaProductoId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
) {
    fun toEntity() = Venta(
        id = id,
        opticaId = opticaId.trim().ifBlank { "mi_optica_base" },
        origen = origen,
        origenId = origenId,
        pacienteId = pacienteId,
        fecha = LocalDate.parse(fecha),
        fechaEntrega = fechaEntrega?.let(LocalDate::parse),
        montoTotal = montoTotal,
        costoUnitarioSnapshot = costoUnitarioSnapshot,
        estado = estado,
        categoriaProductoId = categoriaProductoId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        updatedBy = updatedBy
    )
}

fun Venta.toRemoto() = VentaRemota(
    id = id,
    opticaId = opticaId,
    origen = origen,
    origenId = origenId,
    pacienteId = pacienteId.ifBlank { "" },
    fecha = fecha.toString(),
    fechaEntrega = fechaEntrega?.toString(),
    montoTotal = montoTotal,
    costoUnitarioSnapshot = costoUnitarioSnapshot,
    estado = estado.ifBlank { "Pendiente" },
    categoriaProductoId = categoriaProductoId,
    createdAt = createdAt,
    updatedAt = updatedAt ?: Instant.now().toString(),
    updatedBy = null
)

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
    @SerialName("filtro_discromatopsia_tipo") val filtroDiscromatopsiaTipo: String = "",
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
        filtroDiscromatopsiaTipo = filtroDiscromatopsiaTipo,
        notasDiseno = notasDiseno ?: "",
        monturaId = monturaId ?: "", origenMontura = origenMontura ?: "",
        tipoAro = tipoAro ?: "", materialMontura = materialMontura ?: "",
        descripcionMontura = descripcionMontura ?: "", tipoMontura = tipoMontura ?: "",
        opticaId = opticaId.ifBlank { "mi_optica_base" }
    )
}

// ── GastoOperativo remote DTO ──────────────────────────────────────────

@Serializable
data class GastoOperativoRemoto(
    val id: String,
    @SerialName("optica_id") val opticaId: String,
    val categoria: String,
    val descripcion: String? = null,
    val monto: Double,
    val fecha: String,
    @SerialName("fecha_programada") val fechaProgramada: String? = null,
    val nota: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("es_recurrente") val esRecurrente: Boolean = false,
    val frecuencia: String = "mensual"
)

fun GastoOperativoEntity.toRemoto(): GastoOperativoRemoto = GastoOperativoRemoto(
    id = id,
    opticaId = opticaId,
    categoria = categoria,
    descripcion = descripcion,
    monto = monto,
    fecha = fecha.toString(),
    fechaProgramada = fechaProgramada?.toString(),
    nota = nota,
    createdAt = createdAt,
    esRecurrente = esRecurrente,
    frecuencia = frecuencia
)

// ── ResumenDiario remote DTO ───────────────────────────────────────────

@Serializable
data class ResumenDiarioRemoto(
    val id: String,
    @SerialName("optica_id") val opticaId: String,
    val fecha: String,
    @SerialName("ventas_cantidad") val ventasCantidad: Int = 0,
    @SerialName("ventas_monto_total") val ventasMontoTotal: Double = 0.0,
    @SerialName("ventas_costo_total") val ventasCostoTotal: Double = 0.0,
    @SerialName("cobros_cantidad") val cobrosCantidad: Int = 0,
    @SerialName("cobros_monto_total") val cobrosMontoTotal: Double = 0.0,
    @SerialName("saldo_pendiente_total") val saldoPendienteTotal: Double = 0.0,
    @SerialName("saldo_pendiente_cantidad") val saldoPendienteCantidad: Int = 0,
    @SerialName("inventario_valor") val inventarioValor: Double? = null,
    @SerialName("inventario_unidades") val inventarioUnidades: Int? = null,
    @SerialName("calculado_en") val calculadoEn: String? = null
) {
    fun toEntity() = ResumenDiarioEntity(
        id = id,
        opticaId = opticaId,
        fecha = fecha,
        ventasCantidad = ventasCantidad,
        ventasMontoTotal = ventasMontoTotal,
        ventasCostoTotal = ventasCostoTotal,
        cobrosCantidad = cobrosCantidad,
        cobrosMontoTotal = cobrosMontoTotal,
        saldoPendienteTotal = saldoPendienteTotal,
        saldoPendienteCantidad = saldoPendienteCantidad,
        inventarioValor = inventarioValor,
        inventarioUnidades = inventarioUnidades,
        calculadoEn = calculadoEn
    )
}

// ── ConfiguracionFinanciera remote DTO ─────────────────────────────────

@Serializable
data class ConfiguracionFinancieraRemoto(
    @SerialName("optica_id") val opticaId: String,
    @SerialName("margen_neto_objetivo") val margenNetoObjetivo: Double = 15.0,
    @SerialName("ticket_promedio_objetivo") val ticketPromedioObjetivo: Double? = null,
    @SerialName("caida_ventas_alerta_pct") val caidaVentasAlertaPct: Double = 10.0,
    @SerialName("deuda_vieja_alerta_dias") val deudaViejaAlertaDias: Int = 30,
    @SerialName("deuda_total_alerta_monto") val deudaTotalAlertaMonto: Double = 3000.0,
    @SerialName("stock_estancado_alerta_dias") val stockEstancadoAlertaDias: Int = 180,
    @SerialName("stock_bajo_alerta_unidades") val stockBajoAlertaUnidades: Int = 2,
    @SerialName("min_ventas_para_recomendar") val minVentasParaRecomendar: Int = 5,
    @SerialName("frecuencia_recalculo_dias") val frecuenciaRecalculoDias: Int = 1
) {
    fun toEntity() = ConfiguracionFinancieraEntity(
        opticaId = opticaId,
        margenNetoObjetivo = margenNetoObjetivo,
        ticketPromedioObjetivo = ticketPromedioObjetivo,
        caidaVentasAlertaPct = caidaVentasAlertaPct,
        deudaViejaAlertaDias = deudaViejaAlertaDias,
        deudaTotalAlertaMonto = deudaTotalAlertaMonto,
        stockEstancadoAlertaDias = stockEstancadoAlertaDias,
        stockBajoAlertaUnidades = stockBajoAlertaUnidades,
        minVentasParaRecomendar = minVentasParaRecomendar,
        frecuenciaRecalculoDias = frecuenciaRecalculoDias
    )
}

// ── Sync result ────────────────────────────────────────────────────────

data class FinanzasSyncResult(
    val uploadedDispensaciones: Int,
    val uploadedDispensacionItems: Int = 0,
    val uploadedServicios: Int,
    val uploadedPagos: Int,
    val uploadedVentas: Int = 0,
    val uploadedGastosOperativos: Int = 0,
    val downloadedDispensaciones: Int,
    val downloadedDispensacionItems: Int = 0,
    val downloadedServicios: Int,
    val downloadedPagos: Int,
    val downloadedVentas: Int = 0,
    val downloadedResumenesDiarios: Int = 0,
    val downloadedConfiguracionesFinancieras: Int = 0
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
    filtroDiscromatopsiaTipo = filtroDiscromatopsiaTipo,
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
    updatedBy = updatedBy,
    ventaId = ventaId
)

fun DispensacionItem.toRemoto(): DispensacionItemRemota = DispensacionItemRemota(
    id = id, dispensacionId = dispensacionId,
    tipoLente = tipoLente, materialLente = materialLente,
    tratamientos = tratamientos.joinToString(","), colorLente = colorLente,
    distanciaLente = distanciaLente, altura = altura,
    subTipoBifocal = subTipoBifocal, notasDiseno = notasDiseno,
    filtroDiscromatopsiaTipo = filtroDiscromatopsiaTipo,
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
    updatedBy = updatedBy,
    fechaEntrega = fechaEntrega?.toString()
)


internal fun String?.normalizeOptionalFk(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

internal fun String.remotoServicioExtraMetodoToLocal(): String =
    if (this == FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW) "" else this

internal fun String.remotoOtServicioExtraToLocal(): String =
    if (this == FinanzasRemoteDefaults.ServicioExtra.OT_VACIA) "" else this

internal fun normalizedOtForUnique(ot: String?): String? =
    ot?.trim()?.takeIf { it.isNotBlank() }?.uppercase()


