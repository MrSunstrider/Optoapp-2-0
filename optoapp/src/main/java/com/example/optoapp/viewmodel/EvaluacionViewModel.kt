package com.example.optoapp.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.Resource
import com.example.optoapp.data.SessionManager
import com.example.optoapp.domain.FaceLandmarkerUseCase
import com.example.optoapp.domain.IrisMeasurementExtractor
import com.example.optoapp.domain.MediaPipeNotAvailableException
import com.example.optoapp.notifications.NotificationHelper
import com.example.optoapp.sync.PostSaveSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DipMeasureResult(
    val dipLejos: String,
    val dipTotalMm: Double,
    val dnpOdMm: Double,
    val dnpOiMm: Double,
    val isReliable: Boolean
)

@HiltViewModel
class EvaluacionViewModel @Inject constructor(
    private val repository: OptoRepository,
    private val sessionManager: SessionManager,
    private val postSaveSyncScheduler: PostSaveSyncScheduler,
    private val notificationHelper: NotificationHelper,
    private val faceLandmarkerUseCase: FaceLandmarkerUseCase,
    private val irisMeasurementExtractor: IrisMeasurementExtractor,
    @ApplicationContext private val context: Context
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
                    val dipFormatted = formatDipForUi(e.dipLejos, e.dipTotalMm, e.dnpOdMm, e.dnpOiMm)
                    _uiState.update {
                        e.toEvaluacionUiState().copy(dipLejos = dipFormatted)
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
            val currentOpticaId = sessionManager.opticaId.first()
            val dipParsed = parseDipOrDnp(s.dipLejos)

            val ev = s.toEvaluacionClinica(evaluacionId, pacienteId, currentOpticaId, dipParsed)
            if (evaluacionId != null && evaluacionId != "null") {
                repository.updateEvaluacion(ev)
            } else {
                repository.insertEvaluacion(ev)
            }
            postSaveSyncScheduler.scheduleHistorialSync(currentOpticaId)
            val pResult = repository.getPacienteById(pacienteId)
            val pName = if (pResult is Resource.Success) pResult.data?.nombreCompleto ?: "Paciente" else "Paciente"
            onComplete(ev.id, pName)
        }
    }

    fun saveAndScheduleReminder(
        pacienteId: String,
        evaluacionId: String?,
        programarRecordatorio: Boolean,
        onComplete: (String) -> Unit
    ) {
        saveEvaluacion(pacienteId, evaluacionId) { savedId, pName ->
            if (programarRecordatorio && _uiState.value.proximaCita != null) {
                notificationHelper.scheduleWorkManagerReminder(pName, _uiState.value.proximaCita!!, savedId)
            } else {
                notificationHelper.cancelReminder(savedId)
            }
            onComplete(savedId)
        }
    }

    fun deleteEvaluacion(evaluacionId: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val result = repository.getEvaluacionById(evaluacionId)
            if (result is Resource.Success) {
                repository.deleteEvaluacion(result.data!!)
                val oid = sessionManager.opticaId.first()
                postSaveSyncScheduler.scheduleHistorialSync(oid)
                onComplete()
            }
        }
    }

    // --- Diagnóstico Automático ---

    fun updateDiagnosticAuto() {
        _uiState.update { computeDiagnosticoAuto(it) }
    }

    fun updateOtrosAuto() {
        _uiState.update { computeOtrosAuto(it) }
    }

    fun normalizeAndTranspose(ojo: String) {
        _uiState.update { normalizeAndTranspose(it, ojo) }
    }

    // ─── Iris-based DIP/DNP measurement from photo ───────────────────────

    private val _isMeasuringDip = MutableStateFlow(false)
    val isMeasuringDip: StateFlow<Boolean> = _isMeasuringDip.asStateFlow()

    /**
     * Measures DIP/DNP from a face photo using iris diameter as scale reference.
     *
     * Flow:
     * 1. Loads and decodes the bitmap from [uri]
     * 2. Detects face landmarks via [FaceLandmarkerUseCase]
     * 3. Estimates mm-per-pixel using [IrisMeasurementExtractor] (iris ≈ 11.7mm)
     * 4. Computes DIP and DNP OD/OI
     * 5. Auto-fills the form fields
     *
     * @param uri Content URI of the selected photo
     */
    fun measureDipFromPhoto(uri: Uri) {
        if (_isMeasuringDip.value) return
        _isMeasuringDip.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = decodeBitmap(uri) ?: run {
                    _isMeasuringDip.value = false
                    cleanupTempFile(uri)
                    return@launch
                }

                val detection = faceLandmarkerUseCase.detect(bitmap)
                val landmarks = detection.landmarks.firstOrNull() ?: run {
                    _isMeasuringDip.value = false
                    cleanupTempFile(uri)
                    return@launch
                }

                val result = irisMeasurementExtractor.measure(
                    landmarks = landmarks,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                ) ?: run {
                    _isMeasuringDip.value = false
                    cleanupTempFile(uri)
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    // DIP Lejos redondeado a entero
                    // DIP Cerca = DIP Lejos - 2mm (estándar óptico: convergencia en VP)
                    val dipLejosEntero = kotlin.math.round(result.dipMm).toInt()
                    val dipCercaEntero = (dipLejosEntero - 2).coerceAtLeast(0)
                    _uiState.update { it.copy(
                        dipLejos = dipLejosEntero.toString(),
                        dipCerca = if (dipCercaEntero > 0) dipCercaEntero.toString() else ""
                    ) }
                    _isMeasuringDip.value = false
                }
                cleanupTempFile(uri)
            } catch (e: MediaPipeNotAvailableException) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(error = "Cámara no disponible en este dispositivo") }
                    _isMeasuringDip.value = false
                }
                cleanupTempFile(uri)
            } catch (_: Exception) {
                _isMeasuringDip.value = false
                cleanupTempFile(uri)
            }
        }
    }

    private fun cleanupTempFile(uri: Uri) {
        try { context.contentResolver.delete(uri, null, null) } catch (_: Exception) { }
    }

    /**
     * Decodes a bitmap from a content URI, max 2048px.
     */
    private fun decodeBitmap(uri: Uri): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }

            val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, 2048)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxPx: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (width / sample > maxPx || height / sample > maxPx) {
            sample *= 2
        }
        return sample
    }
}
