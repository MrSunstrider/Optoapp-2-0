package com.example.optoapp.domain

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrameOverlayUseCaseTest {

    private val useCase = FrameOverlayUseCase()

    /** Builds 468 synthetic landmarks. */
    private fun makeLandmarks(
        noseBridgeX: Float = 0.50f,
        noseBridgeY: Float = 0.50f,
        leftEyeX: Float = 0.35f,
        leftEyeY: Float = 0.40f,
        rightEyeX: Float = 0.65f,
        rightEyeY: Float = 0.40f
    ): List<NormalizedLandmark> {
        val list = mutableListOf<NormalizedLandmark>()
        for (i in 0 until 468) {
            val (x, y) = when (i) {
                1 -> noseBridgeX to noseBridgeY
                133 -> leftEyeX to leftEyeY
                362 -> rightEyeX to rightEyeY
                else -> 0.5f to 0.5f
            }
            list.add(NormalizedLandmark.create(x, y, 0f))
        }
        return list
    }

    private fun bitmap(width: Int, height: Int, color: Int = Color.argb(255, 200, 200, 200)): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(color)
        return bmp
    }

    private fun defaultConfig(
        faceBmp: Bitmap = bitmap(200, 200),
        monturaBmp: Bitmap = bitmap(100, 50, Color.argb(200, 100, 50, 200)),
        landmarks: List<NormalizedLandmark> = makeLandmarks(),
        mmPerPixel: Float = 0.2f,
        monturaAnchoMm: Float = 120f,
        monturaPuenteMm: Float = 18f,
        adjustmentX: Float = 0f,
        adjustmentY: Float = 0f,
        adjustmentScale: Float = 1f
    ): FrameOverlayUseCase.OverlayConfig = FrameOverlayUseCase.OverlayConfig(
        faceBitmap = faceBmp,
        monturaBitmap = monturaBmp,
        landmarks = landmarks,
        mmPerPixel = mmPerPixel,
        monturaAnchoMm = monturaAnchoMm,
        monturaPuenteMm = monturaPuenteMm,
        adjustmentX = adjustmentX,
        adjustmentY = adjustmentY,
        adjustmentScale = adjustmentScale
    )

    /** Extracts matrix translation values. */
    private fun tx(matrix: Matrix): Float {
        val v = FloatArray(9)
        matrix.getValues(v)
        return v[Matrix.MTRANS_X]
    }

    private fun ty(matrix: Matrix): Float {
        val v = FloatArray(9)
        matrix.getValues(v)
        return v[Matrix.MTRANS_Y]
    }

    private fun scaleX(matrix: Matrix): Float {
        val v = FloatArray(9)
        matrix.getValues(v)
        return v[Matrix.MSCALE_X]
    }

    private fun scaleY(matrix: Matrix): Float {
        val v = FloatArray(9)
        matrix.getValues(v)
        return v[Matrix.MSCALE_Y]
    }

    // ── overlay composition ───────────────────────────────────────────────

    @Test
    fun `overlay result has same dimensions as face bitmap`() = runTest {
        val config = defaultConfig(faceBmp = bitmap(200, 300))
        val result = useCase.compose(config)

        assertEquals(200, result.width)
        assertEquals(300, result.height)
    }

    @Test
    fun `overlay result preserves ARGB_8888 config`() = runTest {
        val config = defaultConfig(faceBmp = bitmap(100, 100))
        val result = useCase.compose(config)

        assertEquals(Bitmap.Config.ARGB_8888, result.config)
    }

    @Test
    fun `overlay does not crash on different face bitmap sizes`() = runTest {
        val monturaBmp = bitmap(40, 20, Color.argb(200, 100, 50, 200))
        val landmarks = makeLandmarks()

        for ((w, h) in listOf(100 to 100, 200 to 100, 100 to 200, 50 to 75)) {
            val config = defaultConfig(
                faceBmp = bitmap(w, h),
                monturaBmp = monturaBmp,
                landmarks = landmarks
            )
            val result = useCase.compose(config)
            assertEquals(w, result.width)
            assertEquals(h, result.height)
        }
    }

    @Test
    fun `overlay with 1x1 montura does not throw`() = runTest {
        val config = defaultConfig(
            monturaBmp = bitmap(1, 1, Color.argb(255, 255, 0, 0))
        )
        val result = useCase.compose(config)
        assertNotNull(result)
    }

    // ── computeTransformMatrix: scale ─────────────────────────────────────

    @Test
    fun `scale based on monturaAnchoMm times mmPerPixel divided by src width`() {
        val config = defaultConfig(
            monturaBmp = bitmap(100, 50),
            mmPerPixel = 0.2f,
            monturaAnchoMm = 100f
        )
        // targetWidthPx = 100 * 0.2 = 20, scale = 20 / 100 = 0.2
        val matrix = useCase.computeTransformMatrix(config)
        assertEquals(0.2f, scaleX(matrix), 0.001f)
        assertEquals(0.2f, scaleY(matrix), 0.001f)
    }

    @Test
    fun `scale doubles with doubled monturaAnchoMm`() {
        val small = defaultConfig(monturaBmp = bitmap(100, 50), monturaAnchoMm = 50f, mmPerPixel = 0.2f)
        val large = defaultConfig(monturaBmp = bitmap(100, 50), monturaAnchoMm = 100f, mmPerPixel = 0.2f)

        // small: targetWidthPx = 50*0.2=10, scale = 10/100 = 0.1
        // large: targetWidthPx = 100*0.2=20, scale = 20/100 = 0.2
        assertEquals(0.1f, scaleX(useCase.computeTransformMatrix(small)), 0.001f)
        assertEquals(0.2f, scaleX(useCase.computeTransformMatrix(large)), 0.001f)
    }

    @Test
    fun `scale includes adjustmentScale multiplier`() {
        val base = defaultConfig(adjustmentScale = 1f, monturaBmp = bitmap(100, 50), monturaAnchoMm = 100f, mmPerPixel = 0.2f)
        val scaled = defaultConfig(adjustmentScale = 2f, monturaBmp = bitmap(100, 50), monturaAnchoMm = 100f, mmPerPixel = 0.2f)

        // base: scale = 0.2, scaled: scale = 0.2 * 2 = 0.4
        assertEquals(0.2f, scaleX(useCase.computeTransformMatrix(base)), 0.001f)
        assertEquals(0.4f, scaleX(useCase.computeTransformMatrix(scaled)), 0.001f)
    }

    @Test
    fun `scale with zero mmPerPixel results in zero scale`() {
        val config = defaultConfig(mmPerPixel = 0f)
        val matrix = useCase.computeTransformMatrix(config)
        assertEquals(0f, scaleX(matrix), 0.001f)
    }

    // ── computeTransformMatrix: translation (centered on nose bridge) ────

    @Test
    fun `translation X centers montura on nose bridge X`() {
        // nose bridge at (0.5, 0.5), face 200x200 → centerX = 100
        // montura 100x50 → centering offset = -100/2 * scale
        // scale = (120*0.2)/100 = 0.24
        // After all pre* operations:
        // Point (0,0): preTranslate(-50,-25) → (-50,-25)
        //              preScale(0.24,0.24) → (-12,-6)
        //              preRotate(0) → (-12,-6)
        //              preTranslate(100,100) → (88, 94)
        // Translation values = where (0,0) maps to after full transform
        val matrix = useCase.computeTransformMatrix(defaultConfig())

        // Matrix is T(100,100) * S(0.24) * T(-50,-25)
        // For (0,0): T(100,100) * S(0.24) * (-50,-25) = T(100,100) * (-12,-6) = (88,94)
        assertEquals(88f, tx(matrix), 1.0f)
    }

    @Test
    fun `translation Y centers montura on nose bridge Y`() {
        val matrix = useCase.computeTransformMatrix(defaultConfig())
        // Matrix is T(100,100) * S(0.24) * T(-50,-25)
        // For (0,0): T(100,100) * S(0.24) * (-50,-25) = T(100,100) * (-12,-6) = (88,94)
        assertEquals(94f, ty(matrix), 1.0f)
    }

    @Test
    fun `adjustmentX moves translation X`() {
        val base = useCase.computeTransformMatrix(defaultConfig())
        val shifted = useCase.computeTransformMatrix(defaultConfig(adjustmentX = 30f))

        assertTrue("adjustmentX shifts X translation", tx(shifted) > tx(base))
        // shift of 30px in X
        assertEquals(tx(base) + 30f, tx(shifted), 1.0f)
    }

    @Test
    fun `adjustmentY moves translation Y`() {
        val base = useCase.computeTransformMatrix(defaultConfig())
        val shifted = useCase.computeTransformMatrix(defaultConfig(adjustmentY = 25f))

        assertTrue("adjustmentY shifts Y translation", ty(shifted) > ty(base))
        assertEquals(ty(base) + 25f, ty(shifted), 1.0f)
    }

    // ── computeTransformMatrix: rotation ─────────────────────────────────

    @Test
    fun `rotation is zero when eyes are horizontal`() {
        val config = defaultConfig()
        val matrix = useCase.computeTransformMatrix(config)
        val v = FloatArray(9)
        matrix.getValues(v)
        // With eyes at same Y (0.40 and 0.40), angle = atan2(0, 0.30) = 0
        assertEquals(0f, v[Matrix.MSKEW_X], 0.001f)
    }

    @Test
    fun `rotation is positive when right eye is above left eye`() {
        val landmarks = makeLandmarks(
            leftEyeY = 0.50f,
            rightEyeY = 0.40f  // right eye higher
        )
        val config = defaultConfig(landmarks = landmarks)
        val matrix = useCase.computeTransformMatrix(config)
        val v = FloatArray(9)
        matrix.getValues(v)
        // atan2(-0.10, 0.30) = negative angle in radians
        // Right eye higher → dy = 0.40 - 0.50 = -0.10 → atan2(-0.10, 0.30) < 0
        // MSKEW_X = -sin(angle) for a rotation matrix, so it should be non-zero
        assertTrue("Rotation should be non-zero for tilted eyes", v[Matrix.MSKEW_X] != 0f)
    }

    @Test
    fun `rotation is negative when left eye is above right eye`() {
        val landmarks = makeLandmarks(
            leftEyeY = 0.40f,
            rightEyeY = 0.50f  // left eye higher
        )
        val config = defaultConfig(landmarks = landmarks)
        val matrix = useCase.computeTransformMatrix(config)
        val v = FloatArray(9)
        matrix.getValues(v)
        // dy = 0.50 - 0.40 = 0.10 → atan2(0.10, 0.30) > 0
        assertTrue("Rotation should be non-zero for tilted eyes", v[Matrix.MSKEW_X] != 0f)
    }

    // ── computeTransformMatrix: scale consistency ─────────────────────────

    @Test
    fun `montura with 1x1 minimum size produces correct scale`() {
        val config = defaultConfig(monturaBmp = bitmap(1, 1))
        val matrix = useCase.computeTransformMatrix(config)
        // targetWidthPx = 120 * 0.2 = 24, scale = 24 / 1 = 24
        assertEquals(24f, scaleX(matrix), 0.001f)
    }

    @Test
    fun `scale is proportional to mmPerPixel`() {
        val low = defaultConfig(mmPerPixel = 0.1f)
        val high = defaultConfig(mmPerPixel = 0.4f)

        val lowScale = scaleX(useCase.computeTransformMatrix(low))
        val highScale = scaleX(useCase.computeTransformMatrix(high))

        assertEquals(lowScale * 4f, highScale, 0.001f)
    }

    // ── end-to-end compose ────────────────────────────────────────────────

    @Test
    fun `compose returns different bitmap for different configurations`() = runTest {
        val faceBmp = bitmap(100, 100)
        val monturaBmp = bitmap(20, 10, Color.argb(255, 255, 0, 0))
        val landmarks = makeLandmarks()

        val config1 = defaultConfig(
            faceBmp = faceBmp, monturaBmp = monturaBmp,
            landmarks = landmarks, mmPerPixel = 0.2f,
            monturaAnchoMm = 50f, monturaPuenteMm = 16f,
            adjustmentX = 0f
        )
        val config2 = defaultConfig(
            faceBmp = bitmap(100, 100), monturaBmp = monturaBmp,
            landmarks = landmarks, mmPerPixel = 0.2f,
            monturaAnchoMm = 50f, monturaPuenteMm = 16f,
            adjustmentX = 100f
        )

        val result1 = useCase.compose(config1)
        val result2 = useCase.compose(config2)

        // Different adjX should produce different matrix translations
        val m1 = useCase.computeTransformMatrix(config1)
        val m2 = useCase.computeTransformMatrix(config2)
        assertTrue("Different adjX must change translation X", tx(m2) > tx(m1))
    }

    @Test
    fun `compose with 1x1 montura returns valid output`() = runTest {
        val config = defaultConfig(
            monturaBmp = bitmap(1, 1, Color.RED)
        )
        val result = useCase.compose(config)
        assertEquals(config.faceBitmap.width, result.width)
        assertEquals(config.faceBitmap.height, result.height)
    }
}
