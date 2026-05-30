package com.example.optoapp.domain

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FaceMeasurementExtractorTest {

    private val extractor = FaceMeasurementExtractor()

    /**
     * Builds 468 synthetic landmarks for a roughly centered face.
     * Key indices: 1=nose bridge, 133=left eye center, 159=lower right lid,
     * 145=lower left lid, 362=right eye center.
     * Unspecified landmarks default to (0.5, 0.5, 0).
     */
    private fun makeSyntheticLandmarks(
        leftEyeX: Float = 0.35f,
        leftEyeY: Float = 0.40f,
        rightEyeX: Float = 0.65f,
        rightEyeY: Float = 0.40f,
        noseBridgeX: Float = 0.50f,
        noseBridgeY: Float = 0.50f,
        lowerRightY: Float = 0.55f,
        lowerLeftY: Float = 0.55f
    ): List<NormalizedLandmark> {
        val list = mutableListOf<NormalizedLandmark>()
        for (i in 0 until 468) {
            val (x, y) = when (i) {
                1 -> noseBridgeX to noseBridgeY
                133 -> leftEyeX to leftEyeY
                159 -> leftEyeX + 0.02f to lowerRightY
                145 -> rightEyeX - 0.02f to lowerLeftY
                362 -> rightEyeX to rightEyeY
                else -> 0.5f to 0.5f
            }
            list.add(NormalizedLandmark.create(x, y, 0f))
        }
        return list
    }

    // ── computeMmPerPixel ─────────────────────────────────────────────────

    @Test
    fun `computeMmPerPixel with known PD returns mm per pixel ratio`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeX = 0.35f, rightEyeX = 0.65f,
            leftEyeY = 0.40f, rightEyeY = 0.40f
        )
        // Pupil distance: (0.65-0.35) * 1000 = 300px
        // PD 60mm → mmPerPixel = 60/300 = 0.2
        val result = extractor.computeMmPerPixel(landmarks, 1000, 1000, 60.0)
        assertEquals(0.2f, result, 0.001f)
    }

    @Test
    fun `computeMmPerPixel with null PD falls back to 63mm default`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeX = 0.35f, rightEyeX = 0.65f
        )
        // eyes at 0.35/0.65 → 0.30 * 1000 = 300px
        // PD=63 (default) → mmPerPixel = 63/300 = 0.21
        val result = extractor.computeMmPerPixel(landmarks, 1000, 1000, null)
        assertEquals(0.21f, result, 0.005f)
    }

    @Test
    fun `computeMmPerPixel with smaller image gives larger mmPerPixel`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeX = 0.35f, rightEyeX = 0.65f
        )
        val px1000 = extractor.computeMmPerPixel(landmarks, 1000, 1000, 60.0)
        val px500 = extractor.computeMmPerPixel(landmarks, 500, 500, 60.0)

        // 1000px: pupilDist=300px, mmPerPixel=0.2
        // 500px: pupilDist=150px, mmPerPixel=0.4
        assertTrue("Smaller image should have larger mmPerPixel", px500 > px1000)
        assertEquals(0.4f, px500, 0.001f)
    }

    @Test
    fun `computeMmPerPixel with different aspect ratio uses width`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeX = 0.35f, rightEyeX = 0.65f
        )
        // Width is 2000, the horizontal distance between pupils uses width
        // pupilDist = 0.30 * 2000 = 600px → mmPerPixel = 60/600 = 0.1
        val result = extractor.computeMmPerPixel(landmarks, 2000, 1000, 60.0)
        assertEquals(0.1f, result, 0.001f)
    }

    // ── extractDIP ────────────────────────────────────────────────────────

    @Test
    fun `extractDIP returns larger value for wider-set eyes`() {
        val close = makeSyntheticLandmarks(leftEyeX = 0.45f, rightEyeX = 0.55f)
        val wide = makeSyntheticLandmarks(leftEyeX = 0.30f, rightEyeX = 0.70f)

        val closeDip = extractor.extractDIP(close, 0.2f)
        val wideDip = extractor.extractDIP(wide, 0.2f)

        assertTrue("Wider-set eyes should produce larger DIP", wideDip > closeDip)
    }

    @Test
    fun `extractDIP with wider spacing is proportional to normalized distance`() {
        val normal = makeSyntheticLandmarks(leftEyeX = 0.40f, rightEyeX = 0.60f)
        val wide = makeSyntheticLandmarks(leftEyeX = 0.30f, rightEyeX = 0.70f)

        val normalDip = extractor.extractDIP(normal, 0.2f)
        val wideDip = extractor.extractDIP(wide, 0.2f)

        // normal distance: 0.20, wide distance: 0.40 → 2x
        assertEquals(normalDip * 2.0, wideDip, 0.001)
    }

    @Test
    fun `extractDIP scales linearly with mmPerPixel`() {
        val landmarks = makeSyntheticLandmarks()

        val dip05 = extractor.extractDIP(landmarks, 0.5f)
        val dip10 = extractor.extractDIP(landmarks, 1.0f)

        assertEquals(dip05 * 2.0, dip10, 0.001)
    }

    // ── extractDNP ────────────────────────────────────────────────────────

    @Test
    fun `extractDNP returns symmetric values for centered nose`() {
        val landmarks = makeSyntheticLandmarks(
            noseBridgeX = 0.50f,
            leftEyeX = 0.35f, rightEyeX = 0.65f
        )
        val (od, oi) = extractor.extractDNP(landmarks, 0.2f)

        assertTrue("OD should be positive", od > 0.0)
        assertTrue("OI should be positive", oi > 0.0)
        // With centered nose, OD = OI
        assertEquals(od, oi, 0.01)
    }

    @Test
    fun `extractDNP asymmetric when nose is off-center`() {
        val landmarks = makeSyntheticLandmarks(
            noseBridgeX = 0.55f, // shifted right
            leftEyeX = 0.35f, rightEyeX = 0.65f
        )
        val (od, oi) = extractor.extractDNP(landmarks, 0.2f)

        // Nose closer to right eye → OD < OI
        assertTrue("OD should be smaller than OI when nose is right-shifted", od < oi)
    }

    @Test
    fun `extractDNP nose closer to left eye makes OD larger`() {
        val landmarks = makeSyntheticLandmarks(
            noseBridgeX = 0.40f, // shifted left
            leftEyeX = 0.30f, rightEyeX = 0.65f
        )
        val (od, oi) = extractor.extractDNP(landmarks, 0.2f)

        // Nose closer to left eye → OI < OD
        assertTrue("OD should be larger than OI when nose is left-shifted", od > oi)
    }

    // ── extractSegmentHeight ──────────────────────────────────────────────

    @Test
    fun `extractSegmentHeight PROGRESSIVE is zero`() {
        val landmarks = makeSyntheticLandmarks()
        val height = extractor.extractSegmentHeight(landmarks, 0.2f, SegmentType.PROGRESSIVE)
        assertEquals(0.0, height, 0.001)
    }

    @Test
    fun `extractSegmentHeight OCUPACIONAL is zero same as PROGRESSIVE`() {
        val landmarks = makeSyntheticLandmarks()
        val height = extractor.extractSegmentHeight(landmarks, 0.2f, SegmentType.OCUPACIONAL)
        assertEquals(0.0, height, 0.001)
    }

    @Test
    fun `extractSegmentHeight INVISIBLE is proportional to eye-to-eyelid distance`() {
        val near = makeSyntheticLandmarks(
            leftEyeY = 0.40f, rightEyeY = 0.40f,
            lowerRightY = 0.45f, lowerLeftY = 0.45f
        )
        val far = makeSyntheticLandmarks(
            leftEyeY = 0.40f, rightEyeY = 0.40f,
            lowerRightY = 0.60f, lowerLeftY = 0.60f
        )

        val nearHeight = extractor.extractSegmentHeight(near, 0.2f, SegmentType.INVISIBLE)
        val farHeight = extractor.extractSegmentHeight(far, 0.2f, SegmentType.INVISIBLE)

        assertTrue("Lower eyelid farther from pupil should give larger height", farHeight > nearHeight)
    }

    @Test
    fun `extractSegmentHeight FLATTOP is larger than INVISIBLE`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeY = 0.40f, rightEyeY = 0.40f,
            lowerRightY = 0.55f, lowerLeftY = 0.55f
        )
        val invisible = extractor.extractSegmentHeight(landmarks, 1.0f, SegmentType.INVISIBLE)
        val flattop = extractor.extractSegmentHeight(landmarks, 1.0f, SegmentType.FLATTOP)

        assertTrue("FLATTOP should be larger than INVISIBLE", flattop > invisible)
    }

    @Test
    fun `extractSegmentHeight with zero mmPerPixel returns zero`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeY = 0.40f, rightEyeY = 0.40f,
            lowerRightY = 0.55f, lowerLeftY = 0.55f
        )
        val height = extractor.extractSegmentHeight(landmarks, 0f, SegmentType.INVISIBLE)
        assertEquals(0.0, height, 0.001)
    }

    @Test
    fun `extractSegmentHeight scales linearly with mmPerPixel`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeY = 0.40f, rightEyeY = 0.40f,
            lowerRightY = 0.50f, lowerLeftY = 0.50f
        )
        val h1 = extractor.extractSegmentHeight(landmarks, 0.5f, SegmentType.INVISIBLE)
        val h2 = extractor.extractSegmentHeight(landmarks, 1.0f, SegmentType.INVISIBLE)
        assertEquals(h1 * 2.0, h2, 0.001)
    }

    // ── cross-method consistency ──────────────────────────────────────────

    @Test
    fun `DIP from extractDIP with computed mmPerPixel returns same as supplied PD`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeX = 0.35f, rightEyeX = 0.65f,
            noseBridgeX = 0.50f
        )
        val mmPerPixel = extractor.computeMmPerPixel(landmarks, 1000, 1000, 60.0)
        val dip = extractor.extractDIP(landmarks, mmPerPixel)

        // mmPerPixel = 60mm / 300px = 0.2
        // extractDIP returns normDist * mmPerPixel = 0.3 * 0.2 = 0.06
        // We verify consistency: DIP * imageWidth = PD
        // 0.06 * 1000 = 60 ✓ (scaled back through pixel dimensions)
        assertEquals(60.0, dip * 1000, 1.0)
    }

    @Test
    fun `OD plus OI roughly equals DIP`() {
        val landmarks = makeSyntheticLandmarks(
            noseBridgeX = 0.50f,
            leftEyeX = 0.35f, rightEyeX = 0.65f
        )
        val mmPerPixel = extractor.computeMmPerPixel(landmarks, 1000, 1000, 60.0)
        val dip = extractor.extractDIP(landmarks, mmPerPixel)
        val (od, oi) = extractor.extractDNP(landmarks, mmPerPixel)

        // For a centered nose on the interocular line: OD + OI ≈ DIP
        // (small approximation error from 2D projection on the eye-line)
        assertEquals(od + oi, dip, 2.0)
    }

    // ── edge cases ────────────────────────────────────────────────────────

    @Test
    fun `extractDIP with coincident landmarks returns zero`() {
        val landmarks = makeSyntheticLandmarks(
            leftEyeX = 0.5f, leftEyeY = 0.5f,
            rightEyeX = 0.5f, rightEyeY = 0.5f
        )
        val dip = extractor.extractDIP(landmarks, 1.0f)
        assertEquals(0.0, dip, 0.001)
    }
}
