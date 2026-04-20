package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import java.time.LocalDate

@Entity(
    tableName = "pacientes",
    indices = [
        Index(value = ["nombreCompleto"]),
        Index(value = ["opticaId"])
    ]
)
data class Paciente(
    @PrimaryKey val id: String,
    @SerializedName("nombreCompleto", alternate = ["nombre_completo"])
    val nombreCompleto: String,
    val edad: Int,
    val telefono: String,
    @SerializedName("fechaCreacion", alternate = ["fecha_creacion"])
    val fechaCreacion: LocalDate,
    val dni: String? = null,
    val fechaNacimiento: LocalDate? = null,
    val sexo: String? = null,
    val email: String? = null,
    val direccion: String? = null,
    val distrito: String? = null,
    val ocupacion: String? = null,
    val acompanante: String? = null,
    val hobbies: String? = null,
    val ultimasEtiquetas: List<String> = emptyList(),
    @SerializedName("opticaId", alternate = ["optica_id"])
    val opticaId: String = "mi_optica_base"
)

@Entity(
    tableName = "evaluaciones",
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
data class EvaluacionClinica(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val fecha: LocalDate,
    val opticaId: String = "mi_optica_base",
    val motivoConsulta: String = "",
    val sintomas: String = "",
    val antecedentesPersonalesOculares: String = "",
    val antecedentesPersonalesSistemicos: String = "",
    val antecedentesFamiliaresOculares: String = "",
    val antecedentesFamiliaresSistemicos: String = "",
    val medicacion: String = "",
    val alergias: String = "",
    val necesidadVisual: List<String> = emptyList(),
    
    // Agudeza visual SIN corrección
    val avScOdLejos: String = "", val avScOiLejos: String = "",
    val avScOdCerca: String = "", val avScOiCerca: String = "",
    val avScAo: String = "", val avScAoCerca: String = "",
    
    // Agudeza visual CON corrección PX
    val avCcOdLejos: String = "", val avCcOiLejos: String = "",
    val avCcOdCerca: String = "", val avCcOiCerca: String = "",
    val avCcAoPx: String = "", val avCcAoCerca: String = "",
    
    // Otros exámenes
    val phOd: String = "", val phOi: String = "",
    val kappaOd: String = "", val kappaOi: String = "",
    val hirshberg: String = "",
    val duccionesOd: String = "", val duccionesOi: String = "",
    val versionesAo: String = "", // Unificado Ambos Ojos
    
    // Visión binocular y Percepción
    val estereopsisValor: String = "",
    val estereopsisSegundos: String = "",
    val lang: String = "",
    val worth: String = "",
    
    // Percepción color
    val ishihara: String = "",
    val farnsworth: String = "",
    
    // Salud Ocular y Función Visual
    val schirmerOd: String = "",
    val schirmerOi: String = "",
    val osdiPuntuacion: Int? = null,
    val osdiClasificacion: String = "",
    val sensibilidadContraste: String = "",
    val sensibilidadFrecuencia: String = "",
    
    // Otras Pruebas
    val amsler: String = "",
    val campoVisual: String = "",
    val campoVisualDescripcion: String = "",
    
    // Cover test
    val coverTest6m: String = "", val coverTest40cm: String = "", val coverTest10cm: String = "",
    
    // PPC
    val ppcOr: String = "", val ppcLuz: String = "", val ppcFrl: String = "",
    
    // Reflejos
    val reflejoFotomotor: String = "", val reflejoConsensual: String = "", val reflejoAcomodativo: String = "",
    
    // Queratometría (Solo en Contactología ahora)
    val k1Od: String = "", val k2Od: String = "",
    val k1Oi: String = "", val k2Oi: String = "",
    
    // Refracción objetiva
    val objOdEsf: String = "", val objOdCil: String = "", val objOdEje: String = "",
    val objOiEsf: String = "", val objOiCil: String = "", val objOiEje: String = "",
    
    // Refracción subjetiva
    val subjOdEsf: String = "", val subjOdCil: String = "", val subjOdEje: String = "",
    val subjOiEsf: String = "", val subjOiCil: String = "", val subjOiEje: String = "",
    
    // Refracción final / fórmula (lejos) — campos receta* persisten en BD
    val recetaOdEsf: String = "", val recetaOdCil: String = "", val recetaOdEje: String = "", val recetaOdAv: String = "",
    val recetaOiEsf: String = "", val recetaOiCil: String = "", val recetaOiEje: String = "", val recetaOiAv: String = "",
    
    // Adición (ADD)
    val addCercaOd: String = "", val addCercaOi: String = "",
    val addIntermediaOd: String = "", val addIntermediaOi: String = "",
    val addAv: String = "",
    
    // DIP o DNP
    val dipLejos: String = "", val dipCerca: String = "", val dipIntermedio: String = "",
    @SerializedName("dipTotalMm", alternate = ["dip_total_mm"])
    val dipTotalMm: Double? = null,
    @SerializedName("dnpOdMm", alternate = ["dnp_od_mm"])
    val dnpOdMm: Double? = null,
    @SerializedName("dnpOiMm", alternate = ["dnp_oi_mm"])
    val dnpOiMm: Double? = null,
    
    // Prismas
    val prismaOdValor: String = "", val prismaOdBase: String = "",
    val prismaOiValor: String = "", val prismaOiBase: String = "",

    val diagnostico: String = "",
    val diagnosticoOd: List<String> = emptyList(),
    val diagnosticoOi: List<String> = emptyList(),
    val diagnosticoOtros: List<String> = emptyList(),
    val planTratamiento: String = "",
    val observaciones: String = "",
    val proximaFechaControl: String = "",
    val proximaCita: LocalDate? = null,
    /** programada | confirmada | asistio | no_asistio | reprogramada */
    val citaEstado: String = "programada",
    val balanceOd: Boolean = false,
    val balanceOi: Boolean = false,
    val otrosPresbicia: Boolean = false,
    val otrosAnisometropia: Boolean = false,
    val otrosAmbliopia: Boolean = false,
    val autoPresbicia: Boolean = true,
    val autoAnisometropia: Boolean = true,
    val autoAmbliopia: Boolean = true,

    // Contactología
    val lcOdEsf: String = "", val lcOdCil: String = "", val lcOdEje: String = "",
    val lcOiEsf: String = "", val lcOiCil: String = "", val lcOiEje: String = "",
    val lcRadioBaseOd: String = "", val lcDiametroOd: String = "",
    val lcRadioBaseOi: String = "", val lcDiametroOi: String = "",
    val lcLaboratorio: String = "", val lcTipoLente: String = "",
    val lcMaterial: String = "", val lcFechaAdaptacion: LocalDate? = null,
    val lcObservaciones: String = ""
)

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
