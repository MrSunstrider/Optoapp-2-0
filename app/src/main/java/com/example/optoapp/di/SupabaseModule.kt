package com.example.optoapp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://sflhtihqdhrlryeyrzdo.supabase.co",
            supabaseKey = "sb_publishable_YGRw7Rxr8bFabgHGpE-i5A_JJcOdYiK"
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
}
