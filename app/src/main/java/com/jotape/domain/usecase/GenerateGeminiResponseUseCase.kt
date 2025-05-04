package com.jotape.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content
import com.jotape.domain.model.Interaction
import com.jotape.domain.model.Sender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * UseCase responsável por gerar a resposta do Gemini com base em uma mensagem do usuário.
 * A responsabilidade de buscar histórico e salvar a resposta deve ser do repositório.
 */
class GenerateGeminiResponseUseCase @Inject constructor(
    private val generativeModel: GenerativeModel,
    private val promptManager: PromptManager
) {

    /**
     * Gera uma resposta do modelo de IA para a mensagem do usuário.
     * Não busca histórico nem salva a resposta aqui.
     *
     * @param userMessageText O texto da mensagem do usuário.
     * @param history A lista de interações anteriores para dar contexto ao modelo.
     * @param userId O ID do usuário (pode ser necessário para o ID da resposta).
     * @return Um Result contendo a Interaction da resposta do assistente ou um erro.
     */
    suspend operator fun invoke(userMessageText: String, history: List<Interaction>, userId: String): Result<Interaction> {
        return try {
            // Iniciar chat com o histórico fornecido
            val chat = generativeModel.startChat(
                history = history.map { interaction ->
                    content(role = if (interaction.isFromUser) "user" else "model") {
                        text(interaction.text)
                    }
                }
            )

            // Enviar a nova mensagem do usuário (apenas o texto) e obter a resposta
            val systemPrompt = promptManager.getSystemPrompt()
            val response: GenerateContentResponse = chat.sendMessage(systemPrompt + "\n\n" + userMessageText)

            // Criar a interação da resposta do assistente usando o construtor correto
            val assistantMessage = Interaction(
                id = UUID.randomUUID().toString(),
                isFromUser = false,
                text = response.text ?: "Desculpe, não consegui processar sua mensagem.",
                timestamp = Instant.now()
            )

            Result.success(assistantMessage)

        } catch (e: Exception) {
            println("Error generating Gemini response in UseCase: ${e.message}")
            Result.failure(e)
        }
    }
} 