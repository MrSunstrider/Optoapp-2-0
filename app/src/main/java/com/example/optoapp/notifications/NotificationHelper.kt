package com.example.optoapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.optoapp.MainActivity
import com.example.optoapp.R
import java.time.*
import java.util.concurrent.TimeUnit

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Citas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para próximas citas de pacientes"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra la notificación inmediatamente (usado por el Worker).
     */
    fun showNotification(patientName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Sugerencia: Usar un icono de la app real
            .setContentTitle("Recordatorio de Cita")
            .setContentText("Hoy tiene una cita con el paciente: $patientName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(patientName.hashCode(), notification)
    }

    /**
     * Agenda un recordatorio usando WorkManager para las 12:00 PM del día indicado.
     */
    fun scheduleWorkManagerReminder(patientName: String, appointmentDate: LocalDate, evaluationId: String) {
        val workManager = WorkManager.getInstance(context)

        val noonDateTime = appointmentDate.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault())
        val delay = noonDateTime.toInstant().toEpochMilli() - System.currentTimeMillis()

        if (delay <= 0) return // Si ya pasó la hora, no agendamos

        val inputData = workDataOf(
            "patient_name" to patientName,
            "evaluation_id" to evaluationId
        )

        val workRequest = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("reminder_$evaluationId")
            .build()

        workManager.enqueueUniqueWork(
            "reminder_$evaluationId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(evaluationId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_$evaluationId")
    }

    companion object {
        const val CHANNEL_ID = "OPTOAPP_REMINDERS"
    }
}
