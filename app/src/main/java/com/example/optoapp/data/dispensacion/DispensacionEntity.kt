package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

@Entity(
    tableName = "dispensaciones",
    foreignKeys = [ForeignKey(
        entity = Paciente::class,
        parentColumns = ["id"],
        childColumns = ["pacienteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["pacienteId"]),
        Index(value = ["opticaId"])
    ]
)
data class DispensacionOptica(
    @PrimaryKey val id: String,
    val ot: String = "",
    val monturaId: String = "",
    @SerializedName("pacienteId", alternate = ["paciente_id"])
    val pacienteId: String,
    val fecha: LocalDate,
    @SerializedName("opticaId", alternate = ["optica_id"])
    val opticaId: String = "mi_optica_base",
    val tipoMontura: String = "",
    val materialMontura: String = "",
    val tipoLente: String = "",
    val materialLente: String = "",
    val tratamientos: List<String> = emptyList(),
    val colorLente: String = "",
    val notasDiseno: String = "",
    val origenMontura: String = "",
    val tipoAro: String = "",
    val descripcionMontura: String = "",
    @SerializedName("montoTotal", alternate = ["monto_total"])
    val montoTotal: Double = 0.0,
    @SerializedName("metodoPago", alternate = ["metodo_pago"])
    val metodoPago: String = "",
    @SerializedName("montoPagado", alternate = ["monto_pagado"])
    val montoPagado: Double = 0.0,
    @SerializedName("estadoEntrega", alternate = ["estado_entrega"])
    val estadoEntrega: String = "Pendiente",
    val fechaVencimientoGarantia: LocalDate? = null,
    val distanciaLente: String = "",
    val altura: String = "",
    val subTipoBifocal: String = ""
)

@Entity(
    tableName = "pagos",
    foreignKeys = [
        ForeignKey(
            entity = DispensacionOptica::class,
            parentColumns = ["id"],
            childColumns = ["dispensacionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ServicioExtra::class,
            parentColumns = ["id"],
            childColumns = ["servicioExtraId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dispensacionId"]),
        Index(value = ["servicioExtraId"]),
        Index(value = ["opticaId"])
    ]
)
data class Pago(
    @PrimaryKey val id: String,
    @SerializedName("dispensacionId", alternate = ["dispensacion_id"])
    val dispensacionId: String? = null,
    @SerializedName("servicioExtraId", alternate = ["servicio_extra_id"])
    val servicioExtraId: String? = null,
    val fecha: LocalDate,
    val tipo: String,
    val monto: Double,
    @SerializedName("metodoPago", alternate = ["metodo_pago"])
    val metodoPago: String = "",
    val nota: String = "",
    @SerializedName("opticaId", alternate = ["optica_id"])
    val opticaId: String = "mi_optica_base"
)

@Entity(
    tableName = "servicios_extra",
    foreignKeys = [ForeignKey(
        entity = Paciente::class,
        parentColumns = ["id"],
        childColumns = ["pacienteId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [
        Index(value = ["pacienteId"]),
        Index(value = ["opticaId"])
    ]
)
data class ServicioExtra(
    @PrimaryKey val id: String,
    val ot: String = "",
    val descripcion: String,
    @SerializedName("montoTotal", alternate = ["monto_total"])
    val montoTotal: Double,
    @SerializedName("aCuenta", alternate = ["a_cuenta"])
    val aCuenta: Double,
    val estado: String, // Pendiente, Entregado
    val fecha: LocalDate,
    @SerializedName("pacienteId", alternate = ["paciente_id"])
    val pacienteId: String? = null, // Opcional
    @SerializedName("metodoPago", alternate = ["metodo_pago"])
    val metodoPago: String = "",
    @SerializedName("opticaId", alternate = ["optica_id"])
    val opticaId: String = "mi_optica_base"
)

@Entity(
    tableName = "monturas",
    indices = [
        Index(value = ["opticaId"]),
        Index(value = ["sku", "opticaId"], unique = true)
    ]
)
data class Montura(
    @PrimaryKey val id: String,
    val sku: String = "",
    val marca: String = "",
    val modelo: String = "",
    val color: String = "",
    val talla: String = "",
    val costo: Double = 0.0,
    val precio: Double = 0.0,
    val stockActual: Int = 0,
    val stockMinimo: Int = 0,
    val activo: Boolean = true,
    val tipoAro: String = "",
    val materialMontura: String = "",
    @SerializedName("opticaId", alternate = ["optica_id"])
    val opticaId: String = "mi_optica_base"
)

@Entity(
    tableName = "montura_movimientos",
    indices = [
        Index(value = ["monturaId"]),
        Index(value = ["opticaId"]),
        Index(value = ["fecha"]),
        Index(value = ["referenciaId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Montura::class,
            parentColumns = ["id"],
            childColumns = ["monturaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MonturaMovimiento(
    @PrimaryKey val id: String,
    val monturaId: String,
    val fecha: LocalDate = LocalDate.now(),
    val tipo: String, // ENTRADA, SALIDA_VENTA, AJUSTE
    val cantidad: Int,
    val stockPrevio: Int,
    val stockNuevo: Int,
    val referenciaId: String = "",
    val nota: String = "",
    @SerializedName("opticaId", alternate = ["optica_id"])
    val opticaId: String = "mi_optica_base"
)
