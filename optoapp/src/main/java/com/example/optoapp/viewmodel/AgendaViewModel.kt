package com.example.optoapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.notifications.NotificationHelper
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class AgendaFiltro { HOY, SEMANA, MES }

data class AgendaFila(
    val evaluacion: EvaluacionClinica,
    val nombrePaciente: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("optoapp_prefs", Context.MODE_PRIVATE)

    private val filtro = MutableStateFlow(AgendaFiltro.HOY)
    private val mesCursor = MutableStateFlow(YearMonth.now())

    val filtroState: StateFlow<AgendaFiltro> = filtro.asStateFlow()
    val mesCursorState: StateFlow<YearMonth> = mesCursor.asStateFlow()

    fun setFiltro(f: AgendaFiltro) {
        filtro.value = f
    }

    fun mesAnterior() {
        mesCursor.value = mesCursor.value.minusMonths(1)
    }

    fun mesSiguiente() {
        mesCursor.value = mesCursor.value.plusMonths(1)
    }

    private fun rangoPara(f: AgendaFiltro, ym: YearMonth): Pair<LocalDate, LocalDate> {
        val hoy = LocalDate.now()
        return when (f) {
            AgendaFiltro.HOY -> hoy to hoy
            AgendaFiltro.SEMANA -> {
                val lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val domingo = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                lunes to domingo
            }
            AgendaFiltro.MES -> ym.atDay(1) to ym.atEndOfMonth()
        }
    }

    val filas: StateFlow<List<AgendaFila>> =
        combine(sessionManager.opticaId, filtro, mesCursor) { oid, f, ym ->
            Triple(oid, f, ym)
        }.flatMapLatest { (opticaId, f, ym) ->
            if (opticaId.isBlank()) {
                flowOf(emptyList())
            } else {
                val (start, end) = rangoPara(f, ym)
                combine(
                    repository.getEvaluacionesProximaCitaEnRango(opticaId, start, end),
                    repository.pacientesFlowForOptica(opticaId),
                ) { evals, pacientes ->
                    val byId = pacientes.associateBy { it.id }
                    evals.map { e ->
                        AgendaFila(e, byId[e.pacienteId]?.nombreCompleto ?: "Paciente")
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun actualizarEstado(evalId: String, nuevoEstado: String) {
        viewModelScope.launch {
            val res = repository.getEvaluacionById(evalId, sessionManager.opticaId.first())
            if (res !is Resource.Success) return@launch
            val ev = res.data ?: return@launch
            repository.updateEvaluacion(ev.copy(citaEstado = nuevoEstado.trim().ifBlank { "programada" }))
            postSaveSyncScheduler.scheduleHistorialSync(sessionManager.opticaId.first())
        }
    }

    fun reprogramar(evalId: String, nuevaFecha: LocalDate) {
        viewModelScope.launch {
            val res = repository.getEvaluacionById(evalId, sessionManager.opticaId.first())
            if (res !is Resource.Success) return@launch
            val ev = res.data ?: return@launch
            repository.updateEvaluacion(
                ev.copy(proximaCita = nuevaFecha, citaEstado = "reprogramada"),
            )
            postSaveSyncScheduler.scheduleHistorialSync(sessionManager.opticaId.first())

            val remindersOn = prefs.getBoolean("pref_enable_reminders", true)
            val helper = NotificationHelper(appContext)
            if (remindersOn) {
                val pName = when (val pr = repository.getPacienteByIdScoped(ev.pacienteId, sessionManager.opticaId.first())) {
                    is Resource.Success -> pr.data?.nombreCompleto ?: "Paciente"
                    else -> "Paciente"
                }
                helper.scheduleWorkManagerReminder(pName, nuevaFecha, evalId)
            } else {
                helper.cancelReminder(evalId)
            }
        }
    }
}
