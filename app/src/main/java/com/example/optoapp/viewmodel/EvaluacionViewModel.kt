package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class EvaluacionUiState(
    val fecha: Long = System.currentTimeMillis(),
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
    val avScAo: String = "",
    
    // Agudeza visual CON corrección PX
    val avCcOdLejos: String = "", val avCcOiLejos: String = "",
    val avCcOdCerca: String = "", val avCcOiCerca: String = "",
    val avCcAoPx: String = "",
    
    // Otros exámenes
    val phOd: String = "", val phOi: String = "",
    val kappaOd: String = "", val kappaOi: String = "",
    val hirshberg: String = "",
    val duccionesOd: String = "", val duccionesOi: String = "",
    val versionesAo: String = "",
    
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
    val recetaOdEsf: String = "", val recetaOdCil: String = "", val recetaOdEje: String = "", val recetaOdAv: String = "",
    val recetaOiEsf: String = "", val recetaOiCil: String = "", val recetaOiEje: String = "", val recetaOiAv: String = "",
    
    // Adición (ADD)
    val addCercaOd: String = "", val addCercaOi: String = "",
    val addIntermediaOd: String = "", val addIntermediaOi: String = "",
    val addAv: String = "",
    
    // DIP o DNP
    val dipLejos: String = "", val dipCerca: String = "", val dipIntermedio: String = "",
    
    // Prismas
    val prismaOdValor: String = "", val prismaOdBase: String = "",
    val prismaOiValor: String = "", val prismaOiBase: String = "",

    val diagnostico: String = "",
    val planTratamiento: String = "",
    val observaciones: String = "",
    val proximaCita: Long? = null,

    // Contactología
    val lcOdEsf: String = "", val lcOdCil: String = "", val lcOdEje: String = "",
    val lcOiEsf: String = "", val lcOiCil: String = "", val lcOiEje: String = "",
    val lcRadioBaseOd: String = "", val lcOdDia: String = "",
    val lcRadioBaseOi: String = "", val lcOiDia: String = "",
    val lcLaboratorio: String = "", val lcTipoLente: String = "",
    val lcMaterial: String = "", val lcFechaAdaptacion: Long? = null,
    val lcObservaciones: String = "",

    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EvaluacionViewModel @Inject constructor(
    private val repository: OptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EvaluacionUiState())
    val uiState: StateFlow<EvaluacionUiState> = _uiState.asStateFlow()

    fun getEvaluacionesByPaciente(pacienteId: String) = repository.getEvaluacionesByPaciente(pacienteId)

    fun loadEvaluacion(evaluacionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getEvaluacionById(evaluacionId)) {
                is Resource.Success -> {
                    val e = result.data!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fecha = e.fecha,
                            motivoConsulta = e.motivoConsulta,
                            sintomas = e.sintomas,
                            antecedentesPersonalesOculares = e.antecedentesPersonalesOculares,
                            antecedentesPersonalesSistemicos = e.antecedentesPersonalesSistemicos,
                            antecedentesFamiliaresOculares = e.antecedentesFamiliaresOculares,
                            antecedentesFamiliaresSistemicos = e.antecedentesFamiliaresSistemicos,
                            medicacion = e.medicacion,
                            alergias = e.alergias,
                            necesidadVisual = e.necesidadVisual,
                            avScOdLejos = e.avScOdLejos, avScOiLejos = e.avScOiLejos,
                            avScOdCerca = e.avScOdCerca, avScOiCerca = e.avScOiCerca,
                            avScAo = e.avScAo,
                            avCcOdLejos = e.avCcOdLejos, avCcOiLejos = e.avCcOiLejos,
                            avCcOdCerca = e.avCcOdCerca, avCcOiCerca = e.avCcOiCerca,
                            avCcAoPx = e.avCcAoPx,
                            phOd = e.phOd, phOi = e.phOi,
                            kappaOd = e.kappaOd, kappaOi = e.kappaOi,
                            hirshberg = e.hirshberg,
                            duccionesOd = e.duccionesOd, duccionesOi = e.duccionesOi,
                            versionesAo = e.versionesAo,
                            coverTest6m = e.coverTest6m, coverTest40cm = e.coverTest40cm, coverTest10cm = e.coverTest10cm,
                            ppcOr = e.ppcOr, ppcLuz = e.ppcLuz, ppcFrl = e.ppcFrl,
                            reflejoFotomotor = e.reflejoFotomotor, reflejoConsensual = e.reflejoConsensual, reflejoAcomodativo = e.reflejoAcomodativo,
                            k1Od = e.k1Od, k2Od = e.k2Od, k1Oi = e.k1Oi, k2Oi = e.k2Oi,
                            objOdEsf = e.objOdEsf, objOdCil = e.objOdCil, objOdEje = e.objOdEje,
                            objOiEsf = e.objOiEsf, objOiCil = e.objOiCil, objOiEje = e.objOiEje,
                            subjOdEsf = e.subjOdEsf, subjOdCil = e.subjOdCil, subjOdEje = e.subjOdEje,
                            subjOiEsf = e.subjOiEsf, subjOiCil = e.subjOiCil, subjOiEje = e.subjOiEje,
                            recetaOdEsf = e.recetaOdEsf, recetaOdCil = e.recetaOdCil, recetaOdEje = e.recetaOdEje, recetaOdAv = e.recetaOdAv,
                            recetaOiEsf = e.recetaOiEsf, recetaOiCil = e.recetaOiCil, recetaOiEje = e.recetaOiEje, recetaOiAv = e.recetaOiAv,
                            addCercaOd = e.addCercaOd, addCercaOi = e.addCercaOi,
                            addIntermediaOd = e.addIntermediaOd, addIntermediaOi = e.addIntermediaOi,
                            addAv = e.addAv,
                            dipLejos = e.dipLejos, dipCerca = e.dipCerca, dipIntermedio = e.dipIntermedio,
                            prismaOdValor = e.prismaOdValor, prismaOdBase = e.prismaOdBase,
                            prismaOiValor = e.prismaOiValor, prismaOiBase = e.prismaOiBase,
                            diagnostico = e.diagnostico,
                            planTratamiento = e.planTratamiento,
                            observaciones = e.observaciones,
                            proximaCita = e.proximaCita,
                            lcOdEsf = e.lcOdEsf, lcOdCil = e.lcOdCil, lcOdEje = e.lcOdEje,
                            lcOiEsf = e.lcOiEsf, lcOiCil = e.lcOiCil, lcOiEje = e.lcOiEje,
                            lcRadioBaseOd = e.lcRadioBaseOd, lcOdDia = e.lcDiametroOd,
                            lcRadioBaseOi = e.lcRadioBaseOi, lcOiDia = e.lcDiametroOi,
                            lcLaboratorio = e.lcLaboratorio, lcTipoLente = e.lcTipoLente,
                            lcMaterial = e.lcMaterial, lcFechaAdaptacion = e.lcFechaAdaptacion,
                            lcObservaciones = e.lcObservaciones
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> { }
            }
        }
    }

    fun updateUiState(update: (EvaluacionUiState) -> EvaluacionUiState) {
        _uiState.update(update)
    }

    fun saveEvaluacion(pacienteId: String, evaluacionId: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            val ev = EvaluacionClinica(
                id = evaluacionId ?: UUID.randomUUID().toString(),
                pacienteId = pacienteId,
                fecha = s.fecha,
                motivoConsulta = s.motivoConsulta,
                sintomas = s.sintomas,
                antecedentesPersonalesOculares = s.antecedentesPersonalesOculares,
                antecedentesPersonalesSistemicos = s.antecedentesPersonalesSistemicos,
                antecedentesFamiliaresOculares = s.antecedentesFamiliaresOculares,
                antecedentesFamiliaresSistemicos = s.antecedentesFamiliaresSistemicos,
                medicacion = s.medicacion,
                alergias = s.alergias,
                necesidadVisual = s.necesidadVisual,
                avScOdLejos = s.avScOdLejos, avScOiLejos = s.avScOiLejos,
                avScOdCerca = s.avScOdCerca, avScOiCerca = s.avScOiCerca,
                avScAo = s.avScAo,
                avCcOdLejos = s.avCcOdLejos, avCcOiLejos = s.avCcOiLejos,
                avCcOdCerca = s.avCcOdCerca, avCcOiCerca = s.avCcOiCerca,
                avCcAoPx = s.avCcAoPx,
                phOd = s.phOd, phOi = s.phOi, kappaOd = s.kappaOd, kappaOi = s.kappaOi,
                hirshberg = s.hirshberg,
                duccionesOd = s.duccionesOd, duccionesOi = s.duccionesOi,
                versionesAo = s.versionesAo,
                coverTest6m = s.coverTest6m, coverTest40cm = s.coverTest40cm, coverTest10cm = s.coverTest10cm,
                ppcOr = s.ppcOr, ppcLuz = s.ppcLuz, ppcFrl = s.ppcFrl,
                reflejoFotomotor = s.reflejoFotomotor, reflejoConsensual = s.reflejoConsensual, reflejoAcomodativo = s.reflejoAcomodativo,
                k1Od = s.k1Od, k2Od = s.k2Od, k1Oi = s.k1Oi, k2Oi = s.k2Oi,
                objOdEsf = s.objOdEsf, objOdCil = s.objOdCil, objOdEje = s.objOdEje,
                objOiEsf = s.objOiEsf, objOiCil = s.objOiCil, objOiEje = s.objOiEje,
                subjOdEsf = s.subjOdEsf, subjOdCil = s.subjOdCil, subjOdEje = s.subjOdEje,
                subjOiEsf = s.subjOiEsf, subjOiCil = s.subjOiCil, subjOiEje = s.subjOiEje,
                recetaOdEsf = s.recetaOdEsf, recetaOdCil = s.recetaOdCil, recetaOdEje = s.recetaOdEje, recetaOdAv = s.recetaOdAv,
                recetaOiEsf = s.recetaOiEsf, recetaOiCil = s.recetaOiCil, recetaOiEje = s.recetaOiEje, recetaOiAv = s.recetaOiAv,
                addCercaOd = s.addCercaOd, addCercaOi = s.addCercaOi,
                addIntermediaOd = s.addIntermediaOd, addIntermediaOi = s.addIntermediaOi, addAv = s.addAv,
                dipLejos = s.dipLejos, dipCerca = s.dipCerca, dipIntermedio = s.dipIntermedio,
                prismaOdValor = s.prismaOdValor, prismaOdBase = s.prismaOdBase,
                prismaOiValor = s.prismaOiValor, prismaOiBase = s.prismaOiBase,
                diagnostico = s.diagnostico,
                planTratamiento = s.planTratamiento,
                observaciones = s.observaciones,
                proximaCita = s.proximaCita,
                lcOdEsf = s.lcOdEsf, lcOdCil = s.lcOdCil, lcOdEje = s.lcOdEje,
                lcOiEsf = s.lcOiEsf, lcOiCil = s.lcOiCil, lcOiEje = s.lcOiEje,
                lcRadioBaseOd = s.lcRadioBaseOd, lcDiametroOd = s.lcOdDia,
                lcRadioBaseOi = s.lcRadioBaseOi, lcDiametroOi = s.lcOiDia,
                lcLaboratorio = s.lcLaboratorio, lcTipoLente = s.lcTipoLente,
                lcMaterial = s.lcMaterial, lcFechaAdaptacion = s.lcFechaAdaptacion,
                lcObservaciones = s.lcObservaciones
            )
            repository.insertEvaluacion(ev)
            onComplete()
        }
    }
}
