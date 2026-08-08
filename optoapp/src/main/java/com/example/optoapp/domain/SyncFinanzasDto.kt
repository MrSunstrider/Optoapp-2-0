package com.example.optoapp.domain

import com.example.optoapp.data.DispensacionItem
import com.example.optoapp.data.DispensacionOptica
import com.example.optoapp.data.FinanzasRemoteDefaults
import com.example.optoapp.data.Pago
import com.example.optoapp.data.ServicioExtra
import com.example.optoapp.data.configuracionfinanciera.ConfiguracionFinancieraEntity
import com.example.optoapp.data.costobiselado.CostoBiseladoEntity
import com.example.optoapp.data.costoproducto.CostoProductoEntity
import com.example.optoapp.data.gastooperativo.GastoOperativoEntity
import com.example.optoapp.data.regalodispensacion.RegaloDispensacionEntity
import com.example.optoapp.data.resumendiario.ResumenDiarioEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
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
    @SerialName("evaluacion_id") val evaluacionId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
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
        evaluacionId = evaluacionId,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
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
    @SerialName("updated_by") val updatedBy: String? = null,
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
        fechaEntrega = fechaEntrega?.let(LocalDate::parse),
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
    @SerialName("venta_id") val ventaId: String? = null,
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
        ventaId = ventaId,
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
    @SerialName("filtro_discromatopsia_tipo") val filtroDiscromatopsiaTipo: String = "",
    @SerialName("notas_diseno") val notasDiseno: String? = null,
    @SerialName("montura_id") val monturaId: String? = null,
    @SerialName("origen_montura") val origenMontura: String? = null,
    @SerialName("tipo_aro") val tipoAro: String? = null,
    @SerialName("material_montura") val materialMontura: String? = null,
    @SerialName("descripcion_montura") val descripcionMontura: String? = null,
    @SerialName("tipo_montura") val tipoMontura: String? = null,
    @SerialName("optica_id") val opticaId: String = "mi_optica_base",
    @SerialName("alto_indice") val altoIndice: String? = null,
    @SerialName("reduccion_diametro") val reduccionDiametro: String? = null,
    @SerialName("lenticular") val lenticular: String? = null,
    @SerialName("curva_base") val curvaBase: String? = null,
    @SerialName("costo_real_od") val costoRealOd: Double? = null,
    @SerialName("costo_real_oi") val costoRealOi: Double? = null,
    @SerialName("costo_real_montura") val costoRealMontura: Double? = null,
    @SerialName("costo_real_biselado") val costoRealBiselado: Double? = null,
    @SerialName("costo_real_lc") val costoRealLc: Double? = null,
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
        opticaId = opticaId.ifBlank { "mi_optica_base" },
        altoIndice = altoIndice, reduccionDiametro = reduccionDiametro,
        lenticular = lenticular, curvaBase = curvaBase,
        costoRealOd = costoRealOd, costoRealOi = costoRealOi,
        costoRealMontura = costoRealMontura, costoRealBiselado = costoRealBiselado,
        costoRealLc = costoRealLc,
    )
}

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
    @SerialName("es_recurrente") val isRecurring: Boolean = false,

    val frecuencia: String = "mensual",
)

fun GastoOperativoEntity.toRemoto(): GastoOperativoRemoto = GastoOperativoRemoto(
    id = id,
    opticaId = opticaId,
    categoria = categoria,
    descripcion = descripcion,
    monto = monto.toDouble(),
    fecha = fecha.toString(),
    fechaProgramada = fechaProgramada?.toString(),
    nota = nota,
    createdAt = createdAt,
    isRecurring = isRecurring,
    frecuencia = frecuencia,
)

fun GastoOperativoRemoto.toEntity(): GastoOperativoEntity = GastoOperativoEntity(
    id = id,
    opticaId = opticaId,
    categoria = categoria,
    descripcion = descripcion,
    monto = BigDecimal.valueOf(monto),
    fecha = java.time.LocalDate.parse(fecha),
    fechaProgramada = fechaProgramada?.let { java.time.LocalDate.parse(it) },
    nota = nota,
    createdAt = createdAt,
    isRecurring = isRecurring,
    frecuencia = frecuencia,
)

