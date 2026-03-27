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
import java.util.Locale
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
                            diagnosticoOd = e.diagnosticoOd,
                            diagnosticoOi = e.diagnosticoOi,
                            diagnosticoOtros = e.diagnosticoOtros,
                            planTratamiento = e.planTratamiento,
                            observaciones = e.observaciones,
                            proximaCita = e.proximaCita,
                            balanceOd = e.balanceOd,
                            balanceOi = e.balanceOi,
                            otrosPresbicia = e.otrosPresbicia,
                            otrosAnisometropia = e.otrosAnisometropia,
                            otrosAmbliopia = e.otrosAmbliopia,
                            autoPresbicia = e.autoPresbicia,
                            autoAnisometropia = e.autoAnisometropia,
                            autoAmbliopia = e.autoAmbliopia,
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

    fun saveEvaluacion(pacienteId: String, evaluacionId: String?, onComplete: (String, String) -> Unit) {
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
                diagnosticoOd = s.diagnosticoOd,
                diagnosticoOi = s.diagnosticoOi,
                diagnosticoOtros = s.diagnosticoOtros,
                planTratamiento = s.planTratamiento,
                observaciones = s.observaciones,
                proximaCita = s.proximaCita,
                balanceOd = s.balanceOd,
                balanceOi = s.balanceOi,
                otrosPresbicia = s.otrosPresbicia,
                otrosAnisometropia = s.otrosAnisometropia,
                otrosAmbliopia = s.otrosAmbliopia,
                autoAmbliopia = s.autoAmbliopia,
                lcOdEsf = s.lcOdEsf, lcOdCil = s.lcOdCil, lcOdEje = s.lcOdEje,
                lcOiEsf = s.lcOiEsf, lcOiCil = s.lcOiCil, lcOiEje = s.lcOiEje,
                lcRadioBaseOd = s.lcRadioBaseOd, lcDiametroOd = s.lcOdDia,
                lcRadioBaseOi = s.lcRadioBaseOi, lcDiametroOi = s.lcOiDia,
                lcLaboratorio = s.lcLaboratorio, lcTipoLente = s.lcTipoLente,
                lcMaterial = s.lcMaterial, lcFechaAdaptacion = s.lcFechaAdaptacion,
                lcObservaciones = s.lcObservaciones
            )
            repository.insertEvaluacion(ev)
            val pResult = repository.getPacienteById(pacienteId)
            val pName = if (pResult is com.example.optoapp.data.Resource.Success) pResult.data?.nombreCompleto ?: "Paciente" else "Paciente"
            onComplete(ev.id, pName)
        }
    }

    fun deleteEvaluacion(evaluacionId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = repository.getEvaluacionById(evaluacionId)
            if (result is Resource.Success) {
                repository.deleteEvaluacion(result.data!!)
                onComplete()
            }
        }
    }

    // --- Lógica de Diagnóstico Automático ---

    private fun parseRefraction(v: String): Double? {
        val clean = v.lowercase().trim()
        if (clean in listOf("plano", "neutro", "pl", "nt")) return 0.0
        return clean.replace(",", ".").toDoubleOrNull()
    }

    fun calcularDiagnostico(esferaStr: String, cilindroStr: String): String {
        val esf = parseRefraction(esferaStr)
        val cil = parseRefraction(cilindroStr)

        // Si ambos son null (no se ha digitado nada), no hay diagnóstico automático
        if (esf == null && cil == null) return ""

        // Si uno tiene valor y el otro no, el otro se trata como 0.00
        var e = esf ?: 0.0
        var c = cil ?: 0.0

        // Transposición a cilindro negativo para estandarizar
        if (c > 0) {
            e += c
            c = -c
        }

        val meridian1 = e
        val meridian2 = e + c

        return when {
            // Caso 1: Emetropía
            meridian1 == 0.0 && meridian2 == 0.0 -> "Emetropía"
            
            // Caso 2: Miopía pura (sin cilindro)
            meridian1 < 0.0 && c == 0.0 -> "Miopía"
            
            // Caso 3: Hipermetropía pura (sin cilindro)
            meridian1 > 0.0 && c == 0.0 -> "Hipermetropía"
            
            // Caso 4: Astigmatismo miópico simple (un meridiano en cero, el otro negativo)
            (meridian1 == 0.0 && meridian2 < 0.0) || (meridian1 < 0.0 && meridian2 == 0.0) -> "Astigmatismo miópico simple"
            
            // Caso 5: Astigmatismo hipermetrópico simple (un meridiano en cero, el otro positivo)
            (meridian1 == 0.0 && meridian2 > 0.0) || (meridian1 > 0.0 && meridian2 == 0.0) -> "Astigmatismo hipermetrópico simple"
            
            // Caso 6: Astigmatismo miópico compuesto (ambos meridianos negativos)
            meridian1 < 0.0 && meridian2 < 0.0 -> "Astigmatismo miópico compuesto"
            
            // Caso 7: Astigmatismo hipermetrópico compuesto (ambos meridianos positivos)
            meridian1 > 0.0 && meridian2 > 0.0 -> "Astigmatismo hipermetrópico compuesto"
            
            // Caso 8: Astigmatismo mixto (un meridiano positivo y el otro negativo)
            (meridian1 > 0.0 && meridian2 < 0.0) || (meridian1 < 0.0 && meridian2 > 0.0) -> "Astigmatismo mixto"
            
            else -> "Astigmatismo mixto"
        }
    }

    private fun parseSnellenToLogMar(snellen: String): Double? {
        try {
            val clean = snellen.trim()
            if (!clean.contains("/")) return null
            val parts = clean.split("/")
            if (parts.size != 2) return null
            val denominator = parts[1].trim().toDoubleOrNull() ?: return null
            if (denominator <= 0) return null
            val decimalAV = 20.0 / denominator
            return -Math.log10(decimalAV)
        } catch (e: Exception) { return null }
    }

    fun updateDiagnosticAuto() {
        _uiState.update { s ->
            // Diagnostic intention: an eye is considered to have data if any of its distance refraction fields 
            // has something typed (including "plano/neutro").
            val hasDataOd = s.recetaOdEsf.trim().isNotEmpty() || s.recetaOdCil.trim().isNotEmpty()
            val hasDataOi = s.recetaOiEsf.trim().isNotEmpty() || s.recetaOiCil.trim().isNotEmpty()
            
            val diagOd = if (s.balanceOd) "Balance" 
                         else if (hasDataOd) calcularDiagnostico(s.recetaOdEsf, s.recetaOdCil) 
                         else ""
                         
            val diagOi = if (s.balanceOi) "Balance" 
                         else if (hasDataOi) calcularDiagnostico(s.recetaOiEsf, s.recetaOiCil) 
                         else ""
            
            s.copy(
                diagnosticoOd = if (diagOd.isNotEmpty()) listOf(diagOd) else emptyList(),
                diagnosticoOi = if (diagOi.isNotEmpty()) listOf(diagOi) else emptyList()
            )
        }
        updateOtrosAuto()
    }

    fun updateOtrosAuto() {
        _uiState.update { s ->
            val addValStr = if (s.addCercaOd.isNotEmpty()) s.addCercaOd else s.addCercaOi
            val addVal = parseRefraction(addValStr) ?: 0.0
            val presbiciaVal = addVal > 0
            
            val eeOd = (parseRefraction(s.recetaOdEsf) ?: 0.0) + ((parseRefraction(s.recetaOdCil) ?: 0.0) / 2.0)
            val eeOi = (parseRefraction(s.recetaOiEsf) ?: 0.0) + ((parseRefraction(s.recetaOiCil) ?: 0.0) / 2.0)
            val anisometropiaVal = Math.abs(eeOd - eeOi) >= 2.00
            
            val logMarOd = parseSnellenToLogMar(s.avCcOdLejos)
            val logMarOi = parseSnellenToLogMar(s.avCcOiLejos)
            val ambliopiaVal = if (logMarOd != null && logMarOi != null) {
                Math.abs(logMarOd - logMarOi) >= 0.19 
            } else s.otrosAmbliopia
            
            s.copy(
                otrosPresbicia = if (s.autoPresbicia) presbiciaVal else s.otrosPresbicia,
                otrosAnisometropia = if (s.autoAnisometropia) anisometropiaVal else s.otrosAnisometropia,
                otrosAmbliopia = if (s.autoAmbliopia) ambliopiaVal else s.otrosAmbliopia
            )
        }
    }

    fun normalizeAndTranspose(ojo: String) {
        _uiState.update { s ->
            val esfStr = if (ojo == "OD") s.recetaOdEsf else s.recetaOiEsf
            val cilStr = if (ojo == "OD") s.recetaOdCil else s.recetaOiCil
            val ejeStr = if (ojo == "OD") s.recetaOdEje else s.recetaOiEje
            
            val eVal = parseRefraction(esfStr) ?: return@update s
            val cVal = parseRefraction(cilStr) ?: return@update s
            val ejeVal = ejeStr.toIntOrNull() ?: 0
            
            if (cVal > 0) {
                val newEsfNum = eVal + cVal
                val newCilNum = -cVal
                var newEjeNum = ejeVal + 90
                if (newEjeNum > 180) newEjeNum -= 180
                
                val newEsf = if (newEsfNum == 0.0) "plano" else "%.2f".format(Locale.US, newEsfNum)
                val newCil = "%.2f".format(Locale.US, newCilNum)
                val newEje = newEjeNum.toString()
                
                if (ojo == "OD") {
                    s.copy(recetaOdEsf = newEsf, recetaOdCil = newCil, recetaOdEje = newEje)
                } else {
                    s.copy(recetaOiEsf = newEsf, recetaOiCil = newCil, recetaOiEje = newEje)
                }
            } else {
                s
            }
        }
    }
}
