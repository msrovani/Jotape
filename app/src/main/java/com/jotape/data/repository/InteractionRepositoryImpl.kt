package com.jotape.data.repository

import android.util.Log
import com.jotape.data.remote.api.JotapeApiService
import com.jotape.data.remote.dto.ProcessCommandRequest
import com.jotape.domain.model.DomainResult
import com.jotape.domain.model.Interaction
import com.jotape.domain.repository.AuthRepository
import com.jotape.domain.repository.InteractionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import kotlinx.coroutines.channels.awaitClose

/**
 * Implementation of the InteractionRepository that uses Supabase Edge Function for sending/receiving
 * individual messages and Supabase Realtime/Postgrest for fetching history and clearing.
 */
@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val jotapeApiService: JotapeApiService,
    private val json: Json
) : InteractionRepository {

    private val interactionsTable = "interactions"

    @Serializable
    private data class InteractionSupabaseDto(
        val id: Long,
        val timestamp: String,
        val user_id: String,
        val user_input: String?,
        val assistant_response: String?
    ) {
        fun toDomainModel(): Interaction {
            val isFromUser = user_input != null
            val text = if (isFromUser) user_input!! else assistant_response ?: "[Resposta vazia]"

            val parsedTimestamp = try {
                Instant.parse(timestamp)
            } catch (e: Exception) {
                Log.w("InteractionRepositoryImpl", "Failed to parse timestamp '$timestamp', using now.", e)
                Instant.now()
            }

            return Interaction(
                id = id.toString(),
                isFromUser = isFromUser,
                text = text,
                timestamp = parsedTimestamp
            )
        }
    }

    private fun getCurrentUserId(): String? = authRepository.getCurrentUserId()

    /**
     * Gets the list of all interactions for the current user using Supabase Realtime.
     * Emits the initial list and then updates whenever new interactions are inserted.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllInteractions(): Flow<DomainResult<List<Interaction>>> = channelFlow {
        val TAG = "InteractionRepositoryImpl.getAllRealtime"
        Log.d(TAG, "Setting up Realtime channel for interactions")
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Log.w(TAG, "Cannot get interactions: User not logged in.")
            send(DomainResult.Error("Usuário não está logado"))
            close()
            return@channelFlow
        }

        try {
            val channel = supabaseClient.realtime.channel("interactions_user_${currentUserId}")

            // Flow que escuta por mudanças (INSERTs) e busca a lista completa
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = interactionsTable
                filter = "user_id=eq.$currentUserId" // Filtra mudanças apenas para o usuário atual
            }.mapLatest { _ -> // Quando uma mudança ocorrer, busca tudo de novo
                Log.d(TAG, "Realtime change detected, fetching full list...")
                fetchFullInteractionList(currentUserId)
            }.onStart {
                 // Busca a lista inicial ao iniciar a coleta do flow
                 Log.d(TAG, "Realtime flow started, fetching initial list...")
                 emit(fetchFullInteractionList(currentUserId))
            }

            // Coleta o flow de mudanças e envia para o canal do channelFlow
            changeFlow.collect { result ->
                send(result)
            }

            // Inscrever no canal DEPOIS de configurar o flow
            channel.subscribe()

            // Manter o channelFlow aberto até ser cancelado
            awaitClose { channel.unsubscribe() }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Realtime channel: ${e.message}", e)
            send(DomainResult.Error("Erro ao conectar para atualizações em tempo real."))
            close()
        }

    }.flowOn(Dispatchers.IO)

    // Função auxiliar para buscar a lista completa
    private suspend fun fetchFullInteractionList(userId: String): DomainResult<List<Interaction>> {
         val TAG = "InteractionRepositoryImpl.fetchFullList"
         return try {
             Log.d(TAG, "Executing fetch for user: $userId")
             val result = supabaseClient.postgrest
                 .from(interactionsTable)
                 .select {
                     filter { eq("user_id", userId) }
                     order("timestamp", order = Order.ASCENDING)
                 }
             val dtoList = json.decodeFromString<List<InteractionSupabaseDto>>(result.data)
             val interactions = dtoList.map { it.toDomainModel() }
             Log.d(TAG, "Fetched ${interactions.size} interactions.")
             DomainResult.Success(interactions)
         } catch (e: Exception) {
             Log.e(TAG, "Error fetching full interaction list: ${e.message}", e)
             DomainResult.Error("Falha ao buscar lista de interações: ${e.message}")
         }
     }

    override suspend fun sendMessage(userMessage: String): DomainResult<Unit> = withContext(Dispatchers.IO) {
        Log.d("InteractionRepositoryImpl", "Sending user message via Edge Function: $userMessage")
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Log.w("InteractionRepositoryImpl", "Cannot send message: User not logged in.")
            return@withContext DomainResult.Error("Usuário não está logado")
        }

        val request = ProcessCommandRequest(prompt = userMessage, userId = currentUserId)

        try {
            val response = jotapeApiService.processUserCommand(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d("InteractionRepositoryImpl", "Message processed successfully by Edge Function. Bot response: ${response.body()?.response?.take(50)}...")
                DomainResult.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("InteractionRepositoryImpl", "Error sending message via Edge Function: ${response.code()} - $errorBody")
                DomainResult.Error("Falha ao enviar mensagem: ${response.message()} ($errorBody)")
            }
        } catch (e: Exception) {
            Log.e("InteractionRepositoryImpl", "Exception sending message via Edge Function: ${e.message}", e)
            DomainResult.Error("Erro de rede ao enviar mensagem: ${e.message}")
        }
    }

    override suspend fun clearHistory(): DomainResult<Unit> = withContext(Dispatchers.IO) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            Log.w("InteractionRepositoryImpl", "Cannot clear history: User not logged in.")
            return@withContext DomainResult.Error("Usuário não está logado")
        }
        Log.d("InteractionRepositoryImpl", "Clearing remote history for user $currentUserId")

        try {
            supabaseClient.postgrest.from(interactionsTable).delete {
                filter {
                    eq("user_id", currentUserId)
                }
            }
            Log.d("InteractionRepositoryImpl", "Remote history cleared via Postgrest.")
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("InteractionRepositoryImpl", "Error clearing remote history: ${e.message}", e)
            DomainResult.Error("Falha ao limpar o histórico no servidor: ${e.message}")
        }
    }
} 