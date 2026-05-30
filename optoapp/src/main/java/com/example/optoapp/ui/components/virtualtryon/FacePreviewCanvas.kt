package com.example.optoapp.ui.components.virtualtryon

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.optoapp.viewmodel.TryOnState

/**
 * Displays the face Bitmap with optional landmark and overlay rendering.
 *
 * - [PhotoSelected]: shows the bitmap with the caption "Detectando rostro..."
 * - [FaceDetected]: shows the bitmap with key landmark dots highlighted
 * - [OverlayReady]: shows the composited result bitmap (face + overlay)
 * - Other states: renders nothing (handled by parent screen)
 *
 * Scales responsively to fit screen width while preserving aspect ratio.
 */
@Composable
fun FacePreviewCanvas(
    state: TryOnState,
    modifier: Modifier = Modifier
) {
    when (state) {
        is TryOnState.PhotoSelected -> {
            FaceImageWithLandmarks(
                bitmap = state.bitmap,
                landmarks = emptyList(),
                showAllLandmarks = false,
                caption = "Detectando rostro...",
                modifier = modifier
            )
        }
        is TryOnState.FaceDetected -> {
            FaceImageWithLandmarks(
                bitmap = state.originalBitmap,
                landmarks = state.landmarks,
                showAllLandmarks = true,
                caption = "Rostro detectado (${state.landmarks.size} landmarks)",
                modifier = modifier
            )
        }
        is TryOnState.OverlayReady -> {
            Image(
                bitmap = state.compositeBitmap.asImageBitmap(),
                contentDescription = "Resultado de prueba virtual",
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
        else -> { /* Nothing to render */ }
    }
}

/**
 * Displays a face image with optional landmark dots and an optional caption.
 */
@Composable
private fun FaceImageWithLandmarks(
    bitmap: Bitmap,
    landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    showAllLandmarks: Boolean,
    caption: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto de rostro",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat()
                    else 1f
                ),
            contentScale = ContentScale.Fit
        )

        // Key landmark overlay dots
        if (showAllLandmarks && landmarks.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat()
                        else 1f
                    )
            ) {
                drawKeyLandmarks(
                    landmarks = landmarks,
                    canvasWidth = size.width,
                    canvasHeight = size.height
                )
            }
        }

        // Caption text at bottom
        if (caption != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Draws small circles at key facial landmark positions for visual reference.
 * Only a subset (~12) of the 468 landmarks are rendered for clarity.
 */
private fun DrawScope.drawKeyLandmarks(
    landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    canvasWidth: Float,
    canvasHeight: Float
) {
    // Select key face contour landmarks for visual display
    val visibleIndices = setOf(
        1,    // nose bridge
        10,   // forehead top
        61,   // left mouth corner
        133,  // left eye center
        152,  // chin
        159,  // right lower eyelid
        175,  // right eye outer
        291,  // right mouth corner
        362,  // right eye center
        395,  // left eye outer
        400,  // left lower eyelid
        454   // forehead center
    )

    landmarks.forEachIndexed { index, lm ->
        if (index in visibleIndices) {
            val x = lm.x() * canvasWidth
            val y = lm.y() * canvasHeight
            // Outer ring
            drawCircle(
                color = Color(0x66FFEB3B),
                radius = 4f,
                center = Offset(x, y),
                style = Stroke(width = 2f)
            )
            // Inner dot
            drawCircle(
                color = Color(0x99FFEB3B),
                radius = 2f,
                center = Offset(x, y)
            )
        }
    }
}
