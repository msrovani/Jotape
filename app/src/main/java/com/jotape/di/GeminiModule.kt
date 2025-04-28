package com.jotape.di

import android.content.Context
import androidx.work.WorkManager
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.jotape.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.ai.client.generativeai.type.InvalidStateException

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    private const val TAG = "GeminiModule"

    // Configurações de segurança - Ajuste conforme necessário
    // Bloquear conteúdo mais problemático
    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
    )

    // Configuração de Geração - Ajuste conforme necessário
    private val generationConfig = generationConfig {
        temperature = 0.7f
        topK = 16
        topP = 0.9f
        maxOutputTokens = 2048
        // stopSequences = listOf("...") // Opcional
    }

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel {
        // Leia a chave do BuildConfig, que vem do local.properties
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "NO_GEMINI_KEY_IN_PROPERTIES") {
            Log.e(TAG, "Gemini API Key not found in BuildConfig. Check local.properties and build configuration.")
            // Você pode lançar um erro aqui ou retornar um modelo que falhará
            throw InvalidStateException("Gemini API key is missing or invalid in configuration.")
        }

        Log.i(TAG, "Initializing Gemini Model for development/testing with API key from BuildConfig.")
        // !! Lembre-se de mover a chamada da API para um backend seguro em produção !!

        // Use o nome do modelo desejado
        val modelName = "gemini-2.0-flash"

        return GenerativeModel(
             modelName = modelName,
             apiKey = apiKey, // Use a chave real do BuildConfig
             generationConfig = generationConfig,
             safetySettings = safetySettings
         )

        // TODO: Em produção, substitua isso por uma chamada a um backend seguro.
    }

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
} 