@Serializable
data class RegaloDispensacionRemota(
    val id: String,
    @SerialName("dispensacion_id") val dispensacionId: String,
    @SerialName("producto_id") val productoId: String,
    val cantidad: Int,
    @SerialName("costo_unitario") val costoUnitario: Double,
    val descripcion: String = "",
    val motivo: String = "",
    @SerialName("optica_id") val opticaId: String,
) {
    fun toEntity() = RegaloDispensacionEntity(
        id = id,
        dispensacionId = dispensacionId,
        productoId = productoId,
        cantidad = cantidad,
        costoUnitario = costoUnitario,
        descripcion = descripcion,
        motivo = motivo,
        opticaId = opticaId,
    )
}

fun RegaloDispensacionEntity.toRemoto(): RegaloDispensacionRemota = RegaloDispensacionRemota(
    id = id,
    dispensacionId = dispensacionId,
    productoId = productoId,
    cantidad = cantidad,
    costoUnitario = costoUnitario,
    descripcion = descripcion,
    motivo = motivo,
    opticaId = opticaId,
)

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
    @SerialName("calculado_en") val calculadoEn: String? = null,
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
        calculadoEn = calculadoEn,
    )
}

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
    @SerialName("frecuencia_recalculo_dias") val frecuenciaRecalculoDias: Int = 1,
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
        frecuenciaRecalculoDias = frecuenciaRecalculoDias,
    )
}

@Serializable
data class CostoProductoRemoto(
    val id: String,
    @SerialName("optica_id") val opticaId: String,
    val material: String,
    @SerialName("tipo_lente") val tipoLente: String,
    @SerialName("stock_o_fabricacion") val stockOFabricacion: String,
    val tratamiento: String? = null,
    val serie: Int? = null,
    @SerialName("costo_unitario") val costoUnitario: Double,
    @SerialName("laboratorio_id") val laboratorioId: String? = null,
    @SerialName("vigente_desde") val vigenteDesde: String,
    @SerialName("vigente_hasta") val vigenteHasta: String? = null,
) {
    fun toEntity() = CostoProductoEntity(
        id = id, opticaId = opticaId, material = material,
        tipoLente = tipoLente, stockOFabricacion = stockOFabricacion,
        tratamiento = tratamiento, serie = serie,
        costoUnitario = costoUnitario, laboratorioId = laboratorioId,
        vigenteDesde = vigenteDesde, vigenteHasta = vigenteHasta,
    )
}

@Serializable
data class CostoBiseladoRemoto(
    val id: String,
    @SerialName("optica_id") val opticaId: String,
    val material: String,
    @SerialName("tipo_aro") val tipoAro: String,
    @SerialName("stock_o_fabricacion") val stockOFabricacion: String,
    val serie: Int? = null,
    @SerialName("alto_indice") val altoIndice: String? = null,
    @SerialName("costo_por_par") val costoPorPar: Double,
    val proveedor: String? = null,
    @SerialName("vigente_desde") val vigenteDesde: String,
    @SerialName("vigente_hasta") val vigenteHasta: String? = null,
) {
    fun toEntity() = CostoBiseladoEntity(
        id = id, opticaId = opticaId, material = material,
        tipoAro = tipoAro, stockOFabricacion = stockOFabricacion,
        serie = serie, altoIndice = altoIndice,
        costoPorPar = costoPorPar, proveedor = proveedor,
        vigenteDesde = vigenteDesde, vigenteHasta = vigenteHasta,
    )
}

fun CostoProductoEntity.toRemoto(): CostoProductoRemoto = CostoProductoRemoto(
    id = id, opticaId = opticaId, material = material,
    tipoLente = tipoLente, stockOFabricacion = stockOFabricacion,
    tratamiento = tratamiento, serie = serie,
    costoUnitario = costoUnitario, laboratorioId = laboratorioId,
    vigenteDesde = vigenteDesde, vigenteHasta = vigenteHasta,
)

fun CostoBiseladoEntity.toRemoto(): CostoBiseladoRemoto = CostoBiseladoRemoto(
    id = id, opticaId = opticaId, material = material,
    tipoAro = tipoAro, stockOFabricacion = stockOFabricacion,
    serie = serie, altoIndice = altoIndice,
    costoPorPar = costoPorPar, proveedor = proveedor,
    vigenteDesde = vigenteDesde, vigenteHasta = vigenteHasta,
)

