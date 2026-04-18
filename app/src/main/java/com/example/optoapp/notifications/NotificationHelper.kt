package com.example.optoapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.optoapp.MainActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationManagerCompat = NotificationManagerCompat.from(context)

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
    fun showNotification(patientName: String, notificationId: Int = patientName.hashCode()) {
        if (Build.VERSION.SDK_INT >= 33) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (ActivityCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No se muestra recordatorio: falta permiso POST_NOTIFICATIONS")
                return
            }
        }
        if (!notificationManagerCompat.areNotificationsEnabled()) {
            Log.w(TAG, "No se muestra recordatorio: notificaciones desactivadas para la app")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Recordatorio de Cita")
            .setContentText("Hoy tiene una cita con el paciente: $patientName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Agenda un recordatorio usando WorkManager para las 12:00 del día de la cita (hora local).
     * Si ese día ya pasó el mediodía, programa un aviso en breve (mismo día).
     * Si la fecha de cita es anterior a hoy, no programa nada.
     */
    fun scheduleWorkManagerReminder(patientName: String, appointmentDate: LocalDate, evaluationId: String) {
        val workManager = WorkManager.getInstance(context)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        if (appointmentDate.isBefore(today)) {
            Log.w(TAG, "Recordatorio no programado: próxima cita en el pasado ($appointmentDate), eval=$evaluationId")
            cancelReminder(evaluationId)
            return
        }

        val noonDateTime = appointmentDate.atTime(LocalTime.NOON).atZone(zone)
        var delayMs = noonDateTime.toInstant().toEpochMilli() - System.currentTimeMillis()

        if (delayMs <= 0) {
            if (appointmentDate == today) {
                // Mismo día pero ya pasaron las 12:00 — avisar en breve para no perder el recordatorio.
                delayMs = TimeUnit.SECONDS.toMillis(SOON_DELAY_SEC)
                Log.d(TAG, "Cita hoy tras mediodía: recordatorio en ${SOON_DELAY_SEC}s (eval=$evaluationId)")
            } else {
                Log.w(TAG, "Recordatorio no programado: delay<=0 y fecha no es hoy (eval=$evaluationId)")
                return
            }
        } else {
            Log.d(
                TAG,
                "Recordatorio programado en ${delayMs / 1000}s para ${appointmentDate} 12:00 (eval=$evaluationId)"
            )
        }

        val inputData = workDataOf(
            "patient_name" to patientName,
            "evaluation_id" to evaluationId
        )

        val workRequest = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
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
        private const val TAG = "NotificationHelper"
        /** Retraso cuando el mediodía del día de la cita ya pasó (mismo día). */
        private const val SOON_DELAY_SEC = 20L
    }
}
