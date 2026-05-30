package com.example.optoapp.di

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
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
        return try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setOutputFaceBlendshapes(false)
                .build()
            FaceLandmarker.createFromOptions(context, options)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create FaceLandmarker", e)
            null
        }
    }
}
