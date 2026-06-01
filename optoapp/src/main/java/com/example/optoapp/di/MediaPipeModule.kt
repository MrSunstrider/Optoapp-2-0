package com.example.optoapp.di

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaPipeModule {

    private const val TAG = "MediaPipeModule"

    @Provides
    @Singleton
    fun provideFaceLandmarker(@ApplicationContext context: Context): FaceLandmarker? {
        // Try GPU delegate first (faster, works on most devices)
        val gpuResult = tryCreate(context, Delegate.GPU)
        if (gpuResult != null) return gpuResult

        // Fall back to CPU for devices where GPU fails (Huawei/Honor Kirin, etc.)
        return tryCreate(context, Delegate.CPU)
    }

    private fun tryCreate(context: Context, delegate: Delegate): FaceLandmarker? {
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .setDelegate(delegate)
                .build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setOutputFaceBlendshapes(false)
                .build()
            FaceLandmarker.createFromOptions(context, options)
        } catch (e: Throwable) {
            null
        }
    }
}
