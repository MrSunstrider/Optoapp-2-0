package com.example.optoapp.data.mappers

import com.example.optoapp.data.DispensacionOptica as DispensacionEntity
import com.example.optoapp.data.Pago as PagoEntity
import com.example.optoapp.data.ServicioExtra as ServicioExtraEntity
import com.example.optoapp.domain.model.DispensacionModel
import com.example.optoapp.domain.model.PagoModel
import com.example.optoapp.domain.model.ServicioExtraModel

object FinancialMapper {
    
    fun toDomain(entity: DispensacionEntity): DispensacionModel = DispensacionModel(
        id = entity.id,
        ot = entity.ot,
        monturaId = entity.monturaId,
        pacienteId = entity.pacienteId,
        fecha = entity.fecha,
        opticaId = entity.opticaId,
        tipoMontura = entity.tipoMontura,
        materialMontura = entity.materialMontura,
        tipoLente = entity.tipoLente,
        materialLente = entity.materialLente,
        tratamientos = entity.tratamientos,
        colorLente = entity.colorLente,
        notasDiseno = entity.notasDiseno,
        origenMontura = entity.origenMontura,
        tipoAro = entity.tipoAro,
        descripcionMontura = entity.descripcionMontura,
        montoTotal = entity.montoTotal,
        metodoPago = entity.metodoPago,
        montoPagado = entity.montoPagado,
        estadoEntrega = entity.estadoEntrega,
        fechaVencimientoGarantia = entity.fechaVencimientoGarantia,
        distanciaLente = entity.distanciaLente,
        altura = entity.altura,
        subTipoBifocal = entity.subTipoBifocal
    )

    fun toEntity(model: DispensacionModel): DispensacionEntity = DispensacionEntity(
        id = model.id,
        ot = model.ot,
        monturaId = model.monturaId,
        pacienteId = model.pacienteId,
        fecha = model.fecha,
        opticaId = model.opticaId,
        tipoMontura = model.tipoMontura,
        materialMontura = model.materialMontura,
        tipoLente = model.tipoLente,
        materialLente = model.materialLente,
        tratamientos = model.tratamientos,
        colorLente = model.colorLente,
        notasDiseno = model.notasDiseno,
        origenMontura = model.origenMontura,
        tipoAro = model.tipoAro,
        descripcionMontura = model.descripcionMontura,
        montoTotal = model.montoTotal,
        metodoPago = model.metodoPago,
        montoPagado = model.montoPagado,
        estadoEntrega = model.estadoEntrega,
        fechaVencimientoGarantia = model.fechaVencimientoGarantia,
        distanciaLente = model.distanciaLente,
        altura = model.altura,
        subTipoBifocal = model.subTipoBifocal
    )

    fun toDomain(entity: PagoEntity): PagoModel = PagoModel(
        id = entity.id,
        dispensacionId = entity.dispensacionId,
        servicioExtraId = entity.servicioExtraId,
        fecha = entity.fecha,
        tipo = entity.tipo,
        monto = entity.monto,
        metodoPago = entity.metodoPago,
        nota = entity.nota,
        opticaId = entity.opticaId
    )

    fun toEntity(model: PagoModel): PagoEntity = PagoEntity(
        id = model.id,
        dispensacionId = model.dispensacionId,
        servicioExtraId = model.servicioExtraId,
        fecha = model.fecha,
        tipo = model.tipo,
        monto = model.monto,
        metodoPago = model.metodoPago,
        nota = model.nota,
        opticaId = model.opticaId
    )

    fun toDomain(entity: ServicioExtraEntity): ServicioExtraModel = ServicioExtraModel(
        id = entity.id,
        ot = entity.ot,
        descripcion = entity.descripcion,
        montoTotal = entity.montoTotal,
        aCuenta = entity.aCuenta,
        estado = entity.estado,
        fecha = entity.fecha,
        pacienteId = entity.pacienteId,
        metodoPago = entity.metodoPago,
        opticaId = entity.opticaId
    )

    fun toEntity(model: ServicioExtraModel): ServicioExtraEntity = ServicioExtraEntity(
        id = model.id,
        ot = model.ot,
        descripcion = model.descripcion,
        montoTotal = model.montoTotal,
        aCuenta = model.aCuenta,
        estado = model.estado,
        fecha = model.fecha,
        pacienteId = model.pacienteId,
        metodoPago = model.metodoPago,
        opticaId = model.opticaId
    )
}
