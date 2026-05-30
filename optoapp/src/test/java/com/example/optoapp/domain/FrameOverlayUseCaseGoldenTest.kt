package com.example.optoapp.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

/**
 * Golden image test for [FrameOverlayUseCase.compose].
 *
 * On first run, set [GENERATE_GOLDEN] to `true` to generate the golden
 * reference image. On subsequent runs, set it to `false` and the test
 * compares pixel-by-pixel against the golden, allowing <1% difference.
 *
 * Golden file location: `src/test/resources/golden-images/standard_overlay.png`
 */
@RunWith(RobolectricTestRunner::class)
class FrameOverlayUseCaseGoldenTest {

    companion object {
        /** Toggle to generate golden images on first run. */
        private const val GENERATE_GOLDEN = false

        private const val GOLDEN_DIR = "src/test/resources/golden-images"
        private const val GOLDEN_FILE = "standard_overlay.png"

        /** Maximum allowed pixel difference ratio (0.01 = 1%). */
        private const val DIFF_THRESHOLD = 0.01f
    }

    private val useCase = FrameOverlayUseCase()

    @Test
    fun `overlay matches golden image for standard face`() = runTest {
        // ── Test data ──────────────────────────────────────────────────────

        val faceWidth = 200
        val faceHeight = 300
        val faceBitmap = createTestFaceBitmap(faceWidth, faceHeight)

        val monturaBitmap = createTestMonturaBitmap(120, 40)

        val landmarks = createTestLandmarks()

        val config = FrameOverlayUseCase.OverlayConfig(
            faceBitmap = faceBitmap,
            monturaBitmap = monturaBitmap,
            landmarks = landmarks,
            mmPerPixel = 2.5f,
            monturaAnchoMm = 50f,
            monturaPuenteMm = 18f
        )

        val result = useCase.compose(config)

        // ── Golden file management ─────────────────────────────────────────

        val goldenDir = File(GOLDEN_DIR)
        val goldenFile = File(goldenDir, GOLDEN_FILE)

        if (GENERATE_GOLDEN) {
            goldenDir.mkdirs()
            FileOutputStream(goldenFile).use { out ->
                result.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            // Skip comparison when generating — golden didn't exist before
            assertTrue("Golden file created at ${goldenFile.absolutePath}", goldenFile.exists())
            return@runTest
        }

        // ── Pixel-by-pixel comparison ──────────────────────────────────────

        assertTrue(
            "Golden file not found at ${goldenFile.absolutePath}. Set GENERATE_GOLDEN = true first.",
            goldenFile.exists()
        )

        val golden = BitmapFactory.decodeFile(goldenFile.absolutePath)
        assertNotNull("Could not decode golden file", golden)

        assertEquals(
            "Golden image width mismatch", result.width, golden.width
        )
        assertEquals(
            "Golden image height mismatch", result.height, golden.height
        )

        val totalPixels = result.width * result.height
        var diffPixels = 0L

        for (x in 0 until result.width) {
            for (y in 0 until result.height) {
                if (result.getPixel(x, y) != golden.getPixel(x, y)) {
                    diffPixels++
                }
            }
        }

        val diffRatio = diffPixels.toFloat() / totalPixels.toFloat()

        assertTrue(
            "Image differs by ${"%.2f".format(diffRatio * 100)}% " +
                "(threshold: ${"%.1f".format(DIFF_THRESHOLD * 100)}%). " +
                "$diffPixels of $totalPixels pixels differ.",
            diffRatio < DIFF_THRESHOLD
        )
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    /** Creates a solid-color face bitmap with skin-like color. */
    private fun createTestFaceBitmap(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.argb(255, 220, 180, 140))
        // Add a darker oval for the face shape
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 200, 160, 120)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawOval(
            width * 0.1f, height * 0.05f,
            width * 0.9f, height * 0.85f,
            paint
        )
        // Draw eye dots for visual reference
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 50, 50, 50)
        }
        canvas.drawCircle(width * 0.35f, height * 0.30f, 3f, dotPaint) // left eye
        canvas.drawCircle(width * 0.65f, height * 0.30f, 3f, dotPaint) // right eye
        canvas.drawCircle(width * 0.50f, height * 0.42f, 2f, dotPaint) // nose tip
        return bmp
    }

    /**
     * Creates a transparent montura bitmap with a visible frame outline.
     * The frame is drawn as two lens rectangles connected by a bridge.
     */
    private fun createTestMonturaBitmap(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Start with transparent background
        bmp.eraseColor(Color.TRANSPARENT)

        val canvas = Canvas(bmp)
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 80, 80, 80) // semi-transparent dark gray
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 80, 80, 80) // very subtle fill
            style = Paint.Style.FILL
        }

        val bridgeWidth = (width * 0.12f).toInt()
        val lensWidth = (width - bridgeWidth) / 2
        val padding = 2

        // Left lens
        canvas.drawRect(
            padding.toFloat(), padding.toFloat(),
            lensWidth.toFloat(), (height - padding).toFloat(),
            framePaint
        )
        canvas.drawRect(
            padding.toFloat(), padding.toFloat(),
            lensWidth.toFloat(), (height - padding).toFloat(),
            fillPaint
        )

        // Right lens
        val rightStart = lensWidth + bridgeWidth
        canvas.drawRect(
            rightStart.toFloat(), padding.toFloat(),
            (width - padding).toFloat(), (height - padding).toFloat(),
            framePaint
        )
        canvas.drawRect(
            rightStart.toFloat(), padding.toFloat(),
            (width - padding).toFloat(), (height - padding).toFloat(),
            fillPaint
        )

        return bmp
    }

    /**
     * Builds 468 synthetic landmarks with known positions for a centered,
     * front-facing face in a 200x300 image.
     */
    private fun createTestLandmarks(): List<NormalizedLandmark> {
        val list = mutableListOf<NormalizedLandmark>()
        for (i in 0 until 468) {
            val (x, y) = when (i) {
                1 -> 0.50f to 0.42f     // nose bridge
                10 -> 0.50f to 0.15f    // forehead top
                61 -> 0.42f to 0.55f    // left mouth corner
                133 -> 0.35f to 0.30f   // left eye center
                152 -> 0.50f to 0.75f   // chin
                159 -> 0.36f to 0.32f   // right lower eyelid
                175 -> 0.28f to 0.30f   // right eye outer
                291 -> 0.58f to 0.55f   // right mouth corner
                362 -> 0.65f to 0.30f   // right eye center
                395 -> 0.42f to 0.32f   // left eye outer
                400 -> 0.64f to 0.32f   // left lower eyelid
                454 -> 0.50f to 0.18f   // forehead center
                else -> 0.50f to 0.50f
            }
            list.add(NormalizedLandmark.create(x, y, 0f))
        }
        return list
    }
}
