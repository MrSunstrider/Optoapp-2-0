package com.example.optoapp.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import javax.inject.Inject

/**
 * Composes a transparent montura PNG overlay onto a detected face bitmap.
 *
 * Positioning:
 * - Horizontal center: nose bridge (landmark index 1)
 * - Vertical alignment: eye landmarks (133, 362) for natural rotation
 * - Scale: monturaAnchoMm × mmPerPixel → target pixel width
 *
 * The user can fine-tune position, rotation, and scale via [OverlayConfig.adjustmentX],
 * [OverlayConfig.adjustmentY], and [OverlayConfig.adjustmentScale].
 */
class FrameOverlayUseCase @Inject constructor() {

    data class OverlayConfig(
        val faceBitmap: Bitmap,
        val monturaBitmap: Bitmap,
        val landmarks: List<NormalizedLandmark>,
        val mmPerPixel: Float,
        val monturaAnchoMm: Float,
        val monturaPuenteMm: Float,
        val adjustmentX: Float = 0f,
        val adjustmentY: Float = 0f,
        val adjustmentScale: Float = 1f
    )

    private companion object {
        const val NOSE_BRIDGE_IDX = 1
        const val LEFT_EYE_IDX = 133
        const val RIGHT_EYE_IDX = 362
    }

    /**
     * Composes the montura overlay onto the face image.
     *
     * @return A new ARGB_8888 bitmap with the overlay drawn.
     */
    suspend fun compose(config: OverlayConfig): Bitmap = withContext(Dispatchers.IO) {
        val output = config.faceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (config.monturaBitmap.width == 0 || config.monturaBitmap.height == 0) {
            return@withContext output
        }
        val canvas = Canvas(output)
        val matrix = computeTransformMatrix(config)
        canvas.drawBitmap(config.monturaBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
        output
    }

    /**
     * Builds the affine transform that scales, rotates, and positions the
     * montura overlay centered on the nose bridge.
     *
     * Transform order (applied to montura coordinates):
     * 1. Translate montura center to origin
     * 2. Scale by the computed factor
     * 3. Rotate by the inter-eye angle
     * 4. Translate to the nose bridge position + user adjustments
     */
     internal fun computeTransformMatrix(config: OverlayConfig): Matrix {
        val srcW = config.monturaBitmap.width.toFloat()
        val srcH = config.monturaBitmap.height.toFloat()

        val noseBridge = config.landmarks[NOSE_BRIDGE_IDX]
        val leftEye = config.landmarks[LEFT_EYE_IDX]
        val rightEye = config.landmarks[RIGHT_EYE_IDX]

        val faceW = config.faceBitmap.width.toFloat()
        val faceH = config.faceBitmap.height.toFloat()

        // Target pixel width for the montura from real-world mm
        val targetWidthPx = config.monturaAnchoMm * config.mmPerPixel
        val scale = if (srcW > 0f) {
            (targetWidthPx / srcW) * config.adjustmentScale
        } else {
            1f
        }

        // Rotation angle from the line between the eyes
        val dx = (rightEye.x() - leftEye.x()).toDouble()
        val dy = (rightEye.y() - leftEye.y()).toDouble()
        val angleDeg = Math.toDegrees(atan2(dy, dx)).toFloat()

        // Nose bridge in pixel coordinates
        val centerX = noseBridge.x() * faceW + config.adjustmentX
        val centerY = noseBridge.y() * faceH + config.adjustmentY

        return Matrix().apply {
            // Transform order (applied to montura coordinates top-to-bottom):
            // 1. Move montura centre to the origin
            // 2. Scale around the origin
            // 3. Rotate around the origin
            // 4. Move to the nose bridge position on the face
            //
            // `post*` prepends to the left so the first post* call is the
            // innermost transform (applied first to each point).
            postTranslate(-srcW / 2f, -srcH / 2f)
            postScale(scale, scale)
            postRotate(angleDeg)
            postTranslate(centerX, centerY)
        }
    }
}
