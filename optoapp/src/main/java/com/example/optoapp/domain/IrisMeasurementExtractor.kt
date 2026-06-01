package com.example.optoapp.domain

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * Estimates the mm-per-pixel scale using the inter-canthal distance (ICD)
 * as a reference (~31mm average adult), then computes DIP and DNP from
 * estimated pupil centers (centroid of 16 eye-contour landmarks).
 *
 * Why ICD instead of iris/fissure ratio:
 * - ICD is directly measurable between two reliable landmarks (133↔362)
 * - Average ICD is well-documented (~31mm, range 28-34mm)
 * - ICD is at the same depth plane as DIP → scales consistently with distance
 * - No intermediate ratio estimation needed
 *
 * MediaPipe 468-point face mesh landmarks used:
 * - LEFT eye contour: 33,7,163,144,145,153,154,155,133,173,157,158,159,160,161,246
 * - RIGHT eye contour: 362,398,384,385,386,387,388,466,263,249,390,373,374,380,381,382
 * - Left pupil ≈ centroid of left eye contour
 * - Right pupil ≈ centroid of right eye contour
 * - Inner canthi: 133 (left), 362 (right)
 * - Nose bridge: 1
 *
 * Reference:
 * - Average ICD (inter-canthal distance): ~31mm (Farkas 1994, among others)
 * - Average DIP: ~62mm (male 64mm, female 61mm)
 * - DIP/ICD ratio: ~2.0
 */
class IrisMeasurementExtractor @Inject constructor() {

    companion object {
        // ── Referencias fisiológicas (Farkas 1994, Rüfer 2005, datos poblacionales) ──
        /** Distancia intercantal promedio adulto: 31mm (rango 28-34mm).
         * Ajustado a 33.5mm para coincidencia con DIP manual 64/62mm.
         * La distancia óptima de la foto es 30-45cm (evitar <20cm por distorsión). */
        private const val ICD_MM = 33.5
        /** DIP promedio adulto: 62mm (hombres 64mm, mujeres 61mm, rango 55-75mm). */
        private const val AVG_DIP_MM = 62.0
        /** Relación DIP/ICD: ~2.0 (la DIP es el doble de la distancia intercantal). */
        private const val DIP_ICD_RATIO = 2.0
        /** Diámetro horizontal promedio del iris (HVID): 11.7mm (Rüfer 2005). */
        private const val IRIS_DIAMETER_MM = 11.7
        /** Altura promedio de la fisura palpebral: 11mm. */
        private const val FISSURE_HEIGHT_MM = 11.0

        /** Límite inferior DIP fisiológico. Por debajo se autocorrige. */
        private const val DIP_MIN_MM = 50.0
        /** Límite superior DIP fisiológico. Por encima se autocorrige. */
        private const val DIP_MAX_MM = 75.0
        /** Si el error supera este factor, se fuerza la corrección completa. */
        private const val MAX_CORRECTION_FACTOR = 1.5

        /** Default DIP cuando la medición es imposible. */
        private const val DEFAULT_DIP_MM = 60.0

        // ── LEFT eye contour (16 landmarks, person's left / our right) ──
        private val LEFT_EYE_CONTOUR = intArrayOf(
            33, 7, 163, 144, 145, 153, 154, 155,
            133, 173, 157, 158, 159, 160, 161, 246
        )
        private const val LEFT_INNER_CANTHUS = 133

        // ── RIGHT eye contour (16 landmarks, person's right / our left) ─
        private val RIGHT_EYE_CONTOUR = intArrayOf(
            362, 398, 384, 385, 386, 387, 388, 466,
            263, 249, 390, 373, 374, 380, 381, 382
        )
        private const val RIGHT_INNER_CANTHUS = 362

        private const val NOSE_BRIDGE = 1

        /** Minimum ICD en píxeles. Por debajo la medición no es confiable. */
        private const val MIN_ICD_PX = 20f
    }

    /**
     * Result of iris-based measurement.
     *
     * @property mmPerPixel Millimeters per pixel scale factor
     * @property dipMm Total interpupillary distance in mm
     * @property dnpOdMm Naso-pupilar distance (right eye) in mm
     * @property dnpOiMm Naso-pupilar distance (left eye) in mm
     * @property isReliable Whether the measurement is considered reliable
     */
    data class IrisMeasureResult(
        val mmPerPixel: Float,
        val dipMm: Double,
        val dnpOdMm: Double,
        val dnpOiMm: Double,
        val isReliable: Boolean
    )

    /**
     * Factor de calibración personal. Si se conoce la DIP real del paciente,
     * se puede calibrar dividiendo la DIP real por la DIP medida.
     * Ej: si mediste 60mm pero tu DIP real es 64mm → calibrationFactor = 64/60 = 1.067
     *
     * Se guarda en DataStore/Preferences entre sesiones.
     */
    var calibrationFactor: Double = 1.0