data class FinanzasSyncResult(
    val uploadedDispensaciones: Int,
    val uploadedDispensacionItems: Int = 0,
    val uploadedServicios: Int,
    val uploadedPagos: Int,
    val uploadedGastosOperativos: Int = 0,
    val uploadedRegalos: Int = 0,
    val uploadedCostosProductos: Int = 0,
    val uploadedCostosBiselado: Int = 0,
    val downloadedDispensaciones: Int,
    val downloadedDispensacionItems: Int = 0,
    val downloadedServicios: Int,
    val downloadedPagos: Int,
    val downloadedRegalos: Int = 0,
    val downloadedResumenesDiarios: Int = 0,
    val downloadedConfiguracionesFinancieras: Int = 0,
    val downloadedGastosOperativos: Int = 0,
    val downloadedCostosProductos: Int = 0,
    val downloadedCostosBiselado: Int = 0,
)

@Serializable
internal data class ServicioRemotoLookup(
    val id: String,
    val ot: String = "",
)

@Serializable
internal data class DispensacionRemotaLookup(
    val id: String,
    val ot: String? = null,
)

@Serializable
internal data class PagoRemotoLookup(
    val id: String,
    @SerialName("dispensacion_id") val dispensacionId: String? = null,
    val tipo: String = "",
    val monto: Double = 0.0,
    @SerialName("metodo_pago") val metodoPago: String = "",
    val fecha: String = "",
)

fun DispensacionOptica.toRemoto(pagosSum: Double = montoPagado): DispensacionRemota = DispensacionRemota(
    id = id, ot = ot, monturaId = monturaId, pacienteId = pacienteId, fecha = fecha.toString(), opticaId = opticaId,
    tipoMontura = tipoMontura, materialMontura = materialMontura,
    tipoLente = tipoLente, materialLente = materialLente,
    tratamientos = tratamientos.joinToString(","), colorLente = colorLente,
    notasDiseno = notasDiseno, origenMontura = origenMontura,
    tipoAro = tipoAro, descripcionMontura = descripcionMontura,
    montoTotal = montoTotal, metodoPago = metodoPago,
    montoPagado = pagosSum, estadoEntrega = estadoEntrega,
    fechaEntrega = fechaEntrega?.toString(),
    fechaVencimientoGarantia = fechaVencimientoGarantia?.toString(),
    distanciaLente = distanciaLente, altura = altura, subTipoBifocal = subTipoBifocal,
    filtroDiscromatopsiaTipo = filtroDiscromatopsiaTipo,
    evaluacionId = evaluacionId,
    updatedAt = updatedAt, updatedBy = updatedBy,
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
    ventaId = ventaId,
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
    opticaId = opticaId.ifBlank { "mi_optica_base" },
    altoIndice = altoIndice, reduccionDiametro = reduccionDiametro,
    lenticular = lenticular, curvaBase = curvaBase,
    costoRealOd = costoRealOd, costoRealOi = costoRealOi,
    costoRealMontura = costoRealMontura, costoRealBiselado = costoRealBiselado,
    costoRealLc = costoRealLc,
)

fun ServicioExtra.toRemoto(aCuentaSum: Double = aCuenta): ServicioRemoto = ServicioRemoto(
    id = id,
    ot = ot.trim(),
    descripcion = descripcion.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.DESCRIPCION_VACIA },
    montoTotal = montoTotal.coerceAtLeast(0.0),
    aCuenta = aCuentaSum.coerceAtLeast(0.0).coerceAtMost(montoTotal.coerceAtLeast(0.0)),
    estado = estado.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.ESTADO_VACIO },
    fecha = fecha.toString(),
    pacienteId = pacienteId?.trim()?.takeIf { it.isNotBlank() },
    metodoPago = metodoPago.trim().ifBlank { FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW },
    opticaId = opticaId.trim().ifBlank { FinanzasRemoteDefaults.OPTICA_ID_FALLBACK },
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    fechaEntrega = fechaEntrega?.toString(),
)

internal fun String?.normalizeOptionalFk(): String? = this?.trim()?.takeIf { it.isNotBlank() }

internal fun String.remotoServicioExtraMetodoToLocal(): String = if (this == FinanzasRemoteDefaults.ServicioExtra.METODO_PAGO_ROW) "" else this

internal fun String.remotoOtServicioExtraToLocal(): String = if (this == FinanzasRemoteDefaults.ServicioExtra.OT_VACIA) "" else this

internal fun normalizedOtForUnique(ot: String?): String? = ot?.trim()?.takeIf { it.isNotBlank() }?.uppercase()
