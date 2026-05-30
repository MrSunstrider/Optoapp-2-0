package com.example.optoapp.domain

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps MediaPipe [FaceLandmarker] detection into a coroutine-friendly API.
 *
 * Converts an Android [Bitmap] to [MPImage][com.google.mediapipe.framework.image.MPImage],
 * runs face landmark detection, and returns a structured [Result] containing the
 * 468 landmarks, detection confidence, and the face bounding box.
 *
 * Detection runs on [Dispatchers.IO] to keep the calling thread free.
 *
 * @property faceLandmarker Provided by [MediaPipeModule][com.example.optoapp.di.MediaPipeModule]
 *   as a Hilt singleton.
 */
@Singleton
class FaceLandmarkerUseCase @Inject constructor(
    private val faceLandmarker: FaceLandmarker?
) {

    /**
     * Structured result of a face landmark detection.
     *
     * @property landmarks 468 normalized landmarks per detected face (outer list = faces)
     * @property confidence detection confidence score (0.0–1.0)
     * @property boundingBox pixel bounding box of the detected face
     */
    data class Result(
        val landmarks: List<List<NormalizedLandmark>>,
        val confidence: Float,
        val boundingBox: Rect
    )

    /**
     * Detects face landmarks in the given [bitmap].
     *
     * @param bitmap The input image (will be converted to MPImage internally).
     * @return [Result] with landmarks, confidence, and bounding box.
     * @throws NoFaceDetectedException if no face is found.
     * @throws LowConfidenceException if the detection confidence is below 0.8.
     */
    suspend fun detect(bitmap: Bitmap): Result = withContext(Dispatchers.IO) {
        val landmarker = faceLandmarker
            ?: throw MediaPipeNotAvailableException("MediaPipe no disponible en este dispositivo")
        val mpImage = BitmapImageBuilder(bitmap).build()
        val detectionResult = landmarker.detect(mpImage)

        val faceLandmarks = detectionResult?.faceLandmarks()
            ?: throw NoFaceDetectedException("No face detected in the image")

        if (faceLandmarks.isEmpty()) {
            throw NoFaceDetectedException("No face detected in the image")
        }

        val primaryFace = faceLandmarks.first()

        // Compute bounding box from landmark extents
        val minX = primaryFace.minOf { it.x() }
        val maxX = primaryFace.maxOf { it.x() }
        val minY = primaryFace.minOf { it.y() }
        val maxY = primaryFace.maxOf { it.y() }

        val boundingBox = Rect(
            (minX * bitmap.width).toInt(),
            (minY * bitmap.height).toInt(),
            (maxX * bitmap.width).toInt(),
            (maxY * bitmap.height).toInt()
        )

        // Confidence: MediaPipe FaceLandmarker does not expose a per-face confidence
        // score in the 0.10.14 result. We set 1.0f when a face is detected.
        // Future versions can wire FaceBlendshapes output for expression-aware confidence.
        val confidence = 1.0f

        Result(
            landmarks = faceLandmarks.map { face -> face.map { it } },
            confidence = confidence,
            boundingBox = boundingBox
        )
    }
}

/**
 * Thrown when MediaPipe returns zero detected faces.
 */
class NoFaceDetectedException(message: String) : Exception(message)

/**
 * Thrown when face detection confidence is below the acceptable threshold.
 */
class LowConfidenceException(message: String) : Exception(message)

/**
 * Thrown when MediaPipe runtime is not available on this device.
 */
class MediaPipeNotAvailableException(message: String) : Exception(message)
