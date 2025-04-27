package com.jotape.di

import com.jotape.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.googleNativeLogin
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.compose.auth.composeAuth
import javax.inject.Singleton
import android.util.Log

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    private const val TAG = "SupabaseModule"

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY

        Log.d(TAG, "Initializing Supabase with URL: $url")
        Log.d(TAG, "Initializing Supabase with Key: $key")

        if (url.contains("YOUR_DEFAULT_URL") || key.contains("YOUR_DEFAULT_KEY")) {
            Log.w(TAG, "Warning: Supabase client might be initialized with default values!")
        }

        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Auth) {
                // Configurações opcionais do Auth aqui
            }
            install(Postgrest) {
                // Configurações opcionais do Postgrest aqui
            }
            install(Storage) {
                // Configurações opcionais do Storage aqui
            }
            install(Realtime)
            install(ComposeAuth) {
                googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID)
            }
            // Adicione outros plugins aqui (Realtime, Functions, etc.)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(supabaseClient: SupabaseClient): Auth {
        return supabaseClient.auth
    }

    @Provides
    @Singleton
    fun provideSupabaseDatabase(supabaseClient: SupabaseClient): Postgrest {
        return supabaseClient.postgrest
    }

    @Provides
    @Singleton
    fun provideSupabaseComposeAuth(supabaseClient: SupabaseClient): ComposeAuth {
        return supabaseClient.composeAuth
    }

    // Se precisar do Storage:
    /*
    @Provides
    @Singleton
    fun provideSupabaseStorage(supabaseClient: SupabaseClient): Storage {
        return supabaseClient.storage
    }
    */
} 