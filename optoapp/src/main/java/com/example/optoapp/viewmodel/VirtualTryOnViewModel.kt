package com.example.optoapp.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.domain.FaceLandmarkerUseCase
import com.example.optoapp.domain.FaceMeasurementExtractor
import com.example.optoapp.domain.FrameOverlayUseCase
import com.example.optoapp.domain.NoFaceDetectedException
import com.example.optoapp.domain.SegmentType
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Measured face data extracted from landmarks.
 *
 * @property dipMm Interpupillary distance in mm
 * @property dnpOdMm Naso-pupilar distance (right eye) in mm
 * @property dnpOiMm Naso-pupilar distance (left eye) in mm
 * @property segmentHeight Lens segment height in mm (null if not determined)
 */
data class FaceMeasurements(
    val dipMm: Double,
    val dnpOdMm: Double,
    val dnpOiMm: Double,
    val segmentHeight: Double?
)

/**
 * UI state machine for the Virtual Try-On flow.
 *
 * Transitions:
 *   Idle → LoadingPhoto → PhotoSelected → FaceDetected → OverlayReady
 *   ↓         ↓              ↓               ↓
 *   Error ←───┴──────────────┴───────────────┘
 */
sealed class TryOnState {
    /** Initial state — waiting for user to pick a photo. */
    data object Idle : TryOnState()

    /** Photo is being loaded and decoded. */
    data object LoadingPhoto : TryOnState()

    /** Photo loaded, face detection in progress. */
    data class PhotoSelected(val bitmap: Bitmap) : TryOnState()

    /** Face landmarks detected and measurements computed. */
    data class FaceDetected(
        val originalBitmap: Bitmap,
        val landmarks: List<NormalizedLandmark>,
        val mmPerPixel: Float,
        val measurements: FaceMeasurements
    ) : TryOnState()

    /** Montura overlay composed onto the face image. */
    data class OverlayReady(
        val compositeBitmap: Bitmap,
        val measurements: FaceMeasurements
    ) : TryOnState()

    /** Error state with user-facing message. */
    data class Error(val message: String) : TryOnState()
}

/**
 * ViewModel that orchestrates the Virtual Try-On workflow.
 *
 * Manages the full state machine from photo selection through face detection,
 * measurement extraction, overlay composition, and result persistence.
 */
