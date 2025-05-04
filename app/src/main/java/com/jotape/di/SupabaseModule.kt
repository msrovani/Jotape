package com.jotape.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import javax.inject.Singleton

/**
 * Módulo Hilt dedicado a prover dependências específicas do Supabase que
 * não estão no NetworkModule (neste caso, apenas ComposeAuth).
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    private const val TAG = "SupabaseModule"

    // Manter apenas o provider para ComposeAuth
    @Provides
    @Singleton
    fun provideSupabaseComposeAuth(supabaseClient: SupabaseClient): ComposeAuth {
        Log.d(TAG, "Providing Supabase ComposeAuth")
        return supabaseClient.composeAuth
    }
} 