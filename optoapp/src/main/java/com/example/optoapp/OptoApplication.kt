package com.example.optoapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.optoapp.widget.MiNegocioWidgetWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class OptoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleWidgetRefresh()
    }

    private fun scheduleWidgetRefresh() {
        val request = PeriodicWorkRequestBuilder<MiNegocioWidgetWorker>(
            6,
            TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "mi_negocio_widget_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
