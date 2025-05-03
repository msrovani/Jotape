package com.jotape.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.jotape.BuildConfig
import com.jotape.data.remote.api.JotapeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Definir nomes para as Strings injetadas
    const val NAMED_SUPABASE_URL = "supabaseUrl"
    const val NAMED_SUPABASE_ANON_KEY = "supabaseAnonKey"

    // Prover os valores do BuildConfig como Strings nomeadas
    @Provides
    @Named(NAMED_SUPABASE_URL)
    fun provideSupabaseUrl(): String = BuildConfig.SUPABASE_URL

    @Provides
    @Named(NAMED_SUPABASE_ANON_KEY)
    fun provideSupabaseAnonKey(): String = BuildConfig.SUPABASE_ANON_KEY

    // Provider para SupabaseClient (provavelmente já existe em outro módulo, ex: SupabaseModule)
    // Se não existir, precisa ser adicionado. Assumindo que existe por enquanto.
    // @Provides
    // @Singleton
    // fun provideSupabaseClient(...): SupabaseClient { ... }

    // Provider para GoTrue (geralmente obtido do SupabaseClient)
    // Se não existir, precisa ser adicionado. Assumindo que existe.
    // @Provides
    // @Singleton
    // fun provideGoTrue(supabaseClient: SupabaseClient): GoTrue = supabaseClient.gotrue


    @Provides
    @Singleton
    fun provideAuthInterceptor(
        auth: Auth,
        @Named(NAMED_SUPABASE_ANON_KEY) supabaseAnonKey: String
    ): Interceptor {
        return Interceptor { chain ->
            val currentSession = runBlocking {
                try {
                    (auth.sessionStatus.value as? SessionStatus.Authenticated)?.session
                } catch (e: Exception) {
                    null
                }
            }
            val token = currentSession?.accessToken

            var request = chain.request()
            request = request.newBuilder()
                .addHeader("apikey", supabaseAnonKey)
                .build()

            if (token != null) {
                request = request.newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                println("AuthInterceptor: No Supabase token found.")
            }

            chain.proceed(request)
        }
    }


    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideKotlinxSerializationJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
        @Named(NAMED_SUPABASE_URL) supabaseUrl: String
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(supabaseUrl + "/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideJotapeApiService(retrofit: Retrofit): JotapeApiService {
        return retrofit.create(JotapeApiService::class.java)
    }

    // --- REMOVER PROVIDERS DO ROOM ---
    // Se você tiver providers para AppDatabase ou InteractionDao aqui ou em outro módulo (DatabaseModule?),
    // eles precisam ser removidos. Exemplo:
    // @Provides
    // @Singleton
    // fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase { ... }
    //
    // @Provides
    // fun provideInteractionDao(appDatabase: AppDatabase): InteractionDao { ... }

} 