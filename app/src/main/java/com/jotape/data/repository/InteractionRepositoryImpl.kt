package com.jotape.data.repository

import com.jotape.data.remote.dto.InteractionDto
import com.jotape.data.remote.mapper.toDomainModelList
import com.jotape.domain.model.DomainResult
import com.jotape.domain.model.Interaction
import com.jotape.domain.repository.AuthRepository
import com.jotape.domain.repository.InteractionRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import android.util.Log
import com.jotape.data.local.dao.InteractionDao
import com.jotape.data.local.mapper.toDomainModelList as localToDomainModelList
import com.jotape.data.local.mapper.toEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.BlockReason
import com.google.ai.client.generativeai.type.FinishReason
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.SerializationException
import com.google.ai.client.generativeai.type.ServerException
import com.google.ai.client.generativeai.type.UnsupportedUserLocationException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.net.UnknownHostException
import androidx.work.*
import com.jotape.data.worker.SyncInteractionWorker
import java.time.Instant
import com.jotape.data.local.model.InteractionEntity

/**
 * Implementation of the InteractionRepository that uses Supabase Postgrest for fetching, adding, and clearing interactions.
 */
@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val supabasePostgrest: Postgrest,
    private val authRepository: AuthRepository,
    private val interactionDao: InteractionDao,
    private val geminiModel: GenerativeModel,
    private val workManager: WorkManager
) : InteractionRepository {

    private val interactionsTable = "interactions"
    private val TAG = "InteractionRepository"
    private val MAX_HISTORY_FOR_PROMPT = 10

    private fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    /**
     * Gets the list of all interactions for the current user FROM THE LOCAL DATABASE.
     * Note: Synchronization with the remote server happens via SyncInteractionWorker.
     */
    override suspend fun getAllInteractions(): DomainResult<List<Interaction>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting all interactions from local Room DB")
        try {
            val entities = interactionDao.getAllInteractions()
            val interactions = entities.localToDomainModelList()
            Log.d(TAG, "Loaded ${interactions.size} interactions from Room.")
            DomainResult.Success(interactions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting interactions from Room: ${e.message}", e)
            DomainResult.Error("Falha ao carregar interações do banco de dados local.")
        }
    }

    override suspend fun addInteraction(text: String, isFromUser: Boolean): DomainResult<Unit> = withContext(Dispatchers.IO) {
        if (!isFromUser) {
            Log.w(TAG, "addInteraction called with isFromUser=false. Should be triggered internally after Gemini response.")
            return@withContext DomainResult.Success(Unit)
        }

        Log.d(TAG, "Adding user interaction: text=$text")
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Log.w(TAG, "Cannot add interaction: User not logged in.")
            return@withContext DomainResult.Error("Usuário não está logado")
        }

        val userInteractionEntity = InteractionEntity(
            id = 0,
            isFromUser = true,
            text = text,
            timestamp = Instant.now(),
            isSynced = false
        )
        val localUserId: Long
        try {
            localUserId = interactionDao.upsertInteraction(userInteractionEntity)
            Log.d(TAG, "User interaction saved locally with ID: $localUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user interaction locally: ${e.message}", e)
            return@withContext DomainResult.Error("Falha ao salvar interação do usuário localmente.")
        }

        val userEntityWithId = userInteractionEntity.copy(id = localUserId)
        trySyncInteractionImmediately(userEntityWithId, currentUserId)

        val history = try {
            interactionDao.getRecentInteractions(MAX_HISTORY_FOR_PROMPT)
                .localToDomainModelList()
                .reversed()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching history from Room: ${e.message}", e)
            listOf<Interaction>()
        }
        val chatHistoryForPrompt = history.map { interaction ->
            content(if (interaction.isFromUser) "user" else "model") { text(interaction.text) }
        }
        val currentMessageContent = content("user") { text(text) }
        Log.d(TAG, "Calling Gemini with history size: ${chatHistoryForPrompt.size}")

        val geminiResponseText = try {
            val response = geminiModel.generateContent(*chatHistoryForPrompt.toTypedArray(), currentMessageContent)
            
            val blockReason = response.promptFeedback?.blockReason
            if (blockReason != null) {
                Log.w(TAG, "Gemini response blocked. Reason: $blockReason")
                throw PromptBlockedExceptionCustom(blockReason)
            }

            if (response.candidates.firstOrNull()?.finishReason != FinishReason.STOP) {
                 Log.w(TAG, "Gemini generation finished unexpectedly. Reason: ${response.candidates.firstOrNull()?.finishReason}")
                 throw ServerException("Geração de resposta incompleta: ${response.candidates.firstOrNull()?.finishReason}")
            }

            response.text ?: ""

        } catch (e: InvalidAPIKeyException) {
            Log.e(TAG, "Invalid Gemini API Key: ${e.message}", e)
            enqueueSyncWork()
            return@withContext DomainResult.Error("Erro de configuração da IA (Chave Inválida).")
        } catch (e: PromptBlockedExceptionCustom) {
            Log.w(TAG, "Gemini prompt blocked: ${e.message}", e)
             val blockReasonMsg = when(e.blockReason) {
                 BlockReason.SAFETY -> "por motivos de segurança."
                 BlockReason.OTHER -> "por um motivo não especificado."
                 else -> "por um motivo desconhecido."
             }
             enqueueSyncWork()
             return@withContext DomainResult.Error("Sua solicitação foi bloqueada $blockReasonMsg")
        } catch (e: SerializationException) {
             Log.e(TAG, "Gemini serialization error: ${e.message}", e)
             enqueueSyncWork()
             return@withContext DomainResult.Error("Erro ao processar dados da IA.")
        } catch (e: ServerException) {
             Log.e(TAG, "Gemini server error: ${e.message}", e)
             enqueueSyncWork()
             return@withContext DomainResult.Error("Servidor da IA indisponível ou com erro.")
        } catch (e: UnsupportedUserLocationException) {
             Log.e(TAG, "Gemini location error: ${e.message}", e)
             enqueueSyncWork()
             return@withContext DomainResult.Error("Serviço de IA não disponível na sua região.")
        } catch (e: HttpRequestTimeoutException) {
             Log.e(TAG, "Gemini call timeout: ${e.message}", e)
             enqueueSyncWork()
             return@withContext DomainResult.Error("Tempo limite excedido ao contatar a IA.")
        } catch (e: Exception) {
             if (e.cause is UnknownHostException) {
                 Log.e(TAG, "Gemini call network error (UnknownHost): ${e.message}", e)
                 enqueueSyncWork()
                 return@withContext DomainResult.Error("Não foi possível conectar ao servidor da IA.")
             }
             Log.e(TAG, "Unknown error calling Gemini API: ${e.message}", e)
             enqueueSyncWork()
             return@withContext DomainResult.Error("Erro inesperado ao gerar resposta da IA: ${e.message}")
        }

        if (geminiResponseText.isBlank()) {
             Log.w(TAG, "Gemini returned a blank response.")
             enqueueSyncWork()
             return@withContext DomainResult.Error("IA não retornou uma resposta válida.")
        }
        Log.d(TAG, "Received Gemini response: ${geminiResponseText}")

        val assistantInteractionEntity = InteractionEntity(
            id = 0,
            isFromUser = false,
            text = geminiResponseText,
            timestamp = Instant.now(),
            isSynced = false
        )
        val localAssistantId: Long
        try {
            localAssistantId = interactionDao.upsertInteraction(assistantInteractionEntity)
            Log.d(TAG, "Assistant interaction saved locally with ID: $localAssistantId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving assistant interaction locally: ${e.message}", e)
            enqueueSyncWork()
            return@withContext DomainResult.Error("Falha ao salvar resposta da IA localmente.")
        }

        val assistantEntityWithId = assistantInteractionEntity.copy(id = localAssistantId)
        trySyncInteractionImmediately(assistantEntityWithId, currentUserId)

        enqueueSyncWork()

        DomainResult.Success(Unit)
    }

    /**
     * Tenta sincronizar uma única interação com o Supabase imediatamente.
     * Se bem-sucedido, atualiza o status isSynced no banco de dados local.
     * Falhas são apenas registradas, pois o SyncInteractionWorker cuidará da retentativa.
     */
    private suspend fun trySyncInteractionImmediately(entity: InteractionEntity, userId: String) {
         Log.d(TAG, "Attempting immediate sync for interaction (local ID: ${entity.id})")
         val interactionDataForSupabase = mapOf(
             "user_id" to userId,
             "user_input" to (if (entity.isFromUser) entity.text else null),
             "assistant_response" to (if (!entity.isFromUser) entity.text else null),
             "timestamp" to entity.timestamp.toString()
         )
         try {
             supabasePostgrest.from(interactionsTable).insert(interactionDataForSupabase)
             interactionDao.markAsSynced(entity.id)
             Log.i(TAG, "Immediate sync successful for interaction (local ID: ${entity.id})")
         } catch (e: CancellationException) {
             Log.w(TAG, "Immediate sync cancelled for interaction (local ID: ${entity.id})", e)
         } catch (e: Exception) {
             Log.w(TAG, "Immediate sync failed for interaction (local ID: ${entity.id}). Error: ${e.message}. Worker will retry.")
         }
    }

    // override suspend fun clearHistory(): DomainResult<Unit> {
    //     // TODO: Re-implement history clearing logic if needed.
    //     Log.w(TAG, "clearHistory function called but is currently disabled.")
    //     return DomainResult.Error("Funcionalidade de limpar histórico desativada temporariamente.")
    // }

    private fun enqueueSyncWork() {
        Log.d(TAG, "Enqueuing background sync work.")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncInteractionWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "syncInteractions",
            ExistingWorkPolicy.KEEP,
            syncWorkRequest
        )
    }

    init {
        // enqueueSyncWork()
    }
}

class PromptBlockedExceptionCustom(val blockReason: BlockReason?) : Exception("Prompt was blocked. Reason: $blockReason") 