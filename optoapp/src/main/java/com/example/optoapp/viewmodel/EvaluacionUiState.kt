package com.example.optoapp.viewmodel

import com.example.optoapp.util.DateUtils
import java.time.LocalDate

data class EvaluacionUiState(
    val fecha: LocalDate = DateUtils.today(),
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
    val avScOdLejos: String = "",
    val avScOiLejos: String = "",
    val avScOdCerca: String = "",
    val avScOiCerca: String = "",
    val avScAo: String = "",
    val avScAoCerca: String = "",

    // Agudeza visual CON corrección PX
    val avCcOdLejos: String = "",
    val avCcOiLejos: String = "",
    val avCcOdCerca: String = "",
    val avCcOiCerca: String = "",
    val avCcAoPx: String = "",
    val avCcAoCerca: String = "",

    // Otros exámenes
    val phOd: String = "",
    val phOi: String = "",
    val kappaOd: String = "",
    val kappaOi: String = "",
    val hirshberg: String = "",
    val duccionesOd: String = "",
    val duccionesOi: String = "",
    val versionesAo: String = "",

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
    val coverTest6m: String = "",
    val coverTest40cm: String = "",
    val coverTest10cm: String = "",

    // PPC
    val ppcOr: String = "",
    val ppcLuz: String = "",
    val ppcFrl: String = "",

    // Reflejos
    val reflejoFotomotor: String = "",
    val reflejoConsensual: String = "",
    val reflejoAcomodativo: String = "",

    // Queratometría
    val k1Od: String = "",
    val k2Od: String = "",
    val k1Oi: String = "",
    val k2Oi: String = "",

    // Refracción objetiva
    val objOdEsf: String = "",
    val objOdCil: String = "",
    val objOdEje: String = "",
    val objOiEsf: String = "",
    val objOiCil: String = "",
    val objOiEje: String = "",

    // Refracción subjetiva
    val subjOdEsf: String = "",
    val subjOdCil: String = "",
    val subjOdEje: String = "",
    val subjOiEsf: String = "",
    val subjOiCil: String = "",
    val subjOiEje: String = "",

    // Refracción final / fórmula (lejos)
    val recetaOdEsf: String = "",
    val recetaOdCil: String = "",
    val recetaOdEje: String = "",
    val recetaOdAv: String = "",
    val recetaOiEsf: String = "",
    val recetaOiCil: String = "",
    val recetaOiEje: String = "",
    val recetaOiAv: String = "",

    // Adición (ADD)
    val addCercaOd: String = "",
    val addCercaOi: String = "",
    val addIntermediaOd: String = "",
    val addIntermediaOi: String = "",
    val addAv: String = "",

    // DIP o DNP
    val dipLejos: String = "",
    val dipCerca: String = "",
    val dipIntermedio: String = "",

    // Prismas
    val prismaOdValor: String = "",
    val prismaOdBase: String = "",
    val prismaOiValor: String = "",
    val prismaOiBase: String = "",

    val diagnostico: String = "",
    val diagnosticoOd: List<String> = emptyList(),
    val diagnosticoOi: List<String> = emptyList(),
    val diagnosticoOtros: List<String> = emptyList(),
    val balanceOd: Boolean = false,
    val balanceOi: Boolean = false,
    val otrosPresbicia: Boolean = false,
    val otrosAnisometropia: Boolean = false,
    val otrosAmbliopia: Boolean = false,
    val autoPresbicia: Boolean = true,
    val autoAnisometropia: Boolean = true,
    val autoAmbliopia: Boolean = true,
    val planTratamiento: String = "",
    val observaciones: String = "",
    val proximaFechaControl: String = "",
    val proximaCita: LocalDate? = null,
    /** programada | confirmada | asistio | no_asistio | reprogramada */
    val citaEstado: String = "programada",

    // Contactología
    val lcOdEsf: String = "",
    val lcOdCil: String = "",
    val lcOdEje: String = "",
    val lcOiEsf: String = "",
    val lcOiCil: String = "",
    val lcOiEje: String = "",
    val lcRadioBaseOd: String = "",
    val lcOdDia: String = "",
    val lcRadioBaseOi: String = "",
    val lcOiDia: String = "",
    val lcLaboratorio: String = "",
    val lcTipoLente: String = "",
    val lcMaterial: String = "",
    val lcFechaAdaptacion: LocalDate? = null,
    val lcObservaciones: String = "",

    val isVpCerca: Boolean = true,
    val hasAdd: Boolean = false,
    val isAddAo: Boolean = false,
    val pacienteEdad: Int? = null,
    val pacienteNombre: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /**
     * Cerca/Intermedio selector solo es visible si hay adición activa
     * o el paciente es presbícita (≥ 40 años).
     */
    val shouldShowCercaIntermedio: Boolean
        get() = hasAdd || (pacienteEdad ?: 0) >= 40
}
