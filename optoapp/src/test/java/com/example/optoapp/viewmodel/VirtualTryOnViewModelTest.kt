package com.example.optoapp.viewmodel

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.optoapp.data.EvaluacionClinica
import com.example.optoapp.data.Montura
import com.example.optoapp.data.PacienteRepository
import com.example.optoapp.data.montura.MonturaInventoryCoordinator
import com.example.optoapp.domain.FaceLandmarkerUseCase
import com.example.optoapp.domain.FaceMeasurementExtractor
import com.example.optoapp.domain.FrameOverlayUseCase
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * State machine transition tests for [VirtualTryOnViewModel].
 *
 * Verifies [TryOnState] sealed class contracts, [FaceMeasurements] data,
 * and the ViewModel's method signatures.
 */
@RunWith(RobolectricTestRunner::class)
class VirtualTryOnViewModelTest {

    // ─── TryOnState sealed class ──────────────────────────────────────

    @Test
    fun `TryOnState Idle is an object`() {
        assertTrue(TryOnState.Idle is TryOnState)
    }

    @Test
    fun `TryOnState LoadingPhoto is an object`() {
        assertTrue(TryOnState.LoadingPhoto is TryOnState)
    }

    @Test
    fun `TryOnState PhotoSelected holds a bitmap`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val state = TryOnState.PhotoSelected(bitmap)
        assertTrue(state is TryOnState.PhotoSelected)
        assertNotNull(state.bitmap)
        assertEquals(100, state.bitmap.width)
    }

    @Test
    fun `TryOnState FaceDetected holds originalBitmap landmarks mmPerPixel and measurements`() {
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        val lm = NormalizedLandmark.create(0.5f, 0.5f, 0f)
        val measurements = FaceMeasurements(dipMm = 63.0, dnpOdMm = 31.0, dnpOiMm = 32.0, segmentHeight = 18.0)
        val state = TryOnState.FaceDetected(
            originalBitmap = bitmap,
            landmarks = listOf(lm),
            mmPerPixel = 2.5f,
            measurements = measurements
        )
        assertNotNull(state.originalBitmap)
        assertEquals(200, state.originalBitmap.width)
        assertEquals(300, state.originalBitmap.height)
        assertEquals(1, state.landmarks.size)
        assertEquals(2.5f, state.mmPerPixel, 0.001f)
        assertEquals(63.0, state.measurements.dipMm, 0.001)
    }

    @Test
    fun `TryOnState OverlayReady holds compositeBitmap and measurements`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val measurements = FaceMeasurements(dipMm = 62.0, dnpOdMm = 30.0, dnpOiMm = 32.0, segmentHeight = 16.0)
        val state = TryOnState.OverlayReady(bitmap, measurements)
        assertNotNull(state.compositeBitmap)
        assertEquals(62.0, state.measurements.dipMm, 0.001)
    }

    @Test
    fun `TryOnState Error holds message string`() {
        val state = TryOnState.Error("No se detectó rostro")
        assertEquals("No se detectó rostro", state.message)
    }

    @Test
    fun `TryOnState Error can be created with different messages`() {
        val state1 = TryOnState.Error("Error A")
        val state2 = TryOnState.Error("Error B")
        assertEquals("Error A", state1.message)
        assertEquals("Error B", state2.message)
    }

    // ─── FaceMeasurements data class ───────────────────────────────────

    @Test
    fun `FaceMeasurements stores all four fields`() {
        val m = FaceMeasurements(dipMm = 64.0, dnpOdMm = 31.5, dnpOiMm = 32.5, segmentHeight = 20.0)
        assertEquals(64.0, m.dipMm, 0.001)
        assertEquals(31.5, m.dnpOdMm, 0.001)
        assertEquals(32.5, m.dnpOiMm, 0.001)
        assertNotNull(m.segmentHeight)
        assertEquals(20.0, m.segmentHeight!!, 0.001)
    }

    @Test
    fun `FaceMeasurements segmentHeight can be null`() {
        val m = FaceMeasurements(dipMm = 60.0, dnpOdMm = 30.0, dnpOiMm = 30.0, segmentHeight = null)
        assertNull(m.segmentHeight)
    }

    @Test
    fun `FaceMeasurements data class equality`() {
        val m1 = FaceMeasurements(dipMm = 63.0, dnpOdMm = 31.0, dnpOiMm = 32.0, segmentHeight = 18.0)
        val m2 = FaceMeasurements(dipMm = 63.0, dnpOdMm = 31.0, dnpOiMm = 32.0, segmentHeight = 18.0)
        assertEquals(m1, m2)
    }

    @Test
    fun `FaceMeasurements data class inequality`() {
        val m1 = FaceMeasurements(dipMm = 63.0, dnpOdMm = 31.0, dnpOiMm = 32.0, segmentHeight = 18.0)
        val m2 = FaceMeasurements(dipMm = 62.0, dnpOdMm = 31.0, dnpOiMm = 32.0, segmentHeight = 18.0)
        assertNotEquals(m1, m2)
    }

    // ─── VirtualTryOnViewModel method signatures ───────────────────────

    @Test
    fun `viewModel has loadPatientPD method`() {
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        assertTrue("loadPatientPD must be declared", "loadPatientPD" in methods)
    }

    @Test
    fun `viewModel has selectPhoto method`() {
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        assertTrue("selectPhoto must be declared", "selectPhoto" in methods)
    }

    @Test
    fun `viewModel has selectMontura method`() {
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        assertTrue("selectMontura must be declared", "selectMontura" in methods)
    }

    @Test
    fun `viewModel has adjustOverlay method`() {
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        assertTrue("adjustOverlay must be declared", "adjustOverlay" in methods)
    }

    @Test
    fun `viewModel has saveComposite method`() {
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        assertTrue("saveComposite must be declared", "saveComposite" in methods)
    }

    @Test
    fun `viewModel has shareComposite method`() {
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        assertTrue("shareComposite must be declared", "shareComposite" in methods)
    }

    @Test
    fun `viewModel has saveMeasurementsToEvaluacion method`() {
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        assertTrue("saveMeasurementsToEvaluacion must be declared", "saveMeasurementsToEvaluacion" in methods)
    }

    @Test
    fun `viewModel exposes state StateFlow`() {
        val fields = VirtualTryOnViewModel::class.java.declaredFields.map { it.name }
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        val hasState = "state" in fields || "state" in methods || "getState" in methods
        assertTrue("state must be accessible", hasState)
    }

    @Test
    fun `viewModel exposes selectedMontura StateFlow`() {
        val fields = VirtualTryOnViewModel::class.java.declaredFields.map { it.name }
        val methods = VirtualTryOnViewModel::class.java.methods.map { it.name }
        val hasSelected = "selectedMontura" in fields || "selectedMontura" in methods || "getSelectedMontura" in methods
        assertTrue("selectedMontura must be accessible", hasSelected)
    }

    // ─── EvaluacionClinica contract for PD fields ────────────────────

    @Test
    fun `evaluacionClinica has dipTotalMm field`() {
        val field = EvaluacionClinica::class.java.getDeclaredField("dipTotalMm")
        assertNotNull(field)
        assertTrue(
            "dipTotalMm must be Double or double",
            field.type == java.lang.Double::class.java || field.type == Double::class.java
        )
    }

    @Test
    fun `evaluacionClinica has dnpOdMm field`() {
        val field = EvaluacionClinica::class.java.getDeclaredField("dnpOdMm")
        assertNotNull(field)
    }

    @Test
    fun `evaluacionClinica has dnpOiMm field`() {
        val field = EvaluacionClinica::class.java.getDeclaredField("dnpOiMm")
        assertNotNull(field)
    }

    @Test
    fun `evaluacionClinica has updateEvaluacionViaRepository`() {
        val methods = PacienteRepository::class.java.methods.map { it.name }
        assertTrue("updateEvaluacion must exist on PacienteRepository", "updateEvaluacion" in methods)
    }

    @Test
    fun `montura has anchoMm field for scaling`() {
        val field = Montura::class.java.getDeclaredField("anchoMm")
        assertNotNull(field)
    }

    @Test
    fun `montura has imagenUri field for overlay`() {
        val field = Montura::class.java.getDeclaredField("imagenUri")
        assertNotNull(field)
    }

    @Test
    fun `montura has puenteMm field for positioning`() {
        val field = Montura::class.java.getDeclaredField("puenteMm")
        assertNotNull(field)
    }

    // ─── saveMeasurementsToEvaluacion behavioral tests ─────────────────────

    @Test
    fun `saveMeasurementsToEvaluacion updates evaluacion with correct DIP and DNP values`() = runBlocking {
        val mockPacienteRepo = mockk<PacienteRepository>()
        val mockFaceLandmarker = mockk<FaceLandmarkerUseCase>(relaxed = true)
        val mockFrameOverlay = mockk<FrameOverlayUseCase>(relaxed = true)
        val mockMeasurementExtractor = mockk<FaceMeasurementExtractor>(relaxed = true)
        val mockMonturaCoord = mockk<MonturaInventoryCoordinator>(relaxed = true)

        val pacId = "paciente-vto"
        val evalId = "eval-vto-1"
        val evaluacion = EvaluacionClinica(
            id = evalId, pacienteId = pacId, fecha = LocalDate.parse("2026-05-29"),
            opticaId = "o1"
        )

        every { mockPacienteRepo.getEvaluacionesByPaciente(pacId) } returns flowOf(listOf(evaluacion))
        coEvery { mockPacienteRepo.updateEvaluacion(any()) } just Runs

        val viewModel = VirtualTryOnViewModel(
            faceLandmarkerUseCase = mockFaceLandmarker,
            frameOverlayUseCase = mockFrameOverlay,
            measurementExtractor = mockMeasurementExtractor,
            pacienteRepository = mockPacienteRepo,
            monturaCoordinator = mockMonturaCoord,
            context = ApplicationProvider.getApplicationContext()
        )

        // Set latestMeasurements via reflection (pragmatic: skip complex detection flow)
        val measurementsField = VirtualTryOnViewModel::class.java.getDeclaredField("latestMeasurements")
        measurementsField.isAccessible = true
        measurementsField.set(viewModel, FaceMeasurements(
            dipMm = 64.0, dnpOdMm = 31.5, dnpOiMm = 33.0, segmentHeight = 18.0
        ))

        viewModel.saveMeasurementsToEvaluacion(pacId)

        coVerify(exactly = 1) {
            mockPacienteRepo.updateEvaluacion(match { updated ->
                updated.id == evalId &&
                updated.dipTotalMm == 64.0 &&
                updated.dnpOdMm == 31.5 &&
                updated.dnpOiMm == 33.0
            })
        }
    }

    @Test
    fun `saveMeasurementsToEvaluacion does nothing when no measurements exist`() = runBlocking {
        val mockPacienteRepo = mockk<PacienteRepository>()
        val mockFaceLandmarker = mockk<FaceLandmarkerUseCase>(relaxed = true)
        val mockFrameOverlay = mockk<FrameOverlayUseCase>(relaxed = true)
        val mockMeasurementExtractor = mockk<FaceMeasurementExtractor>(relaxed = true)
        val mockMonturaCoord = mockk<MonturaInventoryCoordinator>(relaxed = true)

        val pacId = "no-measurements"

        every { mockPacienteRepo.getEvaluacionesByPaciente(any()) } returns flowOf(emptyList())
        coEvery { mockPacienteRepo.updateEvaluacion(any()) } just Runs

        val viewModel = VirtualTryOnViewModel(
            faceLandmarkerUseCase = mockFaceLandmarker,
            frameOverlayUseCase = mockFrameOverlay,
            measurementExtractor = mockMeasurementExtractor,
            pacienteRepository = mockPacienteRepo,
            monturaCoordinator = mockMonturaCoord,
            context = ApplicationProvider.getApplicationContext()
        )

        // latestMeasurements is null (default) — method should return immediately
        viewModel.saveMeasurementsToEvaluacion(pacId)

        coVerify(exactly = 0) { mockPacienteRepo.updateEvaluacion(any()) }
    }

    @Test
    fun `saveMeasurementsToEvaluacion picks latest evaluacion when mulitple exist`() = runBlocking {
        val mockPacienteRepo = mockk<PacienteRepository>()
        val mockFaceLandmarker = mockk<FaceLandmarkerUseCase>(relaxed = true)
        val mockFrameOverlay = mockk<FrameOverlayUseCase>(relaxed = true)
        val mockMeasurementExtractor = mockk<FaceMeasurementExtractor>(relaxed = true)
        val mockMonturaCoord = mockk<MonturaInventoryCoordinator>(relaxed = true)

        val pacId = "multi-eval"
        val older = EvaluacionClinica(
            id = "eval-old", pacienteId = pacId,
            fecha = LocalDate.parse("2026-01-15"), opticaId = "o1"
        )
        val newer = EvaluacionClinica(
            id = "eval-new", pacienteId = pacId,
            fecha = LocalDate.parse("2026-05-29"), opticaId = "o1"
        )

        every { mockPacienteRepo.getEvaluacionesByPaciente(pacId) } returns flowOf(listOf(older, newer))
        coEvery { mockPacienteRepo.updateEvaluacion(any()) } just Runs

        val viewModel = VirtualTryOnViewModel(
            faceLandmarkerUseCase = mockFaceLandmarker,
            frameOverlayUseCase = mockFrameOverlay,
            measurementExtractor = mockMeasurementExtractor,
            pacienteRepository = mockPacienteRepo,
            monturaCoordinator = mockMonturaCoord,
            context = ApplicationProvider.getApplicationContext()
        )

        val measurementsField = VirtualTryOnViewModel::class.java.getDeclaredField("latestMeasurements")
        measurementsField.isAccessible = true
        measurementsField.set(viewModel, FaceMeasurements(
            dipMm = 62.0, dnpOdMm = 30.0, dnpOiMm = 32.0, segmentHeight = null
        ))

        viewModel.saveMeasurementsToEvaluacion(pacId)

        coVerify(exactly = 1) {
            mockPacienteRepo.updateEvaluacion(match { updated ->
                updated.id == "eval-new" // should update the NEWEST evaluation
            })
        }
    }

    @Test
    fun `saveMeasurementsToEvaluacion preserves other evaluacion fields`() = runBlocking {
        val mockPacienteRepo = mockk<PacienteRepository>()
        val mockFaceLandmarker = mockk<FaceLandmarkerUseCase>(relaxed = true)
        val mockFrameOverlay = mockk<FrameOverlayUseCase>(relaxed = true)
        val mockMeasurementExtractor = mockk<FaceMeasurementExtractor>(relaxed = true)
        val mockMonturaCoord = mockk<MonturaInventoryCoordinator>(relaxed = true)

        val pacId = "preserve-fields"
        val evaluacion = EvaluacionClinica(
            id = "eval-preserve", pacienteId = pacId,
            fecha = LocalDate.parse("2026-05-29"), opticaId = "o1",
            motivoConsulta = "Control rutina",
            diagnostico = "Miopía",
            // Pre-existing optical values
            objOdEsf = "-2.00", objOdCil = "-0.75", objOdEje = "180",
            objOiEsf = "-1.50", objOiCil = "-0.50", objOiEje = "175",
            avScOdLejos = "0.5", avScOiLejos = "0.6",
            avCcOdLejos = "1.0", avCcOiLejos = "1.0",
            // measurement fields are null initially
            dipTotalMm = null, dnpOdMm = null, dnpOiMm = null
        )

        every { mockPacienteRepo.getEvaluacionesByPaciente(pacId) } returns flowOf(listOf(evaluacion))
        coEvery { mockPacienteRepo.updateEvaluacion(any()) } just Runs

        val viewModel = VirtualTryOnViewModel(
            faceLandmarkerUseCase = mockFaceLandmarker,
            frameOverlayUseCase = mockFrameOverlay,
            measurementExtractor = mockMeasurementExtractor,
            pacienteRepository = mockPacienteRepo,
            monturaCoordinator = mockMonturaCoord,
            context = ApplicationProvider.getApplicationContext()
        )

        val measurementsField = VirtualTryOnViewModel::class.java.getDeclaredField("latestMeasurements")
        measurementsField.isAccessible = true
        measurementsField.set(viewModel, FaceMeasurements(
            dipMm = 63.0, dnpOdMm = 31.0, dnpOiMm = 32.0, segmentHeight = 17.5
        ))

        viewModel.saveMeasurementsToEvaluacion(pacId)

        coVerify(exactly = 1) {
            mockPacienteRepo.updateEvaluacion(match { updated ->
                updated.id == "eval-preserve" &&
                updated.motivoConsulta == "Control rutina" &&
                updated.diagnostico == "Miopía" &&
                updated.objOdEsf == "-2.00" &&
                updated.objOiCil == "-0.50" &&
                updated.avCcOdLejos == "1.0" &&
                // Measurement fields updated
                updated.dipTotalMm == 63.0 &&
                updated.dnpOdMm == 31.0 &&
                updated.dnpOiMm == 32.0
            })
        }
    }

    @Test
    fun `saveMeasurementsToEvaluacion handles empty evaluacion list gracefully`() = runBlocking {
        val mockPacienteRepo = mockk<PacienteRepository>()
        val mockFaceLandmarker = mockk<FaceLandmarkerUseCase>(relaxed = true)
        val mockFrameOverlay = mockk<FrameOverlayUseCase>(relaxed = true)
        val mockMeasurementExtractor = mockk<FaceMeasurementExtractor>(relaxed = true)
        val mockMonturaCoord = mockk<MonturaInventoryCoordinator>(relaxed = true)

        every { mockPacienteRepo.getEvaluacionesByPaciente(any()) } returns flowOf(emptyList())
        coEvery { mockPacienteRepo.updateEvaluacion(any()) } just Runs

        val viewModel = VirtualTryOnViewModel(
            faceLandmarkerUseCase = mockFaceLandmarker,
            frameOverlayUseCase = mockFrameOverlay,
            measurementExtractor = mockMeasurementExtractor,
            pacienteRepository = mockPacienteRepo,
            monturaCoordinator = mockMonturaCoord,
            context = ApplicationProvider.getApplicationContext()
        )

        val measurementsField = VirtualTryOnViewModel::class.java.getDeclaredField("latestMeasurements")
        measurementsField.isAccessible = true
        measurementsField.set(viewModel, FaceMeasurements(
            dipMm = 60.0, dnpOdMm = 29.0, dnpOiMm = 31.0, segmentHeight = null
        ))

        // No evaluaciones exist — should not crash, should not call update
        viewModel.saveMeasurementsToEvaluacion("no-evaluaciones-paciente")

        coVerify(exactly = 0) { mockPacienteRepo.updateEvaluacion(any()) }
    }
}
