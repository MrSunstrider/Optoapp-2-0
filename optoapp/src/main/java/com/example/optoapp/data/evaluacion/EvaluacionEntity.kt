package com.example.optoapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.optoapp.data.LocalDateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(
    tableName = "evaluaciones",
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
data class EvaluacionClinica(
    @PrimaryKey val id: String,
    val pacienteId: String,
    @Serializable(with = LocalDateSerializer::class)
    val fecha: LocalDate,
    val opticaId: String = "",

    val motivoConsulta: String? = null,

    val sintomas: String? = null,

    val antecedentesPersonalesOculares: String? = null,

    val antecedentesPersonalesSistemicos: String? = null,

    val antecedentesFamiliaresOculares: String? = null,

    val antecedentesFamiliaresSistemicos: String? = null,

    val medicacion: String? = null,

    val alergias: String? = null,
    val necesidadVisual: List<String>? = null,

    val avScOdLejos: String? = null,
    val avScOiLejos: String? = null,
    val avScOdCerca: String? = null,
    val avScOiCerca: String? = null,
    val avScAo: String? = null,
    val avScAoCerca: String? = null,

    val avCcOdLejos: String? = null,
    val avCcOiLejos: String? = null,
    val avCcOdCerca: String? = null,
    val avCcOiCerca: String? = null,
    val avCcAoPx: String? = null,
    val avCcAoCerca: String? = null,

    val phOd: String? = null,
    val phOi: String? = null,
    val kappaOd: String? = null,
    val kappaOi: String? = null,

    val hirshberg: String? = null,
    val duccionesOd: String? = null,
    val duccionesOi: String? = null,

    val versionesAo: String? = null,

    val estereopsisValor: String? = null,

    val estereopsisSegundos: String? = null,

    val lang: String? = null,

    val worth: String? = null,

    val ishihara: String? = null,

    val farnsworth: String? = null,

    val schirmerOd: String? = null,

    val schirmerOi: String? = null,
    val osdiPuntuacion: Int? = null,

    val osdiClasificacion: String? = null,

    val sensibilidadContraste: String? = null,

    val sensibilidadFrecuencia: String? = null,

    val amsler: String? = null,

    val campoVisual: String? = null,

    val campoVisualDescripcion: String? = null,

    val coverTest6m: String? = null,
    val coverTest40cm: String? = null,
    val coverTest10cm: String? = null,

    val ppcOr: String? = null,
    val ppcLuz: String? = null,
    val ppcFrl: String? = null,

    val reflejoFotomotor: String? = null,
    val reflejoConsensual: String? = null,
    val reflejoAcomodativo: String? = null,

    val k1Od: String? = null,
    val k2Od: String? = null,
    val k1Oi: String? = null,
    val k2Oi: String? = null,

    val objOdEsf: String? = null,
    val objOdCil: String? = null,
    val objOdEje: String? = null,
    val objOiEsf: String? = null,
    val objOiCil: String? = null,
    val objOiEje: String? = null,

    val subjOdEsf: String? = null,
    val subjOdCil: String? = null,
    val subjOdEje: String? = null,
    val subjOiEsf: String? = null,
    val subjOiCil: String? = null,
    val subjOiEje: String? = null,

    val recetaOdEsf: String? = null,
    val recetaOdCil: String? = null,
    val recetaOdEje: String? = null,
    val recetaOdAv: String? = null,
    val recetaOiEsf: String? = null,
    val recetaOiCil: String? = null,
    val recetaOiEje: String? = null,
    val recetaOiAv: String? = null,

    val addCercaOd: String? = null,
    val addCercaOi: String? = null,
    val addIntermediaOd: String? = null,
    val addIntermediaOi: String? = null,

    val addAv: String? = null,

    val dipLejos: String? = null,
    val dipCerca: String? = null,
    val dipIntermedio: String? = null,
    @SerialName("dipTotalMm")
    val dipTotalMm: Double? = null,
    @SerialName("dnpOdMm")
    val dnpOdMm: Double? = null,
    @SerialName("dnpOiMm")
    val dnpOiMm: Double? = null,

    val prismaOdValor: String? = null,
    val prismaOdBase: String? = null,
    val prismaOiValor: String? = null,
    val prismaOiBase: String? = null,

    val diagnostico: String? = null,
    val diagnosticoOd: List<String>? = null,
    val diagnosticoOi: List<String>? = null,
    val diagnosticoOtros: List<String>? = null,

    val planTratamiento: String? = null,

    val observaciones: String? = null,

    val proximaFechaControl: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val proximaCita: LocalDate? = null,
    /** programada | confirmada | asistio | no_asistio | reprogramada */

    val citaEstado: String? = null,
    val balanceOd: Boolean? = null,
    val balanceOi: Boolean? = null,
    val otrosPresbicia: Boolean? = null,
    val otrosAnisometropia: Boolean? = null,
    val otrosAmbliopia: Boolean? = null,
    val autoPresbicia: Boolean? = null,
    val autoAnisometropia: Boolean? = null,
    val autoAmbliopia: Boolean? = null,

    val lcOdEsf: String? = null,
    val lcOdCil: String? = null,
    val lcOdEje: String? = null,
    val lcOiEsf: String? = null,
    val lcOiCil: String? = null,
    val lcOiEje: String? = null,
    val lcRadioBaseOd: String? = null,
    val lcDiametroOd: String? = null,
    val lcRadioBaseOi: String? = null,
    val lcDiametroOi: String? = null,
    val lcLaboratorio: String? = null,
    val lcTipoLente: String? = null,
    val lcMaterial: String? = null,
    @Serializable(with = LocalDateSerializer::class) val lcFechaAdaptacion: LocalDate? = null,

    val lcObservaciones: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    @SerialName("updatedBy")
    val updatedBy: String? = null,
)
