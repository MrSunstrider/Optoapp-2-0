package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters

@Entity(tableName = "pacientes")
data class Paciente(
    @PrimaryKey val id: String,
    val nombreCompleto: String,
    val edad: Int,
    val telefono: String,
    val fechaCreacion: Long,
    val dni: String? = null,
    val fechaNacimiento: String? = null,
    val sexo: String? = null,
    val email: String? = null,
    val direccion: String? = null,
    val distrito: String? = null,
    val ocupacion: String? = null,
    val acompanante: String? = null,
    val hobbies: String? = null,
    val ultimasEtiquetas: List<String> = emptyList()
)

@Entity(
    tableName = "evaluaciones",
    foreignKeys = [ForeignKey(
        entity = Paciente::class,
        parentColumns = ["id"],
        childColumns = ["pacienteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["pacienteId"])]
)
data class EvaluacionClinica(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val fecha: Long,
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
    
    // Agudeza visual CON corrección
    val avCcOdLejos: String = "", val avCcOiLejos: String = "",
    val avCcOdCerca: String = "", val avCcOiCerca: String = "",
    
    // Otros exámenes
    val phOd: String = "", val phOi: String = "",
    val kappaOd: String = "", val kappaOi: String = "",
    
    // Cover test
    val coverTest6m: String = "", val coverTest40cm: String = "", val coverTest10cm: String = "",
    
    // PPC
    val ppcOr: String = "", val ppcLuz: String = "", val ppcFrl: String = "",
    
    // Reflejos
    val reflejoFotomotor: String = "", val reflejoConsensual: String = "", val reflejoAcomodativo: String = "",
    
    // Queratometría
    val k1Od: String = "", val k2Od: String = "",
    val k1Oi: String = "", val k2Oi: String = "",
    
    // Refracción objetiva
    val objOdEsf: String = "", val objOdCil: String = "", val objOdEje: String = "",
    val objOiEsf: String = "", val objOiCil: String = "", val objOiEje: String = "",
    
    // Refracción subjetiva
    val subjOdEsf: String = "", val subjOdCil: String = "", val subjOdEje: String = "",
    val subjOiEsf: String = "", val subjOiCil: String = "", val subjOiEje: String = "",
    
    // Refracción Final (Receta) Lejos
    val recetaOdEsf: String = "", val recetaOdCil: String = "", val recetaOdEje: String = "",
    val recetaOiEsf: String = "", val recetaOiCil: String = "", val recetaOiEje: String = "",
    
    // Adición (ADD)
    val addCercaOd: String = "", val addCercaOi: String = "",
    val addIntermediaOd: String = "", val addIntermediaOi: String = "",
    
    // DIP o DNP
    val dipLejos: String = "", val dipCerca: String = "", val dipIntermedio: String = "",
    
    // Prismas
    val prismaOdValor: String = "", val prismaOdBase: String = "",
    val prismaOiValor: String = "", val prismaOiBase: String = "",

    val diagnostico: String = "",
    val planTratamiento: String = "",
    val observaciones: String = "",
    val proximaFechaControl: String = ""
)

@Entity(
    tableName = "dispensaciones",
    foreignKeys = [ForeignKey(
        entity = Paciente::class,
        parentColumns = ["id"],
        childColumns = ["pacienteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["pacienteId"])]
)
data class DispensacionOptica(
    @PrimaryKey val id: String,
    val pacienteId: String,
    val fecha: Long,
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
    val montoTotal: Double = 0.0,
    val metodoPago: String = "",
    val montoPagado: Double = 0.0,
    val estadoEntrega: String = "Pendiente",
    val fechaVencimientoGarantia: String? = null
)

@Entity(
    tableName = "pagos",
    foreignKeys = [ForeignKey(
        entity = DispensacionOptica::class,
        parentColumns = ["id"],
        childColumns = ["dispensacionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["dispensacionId"])]
)
data class Pago(
    @PrimaryKey val id: String,
    val dispensacionId: String,
    val fecha: Long,
    val tipo: String,
    val monto: Double
)