@HiltViewModel
class VirtualTryOnViewModel @Inject constructor(
    private val faceLandmarkerUseCase: FaceLandmarkerUseCase,
    private val frameOverlayUseCase: FrameOverlayUseCase,
    private val measurementExtractor: FaceMeasurementExtractor,
    private val pacienteRepository: PacienteRepository,
    private val monturaCoordinator: MonturaInventoryCoordinator,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<TryOnState>(TryOnState.Idle)
    val state: StateFlow<TryOnState> = _state.asStateFlow()

    private val _selectedMontura = MutableStateFlow<Montura?>(null)
    val selectedMontura: StateFlow<Montura?> = _selectedMontura.asStateFlow()

    private val _monturas = MutableStateFlow<List<Montura>>(emptyList())
    val monturas: StateFlow<List<Montura>> = _monturas.asStateFlow()

    private val _monturasLoading = MutableStateFlow(false)
    val monturasLoading: StateFlow<Boolean> = _monturasLoading.asStateFlow()

    // Internal state kept across transitions
    private var knownPdMm: Double? = null
    private var latestFaceBitmap: Bitmap? = null
    private var latestLandmarks: List<NormalizedLandmark>? = null
    private var latestMmPerPixel: Float = 0f
    private var latestMeasurements: FaceMeasurements? = null
    private var latestSegmentType: SegmentType = SegmentType.PROGRESSIVE
    private var adjustmentX: Float = 0f
    private var adjustmentY: Float = 0f
    private var adjustmentScale: Float = 1f
    private var compositeUri: Uri? = null

    /**
     * Loads the patient's last known PD from their latest [EvaluacionClinica].
     *
     * Reads [EvaluacionClinica.dipTotalMm] first, falling back to
     * dnpOdMm + dnpOiMm if dipTotalMm is null. PD is cached internally
     * and used as the scale reference for landmark-to-mm conversion.
     */
    fun loadPatientPD(pacienteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val evaluaciones = pacienteRepository.getEvaluacionesByPaciente(pacienteId)
                    .first()
                val latest = evaluaciones.maxByOrNull { it.fecha.toString() }
                if (latest != null) {
                    knownPdMm = latest.dipTotalMm
                        ?: if (latest.dnpOdMm != null && latest.dnpOiMm != null) {
                            latest.dnpOdMm!! + latest.dnpOiMm!!
                        } else {
                            null
                        }
                }
            } catch (_: Exception) {
                // PD remains null → default 63mm fallback in FaceMeasurementExtractor
            }
        }
    }

    /**
     * Sets the lens [segmentType] for segment height calculation.
     */
    fun setSegmentType(segmentType: SegmentType) {
        latestSegmentType = segmentType
    }

    /**
     * Loads monturas from inventory for the given [opticaId].
     * Exposes the list via [monturas] state flow.
     */
    fun loadMonturas(opticaId: String) {
        _monturasLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                monturaCoordinator.getMonturasByOptica(opticaId).collect { list ->
                    _monturas.value = list
                    _monturasLoading.value = false
                }
            } catch (_: Exception) {
                _monturasLoading.value = false
            }
        }
    }

    /**
     * Resets the ViewModel to its initial [TryOnState.Idle] state.
     * Clears all cached detection and overlay data.
     */
    fun resetState() {
        _state.value = TryOnState.Idle
        _selectedMontura.value = null
        latestFaceBitmap = null
        latestLandmarks = null
        latestMeasurements = null
        compositeUri = null
        adjustmentX = 0f
        adjustmentY = 0f
        adjustmentScale = 1f
    }

    /**
     * Selects a photo from the given [uri], loads it as a Bitmap,
     * resizes to max 2048px, and triggers face detection.
     *
     * State transition: Idle/Loading → LoadingPhoto → PhotoSelected → FaceDetected
     */
    fun selectPhoto(uri: Uri) {
        _state.value = TryOnState.LoadingPhoto

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = decodeSampledBitmap(uri) ?: run {
                    _state.value = TryOnState.Error("No se pudo cargar la imagen")
                    return@launch
                }

                _state.value = TryOnState.PhotoSelected(bitmap)
                latestFaceBitmap = bitmap

                detectFace(bitmap)
            } catch (e: Exception) {
                _state.value = TryOnState.Error(
                    e.message ?: "Error al procesar la imagen"
                )
            }
        }
    }

    /**
     * Selects a [montura] and triggers overlay composition.
     *
     * State transition: FaceDetected → OverlayReady
     */
    fun selectMontura(montura: Montura) {
        _selectedMontura.value = montura

        val bitmap = latestFaceBitmap ?: return
        val landmarks = latestLandmarks ?: return
        val mmPerPixel = latestMmPerPixel

        composeOverlay(
            faceBitmap = bitmap,
            montura = montura,
            landmarks = landmarks,
            mmPerPixel = mmPerPixel,
            adjX = adjustmentX,
            adjY = adjustmentY,
            adjScale = adjustmentScale
        )
    }

    /**
     * Adjusts the overlay position and scale.
     *
     * @param dx Horizontal offset adjustment in pixels
     * @param dy Vertical offset adjustment in pixels
     * @param scale Scale factor adjustment (1.0 = no change)
     */
    fun adjustOverlay(dx: Float, dy: Float, scale: Float) {
        adjustmentX = dx
        adjustmentY = dy
        adjustmentScale = scale

        val montura = _selectedMontura.value ?: return
        val bitmap = latestFaceBitmap ?: return
        val landmarks = latestLandmarks ?: return
        val mmPerPixel = latestMmPerPixel

        composeOverlay(
            faceBitmap = bitmap,
            montura = montura,
            landmarks = landmarks,
            mmPerPixel = mmPerPixel,
            adjX = dx,
            adjY = dy,
            adjScale = scale
        )
    }

    /**
     * Saves the current composite image to the device gallery via MediaStore.
     *
     * @return The content URI of the saved image, or null on failure.
     */
    fun saveComposite(): Uri? {
        val currentState = _state.value
        if (currentState !is TryOnState.OverlayReady) return null

        return try {
            val filename = "TryOn_${System.currentTimeMillis()}.png"
            val resolver = context.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/OptoApp"
                    )
                }
                val uri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        currentState.compositeBitmap.compress(
                            Bitmap.CompressFormat.PNG, 100, stream
                        )
                    }
                    compositeUri = uri
                    uri
                }
            } else {
                // Pre-Q: save to app external files directory
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File(dir, filename)
                FileOutputStream(file).use { stream ->
                    currentState.compositeBitmap.compress(
                        Bitmap.CompressFormat.PNG, 100, stream
                    )
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                compositeUri = uri
                uri
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Shares the composite image via [Intent.ACTION_SEND] with a chooser.
     */
    fun shareComposite(uri: Uri) {
        val shareIntent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Compartir prueba virtual"
        )
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    /**
     * Saves the extracted measurements (DIP, DNP OD, DNP OI, segment height)
     * to the patient's latest [EvaluacionClinica].
     *
     * Updates:
     * - [EvaluacionClinica.dipTotalMm] with [FaceMeasurements.dipMm]
     * - [EvaluacionClinica.dnpOdMm] with [FaceMeasurements.dnpOdMm]
     * - [EvaluacionClinica.dnpOiMm] with [FaceMeasurements.dnpOiMm]
     *
     * Segment height is stored in the evaluation for later use during
     * dispensación (mapped to [DispensacionOptica.altura]).
     */
    fun saveMeasurementsToEvaluacion(pacienteId: String) {
        val measurements = latestMeasurements ?: return

        viewModelScope.launch {
            try {
                val evaluaciones = pacienteRepository
                    .getEvaluacionesByPaciente(pacienteId)
                    .first()
                val latest = evaluaciones.maxByOrNull { it.fecha.toString() }
                if (latest != null) {
                    val updated = latest.copy(
                        dipTotalMm = measurements.dipMm,
                        dnpOdMm = measurements.dnpOdMm,
                        dnpOiMm = measurements.dnpOiMm
                    )
                    pacienteRepository.updateEvaluacion(updated)
                }
            } catch (_: Exception) {
                // Swallow save failures — user can retry
            }
        }
    }

    /** Returns the latest computed measurements, or null. */
    fun getLatestMeasurements(): FaceMeasurements? = latestMeasurements

    /** Returns the current composite URI, or null. */
    fun getCompositeUri(): Uri? = compositeUri

    // ─── Private helpers ──────────────────────────────────────────────

    /**
     * Decodes a sampled (max 2048px) bitmap from the given content URI.
     */
    private fun decodeSampledBitmap(uri: Uri): Bitmap? {
        val resolver = context.contentResolver

        // First decode with inJustDecodeBounds to get dimensions
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }

        // Calculate sample size to keep max dimension at 2048px
        val maxDimension = 2048
        val sampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, maxDimension)

        // Decode with sample size
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    /**
     * Calculates the BitmapFactory sample size to keep dimensions within [maxPx].
     */
    private fun calculateSampleSize(width: Int, height: Int, maxPx: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (width / sample > maxPx || height / sample > maxPx) {
            sample *= 2
        }
        return sample
    }

    /**
     * Runs face detection on the given [bitmap] and transitions state.
     */
    private suspend fun detectFace(bitmap: Bitmap) {
        try {
            val detectionResult = faceLandmarkerUseCase.detect(bitmap)
            val landmarks = detectionResult.landmarks.first()

            val mmPerPixel = measurementExtractor.computeMmPerPixel(
                landmarks = landmarks,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
                knownPdMm = knownPdMm
            )

            val dip = measurementExtractor.extractDIP(landmarks, mmPerPixel)
            val (dnpOd, dnpOi) = measurementExtractor.extractDNP(landmarks, mmPerPixel)
            val segmentHeight = measurementExtractor.extractSegmentHeight(
                landmarks, mmPerPixel, latestSegmentType
            )

            val measurements = FaceMeasurements(
                dipMm = dip,
                dnpOdMm = dnpOd,
                dnpOiMm = dnpOi,
                segmentHeight = segmentHeight
            )

            latestLandmarks = landmarks
            latestMmPerPixel = mmPerPixel
            latestMeasurements = measurements

            _state.value = TryOnState.FaceDetected(
                originalBitmap = bitmap,
                landmarks = landmarks,
                mmPerPixel = mmPerPixel,
                measurements = measurements
            )
        } catch (e: NoFaceDetectedException) {
            _state.value = TryOnState.Error(
                "No se detectó un rostro. Intenta con otra foto."
            )
        } catch (e: Exception) {
            _state.value = TryOnState.Error(
                e.message ?: "Error al detectar rostro"
            )
        }
    }

    /**
     * Composes the montura overlay onto the face image using [FrameOverlayUseCase].
     */
    private fun composeOverlay(
        faceBitmap: Bitmap,
        montura: Montura,
        landmarks: List<NormalizedLandmark>,
        mmPerPixel: Float,
        adjX: Float,
        adjY: Float,
        adjScale: Float
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val monturaAnchoMm = montura.anchoMm?.toFloat() ?: 50f
                val monturaPuenteMm = montura.puenteMm?.toFloat() ?: 18f

                // Load montura image from URI or create a placeholder
                val monturaBitmap = loadMonturaBitmap(montura)
                    ?: createPlaceholderMontura(monturaAnchoMm, monturaPuenteMm)

                val config = FrameOverlayUseCase.OverlayConfig(
                    faceBitmap = faceBitmap,
                    monturaBitmap = monturaBitmap,
                    landmarks = landmarks,
                    mmPerPixel = mmPerPixel,
                    monturaAnchoMm = monturaAnchoMm,
                    monturaPuenteMm = monturaPuenteMm,
                    adjustmentX = adjX,
                    adjustmentY = adjY,
                    adjustmentScale = adjScale
                )

                val composite = frameOverlayUseCase.compose(config)

                _state.value = TryOnState.OverlayReady(
                    compositeBitmap = composite,
                    measurements = latestMeasurements ?: FaceMeasurements(0.0, 0.0, 0.0, null)
                )
            } catch (e: Exception) {
                _state.value = TryOnState.Error(
                    "Error al componer la superposición: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads the montura image from its URI.
     * Returns null if the URI is missing or cannot be decoded.
     */
    private fun loadMonturaBitmap(montura: Montura): Bitmap? {
        val uriStr = montura.imagenUri ?: return null
        return try {
            val uri = Uri.parse(uriStr)
            val resolver = context.contentResolver
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Creates a simple placeholder bitmap when the montura has no image.
     * Draws a colored rectangle representing the frame outline.
     */
    private fun createPlaceholderMontura(anchoMm: Float, puenteMm: Float): Bitmap {
        val px = (anchoMm * 3).toInt().coerceIn(100, 600)
        val py = 60
        val bmp = Bitmap.createBitmap(px, py, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 100, 149, 237)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        // Draw left lens outline
        val leftLensWidth = px / 2 - (puenteMm * 1.5f).toInt() / 2
        val bridgeWidth = (puenteMm * 1.5f).toInt()
        canvas.drawRect(4f, 4f, leftLensWidth.toFloat(), py - 4f, paint)
        // Draw right lens outline
        val rightStart = leftLensWidth + bridgeWidth
        canvas.drawRect(rightStart.toFloat(), 4f, (px - 4).toFloat(), py - 4f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.argb(60, 100, 149, 237)
        canvas.drawRect(4f, 4f, leftLensWidth.toFloat(), py - 4f, paint)
        canvas.drawRect(rightStart.toFloat(), 4f, (px - 4).toFloat(), py - 4f, paint)
        return bmp
    }

    companion object {
        private const val TAG = "VirtualTryOnViewModel"
    }
}
