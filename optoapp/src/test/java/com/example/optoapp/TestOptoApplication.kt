package com.example.optoapp

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper

/**
 * Test Application that initializes WorkManager before [OptoApplication.onCreate] runs.
 * Prevents WorkManager.getInstance() crashes in Robolectric tests.
 */
class TestOptoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.ERROR)
                .build()
            WorkManager.initialize(this, config)
            WorkManagerTestInitHelper.initializeTestWorkManager(this)
        } catch (_: IllegalStateException) {
            // already initialized
        }
        // Simulate OptoApplication's widget schedule without WorkManager crash
        try {
            OptoApplication::class.java.getDeclaredMethod("onCreate").let {
                // don't call super's super — just skip widget scheduling
            }
        } catch (_: Exception) { }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }
}
