package com.example.optoapp.di

import com.example.optoapp.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // TODO(CERT-PINNING): Replace with actual SHA-256 fingerprints from infra team.
            // Switch ktor-client-cio → ktor-client-okhttp and add:
            //   httpEngine = OkHttp.create {
            //       certificatePinner = CertificatePinner.Builder()
            //           .add("supabase.co", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            //           .build()
            //   }

            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
            })
            install(Postgrest)
            install(Realtime)
            install(Functions)
            install(Auth) {
                host = BuildConfig.SUPABASE_REDIRECT_HOST
                scheme = BuildConfig.SUPABASE_REDIRECT_SCHEME
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
                // P0-T4: evitar que el Auth plugin limpie la sesión en background
                // y desactive el auto-refresh, lo que causa RLS 401 en syncs posteriores.
                enableLifecycleCallbacks = false
            }
        }
    }

    @Provides
    @Singleton
    fun providePostgrest(client: SupabaseClient): Postgrest = client.postgrest
}
