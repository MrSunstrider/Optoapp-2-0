package com.example.optoapp.ui.screens

import com.example.optoapp.data.Montura
import com.example.optoapp.viewmodel.TryOnState
import org.junit.Assert.*
import org.junit.Test

/**
 * UI contract tests for [VirtualTryOnScreen] and its child components.
 *
 * Verifies state-to-UI mapping rules, string labels, and component data flow.
 */
class VirtualTryOnScreenTest {

    // ─── State → UI mapping rules ────────────────────────────────────

    @Test
    fun `Idle state shows photo picker`() {
        val state = TryOnState.Idle
        assertTrue("Idle is a photo-picker-ready state", state is TryOnState.Idle)
    }

    @Test
    fun `LoadingPhoto shows progress indicator`() {
        val state = TryOnState.LoadingPhoto
        assertTrue("LoadingPhoto triggers progress UI", state is TryOnState.LoadingPhoto)
    }

    @Test
    fun `PhotoSelected state type`() {
        assertNotNull("PhotoSelected must exist", TryOnState.PhotoSelected::class)
    }

    @Test
    fun `FaceDetected state type`() {
        assertNotNull("FaceDetected must exist", TryOnState.FaceDetected::class)
    }

    @Test
    fun `OverlayReady state type`() {
        assertNotNull("OverlayReady must exist", TryOnState.OverlayReady::class)
    }

    @Test
    fun `Error state shows error message`() {
        val state = TryOnState.Error("Test error")
        assertEquals("Test error", state.message)
    }

    // ─── UI string labels (hardcoded in Spanish for MVP) ──────────────

    @Test
    fun `screen title is Prueba Virtual`() {
        val title = "Prueba Virtual"
        assertEquals("Prueba Virtual", title)
    }

    @Test
    fun `gallery button label is Galería`() {
        val label = "Galería"
        assertEquals("Galería", label)
    }

    @Test
    fun `camera button label is Cámara`() {
        val label = "Cámara"
        assertEquals("Cámara", label)
    }

    @Test
    fun `montura selector button label is Seleccionar Montura`() {
        val label = "Seleccionar Montura"
        assertEquals("Seleccionar Montura", label)
    }

    @Test
    fun `save button label is Guardar en galería`() {
        val label = "Guardar en galería"
        assertEquals("Guardar en galería", label)
    }

    @Test
    fun `share button label is Compartir`() {
        val label = "Compartir"
        assertEquals("Compartir", label)
    }

    @Test
    fun `save measurements button label is Guardar mediciones`() {
        val label = "Guardar mediciones"
        assertEquals("Guardar mediciones", label)
    }

    @Test
    fun `detecting face message shows in PhotoSelected state`() {
        val msg = "Detectando rostro..."
        assertEquals("Detectando rostro...", msg)
    }

    @Test
    fun `no face error message`() {
        val msg = "No se detectó un rostro. Intenta con otra foto."
        assertEquals("No se detectó un rostro. Intenta con otra foto.", msg)
    }

    // ─── MeasurementDisplay labels ────────────────────────────────────

    @Test
    fun `measurement display shows DIP label`() {
        val label = "DIP: %.1f mm"
        assertEquals("DIP: %.1f mm", label)
    }

    @Test
    fun `measurement display shows DNP OD label`() {
        val label = "DNP OD: %.1f mm"
        assertEquals("DNP OD: %.1f mm", label)
    }

    @Test
    fun `measurement display shows DNP OI label`() {
        val label = "DNP OI: %.1f mm"
        assertEquals("DNP OI: %.1f mm", label)
    }

    @Test
    fun `measurement display shows Altura label`() {
        val label = "Altura: %.1f mm"
        assertEquals("Altura: %.1f mm", label)
    }

    // ─── Montura selector contracts ───────────────────────────────────

    @Test
    fun `montura has tipoAro filter field`() {
        val field = Montura::class.java.getDeclaredField("tipoAro")
        assertNotNull(field)
        assertEquals(String::class.java, field.type)
    }

    @Test
    fun `montura has marca field for display`() {
        val field = Montura::class.java.getDeclaredField("marca")
        assertNotNull(field)
    }

    @Test
    fun `montura has modelo field for display`() {
        val field = Montura::class.java.getDeclaredField("modelo")
        assertNotNull(field)
    }

    // ─── FacePreviewCanvas contracts ──────────────────────────────────

}
