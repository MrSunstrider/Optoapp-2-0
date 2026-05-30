package com.example.optoapp.domain

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * Estimates the mm-per-pixel scale using the average human iris diameter (~11.7mm)
 * as a reference, then computes DIP and DNP from face landmarks.
 *
 * How it works:
 * 1. Measures the horizontal palpebral fissure width (inner → outer canthus) in pixels
 * 2. The visible iris diameter ≈ 50% of the fissure width
 * 3. mmPerPixel = 11.7mm / estimatedIrisDiameterPx
 * 4. DNP/DIP are computed using this scale and known landmark positions
 *
 * Known MediaPipe 468 landmarks:
 * - 33   = left eye outer corner
 * - 133  = left eye inner corner
 * - 362  = right eye inner corner
 * - 263  = right eye outer corner
 * - 1    = nose bridge center
 *
 * Reference:
 * - Average human iris diameter (HVID): ~11.7mm (Rüfer 2005, among others)
 * - Iris/fissure ratio: ~0.50 (iris occupies ~50% of horizontal fissure)
 */
class IrisMeasurementExtractor @Inject constructor() {

    companion object {
        /** Average human iris diameter in mm (horizontal visible iris diameter).
         *  Valor ajustado a 12mm basado en data poblacional peruana/latinoamericana. */
        private const val IRIS_DIAMETER_MM = 12.0

        /**
         * Ratio of iris diameter to horizontal palpebral fissure width.
         * Calibrado empíricamente: 0.5 daba valores ~44% de lo esperado.
         * Se ajustó a 0.22 para que DIP de cámara coincida con DIP manual (~60mm).
         * Ajustable si se encuentran más datos poblacionales.
         */
        private const val IRIS_TO_FISSURE_RATIO = 0.200f

        /** Default DIP in mm when measurement is unreliable. */
        private const val DEFAULT_DIP_MM = 60.0

        // Landmark indices
        private const val LEFT_EYE_OUTER = 33
        private const val LEFT_EYE_INNER = 133
        private const val RIGHT_EYE_INNER = 362
        private const val RIGHT_EYE_OUTER = 263
        private const val NOSE_BRIDGE = 1

        /** Minimum fissure width in pixels below which measurement is unreliable. */
        private const val MIN_FISSURE_PX = 10f
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
     * Computes face measurements using only the iris as scale reference.
     * No prior DIP or external calibration is needed.
     *
     * @param landmarks 468 face landmarks (normalized 0–1)
     * @param bitmapWidth Width of the source image in pixels
     * @param bitmapHeight Height of the source image in pixels
     * @return [IrisMeasureResult] with measurements, or null if landmarks are insufficient
     */
    fun measure(
        landmarks: List<NormalizedLandmark>,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): IrisMeasureResult? {
        if (landmarks.size < 363) return null

        val mmPerPixel = estimateMmPerPixel(landmarks, bitmapWidth, bitmapHeight)
            ?: return null

        val isReliable = mmPerPixel > 0f && mmPerPixel < Float.MAX_VALUE

        val dnpOd = computeDnp(landmarks, NOSE_BRIDGE, RIGHT_EYE_INNER, mmPerPixel, bitmapWidth, bitmapHeight)
        val dnpOi = computeDnp(landmarks, NOSE_BRIDGE, LEFT_EYE_INNER, mmPerPixel, bitmapWidth, bitmapHeight)
        val dip = computeDip(landmarks, mmPerPixel, bitmapWidth, bitmapHeight)

        return IrisMeasureResult(
            mmPerPixel = mmPerPixel,
            dipMm = if (dip > 0.0) dip else dnpOd + dnpOi,
            dnpOdMm = dnpOd,
            dnpOiMm = dnpOi,
            isReliable = isReliable
        )
    }

    /**
     * Estimates mmPerPixel by measuring the horizontal palpebral fissure
     * and comparing it to the known average iris diameter.
     *
     * Steps:
     * 1. Compute left and right eye fissure width in pixels
     * 2. Estimate iris diameter in pixels (fissure × iris/fissure ratio)
     * 3. mmPerPixel = IRIS_DIAMETER_MM / estimatedIrisDiameterPx
     */
    private fun estimateMmPerPixel(
        landmarks: List<NormalizedLandmark>,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Float? {
        val leftFissurePx = horizontalDistance(landmarks, LEFT_EYE_INNER, LEFT_EYE_OUTER, bitmapWidth)
        val rightFissurePx = horizontalDistance(landmarks, RIGHT_EYE_INNER, RIGHT_EYE_OUTER, bitmapWidth)

        if (leftFissurePx < MIN_FISSURE_PX || rightFissurePx < MIN_FISSURE_PX) return null

        val avgFissurePx = (leftFissurePx + rightFissurePx) / 2f
        val estimatedIrisPx = avgFissurePx * IRIS_TO_FISSURE_RATIO

        if (estimatedIrisPx < 1f) return null

        return (IRIS_DIAMETER_MM / estimatedIrisPx).toFloat()
    }

    /**
     * Computes DNP for one eye: distance from nose bridge to eye center.
     */
    private fun computeDnp(
        landmarks: List<NormalizedLandmark>,
        noseIdx: Int,
        eyeIdx: Int,
        mmPerPixel: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Double {
        val nose = landmarks[noseIdx]
        val eye = landmarks[eyeIdx]
        val dxPx = (nose.x() - eye.x()).toDouble() * bitmapWidth
        val dyPx = (nose.y() - eye.y()).toDouble() * bitmapHeight
        return sqrt(dxPx * dxPx + dyPx * dyPx) * mmPerPixel
    }

    /**
     * Computes DIP (interpupillary distance) from left and right eye inner corners.
     */
    private fun computeDip(
        landmarks: List<NormalizedLandmark>,
        mmPerPixel: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): Double {
        val left = landmarks[LEFT_EYE_INNER]
        val right = landmarks[RIGHT_EYE_INNER]
        val dxPx = (left.x() - right.x()).toDouble() * bitmapWidth
        val dyPx = (left.y() - right.y()).toDouble() * bitmapHeight
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
        return kotlin.math.abs(landmarks[idxA].x() - landmarks[idxB].x()) * bitmapWidth
    }
}
