package com.example.optoapp.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AppointmentReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val hasPermission = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return doWorkCore(
            patientName = inputData.getString("patient_name"),
            evaluationId = inputData.getString("evaluation_id"),
            hasPermission = hasPermission,
            showNotification = { name, id -> notificationHelper.showNotification(name, id) },
        )
    }

    companion object {
        fun doWorkCore(
            patientName: String?,
            evaluationId: String?,
            hasPermission: Boolean,
            showNotification: (patientName: String, notificationId: Int) -> Unit,
        ): ListenableWorker.Result {
            if (patientName == null || evaluationId == null) return ListenableWorker.Result.failure()
            if (!hasPermission) return ListenableWorker.Result.success()
            showNotification(patientName, evaluationId.hashCode())
            return ListenableWorker.Result.success()
        }
    }
}
