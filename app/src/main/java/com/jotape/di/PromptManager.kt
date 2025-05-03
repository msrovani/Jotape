package com.jotape.di

import android.content.Context
import com.jotape.R // Import R class to access resources
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerencia e fornece os prompts usados na comunicação com a IA.
 * Centraliza os textos para facilitar a manutenção e futura internacionalização.
 */
@Singleton
class PromptManager @Inject constructor(
    @ApplicationContext private val context: Context // Inject ApplicationContext
) {

    /**
     * Retorna o prompt inicial do sistema para configurar o comportamento do Gemini.
     */
    fun getSystemPrompt(): String {
        // Recupera a string do arquivo de recursos strings.xml
        return context.getString(R.string.system_prompt)
    }

    // TODO: Adicionar outros métodos para obter diferentes prompts conforme necessário
    // fun getErrorPrompt(errorCode: String): String { ... }
    // fun getFollowUpPrompt(): String { ... }

} 