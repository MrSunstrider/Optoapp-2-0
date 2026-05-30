package com.example.optoapp.domain

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * Pure domain class that extracts face measurements from MediaPipe face landmarks.
 *
 * All coordinates are normalized (0.0–1.0). Pixel dimensions are required for
 * mm-per-pixel calibration. The caller supplies knownPD_mm (typically from
 * [Evaluacion.dipTotalMm]) to convert pixel distances to millimeters.
 *
 * Known landmark indices (MediaPipe 468-point face mesh):
 * - 1   = nose bridge center
 * - 133 = left eye center (pupil)
 * - 159 = right eye lower eyelid margin
 * - 145 = left eye lower eyelid margin
 * - 362 = right eye center (pupil)
 *
 * NORMS OF CALCULATION (from design doc):
 * - mmPerPixel = knownPD_mm / pupilDistance_px
 * - Pupil landmarks: 133 (left), 362 (right)
 * - Nose bridge center: index 1
 * - DIP: distance between pupil centers in mm
 * - DNP (OD): distance from nose bridge to right pupil in mm
 * - DNP (OI): distance from nose bridge to left pupil in mm
 * - Segment height for FLATTOP: 3mm below lower eyelid
 * - Segment height for INVISIBLE: at lower eyelid
 * - Segment height for PROGRESSIVE: 0 (at pupil center)
 * - Segment height for OCUPACIONAL: 0 (at pupil center, same as PROGRESSIVE)
 */
class FaceMeasurementExtractor @Inject constructor() {

    companion object {
        private const val DEFAULT_PD_MM = 63.0
        private const val NOSE_BRIDGE = 1
        private const val LEFT_EYE = 133
        private const val LEFT_EYE_LOWER = 145
        private const val RIGHT_EYE = 362
        private const val RIGHT_EYE_LOWER = 159
        private const val FLATTOP_OFFSET_MM = 3.0
    }

    /**
     * Computes the millimeters-per-pixel scale from a known pupillary distance.
     *
     * @param landmarks 468 face landmarks (normalized 0–1)
     * @param bitmapWidth pixel width of the source image
     * @param bitmapHeight pixel height of the source image
     * @param knownPdMm the patient's PD in mm, or null to use default (63mm)
     * @return mm per pixel ratio
     */
    fun computeMmPerPixel(
        landmarks: List<NormalizedLandmark>,
        bitmapWidth: Int,
        bitmapHeight: Int,
        knownPdMm: Double?
    ): Float {
        val pdMm = knownPdMm ?: DEFAULT_PD_MM
        val leftEye = landmarks[LEFT_EYE]
        val rightEye = landmarks[RIGHT_EYE]
        val dxPx = (leftEye.x() - rightEye.x()) * bitmapWidth
        val dyPx = (leftEye.y() - rightEye.y()) * bitmapHeight
        val pupilDistancePx = sqrt(dxPx * dxPx + dyPx * dyPx)
        if (pupilDistancePx < 1f) return Float.MAX_VALUE
        return (pdMm / pupilDistancePx).toFloat()
    }

    /**
     * Extracts the interpupillary distance (DIP) in millimeters.
     *
     * Uses the Euclidean distance between pupil landmarks scaled by [mmPerPixel].
     * Since landmarks are normalized (0–1), the result is in a relative scale;
     * multiply by the image reference dimension to get absolute mm.
     */
    fun extractDIP(
        landmarks: List<NormalizedLandmark>,
        mmPerPixel: Float
    ): Double {
        val leftEye = landmarks[LEFT_EYE]
        val rightEye = landmarks[RIGHT_EYE]
        val dx = (leftEye.x() - rightEye.x()).toDouble()
        val dy = (leftEye.y() - rightEye.y()).toDouble()
        return sqrt(dx * dx + dy * dy) * mmPerPixel
    }

    /**
     * Extracts the naso-pupilar distances (DNP) for both eyes.
     *
     * @return Pair(OD, OI) where OD = distance from nose bridge to RIGHT pupil,
     *         OI = distance from nose bridge to LEFT pupil.
     */
    fun extractDNP(
        landmarks: List<NormalizedLandmark>,
        mmPerPixel: Float
    ): Pair<Double, Double> {
        val nose = landmarks[NOSE_BRIDGE]
        val rightEye = landmarks[RIGHT_EYE]
        val leftEye = landmarks[LEFT_EYE]

        val odDx = (nose.x() - rightEye.x()).toDouble()
        val odDy = (nose.y() - rightEye.y()).toDouble()
        val oiDx = (nose.x() - leftEye.x()).toDouble()
        val oiDy = (nose.y() - leftEye.y()).toDouble()

        val od = sqrt(odDx * odDx + odDy * odDy) * mmPerPixel
        val oi = sqrt(oiDx * oiDx + oiDy * oiDy) * mmPerPixel
        return Pair(od, oi)
    }

    /**
     * Extracts the vertical segment height based on lens type.
     *
     * Reference points:
     * - Eye center: average Y of both pupils
     * - Lower eyelid: average Y of right/left lower lid landmarks
     * - FLATTOP: eyelid → eyelid distance + 3mm
     * - INVISIBLE: pupil → lower eyelid distance
     * - PROGRESSIVE: 0mm (lens measurement at pupil center)
     * - OCUPACIONAL: 0mm (same as PROGRESSIVE)
     */
    fun extractSegmentHeight(
        landmarks: List<NormalizedLandmark>,
        mmPerPixel: Float,
        segmentType: SegmentType
    ): Double {
        val leftEye = landmarks[LEFT_EYE]
        val rightEye = landmarks[RIGHT_EYE]
        val avgEyeY = (leftEye.y() + rightEye.y()) / 2f

        return when (segmentType) {
            SegmentType.PROGRESSIVE, SegmentType.OCUPACIONAL -> 0.0
            SegmentType.INVISIBLE -> {
                val lowerRight = landmarks[RIGHT_EYE_LOWER]
                val lowerLeft = landmarks[LEFT_EYE_LOWER]
                val avgLowerY = (lowerRight.y() + lowerLeft.y()) / 2f
                (avgLowerY - avgEyeY).toDouble() * mmPerPixel
            }
            SegmentType.FLATTOP -> {
                val lowerRight = landmarks[RIGHT_EYE_LOWER]
                val lowerLeft = landmarks[LEFT_EYE_LOWER]
                val avgLowerY = (lowerRight.y() + lowerLeft.y()) / 2f
                (avgLowerY - avgEyeY).toDouble() * mmPerPixel + FLATTOP_OFFSET_MM
            }
        }
    }
}
