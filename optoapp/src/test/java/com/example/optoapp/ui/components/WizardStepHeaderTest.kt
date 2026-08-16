package com.example.optoapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JUnit tests for the wizard step-header helpers that replace the numbered
 * StepIndicator circles with a step title + progress caption.
 */
class WizardStepHeaderTest {

    private val labels = listOf("Anamnesis", "Examen Visual", "Refracción", "Contactología", "Cierre")

    @Test
    fun `title returns the label at current step`() {
        assertEquals("Anamnesis", wizardStepTitle(labels, 0))
        assertEquals("Refracción", wizardStepTitle(labels, 2))
        assertEquals("Cierre", wizardStepTitle(labels, 4))
    }

    @Test
    fun `title is empty for out-of-range index`() {
        assertEquals("", wizardStepTitle(labels, -1))
        assertEquals("", wizardStepTitle(labels, 5))
        assertEquals("", wizardStepTitle(emptyList(), 0))
    }

    @Test
    fun `progress is human readable 1-based`() {
        assertEquals("Paso 1 de 5", wizardStepProgress(5, 0))
        assertEquals("Paso 3 de 5", wizardStepProgress(5, 2))
        assertEquals("Paso 3 de 3", wizardStepProgress(3, 2))
    }

    @Test
    fun `progress clamps out-of-range step within bounds`() {
        assertEquals("Paso 1 de 5", wizardStepProgress(5, -3))
        assertEquals("Paso 5 de 5", wizardStepProgress(5, 99))
    }

    @Test
    fun `progress tolerates non-positive total`() {
        assertEquals("Paso 1 de 1", wizardStepProgress(0, 0))
        assertEquals("Paso 1 de 1", wizardStepProgress(-2, 0))
    }
}