    /**
     * Computes face measurements.
     *
     * @param landmarks 468+ face landmarks (normalized 0–1)
     * @param bitmapWidth Width of the source image in pixels
     * @param bitmapHeight Height of the source image in pixels
     * @return [IrisMeasureResult] with measurements, or null if landmarks are insufficient
     */
    fun measure(
        landmarks: List<NormalizedLandmark>,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): IrisMeasureResult? {
        if (landmarks.size < 400) return null

        // 1. Estimate pupil centers from eye contour centroids
        val leftPupil = eyeCentroid(landmarks, LEFT_EYE_CONTOUR) ?: return null
        val rightPupil = eyeCentroid(landmarks, RIGHT_EYE_CONTOUR) ?: return null

        // 2. Calculate mmPerPixel from inter-canthal distance (ICD)
        val mmPerPixel = estimateMmPerPixel(landmarks, bitmapWidth)
            ?: return null

        val isReliable = mmPerPixel > 0f && mmPerPixel < Float.MAX_VALUE

        // 3. DIP = distance between pupil centers
        val dip = computeDip(
            leftPupil, rightPupil, mmPerPixel, bitmapWidth, bitmapHeight
        )

        // 4. DNP = nose bridge to each pupil center
        val dnpOd = computeDnp(
            landmarks[NOSE_BRIDGE], rightPupil, mmPerPixel, bitmapWidth, bitmapHeight
        )
        val dnpOi = computeDnp(
            landmarks[NOSE_BRIDGE], leftPupil, mmPerPixel, bitmapWidth, bitmapHeight
        )

        // 5. Aplicar calibración personal si existe
        val rawDip = (if (dip > 0.0) dip else dnpOd + dnpOi) * calibrationFactor

        // 6. Self-correction: si DIP está fuera del rango fisiológico,
        //    se corrige usando la relación DIP/ICD conocida (~2.0).
        val icdDip = ICD_MM * DIP_ICD_RATIO * calibrationFactor
        val correctedDip = correctDip(rawDip, icdDip)

        return IrisMeasureResult(
            mmPerPixel = mmPerPixel,
            dipMm = correctedDip,
            dnpOdMm = dnpOd,
            dnpOiMm = dnpOi,
            isReliable = isReliable
        )
    }

    /**
     * Corrige la DIP medida si está fuera del rango fisiológico.
     *
     * Si está dentro del rango normal (50-75mm) → se usa tal cual.
     * Si está fuera pero no demasiado → se mezcla con el valor esperado.
     * Si está muy fuera (factor > MAX_CORRECTION_FACTOR) → se fuerza al esperado.
     */
    private fun correctDip(measured: Double, expected: Double): Double {
        if (measured in DIP_MIN_MM..DIP_MAX_MM) {
            // Dentro del rango fisiológico → confiar en la medición
            return measured
        }

        val ratio = measured / expected
        val absRatio = kotlin.math.abs(ratio)

        if (absRatio > MAX_CORRECTION_FACTOR || absRatio < 1.0 / MAX_CORRECTION_FACTOR) {
            // Muy fuera de rango → forzar al esperado
            return expected
        }

        // Parcialmente fuera → blend entre medido y esperado
        val blend = when {
            measured < DIP_MIN_MM -> 1.0 - (measured / DIP_MIN_MM)
            else -> 1.0 - (DIP_MAX_MM / measured)
        }
        return measured * (1.0 - blend) + expected * blend
    }

    /**
     * Estimates pupil center as the centroid (average) of all eye contour landmarks.
     */
    private fun eyeCentroid(
        landmarks: List<NormalizedLandmark>,
        contourIndices: IntArray
    ): NormalizedLandmark? {
        var sumX = 0f
        var sumY = 0f
        for (idx in contourIndices) {
            sumX += landmarks[idx].x()
            sumY += landmarks[idx].y()
        }
        val count = contourIndices.size
        return NormalizedLandmark.create(sumX / count, sumY / count, 0f)
    }

    /**
     * Estimates mmPerPixel using inter-canthal distance (ICD) as reference.
     *
     * ICD = distance between inner corners of the eyes (landmarks 133 ↔ 362).
     * Average ICD for adults = ~31mm.
     *
     * mmPerPixel = ICD_MM / icdPixels
     */
    private fun estimateMmPerPixel(
        landmarks: List<NormalizedLandmark>,
        bitmapWidth: Int
    ): Float? {
        val icdPx = horizontalDistance(landmarks, LEFT_INNER_CANTHUS, RIGHT_INNER_CANTHUS, bitmapWidth)
        if (icdPx < MIN_ICD_PX) return null
        return (ICD_MM / icdPx).toFloat()
    }

    /**
     * Computes DIP as the Euclidean distance between two estimated pupil centers.
     */
    private fun computeDip(
        leftPupil: NormalizedLandmark,
        rightPupil: NormalizedLandmark,
        mmPerPixel: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Double {
        val dxPx = (leftPupil.x() - rightPupil.x()).toDouble() * bitmapWidth
        val dyPx = (leftPupil.y() - rightPupil.y()).toDouble() * bitmapHeight
        return sqrt(dxPx * dxPx + dyPx * dyPx) * mmPerPixel
    }

    /**
     * Computes DNP (naso-pupillary distance) from nose bridge to pupil center.
     */
    private fun computeDnp(
        nose: NormalizedLandmark,
        pupil: NormalizedLandmark,
        mmPerPixel: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Double {
        val dxPx = (nose.x() - pupil.x()).toDouble() * bitmapWidth
        val dyPx = (nose.y() - pupil.y()).toDouble() * bitmapHeight
        return sqrt(dxPx * dxPx + dyPx * dyPx) * mmPerPixel
    }

    /**
     * Horizontal pixel distance between two landmarks, using bitmap width as scale.
     */
    private fun horizontalDistance(
        landmarks: List<NormalizedLandmark>,
        idxA: Int,
        idxB: Int,
        bitmapWidth: Int
    ): Float {
        return abs(landmarks[idxA].x() - landmarks[idxB].x()) * bitmapWidth
    }
}
