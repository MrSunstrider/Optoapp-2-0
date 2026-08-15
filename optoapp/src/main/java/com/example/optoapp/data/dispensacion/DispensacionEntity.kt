package com.example.optoapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(
    tableName = "dispensaciones",
    foreignKeys = [
        ForeignKey(
            entity = Paciente::class,
            parentColumns = ["id"],
            childColumns = ["pacienteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["pacienteId"]),
        Index(value = ["opticaId"]),
    ],
)
@Serializable
data class DispensacionOptica(
    @PrimaryKey val id: String,
    val ot: String = "",
    val monturaId: String = "",
    @SerialName("pacienteId")
    val pacienteId: String,
    @Serializable(with = LocalDateSerializer::class)
    val fecha: LocalDate,
    @SerialName("opticaId")
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
    @SerialName("montoTotal")
    val montoTotal: Double = 0.0,
    @SerialName("metodoPago")
    val metodoPago: String = "",
    @SerialName("montoPagado")
    val montoPagado: Double = 0.0,
    @SerialName("estadoEntrega")
    val estadoEntrega: String = "Pendiente",
    @Serializable(with = LocalDateSerializer::class)
    val fechaEntrega: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val fechaVencimientoGarantia: LocalDate? = null,
    val distanciaLente: String = "",
    val altura: String = "",
    val subTipoBifocal: String = "",
    @ColumnInfo(name = "filtro_discromatopsia_tipo") val filtroDiscromatopsiaTipo: String = "",
    @ColumnInfo(name = "reclamo_origen_id") val reclamoOrigenId: String? = null,
    @ColumnInfo(name = "evaluacion_id") val evaluacionId: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("updatedBy")
    val updatedBy: String? = null,
)

@Entity(
    tableName = "pagos",
    foreignKeys = [
        ForeignKey(
            entity = DispensacionOptica::class,
            parentColumns = ["id"],
            childColumns = ["dispensacionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ServicioExtra::class,
            parentColumns = ["id"],
            childColumns = ["servicioExtraId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["dispensacionId"]),
        Index(value = ["servicioExtraId"]),
        Index(value = ["opticaId"]),
        Index(value = ["reversaPagoId"]),
    ],
)
@Serializable
data class Pago(
    @PrimaryKey val id: String,
    @SerialName("dispensacionId")
    val dispensacionId: String? = null,
    @SerialName("servicioExtraId")
    val servicioExtraId: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val fecha: LocalDate,
    val tipo: String,
    val monto: Double,
    @SerialName("metodoPago")
    val metodoPago: String = "",
    val nota: String = "",
    @SerialName("opticaId")
    val opticaId: String = "mi_optica_base",
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("updatedBy")
    val updatedBy: String? = null,
    @SerialName("ventaId")
    val ventaId: String? = null,
    @SerialName("reversaPagoId")
    val reversaPagoId: String? = null,
)

@Entity(
    tableName = "servicios_extra",
    foreignKeys = [
        ForeignKey(
            entity = Paciente::class,
            parentColumns = ["id"],
            childColumns = ["pacienteId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["pacienteId"]),
        Index(value = ["opticaId"]),
    ],
)
@Serializable
data class ServicioExtra(
    @PrimaryKey val id: String,
    val ot: String = "",
    val descripcion: String,
    @SerialName("montoTotal")
    val montoTotal: Double,
    @SerialName("aCuenta")
    val aCuenta: Double = 0.0,
    val estado: String, // Delivery status lifecycle: Pendiente → Entregado
    @Serializable(with = LocalDateSerializer::class)
    val fecha: LocalDate,
    @SerialName("pacienteId")
    val pacienteId: String? = null, // Opcional
    @SerialName("metodoPago")
    val metodoPago: String = "",
    @SerialName("opticaId")
    val opticaId: String = "mi_optica_base",
    @SerialName("fechaEntrega")
    @ColumnInfo(name = "fecha_entrega")
    @Serializable(with = LocalDateSerializer::class)
    val fechaEntrega: LocalDate? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("updatedBy")
    val updatedBy: String? = null,
)

@Entity(
    tableName = "monturas",
    indices = [
        Index(value = ["opticaId"]),
        Index(value = ["sku", "opticaId"], unique = true),
        Index(value = ["estadoComercial"]),
        Index(value = ["categoria"]),
    ],
)
@Serializable
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
    @SerialName("anchoMm")
    val anchoMm: Double? = null,
    @SerialName("puenteMm")
    val puenteMm: Double? = null,
    @SerialName("alturaMm")
    val alturaMm: Double? = null,
    @SerialName("imagenUri")
    val imagenUri: String? = null,
    val categoria: String = "",
    val coleccion: String = "",
    val temporada: String = "",
    val estadoComercial: String = "",
    val genero: String = "",
    @SerialName("opticaId")
    val opticaId: String = "mi_optica_base",
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("updatedBy")
    val updatedBy: String? = null,
)

@Entity(
    tableName = "montura_movimientos",
    indices = [
        Index(value = ["monturaId"]),
        Index(value = ["opticaId"]),
        Index(value = ["fecha"]),
        Index(value = ["referenciaId"]),
        Index(value = ["referenciaId", "tipo", "monturaId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Montura::class,
            parentColumns = ["id"],
            childColumns = ["monturaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
@Serializable
data class MonturaMovimiento(
    @PrimaryKey val id: String,
    val monturaId: String,
    @Serializable(with = LocalDateSerializer::class)
    val fecha: LocalDate = LocalDate.now(),
    val tipo: String, // ENTRADA, SALIDA_VENTA, AJUSTE
    val cantidad: Int,
    val stockPrevio: Int,
    val stockNuevo: Int,
    val referenciaId: String = "",
    val nota: String = "",
    @SerialName("opticaId")
    val opticaId: String = "mi_optica_base",
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("updatedBy")
    val updatedBy: String? = null,
    val userId: String = "",
    val costoUnitario: Double = 0.0,
    val tipoDocumento: String = "",
)
