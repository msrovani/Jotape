package com.jotape.di

import android.content.Context
import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.jotape.BuildConfig
import com.jotape.data.remote.api.JotapeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Módulo Hilt para fornecer dependências relacionadas à rede, como Retrofit e serviços de API.
 */
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
    @Provides
    @Singleton
    fun provideSupabaseClient(
        @ApplicationContext context: Context
    ): SupabaseClient {
        // Ler diretamente do BuildConfig
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

        Log.d("SupabaseClientProvider", "URL: $supabaseUrl, Key: ${supabaseAnonKey.take(5)}...") // Log para verificar
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            Log.e("SupabaseClientProvider", "Supabase URL or Key is BLANK in BuildConfig!")
            // Lançar exceção ou retornar um cliente dummy pode ser apropriado aqui
            // throw IllegalStateException("Supabase URL/Key not found in BuildConfig")
        }

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey
        ) {
            install(Auth)
            install(ComposeAuth) { googleNativeLogin(serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID) }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideAuth(supabaseClient: SupabaseClient): Auth {
        return supabaseClient.auth
    }

    @Provides
    @Singleton
    fun providePostgrest(supabaseClient: SupabaseClient): Postgrest {
        return supabaseClient.postgrest
    }

    @Provides
    @Singleton
    fun provideStorage(supabaseClient: SupabaseClient): Storage {
        return supabaseClient.storage
    }

    @Provides
    @Singleton
    fun provideRealtime(supabaseClient: SupabaseClient): Realtime {
        return supabaseClient.realtime
    }

    @Provides
    @Singleton
    @Named("SupabaseFunctionApiKey")
    fun provideSupabaseFunctionApiKey(): String = BuildConfig.SUPABASE_SERVICE_ROLE_KEY

    @Provides
    @Singleton
    fun provideOkHttpClient(
        auth: Auth,
        @Named("SupabaseFunctionApiKey") supabaseFunctionApiKey: String
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val builder = originalRequest.newBuilder()
                builder.header("apikey", supabaseFunctionApiKey)
                val token = auth.currentSessionOrNull()?.accessToken
                if (token != null) {
                    builder.header("Authorization", "Bearer $token")
                } else {
                    Log.w("OkHttpClientAuth", "No auth token available for request to ${originalRequest.url}")
                }
                if (originalRequest.header("Content-Type") == null) {
                    builder.header("Content-Type", "application/json")
                }
                chain.proceed(builder.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        // Ler diretamente do BuildConfig
        val supabaseUrl = BuildConfig.SUPABASE_URL
        if (supabaseUrl.isBlank()) {
             Log.e("RetrofitProvider", "Supabase URL is BLANK in BuildConfig!")
             // Lançar exceção?
             // throw IllegalStateException("Supabase URL not found in BuildConfig for Retrofit")
        }
        val functionsBaseUrl = "$supabaseUrl/functions/v1/"
        Log.d("RetrofitProvider", "Functions Base URL: $functionsBaseUrl")

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(functionsBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideJotapeApiService(retrofit: Retrofit): JotapeApiService {
        return retrofit.create(JotapeApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
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