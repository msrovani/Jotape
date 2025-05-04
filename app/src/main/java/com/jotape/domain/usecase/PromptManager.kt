package com.jotape.domain.usecase

import android.content.Context
import com.jotape.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerencia o acesso ao prompt do sistema para o modelo de IA.
 * Utiliza injeção de construtor Hilt.
 */
@Singleton // Marcar como Singleton se deve haver apenas uma instância
class PromptManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Obtém o prompt do sistema a partir dos recursos de string.
     */
    fun getSystemPrompt(): String {
        return context.getString(R.string.system_prompt)
    }
} 