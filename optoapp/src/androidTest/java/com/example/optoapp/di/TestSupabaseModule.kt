package com.example.optoapp.di

import com.example.optoapp.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

/**
 * Test module that replaces [SupabaseModule] for instrumented tests.
 *
 * Uses the test Supabase project credentials (SUPABASE_TEST_URL, SUPABASE_TEST_ANON_KEY)
 * configured in `local.properties`. Falls back to a no-op client if credentials
 * are not available — tests that need real Supabase will be skipped via
 * `AssumptionViolatedException`.
 *
 * This module is loaded automatically by Hilt's test infrastructure — no
 * `@UninstallModules` annotation needed on individual test classes.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SupabaseModule::class]
)
object TestSupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val url = BuildConfig.SUPABASE_TEST_URL
        val anonKey = BuildConfig.SUPABASE_TEST_ANON_KEY
        return if (url.isNotBlank() && anonKey.isNotBlank()) {
            createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = anonKey
            ) {
                defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    encodeDefaults = true
                })
                install(Postgrest)
                install(Auth)
            }
        } else {
            // No-op client — tests must check BuildConfig or skip via assumeTrue
            createSupabaseClient(
                supabaseUrl = "https://test-optoapp.supabase.co",
                supabaseKey = "test-anon-key"
            ) {
                install(Postgrest)
                install(Auth)
            }
        }
    }
}
