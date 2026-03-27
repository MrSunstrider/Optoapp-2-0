package com.example.optoapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AppointmentReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val patientName = inputData.getString("patient_name") ?: return Result.failure()
        val evaluationId = inputData.getString("evaluation_id") ?: return Result.failure()
        
        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.showNotification(patientName)
        
        return Result.success()
    }
}

// Nota: Necesitamos actualizar NotificationHelper para que tenga un método simple de 'showNotification'
// sin depender de AlarmManager para la parte del 'disparo'.
