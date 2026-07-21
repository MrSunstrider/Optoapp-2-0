package com.example.optoapp.notifications

import androidx.work.ListenableWorker
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests puros para [AppointmentReminderWorker.doWorkCore].
 *
 * No dependen de Android Framework, Robolectric, WorkManager, ni corrutinas.
 * La lógica de negocio del worker se testea directamente con parámetros planos.
 *
 * El worker real ([AppointmentReminderWorker.doWork]) solo resuelve
 * `hasPermission` contra el sistema y delega en doWorkCore.
 */
class AppointmentReminderWorkerTest {

    @Test
    fun `with valid inputs and permission granted returns success and invokes notification`() {
        var notifiedName: String? = null
        var notifiedId: Int? = null

        val result = AppointmentReminderWorker.doWorkCore(
            patientName = "Juan Pérez",
            evaluationId = "eval-123",
            hasPermission = true,
            showNotification = { name, id ->
                notifiedName = name
                notifiedId = id
            },
        )

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals("Juan Pérez", notifiedName)
        assertEquals("eval-123".hashCode(), notifiedId)
    }

    @Test
    fun `with valid inputs and no permission returns success without notification`() {
        var notified = false

        val result = AppointmentReminderWorker.doWorkCore(
            patientName = "Juan Pérez",
            evaluationId = "eval-123",
            hasPermission = false,
            showNotification = { _, _ -> notified = true },
        )

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals("no se debe notificar sin permiso", false, notified)
    }

    @Test
    fun `without patient_name returns failure`() {
        val result = AppointmentReminderWorker.doWorkCore(
            patientName = null,
            evaluationId = "eval-123",
            hasPermission = true,
            showNotification = { _, _ -> },
        )

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `without evaluation_id returns failure`() {
        val result = AppointmentReminderWorker.doWorkCore(
            patientName = "Juan Pérez",
            evaluationId = null,
            hasPermission = true,
            showNotification = { _, _ -> },
        )

        assertEquals(ListenableWorker.Result.failure(), result)
    }

    @Test
    fun `with both inputs missing returns failure`() {
        val result = AppointmentReminderWorker.doWorkCore(
            patientName = null,
            evaluationId = null,
            hasPermission = true,
            showNotification = { _, _ -> },
        )

        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
